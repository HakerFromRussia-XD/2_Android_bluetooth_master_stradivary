// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation

struct AdListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    let title: String
    let overview: String
    let title_2: String
    let posterImagePath: String?
}

extension AdListItemViewModel {
    init(movie: Movie) {
        self.title = movie.title ?? ""
        self.title_2 = movie.title_2 ?? ""
        self.posterImagePath = movie.posterPath
        self.overview = movie.overview ?? ""
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
