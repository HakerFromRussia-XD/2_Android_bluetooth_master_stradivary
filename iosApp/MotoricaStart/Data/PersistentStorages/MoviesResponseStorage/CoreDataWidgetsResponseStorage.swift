import Foundation
import CoreData

final class CoreDataWidgetsResponseStorage {

    private let coreDataStorage: CoreDataStorage

    init(coreDataStorage: CoreDataStorage = CoreDataStorage.shared) {
        self.coreDataStorage = coreDataStorage
    }

    // MARK: - Private

    private func fetchRequest(
        for requestDto: WidgetsRequestDTO
    ) -> NSFetchRequest<WidgetsRequestEntity> {
        let request: NSFetchRequest = WidgetsRequestEntity.fetchRequest()
        request.predicate = NSPredicate(format: "%K = %@ AND %K = %d",
                                        #keyPath(WidgetsRequestEntity.query), requestDto.query,
                                        #keyPath(WidgetsRequestEntity.page), requestDto.page)
        return request
    }

    private func deleteResponse(
        for requestDto: WidgetsRequestDTO,
        in context: NSManagedObjectContext
    ) {
        let request = fetchRequest(for: requestDto)

        do {
            if let result = try context.fetch(request).first {
                context.delete(result)
            }
        } catch {
            print(error)
        }
    }
}

extension CoreDataWidgetsResponseStorage: WidgetsResponseStorage {

    func getResponse(
        for requestDto: WidgetsRequestDTO,
        completion: @escaping (Result<WidgetsResponseDTO?, Error>) -> Void
    ) {
        coreDataStorage.performBackgroundTask { context in
            do {
                let fetchRequest = self.fetchRequest(for: requestDto)
                let requestEntity = try context.fetch(fetchRequest).first

                completion(.success(requestEntity?.response?.toDTO()))
            } catch {
                completion(.failure(CoreDataStorageError.readError(error)))
            }
        }
    }

    func save(
        response responseDto: WidgetsResponseDTO,
        for requestDto: WidgetsRequestDTO
    ) {
        coreDataStorage.performBackgroundTask { context in
            do {
                self.deleteResponse(for: requestDto, in: context)

                let requestEntity = requestDto.toEntity(in: context)
                requestEntity.response = responseDto.toEntity(in: context)

                try context.save()
            } catch {
                // TODO: - Log to Crashlytics
                debugPrint("CoreDataWidgetsResponseStorage Unresolved error \(error), \((error as NSError).userInfo)")
            }
        }
    }
}

extension CoreDataWidgetsResponseStorage {
    /// Сохраняет DTO и вызывает `completion` на главном потоке,
    /// когда `context.save()` завершён.
    func save(
        response responseDto: WidgetsResponseDTO,
        for requestDto: WidgetsRequestDTO,
        completion: @escaping () -> Void
    ) {
        coreDataStorage.performBackgroundTask { context in
            do {
                self.deleteResponse(for: requestDto, in: context)

                let requestEntity = requestDto.toEntity(in: context)
                requestEntity.response = responseDto.toEntity(in: context)

                try context.save()
            } catch {
                // TODO: отправить в Crashlytics
                debugPrint("CoreDataWidgetsResponseStorage save error \(error)")
            }
            DispatchQueue.main.async { completion() }          // Assistant: уведомляем UI
        }
    }
}
