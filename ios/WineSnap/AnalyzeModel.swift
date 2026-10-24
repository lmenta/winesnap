import SwiftUI
import Observation

enum AnalyzeStep: Int, CaseIterable {
    case identifying, researching, recommending

    var label: String {
        switch self {
        case .identifying: "Reading wine labels…"
        case .researching: "Looking up prices and ratings…"
        case .recommending: "Picking the best bottle…"
        }
    }
}

struct AnalyzeResult {
    var winner: Wine
    var allWines: [Wine]
    var winesIdentified: Int
    var winesWithData: Int
}

@MainActor
@Observable
final class AnalyzeModel {
    var image: UIImage?
    var preference = WineSnapAPI.defaultPreference
    var step: AnalyzeStep?
    var result: AnalyzeResult?
    var error: String?

    var isLoading: Bool { step != nil }

    private var jpeg: Data?
    private var task: Task<Void, Never>?
    private var runID = 0
    private let api = WineSnapAPI()

    func setImage(_ picked: UIImage) {
        guard let prepared = picked.preparedForUpload() else {
            error = "Couldn't open that photo."
            return
        }
        image = prepared.image
        jpeg = prepared.jpeg
        result = nil
        error = nil
    }

    func analyze() {
        guard let jpeg, !isLoading else { return }
        result = nil
        error = nil
        step = .identifying
        runID += 1
        let run = runID

        task = Task {
            // The backend doesn't stream progress; advance the label on the same
            // schedule as the web app so the wait feels shorter.
            let progress = Task {
                try await Task.sleep(for: .seconds(4))
                try Task.checkCancellation()
                step = .researching
                try await Task.sleep(for: .seconds(6))
                try Task.checkCancellation()
                step = .recommending
            }
            defer {
                progress.cancel()
                // A reset + new run may have started while this one was cancelling.
                if run == runID { step = nil }
            }
            do {
                let (winner, response) = try await api.analyze(jpeg: jpeg, preference: preference)
                guard run == runID else { return }
                result = AnalyzeResult(
                    winner: winner,
                    allWines: response.allWines,
                    winesIdentified: response.winesIdentified,
                    winesWithData: response.winesWithData
                )
            } catch is CancellationError {
                // Reset by the user; nothing to show.
            } catch {
                if run == runID { self.error = error.localizedDescription }
            }
        }
    }

    func reset() {
        task?.cancel()
        task = nil
        runID += 1
        image = nil
        jpeg = nil
        result = nil
        error = nil
        step = nil
    }
}
