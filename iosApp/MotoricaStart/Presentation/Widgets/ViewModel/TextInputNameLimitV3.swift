import Foundation

enum TextInputNameLimitV3 {
    static let maxBytes = 13

    static func trimToLimit(_ value: String) -> String {
        var result = ""
        for character in value {
            let candidate = result + String(character)
            if candidate.utf8.count > maxBytes {
                break
            }
            result = candidate
        }
        return result
    }
}
