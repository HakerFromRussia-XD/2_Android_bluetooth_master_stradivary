import Foundation

struct WidgetQueriesListUDS: Codable {
    var list: [WidgetQueryUDS]
}

struct WidgetQueryUDS: Codable {
    let query: String
}

extension WidgetQueryUDS {
    init(widgetQuery: WidgetQuery) {
        query = widgetQuery.query
    }
}

extension WidgetQueryUDS {
    func toDomain() -> WidgetQuery {
        return .init(query: query)
    }
}
