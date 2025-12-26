import Foundation
import shared

struct AnyCodable: Codable, Equatable {
    let value: Any

    init(_ value: Any?) {
        switch value {
        case let codable as AnyCodable:
            self.value = codable.value
        case let dictionary as [String: Any]:
            self.value = dictionary.mapValues { AnyCodable($0) }
        case let array as [Any]:
            self.value = array.map { AnyCodable($0) }
        case .none:
            self.value = NSNull()
        default:
            self.value = value ?? NSNull()
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            self.value = NSNull()
        } else if let bool = try? container.decode(Bool.self) {
            self.value = bool
        } else if let int = try? container.decode(Int.self) {
            self.value = int
        } else if let double = try? container.decode(Double.self) {
            self.value = double
        } else if let string = try? container.decode(String.self) {
            self.value = string
        } else if let array = try? container.decode([AnyCodable].self) {
            self.value = array
        } else if let dictionary = try? container.decode([String: AnyCodable].self) {
            self.value = dictionary
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported value")
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch value {
        case is NSNull:
            try container.encodeNil()
        case let bool as Bool:
            try container.encode(bool)
        case let int as Int:
            try container.encode(int)
        case let int as Int8:
            try container.encode(int)
        case let int as Int16:
            try container.encode(int)
        case let int as Int32:
            try container.encode(int)
        case let int as Int64:
            try container.encode(int)
        case let uint as UInt:
            try container.encode(uint)
        case let uint as UInt8:
            try container.encode(uint)
        case let uint as UInt16:
            try container.encode(uint)
        case let uint as UInt32:
            try container.encode(uint)
        case let uint as UInt64:
            try container.encode(uint)
        case let double as Double:
            try container.encode(double)
        case let float as Float:
            try container.encode(float)
        case let string as String:
            try container.encode(string)
        case let array as [AnyCodable]:
            try container.encode(array)
        case let dictionary as [String: AnyCodable]:
            try container.encode(dictionary)
        case let number as NSNumber:
            try container.encode(number.intValue)
        default:
            throw EncodingError.invalidValue(value, EncodingError.Context(codingPath: container.codingPath, debugDescription: "Unsupported value"))
        }
    }

    static func == (lhs: AnyCodable, rhs: AnyCodable) -> Bool {
        switch (lhs.value, rhs.value) {
        case (is NSNull, is NSNull):
            return true
        case let (l as Bool, r as Bool):
            return l == r
        case let (l as Int, r as Int):
            return l == r
        case let (l as Int8, r as Int8):
            return l == r
        case let (l as Int16, r as Int16):
            return l == r
        case let (l as Int32, r as Int32):
            return l == r
        case let (l as Int64, r as Int64):
            return l == r
        case let (l as UInt, r as UInt):
            return l == r
        case let (l as UInt8, r as UInt8):
            return l == r
        case let (l as UInt16, r as UInt16):
            return l == r
        case let (l as UInt32, r as UInt32):
            return l == r
        case let (l as UInt64, r as UInt64):
            return l == r
        case let (l as Double, r as Double):
            return l == r
        case let (l as Float, r as Float):
            return l == r
        case let (l as String, r as String):
            return l == r
        case let (l as NSNumber, r as NSNumber):
            return l == r
        case let (l as [AnyCodable], r as [AnyCodable]):
            return l == r
        case let (l as [String: AnyCodable], r as [String: AnyCodable]):
            return l == r
        case let (l as NSObject, r as NSObject):
            return l == r
        default:
            return false
        }
    }
}

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
            case widget
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
        let widget: AnyCodable?
        
        init(
            id: Int,
            title: String?,
            widgetType: WidgetTypeDTO?,
            widget: AnyCodable? = nil
        ) {
            self.id = id
            self.title = title
            self.widgetType = widgetType
            self.widget = widget
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
        let metadata = WidgetMetadataExtractor.metadata(from: widget)
        let widget = Widget(
                id: Widget.Identifier(id),
                title: title,
                title_2: title,
                widgetType: widgetType?.toDomain(),
                deviceAddress: metadata.deviceAddress ?? 0,
                parameterID: metadata.parameterID ?? 0,
                widget: widget
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
        case .unknown: return .commandWidget
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
