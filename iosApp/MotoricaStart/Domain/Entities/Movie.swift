import Foundation

struct Widget: Equatable, Identifiable {
    typealias Identifier = String
    enum Genre {
        case adventure
        case scienceFiction
    }
    let id: Identifier
    let title: String?
    let title_2: String?
    let genre: Genre?
    let posterPath: String?
    let overview: String?
    let isAd: Bool
}

struct WidgetsPage: Equatable {
    let page: Int
    let totalPages: Int
    let widgets: [Widget]
}
