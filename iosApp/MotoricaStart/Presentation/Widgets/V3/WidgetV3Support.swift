import Foundation
import shared

struct WidgetV3BindingInfo: Hashable {
    let parameterID: Int
    let dataCode: Int
    let deviceAddress: Int
    let dataOffset: Int
}

enum WidgetV3Support {
    enum WidgetCode {
        static let buttonV3 = 0x12
        static let switchV3 = 0x13
        static let comboboxV3 = 0x14
        static let sliderV3 = 0x15
        static let plotV3 = 0x16
        static let toggleSliderV3 = 0x17
        static let gesturesV3 = 0x18
        static let spinboxV3 = 0x19
        static let textInputV3 = 0x1A
    }

    static func widgetCode(from widget: Widget) -> Int {
        Int(WidgetMetadataExtractor.extractBaseStruct(from: widget.widget?.value)?.widgetCode ?? -1)
    }

    static func isV3Widget(_ widget: Widget) -> Bool {
        widgetCode(from: widget) >= WidgetCode.buttonV3
    }

    static func bindings(from widget: Widget) -> [WidgetV3BindingInfo] {
        guard let baseStruct = WidgetMetadataExtractor.extractBaseStruct(from: widget.widget?.value) else {
            return []
        }

        return ParameterInfoData
            .makeSet(from: baseStruct.parameterInfoSet)
            .sorted { lhs, rhs in
                if lhs.dataOffset != rhs.dataOffset {
                    return lhs.dataOffset < rhs.dataOffset
                }
                if lhs.parameterID != rhs.parameterID {
                    return lhs.parameterID < rhs.parameterID
                }
                if lhs.dataCode != rhs.dataCode {
                    return lhs.dataCode < rhs.dataCode
                }
                return lhs.deviceAddress < rhs.deviceAddress
            }
            .map {
                WidgetV3BindingInfo(
                    parameterID: $0.parameterID,
                    dataCode: $0.dataCode,
                    deviceAddress: $0.deviceAddress,
                    dataOffset: $0.dataOffset
                )
            }
    }

    static func primaryBinding(from widget: Widget) -> WidgetV3BindingInfo? {
        bindings(from: widget).first
    }

    static func splitTextInputTitle(_ rawTitle: String) -> (placeholder: String, buttonTitle: String) {
        let parts = rawTitle
            .split(separator: "%", omittingEmptySubsequences: false)
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }

        let placeholder = parts.first?.isEmpty == false ? parts.first! : SharedLocalizedText.text(SharedRes.strings().enter_text)
        let buttonTitle = parts.count > 1 && !parts[1].isEmpty ? parts[1] : SharedLocalizedText.text(SharedRes.strings().send)
        return (placeholder, buttonTitle)
    }
}

enum V3SnapshotParser {
    static func intField(from serialized: String, field: String) -> Int? {
        guard
            let data = serialized.data(using: .utf8),
            let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }

        if let value = json[field] as? NSNumber {
            return value.intValue
        }
        if let value = json[field] as? String {
            return Int(value)
        }
        return nil
    }

    static func boolField(from serialized: String, field: String) -> Bool? {
        guard
            let data = serialized.data(using: .utf8),
            let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }

        if let value = json[field] as? Bool {
            return value
        }
        if let value = json[field] as? NSNumber {
            return value.boolValue
        }
        if let value = json[field] as? String {
            return (value as NSString).boolValue
        }
        return nil
    }
}
