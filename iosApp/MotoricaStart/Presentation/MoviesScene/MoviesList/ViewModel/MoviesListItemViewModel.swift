// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation

struct MoviesListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    let title: String
    let overview: String
    let releaseDate: String
    let posterImagePath: String?
    let isAd: Bool
}

extension MoviesListItemViewModel {
    init(movie: Movie) {
        self.title = movie.title ?? ""
        self.posterImagePath = movie.posterPath
        self.overview = movie.overview ?? ""
        self.isAd = movie.isAd
        self.releaseDate = movie.title_2 ?? ""
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
