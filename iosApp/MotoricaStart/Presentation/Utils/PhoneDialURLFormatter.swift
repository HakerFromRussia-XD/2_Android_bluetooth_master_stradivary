import Foundation

enum PhoneDialURLFormatter {
    static func dialURL(from phone: String) -> URL? {
        let digits = phone.filter(\.isNumber)
        guard !digits.isEmpty else { return nil }

        let normalized: String
        if digits.count == 11, digits.hasPrefix("8") {
            normalized = "+7" + digits.dropFirst()
        } else if digits.count == 11, digits.hasPrefix("7") {
            normalized = "+" + digits
        } else if phone.trimmingCharacters(in: .whitespacesAndNewlines).hasPrefix("+") {
            if digits.count == 10, digits.hasPrefix("880") {
                return nil
            }
            normalized = "+" + digits
        } else if digits.hasPrefix("8") {
            return nil
        } else {
            normalized = digits
        }
        return URL(string: "tel:\(normalized)")
    }
}
