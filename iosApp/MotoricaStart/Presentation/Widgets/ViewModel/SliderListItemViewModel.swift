import Foundation

struct SliderListItemViewModel: Equatable, Hashable {
    let title: String
    let overview: String
    let title_2: String
    let posterImagePath: String?
    let showSecondSlider: Bool
    let deviceAddress: Int
    let parameterID: Int
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false) {
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.posterImagePath = widget.posterPath
        self.overview = widget.overview ?? ""
        self.showSecondSlider = showSecondSlider
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
