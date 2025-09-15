import Foundation

protocol WidgetsRepository {
    @discardableResult
    func fetchWidgetsList(
        query: WidgetQuery,
        page: Int,
        requestID: Int,
        cached: @escaping (_ requestID: Int, _ page: WidgetsPage) -> Void,
        completion: @escaping (_ requestID: Int, _ result: Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable?
}
