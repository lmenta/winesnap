import Foundation

/// One bottle as returned by `POST /api/analyze`. Every field is optional on the wire.
struct Wine: Decodable, Hashable {
    var name: String?
    var winery: String?
    var vintage: String?
    var region: String?
    var price: Double?
    var score: Double?
    var valueScore: Double?
    var reason: String?
    var tastingNote: String?
    var valueTip: String?

    enum CodingKeys: String, CodingKey {
        case name, winery, vintage, region, price, score, reason
        case valueScore = "value_score"
        case tastingNote = "tasting_note"
        case valueTip = "value_tip"
    }

    // The backend's output is partly LLM-generated, so types drift (vintage as 2018 or "NV",
    // score as 94 or "94"). Decode leniently instead of failing the whole response.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.lenientString(.name)
        winery = c.lenientString(.winery)
        vintage = c.lenientString(.vintage)
        region = c.lenientString(.region)
        price = c.lenientDouble(.price)
        score = c.lenientDouble(.score)
        valueScore = c.lenientDouble(.valueScore)
        reason = c.lenientString(.reason)
        tastingNote = c.lenientString(.tastingNote)
        valueTip = c.lenientString(.valueTip)
    }

    var displayName: String {
        let parts = [winery, name].compactMap { $0 }
        return parts.isEmpty ? "Unknown wine" : parts.joined(separator: " ")
    }

    var formattedPrice: String? {
        guard let price, price > 0 else { return nil }
        return String(format: "£%.2f", price)
    }

    var formattedScore: String? {
        guard let score, score > 0 else { return nil }
        return score.rounded() == score ? String(Int(score)) : String(format: "%.1f", score)
    }

    var formattedValue: String? {
        guard let valueScore, valueScore > 0 else { return nil }
        return String(format: "%.1f", valueScore)
    }
}

struct AnalyzeResponse: Decodable {
    var winner: Wine?
    var allWines: [Wine]
    var winesIdentified: Int
    var winesWithData: Int
    var error: String?

    enum CodingKeys: String, CodingKey {
        case winner, error
        case allWines = "all_wines"
        case winesIdentified = "wines_identified"
        case winesWithData = "wines_with_data"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        winner = try? c.decodeIfPresent(Wine.self, forKey: .winner)
        allWines = (try? c.decodeIfPresent([Wine].self, forKey: .allWines)) ?? []
        winesIdentified = c.lenientDouble(.winesIdentified).map(Int.init) ?? allWines.count
        winesWithData = c.lenientDouble(.winesWithData).map(Int.init) ?? allWines.count
        error = c.lenientString(.error)
    }
}

/// FastAPI error body: `{"detail": "..."}` or `{"detail": [{"msg": "..."}]}`.
struct APIErrorBody: Decodable {
    struct Item: Decodable { var msg: String? }
    var message: String?

    enum CodingKeys: String, CodingKey { case detail }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        if let s = try? c.decode(String.self, forKey: .detail) {
            message = s
        } else if let items = try? c.decode([Item].self, forKey: .detail) {
            let joined = items.compactMap(\.msg).joined(separator: "; ")
            message = joined.isEmpty ? nil : joined
        }
    }
}

private extension KeyedDecodingContainer {
    func lenientString(_ key: Key) -> String? {
        if let s = try? decodeIfPresent(String.self, forKey: key) {
            let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty || trimmed == "null" ? nil : trimmed
        }
        if let i = try? decodeIfPresent(Int.self, forKey: key) { return String(i) }
        if let d = try? decodeIfPresent(Double.self, forKey: key) {
            return d.rounded() == d ? String(Int(d)) : String(d)
        }
        return nil
    }

    func lenientDouble(_ key: Key) -> Double? {
        if let d = try? decodeIfPresent(Double.self, forKey: key) { return d }
        if let s = try? decodeIfPresent(String.self, forKey: key) { return Double(s) }
        return nil
    }
}
