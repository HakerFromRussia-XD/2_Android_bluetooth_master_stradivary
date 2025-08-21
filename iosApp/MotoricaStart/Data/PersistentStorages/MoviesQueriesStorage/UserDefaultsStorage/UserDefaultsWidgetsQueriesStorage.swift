import Foundation

final class UserDefaultsWidgetsQueriesStorage {
    private let maxStorageLimit: Int
    private let recentsWidgetsQueriesKey = "recentsWidgetsQueries"
    private var userDefaults: UserDefaults
    private let backgroundQueue: DispatchQueueType
    
    init(
        maxStorageLimit: Int,
        userDefaults: UserDefaults = UserDefaults.standard,
        backgroundQueue: DispatchQueueType = DispatchQueue.global(qos: .userInitiated)
    ) {
        self.maxStorageLimit = maxStorageLimit
        self.userDefaults = userDefaults
        self.backgroundQueue = backgroundQueue
    }

    private func fetchWidgetsQueries() -> [WidgetQuery] {
        if let queriesData = userDefaults.object(forKey: recentsWidgetsQueriesKey) as? Data {
            if let widgetQueryList = try? JSONDecoder().decode(WidgetQueriesListUDS.self, from: queriesData) {
                return widgetQueryList.list.map { $0.toDomain() }
            }
        }
        return []
    }

    private func persist(widgetsQueries: [WidgetQuery]) {
        let encoder = JSONEncoder()
        let widgetQueryUDSs = widgetsQueries.map(WidgetQueryUDS.init)
        if let encoded = try? encoder.encode(WidgetQueriesListUDS(list: widgetQueryUDSs)) {
            userDefaults.set(encoded, forKey: recentsWidgetsQueriesKey)
        }
    }
}

extension UserDefaultsWidgetsQueriesStorage: WidgetsQueriesStorage {

    func fetchRecentsQueries(
        maxCount: Int,
        completion: @escaping (Result<[WidgetQuery], Error>) -> Void
    ) {
        backgroundQueue.async { [weak self] in
            guard let self = self else { return }

            var queries = self.fetchWidgetsQueries()
            queries = queries.count < self.maxStorageLimit ? queries : Array(queries[0..<maxCount])
            completion(.success(queries))
        }
    }

    func saveRecentQuery(
        query: WidgetQuery,
        completion: @escaping (Result<WidgetQuery, Error>) -> Void
    ) {
        backgroundQueue.async { [weak self] in
            guard let self = self else { return }

            var queries = self.fetchWidgetsQueries()
            self.cleanUpQueries(for: query, in: &queries)
            queries.insert(query, at: 0)
            self.persist(widgetsQueries: queries)

            completion(.success(query))
        }
    }
}


// MARK: - Private
extension UserDefaultsWidgetsQueriesStorage {

    private func cleanUpQueries(for query: WidgetQuery, in queries: inout [WidgetQuery]) {
        removeDuplicates(for: query, in: &queries)
        removeQueries(limit: maxStorageLimit - 1, in: &queries)
    }

    private func removeDuplicates(for query: WidgetQuery, in queries: inout [WidgetQuery]) {
        queries = queries.filter { $0 != query }
    }

    private func removeQueries(limit: Int, in queries: inout [WidgetQuery]) {
        queries = queries.count <= limit ? queries : Array(queries[0..<limit])
    }
}
