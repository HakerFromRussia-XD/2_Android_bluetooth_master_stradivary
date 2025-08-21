//
//  CoreDataMoviesResponseStorage+Async.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 15.05.2025.
//
import Foundation

// Assistant: расширяем хранилище, чтобы вернуть completion после успешного сохранения
extension CoreDataMoviesResponseStorage {

    /// Сохраняет DTO и вызывает `completion` на главном потоке,
    /// когда `context.save()` завершён.
    func save(
        response responseDto: MoviesResponseDTO,
        for requestDto: MoviesRequestDTO,
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
                debugPrint("CoreDataMoviesResponseStorage save error \(error)")
            }
            DispatchQueue.main.async { completion() }          // Assistant: уведомляем UI
        }
    }
}
