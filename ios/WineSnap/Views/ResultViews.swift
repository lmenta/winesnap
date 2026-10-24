import SwiftUI

struct WinnerCard: View {
    let wine: Wine

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Label("BEST PICK", systemImage: "star.fill")
                .font(.caption.weight(.semibold))
                .tracking(0.5)
                .foregroundStyle(Color.wine)
                .labelStyle(TightLabelStyle())

            (Text(wine.displayName).foregroundStyle(Color.stone900)
             + Text(wine.vintage.map { " \($0)" } ?? "").font(.body).fontWeight(.regular).foregroundStyle(Color.stone500))
                .font(.title3.bold())
                .padding(.top, 8)

            if let region = wine.region {
                Text(region).font(.caption).foregroundStyle(Color.stone400).padding(.top, 2)
            }

            let stats = [
                wine.formattedPrice.map { Stat(value: $0, label: "Price", color: .stone900) },
                wine.formattedScore.map { Stat(value: $0, label: "Score/100", color: .stone900) },
                wine.formattedValue.map { Stat(value: $0, label: "Value", color: .wine) },
            ].compactMap { $0 }

            if !stats.isEmpty {
                HStack(spacing: 20) {
                    ForEach(stats) { stat in
                        VStack(alignment: .leading, spacing: 0) {
                            Text(stat.value).font(.title3.bold()).foregroundStyle(stat.color)
                            Text(stat.label).font(.caption).foregroundStyle(Color.stone400)
                        }
                    }
                }
                .padding(.top, 12)
            }

            if let reason = wine.reason {
                Text(reason).font(.subheadline).foregroundStyle(Color.stone700).lineSpacing(3).padding(.top, 12)
            }
            if let note = wine.tastingNote {
                Text(note).font(.subheadline).italic().foregroundStyle(Color.stone500).padding(.top, 8)
            }
            if let tip = wine.valueTip {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                    Text(tip)
                }
                .font(.caption)
                .foregroundStyle(Color.emerald700)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.emerald50, in: RoundedRectangle(cornerRadius: 8))
                .padding(.top, 12)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(.white, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.wine, lineWidth: 2))
    }
}

struct WineRow: View {
    let wine: Wine
    let highlight: Bool

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text([wine.displayName, wine.vintage].compactMap { $0 }.joined(separator: " "))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.stone900)
                    .lineLimit(1)
                Text(wine.region ?? "—")
                    .font(.caption)
                    .foregroundStyle(Color.stone400)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            VStack(alignment: .trailing, spacing: 2) {
                if let price = wine.formattedPrice {
                    Text(price).fontWeight(.medium).foregroundStyle(Color.stone700)
                }
                if let score = wine.formattedScore {
                    Text("\(score)pts").foregroundStyle(Color.stone400)
                }
                if let value = wine.formattedValue {
                    Text(value).bold().foregroundStyle(highlight ? Color.wine : Color.stone200)
                }
            }
            .font(.caption)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.white, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.stone200, lineWidth: 1))
    }
}

private struct Stat: Identifiable {
    let value: String
    let label: String
    let color: Color
    var id: String { label }
}

private struct TightLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 6) {
            configuration.icon.imageScale(.small)
            configuration.title
        }
    }
}
