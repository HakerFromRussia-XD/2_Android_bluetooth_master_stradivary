import Foundation

protocol SearchWidgetsUseCase {
    func execute(
        requestValue: SearchWidgetsUseCaseRequestValue,
        cached: @escaping (WidgetsPage) -> Void,
        completion: @escaping (Result<WidgetsPage, Error>) -> Void
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
        cached: @escaping (WidgetsPage) -> Void,
        completion: @escaping (Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable? {

        return widgetsRepository.fetchWidgetsList(
            query: requestValue.query,
            page: requestValue.page,
            cached: cached,
            completion: { result in

            if case .success = result {
                self.widgetsQueriesRepository.saveRecentQuery(query: requestValue.query) { _ in }
            }

            completion(result)
        })
    }
}

struct SearchWidgetsUseCaseRequestValue {
    let query: WidgetQuery
    let page: Int
}
