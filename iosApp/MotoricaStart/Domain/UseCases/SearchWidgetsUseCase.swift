import Foundation

protocol SearchWidgetsUseCase {
    func execute(
        requestValue: SearchWidgetsUseCaseRequestValue,
        requestID: Int,
        cached: @escaping (_ requestID: Int, _ page: WidgetsPage) -> Void,
        completion: @escaping (_ requestID: Int, _ result: Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable?
}


final class DefaultSearchWidgetsUseCase: SearchWidgetsUseCase {
    private let widgetsRepository: WidgetsRepository
    private let widgetsQueriesRepository: WidgetsQueriesRepository

    init(
                    widgetsRepository: WidgetsRepository,
                    widgetsQueriesRepository: WidgetsQueriesRepository
    ) {

        self.widgetsRepository =             widgetsRepository
        self.widgetsQueriesRepository =             widgetsQueriesRepository
    }

    func execute(
        requestValue: SearchWidgetsUseCaseRequestValue,
        requestID: Int,
        cached: @escaping (_ requestID: Int, _ page: WidgetsPage) -> Void,
        completion: @escaping (_ requestID: Int, _ result: Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable? {

        return widgetsRepository.fetchWidgetsList(
            query: requestValue.query,
            page: requestValue.page,
            requestID: requestID,
            cached: cached,
            completion: { id, result in

            if case .success = result {
                self.widgetsQueriesRepository.saveRecentQuery(query: requestValue.query) { _ in }
            }

            completion(id, result)
        })
    }
}

struct SearchWidgetsUseCaseRequestValue {
    let query: WidgetQuery
    let page: Int
}
