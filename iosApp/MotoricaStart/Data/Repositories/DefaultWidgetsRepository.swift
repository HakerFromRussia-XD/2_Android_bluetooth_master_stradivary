// **Note**: DTOs structs are mapped into Domains here, and Repository protocols does not contain DTOs

import Foundation

final class DefaultWidgetsRepository {

    private let dataTransferService: DataTransferService
    private let cache: WidgetsResponseStorage
    private let backgroundQueue: DataTransferDispatchQueue

    init(
        dataTransferService: DataTransferService,
        cache: WidgetsResponseStorage,
        backgroundQueue: DataTransferDispatchQueue = DispatchQueue.global(qos: .userInitiated)
    ) {
        self.dataTransferService = dataTransferService
        self.cache = cache
        self.backgroundQueue = backgroundQueue
    }
}

extension DefaultWidgetsRepository: WidgetsRepository {
    
    func fetchWidgetsList(
        query: WidgetQuery,
        page: Int,
        cached: @escaping (WidgetsPage) -> Void,
        completion: @escaping (Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable? {

        let requestDTO = WidgetsRequestDTO(query: query.query, page: page)
        let task = RepositoryTask()

        cache.getResponse(for: requestDTO) { [weak self] result in
            
            guard self != nil else { return } // Защищаем от слабой ссылки на self

            if case let .success(responseDTO?) = result {
                cached(responseDTO.toDomain())
                completion(.success(responseDTO.toDomain()))
                return // если нет этого ретёрна то выполняется ещё и сетевой запрос
            }
            guard !task.isCancelled else {
                completion(.failure(URLError.cancelled as! any Error as Error))
                return
            }

//            let endpoint = APIEndpoints.getWidgets(with: requestDTO)
//            task.networkTask = self?.dataTransferService.request(
//                with: endpoint,
//                on: backgroundQueue
//            ) { result in
//                switch result {
//                case .success(let responseDTO):
//                    self?.cache.save(response: responseDTO, for: requestDTO)
//                    completion(.success(responseDTO.toDomain()))
//                case .failure(let error):
//                    completion(.failure(error))
//                }
//            }
        }
        return task
    }
}
