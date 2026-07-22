import Foundation
import shared

enum WidgetDescriptorFactoryV3 {
    static let bleLogPayload = "__ble_log_button__"

    private enum WidgetCode {
        static let button = 0x01
        static let `switch` = 0x02
        static let slider = 0x04
        static let plot = 0x05
        static let gestures = 0x0E
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

    static func makeWidgetsDTO(from kotlinWidgets: [Any]) -> [WidgetsResponseDTO.WidgetDTO] {
        return kotlinWidgets.enumerated().map { index, source in
            let prepared = prepare(source: source, index: index)
            return WidgetsResponseDTO.WidgetDTO(
                id: index,
                title: prepared.title,
                widgetType: prepared.type,
                widget: AnyCodable(prepared.payload)
            )
        }
    }

    private static func prepare(source: Any, index: Int) -> (title: String, type: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO, payload: Any) {
        var title: String?
        var payload: Any = source
        var explicitType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?

        switch source {
        case is BleLogButtonItem:
            title = bleLogTitle
            payload = bleLogPayload
            explicitType = .commandWidget
        case let item as PlotItem:
            title = item.title
            payload = item.widget
            explicitType = .plotWidget
        case let item as PlotItemV3:
            title = item.title
            payload = item.widget
            explicitType = .plotWidget
        case let item as SliderItem:
            title = item.title
            payload = item.widget
            explicitType = .sliderWidget
        case let item as SliderItemV3:
            title = item.title
            payload = item.widget
            explicitType = .sliderWidget
        case let item as SwitchItem:
            title = item.title
            payload = item.widget
            explicitType = .switchWidget
        case let item as SwitchItemV3:
            title = item.title
            payload = item.widget
            explicitType = .switchWidget
        case let item as GesturesItem:
            title = item.title
            payload = item.widget
            explicitType = .gestureOpticWidget
        case let item as GesturesItemV3:
            title = item.title
            payload = item.widget
            explicitType = .gestureWidget
        case let item as OneButtonItem:
            title = item.title
            payload = item.widget
            explicitType = .commandWidget
        case let item as ButtonsItemV3:
            title = [item.title, item.title2, item.title3]
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .joined(separator: "%")
            payload = item.widget
            explicitType = .commandWidget
        case let item as SpinnerItem:
            title = item.title
            payload = item.widget
            explicitType = .spinnerWidget
        case let item as SpinnerItemV3:
            title = item.title
            payload = item.widget
            explicitType = .spinnerWidget
        case let item as ToggleSliderItemV3:
            title = item.title
            payload = item.widget
            explicitType = .toggleSliderWidget
        case let item as ToggleSliderItem:
            title = item.title
            payload = item.widget
            explicitType = .toggleSliderWidget
        case let item as TrainingGestureItem:
            title = item.title
            payload = item.widget
            explicitType = .opticStartLearningWidget
        case let item as TextInputItemV3:
            title = "\(item.title)%\(item.buttonTitle)"
            payload = item.widget
            explicitType = .textInputWidget
        default:
            break
        }

        let widgetCode = Int(WidgetMetadataExtractor.extractBaseStruct(from: payload)?.widgetCode ?? -1)
        let inferredType = explicitType ?? mapTypeByWidgetCode(widgetCode)
        let resolvedTitle = title ?? fallbackTitle(from: source) ?? "Widget \(index)"

        return (
            title: resolvedTitle,
            type: inferredType,
            payload: payload
        )
    }

    private static func mapTypeByWidgetCode(_ code: Int) -> WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO {
        switch code {
        case WidgetCode.plot, WidgetCode.plotV3:
            return .plotWidget
        case WidgetCode.slider, WidgetCode.sliderV3:
            return .sliderWidget
        case WidgetCode.switch, WidgetCode.switchV3:
            return .switchWidget
        case WidgetCode.comboboxV3, WidgetCode.spinboxV3:
            return .spinnerWidget
        case WidgetCode.toggleSliderV3:
            return .toggleSliderWidget
        case WidgetCode.gesturesV3:
            return .gestureWidget
        case WidgetCode.gestures:
            return .gestureOpticWidget
        case WidgetCode.textInputV3:
            return .textInputWidget
        case WidgetCode.button, WidgetCode.buttonV3:
            return .commandWidget
        default:
            return .commandWidget
        }
    }

    private static func fallbackTitle(from widget: Any?) -> String? {
        switch widget {
        case is BleLogButtonItem:
            return bleLogTitle
        case let plotItem as PlotItem:
            return plotItem.title
        case let plotItem as PlotItemV3:
            return plotItem.title
        case let sliderItem as SliderItem:
            return sliderItem.title
        case let sliderItem as SliderItemV3:
            return sliderItem.title
        case let buttonItem as OneButtonItem:
            return buttonItem.title
        case let buttonItem as ButtonsItemV3:
            return [buttonItem.title, buttonItem.title2, buttonItem.title3]
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .joined(separator: "%")
        case let gesturesItem as GesturesItem:
            return gesturesItem.title
        case let gesturesItem as GesturesItemV3:
            return gesturesItem.title
        case let switchItem as SwitchItem:
            return switchItem.title
        case let switchItem as SwitchItemV3:
            return switchItem.title
        case let trainingItem as TrainingGestureItem:
            return trainingItem.title
        case let spinnerItem as SpinnerItem:
            return spinnerItem.title
        case let spinnerItem as SpinnerItemV3:
            return spinnerItem.title
        case let toggleItem as ToggleSliderItemV3:
            return toggleItem.title
        case let toggleItem as ToggleSliderItem:
            return toggleItem.title
        case let textInputItem as TextInputItemV3:
            return "\(textInputItem.title)%\(textInputItem.buttonTitle)"
        default:
            return nil
        }
    }

    private static var bleLogTitle: String {
        Locale.preferredLanguages.first?.hasPrefix("ru") == true ? "Журнал BLE" : "BLE Log"
    }
}
