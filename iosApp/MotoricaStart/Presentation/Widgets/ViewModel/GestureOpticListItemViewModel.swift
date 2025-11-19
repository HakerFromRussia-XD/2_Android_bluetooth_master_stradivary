//
//  SwitchListItemViewModel.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.10.2025.
//
import Foundation
import shared
import UIKit


struct GestureListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    private let gestureNameList: [String]
    private let parameterInfoSet: Set<ParameterInfoData>
    
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.gestureNameList = GestureListItemViewModel.makeGestureNames()
        
        if let baseStruct = WidgetMetadataExtractor.extractBaseStruct(from: widget.widget) {
            self.parameterInfoSet = ParameterInfoData.makeSet(from: baseStruct.parameterInfoSet)
        } else {
            self.parameterInfoSet = []
        }
    }
}

extension GestureListItemViewModel {
    func makeProvider() -> GesturesProvider {
        let factory = GestureCatalog.factoryGestures
        let custom = GestureCatalog.customGestures(withTitles: gestureNameList)
        let rotation = Array(factory.prefix(4))
        let spr: [GesturesProvider.SprGestureDisplayItem] = []
        return GesturesProvider(
            factoryGestures: factory.map {
                GesturesProvider.GestureDisplayItem(
                    id: $0.id,
                    title: $0.title,
                    subtitle: nil,
                    image: $0.image
                )
            },
            customGestures: custom.map {
                GesturesProvider.GestureDisplayItem(
                    id: $0.id,
                    title: $0.title,
                    subtitle: $0.subtitle,
                    image: nil
                )
            },
            rotationGroup: rotation.map {
                GesturesProvider.GestureDisplayItem(
                    id: $0.id,
                    title: $0.title,
                    subtitle: nil,
                    image: $0.image
                )
            },
            sprGestures: spr,
            activeGestureId: 0,
            activeGestureTitle: nil
        )
    }
    func selectFactoryGesture(_ item: GesturesProvider.GestureDisplayItem, provider: GesturesProvider) {
        provider.activeGestureId = item.id
        provider.activeGestureTitle = item.title
        sendActiveGesture(gestureId: item.id)
    }

    func selectCustomGesture(_ item: GesturesProvider.GestureDisplayItem, provider: GesturesProvider) {
        provider.activeGestureId = item.id
        provider.activeGestureTitle = item.title
        sendActiveGesture(gestureId: item.id)
    }

    func openGestureSettings(for item: GesturesProvider.GestureDisplayItem) {
        requestGestureSettings(gestureId: item.id)
    }

    func moveRotationGestureUp(at index: Int, provider: GesturesProvider) {
        guard index > 0 else { return }
        provider.rotationGroup.swapAt(index, index - 1)
        sendRotationGroup(with: provider.rotationGroup)
    }

    func moveRotationGestureDown(at index: Int, provider: GesturesProvider) {
        guard index < provider.rotationGroup.count - 1 else { return }
        provider.rotationGroup.swapAt(index, index + 1)
        sendRotationGroup(with: provider.rotationGroup)
    }

    func removeRotationGesture(at index: Int, provider: GesturesProvider) {
        guard provider.rotationGroup.indices.contains(index) else { return }
        provider.rotationGroup.remove(at: index)
        sendRotationGroup(with: provider.rotationGroup)
    }

//    func appendRotationGesture(provider: GesturesProvider) {
//        guard let gesture = GestureCatalog.factoryGestures.first(where: { item in
//            provider.rotationGroup.contains(where: { $0.id == item.id }) == false
//        }) else { return }
//        provider.rotationGroup.append(
//            GesturesProvider.GestureDisplayItem(
//                id: gesture.id,
//                title: gesture.title,
//                subtitle: nil,
//                image: gesture.image
//            )
//        )
//        sendRotationGroup(with: provider.rotationGroup)
//    }
    func updateRotationGestures(_ gestures: [GesturesProvider.GestureDisplayItem], provider: GesturesProvider) {
        provider.rotationGroup = gestures
        sendRotationGroup(with: provider.rotationGroup)
    }

    func requestRotationGroup() {
        let parameterID = parameterID(for: ParameterCode.gestureGroup)
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestRotationGroup(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID)
        )
        sendBytes(data)
    }

    func requestBindingGroup() {
        let parameterID = parameterID(for: ParameterCode.bindingGroup)
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestBindingGroup(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID)
        )
        sendBytes(data)
    }

    private func sendActiveGesture(gestureId: Int) {
        let parameterID = parameterID(for: ParameterCode.selectGesture)
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.sendActiveGesture(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            activeGesture: Int32(gestureId)
        )
        sendBytes(data)
    }

    private func requestGestureSettings(gestureId: Int) {
        let parameterID = parameterID(for: ParameterCode.gestureSettings)
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestGestureInfo(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            gestureId: Int32(gestureId)
        )
        sendBytes(data)
    }

    private func sendRotationGroup(with gestures: [GesturesProvider.GestureDisplayItem]) {
        let rotationGroup = RotationGroup.make(from: gestures)
        let parameterID = parameterID(for: ParameterCode.gestureGroup)
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.sendRotationGroupInfo(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            rotationGroup: rotationGroup
        )
        sendBytes(data)
    }
    
    private func parameterID(for dataCode: Int) -> Int {
        parameterInfoSet.first(where: { $0.dataCode == dataCode })?.parameterID ?? 0
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }
   
    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }
    static func == (lhs: GestureListItemViewModel, rhs: GestureListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
}


private enum GestureCatalog {
    struct GestureItem {
        let id: Int
        let title: String
        var subtitle: String? = nil
        let image: UIImage?
    }

    static let factoryGestures: [GestureItem] = [
        .init(
            id: 1,
            title: NSLocalizedString("Fist", comment: ""),
            image: SharedRes.images().collection_fist_1.toUIImage()
        ),
        .init(
            id: 2,
            title: NSLocalizedString("Point", comment: ""),
            image: SharedRes.images().collection_point.toUIImage()
        ),
        .init(
            id: 3,
            title: NSLocalizedString("Pinch", comment: ""),
            image: SharedRes.images().collection_pinch.toUIImage()
        ),
        .init(
            id: 4,
            title: NSLocalizedString("Fist + thumb", comment: ""),
            image: SharedRes.images().collection_fist_2.toUIImage()
        ),
        .init(
            id: 5,
            title: NSLocalizedString("Key", comment: ""),
            image: SharedRes.images().collection_key.toUIImage()
        ),
        .init(
            id: 6,
            title: NSLocalizedString("Rock", comment: ""),
            image: SharedRes.images().collection_rock.toUIImage()
        ),
        .init(
            id: 7,
            title: NSLocalizedString("Tweezers", comment: ""),
            image: SharedRes.images().collection_twizzers.toUIImage()
        ),
        .init(
            id: 8,
            title: NSLocalizedString("Cupholder", comment: ""),
            image: SharedRes.images().collection_cupholder.toUIImage()
        ),
        .init(
            id: 9,
            title: NSLocalizedString("Half grab", comment: ""),
            image: SharedRes.images().collect_half_grab.toUIImage()
        ),
        .init(
            id: 10,
            title: NSLocalizedString("OK", comment: ""),
            image: SharedRes.images().collection_ok.toUIImage()
        ),
        .init(
            id: 11,
            title: NSLocalizedString("Thumb up", comment: ""),
            image: SharedRes.images().collection_thumb_up.toUIImage()
        ),
//        .init(
//            id: 12,
//            title: NSLocalizedString("Middle finger", comment: ""),
//            image: SharedRes.images().collection_middle_finger.toUIImage()
//        ),
        .init(
            id: 13,
            title: NSLocalizedString("Double point", comment: ""),
            image: SharedRes.images().collection_double_point.toUIImage()
        ),
        .init(
            id: 14,
            title: NSLocalizedString("Call me", comment: ""),
            image: SharedRes.images().collection_call_me.toUIImage()
        ),
        .init(
            id: 15,
            title: NSLocalizedString("Natural", comment: ""),
            image: SharedRes.images().collection_natural_position.toUIImage()
        )
    ]

    static func customGestures(withTitles titles: [String]) -> [GestureItem] {
        let baseIdentifier = 64
        return titles.enumerated().map { index, title in
            GestureItem(
                id: baseIdentifier + index,
                title: title,
                subtitle: NSLocalizedString("Custom gesture", comment: ""),
                image: nil
            )
        }
    }
}
private extension RotationGroup {
    static func make(from gestures: [GesturesProvider.GestureDisplayItem]) -> RotationGroup {
        func id(_ index: Int) -> Int32 {
            guard gestures.indices.contains(index) else { return 0 }
            return Int32(gestures[index].id)
        }

        return RotationGroup(
            gesture1Id: id(0), gesture1ImageId: 0,
            gesture2Id: id(1), gesture2ImageId: 0,
            gesture3Id: id(2), gesture3ImageId: 0,
            gesture4Id: id(3), gesture4ImageId: 0,
            gesture5Id: id(4), gesture5ImageId: 0,
            gesture6Id: id(5), gesture6ImageId: 0,
            gesture7Id: id(6), gesture7ImageId: 0,
            gesture8Id: id(7), gesture8ImageId: 0
        )
    }
}
private extension GestureListItemViewModel {
    static func makeGestureNames() -> [String] {
        return (1...14).map { index in
            String(format: NSLocalizedString("Gesture %d", comment: ""), index)
        }
    }
}
private enum ParameterCode {
    static let selectGesture = 1
    static let gestureSettings = 31
    static let gestureGroup = 32
    static let bindingGroup = 43
}
