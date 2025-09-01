import Foundation

struct Widget: Equatable, Identifiable {
    typealias Identifier = String
    enum WidgetType {
        case commandWidget
        case gestureOpticWidget
        case gestureWidget
        case opticStartLearningWidget
        case plotWidget
        case sliderWidget
        case spinnerWidget
        case switchWidget
        case thresholdWidget
    }
    let id: Identifier
    let title: String?
    let title_2: String?
    let widgetType: WidgetType?
    let posterPath: String?
    let overview: String?
    let isAd: Bool
    let deviceAddress: Int
    let parameterID: Int
}

struct WidgetsPage: Equatable {
    let page: Int
    let totalPages: Int
    let widgets: [Widget]
}
