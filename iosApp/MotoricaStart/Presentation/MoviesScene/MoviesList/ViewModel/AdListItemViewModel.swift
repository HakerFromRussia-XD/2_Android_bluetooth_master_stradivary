import Foundation

struct AdListItemViewModel: Equatable, Hashable {
    let title: String
    let overview: String
    let title_2: String
    let posterImagePath: String?,
    let hideSecondSlider: Bool
}

extension AdListItemViewModel {
    init(movie: Movie, hideSecondSlider: Bool = false) {
        self.title = movie.title ?? ""
        self.title_2 = movie.title_2 ?? ""
        self.posterImagePath = movie.posterPath
        self.overview = movie.overview ?? ""
        self.hideSecondSlider = hideSecondSlider
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
