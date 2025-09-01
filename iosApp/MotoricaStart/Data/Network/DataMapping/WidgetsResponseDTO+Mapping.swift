import Foundation

// MARK: - Data Transfer Object

struct WidgetsResponseDTO: Decodable {
    private enum CodingKeys: String, CodingKey {
        case page
        case totalPages = "total_pages"
        case widgets = "results"
    }
    let page: Int
    let totalPages: Int
    let widgets: [WidgetDTO]
}

extension WidgetsResponseDTO {
    struct WidgetDTO: Decodable {
        private enum CodingKeys: String, CodingKey {
            case id
            case title
            case widgetType
            case posterPath = "poster_path"
            case overview
            case releaseDate = "release_date"
            case isAd
            case deviceAddress
            case parameterID
        }
        enum WidgetTypeDTO: String, Decodable {
            case commandWidget = "command_widget"
            case gestureOpticWidget = "gesture_optic_widget"
            case gestureWidget = "gesture_widget"
            case opticStartLearningWidget = "optic_start_learning_widget"
            case plotWidget = "plot_widget"
            case sliderWidget = "slider_widget"
            case spinnerWidget = "spinner_widget"
            case switchWidget = "switch_widget"
            case thresholdWidget = "threshold_widget"
            case unknown
        }
        let id: Int
        let title: String?
        let widgetType: WidgetTypeDTO?
        let posterPath: String?
        let overview: String?
        let releaseDate: String?
        var isAd: Bool? = false
        let deviceAddress: Int?
        let parameterID: Int?
        
        init(
            id: Int,
            title: String?,
            widgetType: WidgetTypeDTO?,
            posterPath: String?,
            overview: String?,
            releaseDate: String?,
            isAd: Bool? = false,
            deviceAddress: Int? = nil,
            parameterID: Int? = nil
        ) {
            self.id = id
            self.title = title
            self.widgetType = widgetType
            self.posterPath = posterPath
            self.overview = overview
            self.releaseDate = releaseDate
            self.isAd = isAd
            self.deviceAddress = deviceAddress
            self.parameterID = parameterID
        }
    }
}

// MARK: - Mappings to Domain

extension WidgetsResponseDTO {
    func toDomain() -> WidgetsPage {
        return .init(page: page,
                     totalPages: totalPages,
                     widgets: widgets.map { $0.toDomain() })
    }
}

extension WidgetsResponseDTO.WidgetDTO {
    func toDomain() -> Widget {
        let widget = Widget(
                id: Widget.Identifier(id),
                title: title,
                title_2: title,
                widgetType: widgetType?.toDomain(),
                posterPath: posterPath,
                overview: overview,
                isAd: isAd ?? false,
                deviceAddress: deviceAddress ?? 0,
                parameterID: parameterID ?? 0
            )
        print("Mapped Widget: \(widget)")
        return widget
    }
}

extension WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO {
    func toDomain() -> Widget.WidgetType {
        switch self {
        case .commandWidget: return .commandWidget
        case .gestureOpticWidget: return .gestureOpticWidget
        case .gestureWidget: return .gestureWidget
        case .opticStartLearningWidget: return .opticStartLearningWidget
        case .plotWidget: return .plotWidget
        case .sliderWidget: return .sliderWidget
        case .spinnerWidget: return .spinnerWidget
        case .switchWidget: return .switchWidget
        case .thresholdWidget: return .thresholdWidget
        case .unknown: return .plotWidget
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
