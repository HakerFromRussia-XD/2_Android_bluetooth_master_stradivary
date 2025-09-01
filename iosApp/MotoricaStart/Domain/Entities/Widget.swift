import Foundation

struct Widget: Equatable, Identifiable {
    typealias Identifier = String
    enum WidgetType {
        case adventure
        case scienceFiction
    }
    let id: Identifier
    let title: String?
    let title_2: String?
    let widgetType: WidgetType?
    let posterPath: String?
    let overview: String?
    let isAd: Bool
    let deviceAddress: Int
    let parameterID: Int
}

struct WidgetsPage: Equatable {
    let page: Int
    let totalPages: Int
    let widgets: [Widget]
}
