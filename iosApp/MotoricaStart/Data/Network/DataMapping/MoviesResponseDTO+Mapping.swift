import Foundation

// MARK: - Data Transfer Object

struct MoviesResponseDTO: Decodable {
    private enum CodingKeys: String, CodingKey {
        case page
        case totalPages = "total_pages"
        case movies = "results"
    }
    let page: Int
    let totalPages: Int
    let movies: [MovieDTO]
}

extension MoviesResponseDTO {
    struct MovieDTO: Decodable {
        private enum CodingKeys: String, CodingKey {
            case id
            case title
            case genre
            case posterPath = "poster_path"
            case overview
            case releaseDate = "release_date"
            case isAd
        }
        enum GenreDTO: String, Decodable {
            case adventure = "adventure"
            case scienceFiction = "science_fiction"
            case unknown
        }
        let id: Int
        let title: String?
        let genre: GenreDTO?
        let posterPath: String?
        let overview: String?
        let releaseDate: String?
        var isAd: Bool? = false
        
        init(
            id: Int,
            title: String?,
            genre: GenreDTO?,
            posterPath: String?,
            overview: String?,
            releaseDate: String?,
            isAd: Bool? = false
        ) {
            self.id = id
            self.title = title
            self.genre = genre
            self.posterPath = posterPath
            self.overview = overview
            self.releaseDate = releaseDate
            self.isAd = isAd
        }
    }
}

// MARK: - Mappings to Domain

extension MoviesResponseDTO {
    func toDomain() -> MoviesPage {
        return .init(page: page,
                     totalPages: totalPages,
                     movies: movies.map { $0.toDomain() })
    }
}

extension MoviesResponseDTO.MovieDTO {
    func toDomain() -> Movie {
        let movie = Movie(
                id: Movie.Identifier(id),
                title: title,
                title_2: title,
                genre: genre?.toDomain(),
                posterPath: posterPath,
                overview: overview,
                isAd: isAd ?? false
            )
        print("Mapped Movie: \(movie)")
        return movie
    }
}

extension MoviesResponseDTO.MovieDTO.GenreDTO {
    func toDomain() -> Movie.Genre {
        switch self {
        case .adventure: return .adventure
        case .scienceFiction: return .scienceFiction
        case .unknown: return .adventure
        }
    }
}

// MARK: - Private

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.calendar = Calendar(identifier: .iso8601)
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    return formatter
}()
