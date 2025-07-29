import Foundation

// This is another option to create Use Case using more generic way
final class FetchRecentWidgetQueriesUseCase: UseCase {

    struct RequestValue {
        let maxCount: Int
    }
    typealias ResultValue = (Result<[WidgetQuery], Error>)

    private let requestValue: RequestValue
    private let completion: (ResultValue) -> Void
    private let widgetsQueriesRepository: WidgetsQueriesRepository

    init(
        requestValue: RequestValue,
        completion: @escaping (ResultValue) -> Void,
                    widgetsQueriesRepository: WidgetsQueriesRepository
    ) {

        self.requestValue = requestValue
        self.completion = completion
        self.widgetsQueriesRepository =             widgetsQueriesRepository
    }
    
    func start() -> Cancellable? {

        widgetsQueriesRepository.fetchRecentsQueries(
            maxCount: requestValue.maxCount,
            completion: completion
        )
        return nil
    }
}
