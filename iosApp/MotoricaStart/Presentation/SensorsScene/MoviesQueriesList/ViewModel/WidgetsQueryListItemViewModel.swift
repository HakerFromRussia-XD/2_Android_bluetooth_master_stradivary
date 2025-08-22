import Foundation

class WidgetsQueryListItemViewModel {
    let query: String

    init(query: String) {
        self.query = query
    }
}

extension WidgetsQueryListItemViewModel: Equatable {
    static func == (lhs: WidgetsQueryListItemViewModel, rhs: WidgetsQueryListItemViewModel) -> Bool {
        return lhs.query == rhs.query
    }
}
