import SwiftUI
import PhotosUI

struct ContentView: View {
    @State private var model = AnalyzeModel()
    @State private var showCamera = false
    @State private var photoItem: PhotosPickerItem?
    @State private var showAll = false
    @FocusState private var preferenceFocused: Bool

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header.padding(.bottom, 24)

                if let image = model.image {
                    preview(image)
                } else {
                    picker
                }

                TextField("What matters? e.g. good with steak, under £15", text: $model.preference)
                    .font(.subheadline)
                    .focused($preferenceFocused)
                    .submitLabel(.done)
                    .disabled(model.isLoading)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(.white, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12)
                        .stroke(preferenceFocused ? Color.wine.opacity(0.6) : Color.stone200, lineWidth: 1))
                    .padding(.top, 16)

                analyzeButton.padding(.top, 12)

                if let step = model.step {
                    StepProgress(current: step).padding(.top, 12)
                }

                if let error = model.error {
                    errorBanner(error).padding(.top, 20)
                }

                if let result = model.result {
                    results(result).padding(.top, 24)
                }
            }
            .frame(maxWidth: 520)
            .padding(.horizontal, 16)
            .padding(.vertical, 24)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.stone50.ignoresSafeArea())
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker { model.setImage($0) }
                .ignoresSafeArea()
        }
        .onChange(of: photoItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    model.setImage(image)
                    showAll = false
                } else {
                    model.error = "Couldn't open that photo."
                }
                photoItem = nil
            }
        }
    }

    // MARK: - Sections

    private var header: some View {
        VStack(spacing: 2) {
            Image(systemName: "wineglass.fill")
                .font(.system(size: 22))
                .foregroundStyle(.white)
                .frame(width: 48, height: 48)
                .background(Color.wine, in: RoundedRectangle(cornerRadius: 16))
                .padding(.bottom, 10)
            Text("WineSnap").font(.title2.bold()).foregroundStyle(Color.stone900)
            Text("Photo a wine shelf. Get the best bottle.").font(.subheadline).foregroundStyle(Color.stone500)
        }
    }

    private var picker: some View {
        HStack(spacing: 0) {
            Button {
                if CameraPicker.isAvailable {
                    showCamera = true
                } else {
                    model.error = "Camera isn't available on this device. Choose a photo instead."
                }
            } label: {
                pickerOption(icon: "camera", title: "Take Photo", subtitle: "Open camera", tint: .wine)
            }

            Rectangle().fill(Color.stone200).frame(width: 1, height: 110)

            PhotosPicker(selection: $photoItem, matching: .images) {
                pickerOption(icon: "photo.on.rectangle", title: "Choose Photo", subtitle: "From gallery", tint: .stone500)
            }
        }
        .buttonStyle(.plain)
        .background(.white, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.stone200, style: StrokeStyle(lineWidth: 2, dash: [6, 4])))
    }

    private func pickerOption(icon: String, title: String, subtitle: String, tint: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon).font(.system(size: 28)).foregroundStyle(tint)
            Text(title).font(.subheadline.weight(.medium)).foregroundStyle(Color.stone700)
            Text(subtitle).font(.caption).foregroundStyle(Color.stone400)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .contentShape(Rectangle())
    }

    private func preview(_ image: UIImage) -> some View {
        Image(uiImage: image)
            .resizable()
            .scaledToFill()
            .frame(height: 256)
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .accessibilityLabel("Wine shelf")
            .overlay(alignment: .topTrailing) {
                Button("Change") {
                    model.reset()
                    showAll = false
                }
                .font(.caption)
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(.black.opacity(0.6), in: Capsule())
                .padding(8)
                .disabled(model.isLoading)
            }
    }

    private var analyzeButton: some View {
        Button {
            preferenceFocused = false
            showAll = false
            model.analyze()
        } label: {
            HStack(spacing: 8) {
                if let step = model.step {
                    ProgressView().tint(.white)
                    Text(step.label)
                } else {
                    Image(systemName: "wineglass")
                    Text("Pick the best wine")
                }
            }
            .font(.body.weight(.semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(Color.wine, in: RoundedRectangle(cornerRadius: 12))
            .opacity(model.image == nil || model.isLoading ? 0.4 : 1)
        }
        .disabled(model.image == nil || model.isLoading)
    }

    private func errorBanner(_ message: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.circle")
            Text(message)
            Spacer(minLength: 0)
        }
        .font(.subheadline)
        .foregroundStyle(Color(red: 0.73, green: 0.11, blue: 0.11))
        .padding(16)
        .background(Color.wineLight, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.996, green: 0.79, blue: 0.79), lineWidth: 1))
    }

    private func results(_ result: AnalyzeResult) -> some View {
        VStack(spacing: 16) {
            WinnerCard(wine: result.winner)

            if result.allWines.count > 1 {
                VStack(spacing: 8) {
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { showAll.toggle() }
                    } label: {
                        HStack {
                            Text(allWinesTitle(result))
                            Spacer()
                            Image(systemName: showAll ? "chevron.up" : "chevron.down")
                        }
                        .font(.subheadline)
                        .foregroundStyle(Color.stone500)
                        .padding(.vertical, 8)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    if showAll {
                        ForEach(Array(result.allWines.enumerated()), id: \.offset) { index, wine in
                            WineRow(wine: wine, highlight: index == 0)
                        }
                    }
                }
            }

            Button {
                model.reset()
                showAll = false
            } label: {
                Text("Try another photo")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.stone700)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(.white, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.stone200, lineWidth: 1))
            }
            .buttonStyle(.plain)
        }
    }

    private func allWinesTitle(_ r: AnalyzeResult) -> String {
        let extra = r.winesWithData < r.winesIdentified ? ", \(r.winesWithData) with price/score data" : ""
        return "All wines (\(r.winesIdentified) found\(extra))"
    }
}

private struct StepProgress: View {
    let current: AnalyzeStep

    var body: some View {
        HStack(spacing: 8) {
            ForEach(AnalyzeStep.allCases, id: \.self) { step in
                Capsule()
                    .fill(step.rawValue <= current.rawValue ? Color.wine : Color.stone200)
                    .frame(height: 4)
            }
        }
        .animation(.easeInOut, value: current)
    }
}

#Preview {
    ContentView()
}
