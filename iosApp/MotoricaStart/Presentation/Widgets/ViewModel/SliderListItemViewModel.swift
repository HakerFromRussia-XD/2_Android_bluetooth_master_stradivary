import Foundation

struct SliderListItemViewModel: Equatable, Hashable {
    private let uuid = UUID()
    let title: String
    let overview: String
    let title_2: String
    let showSecondSlider: Bool
    let deviceAddress: Int
    let parameterID: Int
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false) {
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.overview = widget.overview ?? ""
        self.showSecondSlider = showSecondSlider
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(uuid)
    }

    static func == (lhs: SliderListItemViewModel, rhs: SliderListItemViewModel) -> Bool {
        lhs.uuid == rhs.uuid
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    return formatter
}()
