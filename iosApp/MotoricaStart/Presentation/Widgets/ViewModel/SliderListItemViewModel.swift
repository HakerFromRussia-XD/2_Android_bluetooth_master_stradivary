import Foundation
import shared

struct SliderListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let title_2: String
    let showSecondSlider: Bool
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.showSecondSlider = showSecondSlider
        self.widget = widget
        self.bleManager = bleManager
    }
    
    func requestSlider() {
        let data = BLECommands.shared.requestSlider(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )
        
        sendBytes(data)
    }
    func sendSliderProgress (progress: [KotlinInt]) {
        guard widget.sliderEStruct != nil || widget.sliderSStruct != nil else { return }
        if let sliderE = widget.sliderEStruct {
            print("🎚 minProgress = \(sliderE.minProgress)  SliderListItemViewModel sendSliderProgress")
            print("🎚 maxProgress = \(sliderE.maxProgress)  SliderListItemViewModel sendSliderProgress")
            print("🎚 labelCode = \(sliderE.baseParameterWidgetEStruct.labelCode)  SliderListItemViewModel sendSliderProgress")
        }
        if let sliderS = widget.sliderSStruct {
            print("🎚 minProgress = \(sliderS.minProgress)  SliderListItemViewModel sendSliderProgress")
            print("🎚 maxProgress = \(sliderS.maxProgress)  SliderListItemViewModel sendSliderProgress")
            print("🎚 label = \(sliderS.baseParameterWidgetSStruct.label)  SliderListItemViewModel sendSliderProgress")
        }
        
        let data = BLECommands.shared.sendSliderCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            progress: progress
        )
        
        sendBytes(data)
    }
    private func sendBytes (_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {})
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }
    static func == (lhs: SliderListItemViewModel, rhs: SliderListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
    
}
    /// Достаём метаданные из разных структур (E/S)
//    func extractMetadata() -> (
//        addressDevice: Int,
//        parameterID: Int,
//        dataCode: Int,
//        dataOffset: [Int],
//        minProgress: Int,
//        maxProgress: Int,
//        widgetPosition: Int
//    )? {
//        print("✅❌✅ widget =  \(widget.widget)   SliderListItemViewModel sendSliderProgress")
//        guard let value = widget.widget?.value else {
//            print("❌ widget.widget?.value == nil   SliderListItemViewModel sendSliderProgress")
//            return nil
//        }
//        
//        print("✅ widget.widget?.value type = \(type(of: value))   SliderListItemViewModel sendSliderProgress")
//
//        if let sliderE = value as? SliderParameterWidgetEStruct {
//            print("✅ Это SliderParameterWidgetEStruct   SliderListItemViewModel sendSliderProgress")
//            let base = sliderE.baseParameterWidgetEStruct.baseParameterWidgetStruct
//            print("base.parameterInfoSet count = \(base.parameterInfoSet.count)   SliderListItemViewModel sendSliderProgress")
//            
//            // Берём первый параметр из множества
//            guard let first = base.parameterInfoSet.first as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt> else {
//                print("❌ parameterInfoSet.first не кастанулся в ParameterInfo<KotlinInt,...>   SliderListItemViewModel sendSliderProgress")
//                return nil
//            }
//
//            print("✅ first.deviceAddress = \(String(describing: first.deviceAddress))   SliderListItemViewModel sendSliderProgress")
//            print("✅ minProgress = \(sliderE.minProgress), maxProgress = \(sliderE.maxProgress)   SliderListItemViewModel sendSliderProgress")
//            
//            let addressDevice = Int(first.deviceAddress?.intValue ?? 0)
//            let parameterID   = Int(first.parameterID?.intValue ?? 0)
//            let dataCode      = Int(first.dataCode?.intValue ?? 0)
//            let dataOffset: [Int] = base.parameterInfoSet.compactMap { any in
//                guard let info = any as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt>,
//                      let raw = info.dataOffset?.intValue else {
//                    return nil
//                }
//                return Int(raw)
//            }
//
//            return (
//                addressDevice,
//                parameterID,
//                dataCode,
//                dataOffset,
//                Int(sliderE.minProgress),
//                Int(sliderE.maxProgress),
//                Int(base.widgetPosition)
//            )
//        }
//        else if let sliderS = value as? SliderParameterWidgetSStruct {
//            print("✅ ✅ Это SliderParameterWidgetSStruct   SliderListItemViewModel sendSliderProgress")
//            let base = sliderS.baseParameterWidgetSStruct.baseParameterWidgetStruct
//            print("base.parameterInfoSet count = \(base.parameterInfoSet.count)   SliderListItemViewModel sendSliderProgress")
//
////            guard let first = base.parameterInfoSet.first as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt> else {
////                print("❌ ✅  parameterInfoSet.first не кастанулся в ParameterInfo<KotlinInt,...>   SliderListItemViewModel sendSliderProgress")
////                return nil
////            }
//            var addressDevice: Int = 0
//            var parameterID: Int = 0
//            var dataCode: Int = 0
//            var dataOffset: [Int] = []
//            
//            for element in base.parameterInfoSet {
//                if let obj = element as? NSObject {
//                    let devAddr = (obj.value(forKey: "deviceAddress") as? KotlinInt)?.intValue
//                        ?? (obj.value(forKey: "deviceAddress") as? NSNumber)?.intValue
//                    let paramID = (obj.value(forKey: "parameterID") as? KotlinInt)?.intValue
//                        ?? (obj.value(forKey: "parameterID") as? NSNumber)?.intValue
//                    let dCode = (obj.value(forKey: "dataCode") as? KotlinInt)?.intValue
//                        ?? (obj.value(forKey: "dataCode") as? NSNumber)?.intValue
//                    let offset = (obj.value(forKey: "dataOffset") as? KotlinInt)?.intValue
//                        ?? (obj.value(forKey: "dataOffset") as? NSNumber)?.intValue
//
//                    if let devAddr { addressDevice = Int(devAddr) }
//                    if let paramID { parameterID = Int(paramID) }
//                    if let dCode { dataCode = Int(dCode) }
//                    if let offset { dataOffset.append(Int(offset)) }
//                }
//            }
//            
//            let minProgress = (sliderS.value(forKey: "minProgress") as? KotlinInt)?.intValue
//                ?? (sliderS.value(forKey: "minProgress") as? NSNumber)?.intValue
//                ?? 0
//
//            let maxProgress = (sliderS.value(forKey: "maxProgress") as? KotlinInt)?.intValue
//                ?? (sliderS.value(forKey: "maxProgress") as? NSNumber)?.intValue
//                ?? 100
//
//            print("✅ first.deviceAddress = \(addressDevice)   SliderListItemViewModel sendSliderProgress")
//            print("✅ ✅  minProgress = \(minProgress), maxProgress = \(maxProgress)   SliderListItemViewModel sendSliderProgress")
////
////            let addressDevice = Int(first.deviceAddress?.intValue ?? 0)
////            let parameterID   = Int(first.parameterID?.intValue ?? 0)
////            let dataCode      = Int(first.dataCode?.intValue ?? 0)
////
////            let dataOffset: [Int] = base.parameterInfoSet.compactMap { any in
////                guard let info = any as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt>,
////                      let raw = info.dataOffset?.intValue else {
////                    return nil
////                }
////                return Int(raw)
////            }
//            
//            return (
//                addressDevice,
//                parameterID,
//                dataCode,
//                dataOffset,
//                Int(sliderS.minProgress),
//                Int(sliderS.maxProgress),
//                Int(base.widgetPosition)
//            )
//        }
//
//        print("❌ extractMetadata(): value не SliderParameterWidgetEStruct и не SliderParameterWidgetSStruct   SliderListItemViewModel sendSliderProgress")
//        return nil
//    }
// 
//}
