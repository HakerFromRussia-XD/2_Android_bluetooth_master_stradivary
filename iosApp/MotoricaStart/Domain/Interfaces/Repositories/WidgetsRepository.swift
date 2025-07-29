import Foundation

protocol WidgetsRepository {
    @discardableResult
    func fetchWidgetsList(
        query: WidgetQuery,
        page: Int,
        cached: @escaping (WidgetsPage) -> Void,
        completion: @escaping (Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable?
}
