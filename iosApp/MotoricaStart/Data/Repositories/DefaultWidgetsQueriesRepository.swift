import Foundation

final class DefaultWidgetsQueriesRepository {
    
    private var widgetsQueriesPersistentStorage: WidgetsQueriesStorage
    
    init(            widgetsQueriesPersistentStorage: WidgetsQueriesStorage) {
        self.widgetsQueriesPersistentStorage =             widgetsQueriesPersistentStorage
    }
}

extension DefaultWidgetsQueriesRepository: WidgetsQueriesRepository {
    
    func fetchRecentsQueries(
        maxCount: Int,
        completion: @escaping (Result<[WidgetQuery], Error>) -> Void
    ) {
        return widgetsQueriesPersistentStorage.fetchRecentsQueries(
            maxCount: maxCount,
            completion: completion
        )
    }
    
    func saveRecentQuery(
        query: WidgetQuery,
        completion: @escaping (Result<WidgetQuery, Error>) -> Void
    ) {
        widgetsQueriesPersistentStorage.saveRecentQuery(
            query: query,
            completion: completion
        )
    }
}
