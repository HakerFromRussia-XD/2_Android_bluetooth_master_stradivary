import Foundation
import CoreData

extension MoviesResponseEntity {
    func toDTO() -> WidgetsResponseDTO {
        return .init(
            page: Int(page),
            totalPages: Int(totalPages),

            widgets: (movies?.allObjects as? [MovieResponseEntity] ?? [])
                .sorted { $0.id < $1.id }
                .map { $0.toDTO() }
        )
    }
}

extension MovieResponseEntity {
    func toDTO() -> WidgetsResponseDTO.WidgetDTO {
        return .init(
            id: Int(id),
            title: title,
            genre: WidgetsResponseDTO.WidgetDTO.GenreDTO(rawValue: genre ?? ""),
            posterPath: posterPath,
            overview: overview,
            releaseDate: releaseDate,
            isAd: isAd
        )
    }
}

extension MoviesRequestDTO {
    func toEntity(in context: NSManagedObjectContext) -> MoviesRequestEntity {
        let entity: MoviesRequestEntity = .init(context: context)
        entity.query = query
        entity.page = Int32(page)
        return entity
    }
}

extension WidgetsResponseDTO {
    func toEntity(in context: NSManagedObjectContext) -> MoviesResponseEntity {
        let entity: MoviesResponseEntity = .init(context: context)
        entity.page = Int32(page)
        entity.totalPages = Int32(totalPages)
        widgets.forEach {
            entity.addToMovies($0.toEntity(in: context))
        }
        return entity
    }
}

extension WidgetsResponseDTO.WidgetDTO {
    func toEntity(in context: NSManagedObjectContext) -> MovieResponseEntity {
        let entity: MovieResponseEntity = .init(context: context)
        entity.id = Int64(id)
        entity.title = title
        entity.genre = genre?.rawValue
        entity.posterPath = posterPath
        entity.overview = overview
        entity.releaseDate = releaseDate
        entity.isAd = isAd ?? false
        return entity
    }
}
