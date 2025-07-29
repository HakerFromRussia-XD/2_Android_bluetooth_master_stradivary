// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation

struct WidgetsListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    let title: String
    let overview: String
    let releaseDate: String
    let posterImagePath: String?
    let isAd: Bool
    let showSecondSlider: Bool
}

extension WidgetsListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false) {
        self.title = widget.title ?? ""
        self.posterImagePath = widget.posterPath
        self.overview = widget.overview ?? ""
        self.isAd = widget.isAd
        self.releaseDate = widget.title_2 ?? ""
        self.showSecondSlider = showSecondSlider
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
