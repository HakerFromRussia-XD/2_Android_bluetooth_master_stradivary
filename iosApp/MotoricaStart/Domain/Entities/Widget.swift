import Foundation
import shared

struct Widget: Equatable, Identifiable {
    typealias Identifier = String
    enum WidgetType: Equatable {
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
    let deviceAddress: Int
    let parameterID: Int
    let widget: AnyCodable?

    static func == (lhs: Widget, rhs: Widget) -> Bool {
        lhs.id == rhs.id &&
        lhs.title == rhs.title &&
        lhs.title_2 == rhs.title_2 &&
        lhs.widgetType == rhs.widgetType &&
        lhs.deviceAddress == rhs.deviceAddress &&
        lhs.parameterID == rhs.parameterID
    }
}

struct WidgetsPage: Equatable {
    let page: Int
    let totalPages: Int
    let widgets: [Widget]
}

enum WidgetMetadataExtractor {
    static func metadata(from widget: AnyCodable?) -> (deviceAddress: Int?, parameterID: Int?) {
        guard let widget else { return (nil, nil) }
        return metadata(from: widget.value)
    }

    static func metadata(from widget: Any?) -> (deviceAddress: Int?, parameterID: Int?) {
        if let anyCodable = widget as? AnyCodable {
            return metadata(from: anyCodable.value)
        }
        if let metadata = metadata(fromDictionaryAny: widget) {
            return metadata
        }
        if let array = widget as? [Any] {
            return metadata(fromArray: array)
        }
        return metadataFromKotlin(widget)
    }

    private static func metadata(fromDictionaryAny value: Any?) -> (Int?, Int?)? {
        if let dictionary = value as? [String: AnyCodable] {
            return metadata(fromDictionary: dictionary.mapValues { $0.value })
        }
        if let dictionary = value as? [String: Any] {
            return metadata(fromDictionary: dictionary)
        }
        if let dictionary = value as? NSDictionary {
            var mapped: [String: Any] = [:]
            dictionary.forEach { key, value in
                if let key = key as? String {
                    mapped[key] = value
                }
            }
            return metadata(fromDictionary: mapped)
        }
        return nil
    }

    private static func metadata(fromDictionary dictionary: [String: Any]) -> (Int?, Int?) {
        var deviceAddress: Int?
        var parameterID: Int?

        for (key, value) in dictionary {
            let lowercasedKey = key.lowercased()
            if deviceAddress == nil, ["deviceaddress", "device_address", "deviceid", "device_id"].contains(lowercasedKey) {
                deviceAddress = intValue(from: value)
            }
            if parameterID == nil, ["parameterid", "parameter_id", "parameteridvalue"].contains(lowercasedKey) {
                parameterID = intValue(from: value)
            }

            if deviceAddress != nil, parameterID != nil {
                break
            }

            if let nested = value as? [String: Any] {
                let nestedMetadata = metadata(fromDictionary: nested)
                if deviceAddress == nil { deviceAddress = nestedMetadata.0 }
                if parameterID == nil { parameterID = nestedMetadata.1 }
            } else if let nestedArray = value as? [Any] {
                let nestedMetadata = metadata(fromArray: nestedArray)
                if deviceAddress == nil { deviceAddress = nestedMetadata.0 }
                if parameterID == nil { parameterID = nestedMetadata.1 }
            } else if let anyCodable = value as? AnyCodable {
                let nestedMetadata = metadata(from: anyCodable.value)
                if deviceAddress == nil { deviceAddress = nestedMetadata.deviceAddress }
                if parameterID == nil { parameterID = nestedMetadata.parameterID }
            }
        }

        return (deviceAddress, parameterID)
    }

    private static func metadata(fromArray array: [Any]) -> (Int?, Int?) {
        var deviceAddress: Int?
        var parameterID: Int?

        for element in array {
            let nestedMetadata = metadata(from: element)
            if deviceAddress == nil { deviceAddress = nestedMetadata.deviceAddress }
            if parameterID == nil { parameterID = nestedMetadata.parameterID }
            if deviceAddress != nil, parameterID != nil { break }
        }

        return (deviceAddress, parameterID)
    }

    private static func metadataFromKotlin(_ widget: Any?) -> (Int?, Int?) {
        guard let baseStruct = extractBaseStruct(from: widget) else {
            return (nil, nil)
        }

        var deviceAddress: Int? = Int(baseStruct.deviceId)
        var parameterID: Int?

        if let parameterInfo = firstParameterInfo(in: baseStruct.parameterInfoSet) {
            if let parameterIdValue = parameterInfo.value(forKey: "parameterID") as? KotlinInt {
                parameterID = Int(parameterIdValue.intValue)
            } else if let parameterIdNumber = parameterInfo.value(forKey: "parameterID") as? NSNumber {
                parameterID = parameterIdNumber.intValue
            }

            if let deviceAddressValue = parameterInfo.value(forKey: "deviceAddress") as? KotlinInt {
                deviceAddress = Int(deviceAddressValue.intValue)
            } else if let deviceAddressNumber = parameterInfo.value(forKey: "deviceAddress") as? NSNumber {
                deviceAddress = deviceAddressNumber.intValue
            }
        }

        return (deviceAddress, parameterID)
    }

    static func extractBaseStruct(from widget: Any?) -> BaseParameterWidgetStruct? {
        switch widget {
        case let baseStruct as BaseParameterWidgetStruct:
            return baseStruct
        case let baseEStruct as BaseParameterWidgetEStruct:
            return baseEStruct.baseParameterWidgetStruct
        case let baseSStruct as BaseParameterWidgetSStruct:
            return baseSStruct.baseParameterWidgetStruct
        case let commandEStruct as CommandParameterWidgetEStruct:
            return commandEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let commandSStruct as CommandParameterWidgetSStruct:
            return commandSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let plotEStruct as PlotParameterWidgetEStruct:
            return plotEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let plotSStruct as PlotParameterWidgetSStruct:
            return plotSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let sliderEStruct as SliderParameterWidgetEStruct:
            return sliderEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let sliderSStruct as SliderParameterWidgetSStruct:
            return sliderSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let spinnerEStruct as SpinnerParameterWidgetEStruct:
            return spinnerEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let spinnerSStruct as SpinnerParameterWidgetSStruct:
            return spinnerSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let switchEStruct as SwitchParameterWidgetEStruct:
            return switchEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let switchSStruct as SwitchParameterWidgetSStruct:
            return switchSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let opticStruct as OpticStartLearningWidgetEStruct:
            return opticStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let opticSStruct as OpticStartLearningWidgetSStruct:
            return opticSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let gestureOpticStruct as GestureOpticParameterWidgetEStruct:
            return gestureOpticStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let gestureStruct as GestureParameterWidgetEStruct:
            return gestureStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let thresholdStruct as ThresholdParameterWidgetEStruct:
            return thresholdStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let thresholdSStruct as ThresholdParameterWidgetSStruct:
            return thresholdSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        default:
            return nil
        }
    }

    static func firstParameterInfo(in set: Any?) -> NSObject? {
        if let kotlinSet = set as? KotlinMutableSet<AnyObject> {
            for element in kotlinSet {
                if let parameterInfo = element as? NSObject {
                    return parameterInfo
                }
            }
        }

        if let nsSet = set as? NSSet {
            return nsSet.anyObject() as? NSObject
        }
        return nil
    }
    
    private static func intValue(from value: Any?) -> Int? {
        switch value {
        case let number as NSNumber:
            return number.intValue
        case let string as NSString:
            return string.integerValue
        case let string as String:
            return Int(string)
        case let anyCodable as AnyCodable:
            return intValue(from: anyCodable.value)
        case let kotlinInt as KotlinInt:
            return Int(kotlinInt.intValue)
        default:
            return nil
        }
    }
}

extension Widget {
    /// Универсальный generic-декодер
    func decode<T>() -> T? {
        widget?.value as? T
    }

    /// Удобные computed-свойства под самые частые типы
    private var sliderEStruct: SliderParameterWidgetEStruct? { decode() }
    private var sliderSStruct: SliderParameterWidgetSStruct? { decode() }
    var sliderUnified: SliderProtocol? { SliderEProtocol(sliderEStruct) ?? SliderSProtocol(sliderSStruct) }
    
    private var plotEStruct: PlotParameterWidgetEStruct? { decode() }
    private var plotSStruct: PlotParameterWidgetSStruct? { decode() }
    var plotUnified: PlotProtocol? { PlotEProtocol(plotEStruct) ?? PlotSProtocol(plotSStruct) }
    
    private var commandEStruct: CommandParameterWidgetEStruct? { decode() }
    private var commandSStruct: CommandParameterWidgetSStruct? { decode() }
    var commandUnified: CommandProtocol? { CommandEProtocol(commandEStruct) ?? CommandSProtocol(commandSStruct) }
}

protocol CommandProtocol {
    var clickCommand: Int32 { get }
    var pressedCommand: Int32 { get }
    var releasedCommand: Int32 { get }
}
struct CommandEProtocol: CommandProtocol {
    private let src: CommandParameterWidgetEStruct
    init?(_ src: CommandParameterWidgetEStruct?) { guard let src else { return nil }; self.src = src }

    var clickCommand: Int32    { src.clickCommand }
    var pressedCommand: Int32  { src.pressedCommand }
    var releasedCommand: Int32 { src.releasedCommand }
}
struct CommandSProtocol: CommandProtocol {
    private let src: CommandParameterWidgetSStruct
    init?(_ src: CommandParameterWidgetSStruct?) { guard let src else { return nil }; self.src = src }

    var clickCommand: Int32    { src.clickCommand }
    var pressedCommand: Int32  { src.pressedCommand }
    var releasedCommand: Int32 { src.releasedCommand }
}

protocol PlotProtocol {
    var color: Int32 { get }
    var maxSize: Int32 { get }
    var minSize: Int32 { get }
}
struct PlotEProtocol: PlotProtocol {
    private let src: PlotParameterWidgetEStruct
    init?(_ src: PlotParameterWidgetEStruct?) { guard let src else { return nil }; self.src = src }
    
    var color: Int32 { src.color }
    var maxSize: Int32 { src.maxSize }
    var minSize: Int32 { src.minSize }
}
struct PlotSProtocol: PlotProtocol {
    private let src: PlotParameterWidgetSStruct
    init?(_ src: PlotParameterWidgetSStruct?) { guard let src else { return nil }; self.src = src }
    
    var color: Int32 { src.color }
    var maxSize: Int32 { src.maxSize }
    var minSize: Int32 { src.minSize }
}

protocol SliderProtocol {
    var minProgress: Int32 { get }
    var maxProgress: Int32 { get }
    var baseParameterWidgetStruct: BaseParameterWidgetStruct? { get }
}
struct SliderEProtocol: SliderProtocol {
    private let src: SliderParameterWidgetEStruct
    init?(_ src: SliderParameterWidgetEStruct?) { guard let src else { return nil }; self.src = src }
    
    var minProgress: Int32 { src.minProgress }
    var maxProgress: Int32 { src.maxProgress }
    var baseParameterWidgetStruct: BaseParameterWidgetStruct? { src.baseParameterWidgetEStruct.baseParameterWidgetStruct }
}
struct SliderSProtocol: SliderProtocol {
    private let src: SliderParameterWidgetSStruct
    init?(_ src: SliderParameterWidgetSStruct?) { guard let src else { return nil }; self.src = src }
    
    var minProgress: Int32 { src.minProgress }
    var maxProgress: Int32 { src.maxProgress }
    var baseParameterWidgetStruct: BaseParameterWidgetStruct? { src.baseParameterWidgetSStruct.baseParameterWidgetStruct }
}
