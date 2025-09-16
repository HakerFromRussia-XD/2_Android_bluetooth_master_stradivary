import Foundation

struct SliderListItemViewModel: Equatable, Hashable {
    let id: Widget.Identifier
    let title: String
    let overview: String
    let title_2: String
    let showSecondSlider: Bool
    let deviceAddress: Int
    let parameterID: Int
}

extension SliderListItemViewModel {
    init(id: Widget.Identifier, widget: Widget, showSecondSlider: Bool = false) {
        self.id = id
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.overview = widget.overview ?? ""
        self.showSecondSlider = showSecondSlider
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: SliderListItemViewModel, rhs: SliderListItemViewModel) -> Bool {
        lhs.id == rhs.id
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
