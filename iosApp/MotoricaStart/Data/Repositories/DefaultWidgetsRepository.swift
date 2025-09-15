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
        requestID: Int,
        cached: @escaping (_ requestID: Int, _ page: WidgetsPage) -> Void,
        completion: @escaping (_ requestID: Int, _ result: Result<WidgetsPage, Error>) -> Void
    ) -> Cancellable? {

        let requestDTO = WidgetsRequestDTO(query: query.query, page: page)
        let task = RepositoryTask()

        cache.getResponse(for: requestDTO) { [weak self] result in
            
            guard self != nil else { return } // Защищаем от слабой ссылки на self
            guard !task.isCancelled else { return }

            switch result {
            case let .success(responseDTO?):
                cached(requestID, responseDTO.toDomain())
                return
            case let .failure(error):
                completion(requestID, .failure(error))
                return
            case .success(nil):
                break
            }

//            let endpoint = APIEndpoints.getWidgets(with: requestDTO)
//            task.networkTask = self?.dataTransferService.request(
//                with: endpoint,
//                on: backgroundQueue
//            ) { result in
//                switch result {
//                case .success(let responseDTO):
//                    self?.cache.save(response: responseDTO, for: requestDTO)
//                    completion(requestID, .success(responseDTO.toDomain()))
//                case .failure(let error):
//                    completion(requestID, .failure(error))
//                }
//            }
        }
        return task
    }
}
