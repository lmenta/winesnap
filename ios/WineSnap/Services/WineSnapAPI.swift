import Foundation

struct APIError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Client for the WineSnap FastAPI backend (see src/api.py).
struct WineSnapAPI {
    static let defaultPreference = "best price/quality ratio"

    /// Read from Info.plist (`WineSnapAPIURL`, set via Config/Config.xcconfig).
    static var baseURL: URL {
        let raw = (Bundle.main.object(forInfoDictionaryKey: "WineSnapAPIURL") as? String)?
            .trimmingCharacters(in: CharacterSet(charactersIn: "/ "))
        return raw.flatMap { $0.isEmpty ? nil : URL(string: $0) } ?? URL(string: "http://localhost:8000")!
    }

    var baseURL: URL = WineSnapAPI.baseURL

    // The three-step pipeline (vision → search → LLM) can take 20s+.
    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 90
        config.timeoutIntervalForResource = 120
        return URLSession(configuration: config)
    }()

    func analyze(jpeg: Data, preference: String) async throws -> (winner: Wine, response: AnalyzeResponse) {
        let boundary = "WineSnap-\(UUID().uuidString)"
        var request = URLRequest(url: baseURL.appendingPathComponent("api/analyze"))
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        let pref = preference.trimmingCharacters(in: .whitespacesAndNewlines)
        var body = Data()
        body.appendFormField(name: "preference", value: pref.isEmpty ? Self.defaultPreference : pref, boundary: boundary)
        body.appendFileField(name: "image", filename: "shelf.jpg", mimeType: "image/jpeg", data: jpeg, boundary: boundary)
        body.append("--\(boundary)--\r\n")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await Self.session.upload(for: request, from: body)
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as URLError where error.code == .cancelled {
            throw CancellationError()
        } catch {
            throw APIError(message: "Couldn't reach WineSnap. Check your connection and try again.")
        }

        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(status) else {
            let detail = (try? JSONDecoder().decode(APIErrorBody.self, from: data))?.message
            throw APIError(message: detail ?? "Analysis failed (HTTP \(status))")
        }

        guard let decoded = try? JSONDecoder().decode(AnalyzeResponse.self, from: data) else {
            throw APIError(message: "Unexpected response from server.")
        }
        if let error = decoded.error { throw APIError(message: error) }
        guard let winner = decoded.winner else { throw APIError(message: "No recommendation returned.") }
        return (winner, decoded)
    }
}

private extension Data {
    mutating func append(_ string: String) {
        append(Data(string.utf8))
    }

    mutating func appendFormField(name: String, value: String, boundary: String) {
        append("--\(boundary)\r\n")
        append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n")
        append("\(value)\r\n")
    }

    mutating func appendFileField(name: String, filename: String, mimeType: String, data: Data, boundary: String) {
        append("--\(boundary)\r\n")
        append("Content-Disposition: form-data; name=\"\(name)\"; filename=\"\(filename)\"\r\n")
        append("Content-Type: \(mimeType)\r\n\r\n")
        append(data)
        append("\r\n")
    }
}
