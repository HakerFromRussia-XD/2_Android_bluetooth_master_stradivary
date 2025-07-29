import Foundation

protocol WidgetsQueriesRepository {
    func fetchRecentsQueries(
        maxCount: Int,
        completion: @escaping (Result<[WidgetQuery], Error>) -> Void
    )
    func saveRecentQuery(
        query: WidgetQuery,
        completion: @escaping (Result<WidgetQuery, Error>) -> Void
    )
}
