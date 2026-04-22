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
    private let isV3Widget: Bool
    private let bindings: [WidgetV3BindingInfo]
    
    private let openCustomGestureSettings: ((Int, Bool) -> Void)?

    init(widget: Widget, bleManager: BleManagerKmm, openCustomGestureSettings: ((Int, Bool) -> Void)? = nil) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.gestureNameList = GestureListItemViewModel.makeGestureNames()
        self.isV3Widget = WidgetV3Support.isV3Widget(widget)
        self.bindings = WidgetV3Support.bindings(from: widget)
        
        self.openCustomGestureSettings = openCustomGestureSettings

        
        if let baseStruct = WidgetMetadataExtractor.extractBaseStruct(from: widget.widget) {
            self.parameterInfoSet = ParameterInfoData.makeSet(from: baseStruct.parameterInfoSet)
        } else {
            self.parameterInfoSet = []
        }
    }
}

extension GestureListItemViewModel {
    static func sendFestData(data: KotlinByteArray, bleManager: BleManagerKmm,) {
        let gatt = SampleGattAttributes()
        print("[BLE-COMMUNICATION] sendDataToFest data: \(data.hex)")
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }
    
    func makeProvider() -> GesturesProvider {
        let factory = GestureCatalog.factoryGestures
        let custom = GestureCatalog.customGestures(withTitles: gestureNameList)
        let rotation: [GestureCatalog.GestureItem] = []
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

    func selectRotationGesture(_ item: GesturesProvider.GestureDisplayItem, provider: GesturesProvider) {
        provider.activeGestureId = item.id
        provider.activeGestureTitle = item.title
        sendActiveGesture(gestureId: item.id)
    }

    func openGestureSettings(for item: GesturesProvider.GestureDisplayItem) {
        _ = GestureSettingsViewModel.shared
        if isV3Widget {
            _ = GestureSettingsViewModelV3.shared
        } else {
            requestGestureSettings(gestureId: item.id)
        }
        openCustomGestureSettings?(item.id, isV3Widget)
    }

    func removeRotationGesture(at index: Int, provider: GesturesProvider) {
        print("Rotation removeRotationGesture")
        guard provider.rotationGroup.indices.contains(index) else { return }
        provider.rotationGroup.remove(at: index)
        sendRotationGroup(with: provider.rotationGroup)
    }

    func updateRotationGestures(_ gestures: [GesturesProvider.GestureDisplayItem], provider: GesturesProvider) {
        print("Rotation updateRotationGestures")
        provider.rotationGroup = gestures
        sendRotationGroup(with: provider.rotationGroup)
    }
    
    func rotationGroup(from parameterData: String, provider: GesturesProvider) -> [GesturesProvider.GestureDisplayItem] {
        let gestureIds = rotationGroupIds(from: parameterData)
        let catalog = GestureCatalog.factoryGestures + GestureCatalog.customGestures(withTitles: gestureNameList)

        return gestureIds.compactMap { id in
            guard id != 0,
                  let gesture = catalog.first(where: { $0.id == id }) else { return nil }

            return GesturesProvider.GestureDisplayItem(
                id: gesture.id,
                title: gesture.title,
                subtitle: gesture.subtitle,
                image: gesture.image
            )
        }
    }
    func bindingGroup(from parameterData: String) -> [GesturesProvider.SprGestureDisplayItem] {
        let sanitized = parameterData.trimmingCharacters(in: .whitespacesAndNewlines)
        var items: [GesturesProvider.SprGestureDisplayItem] = []

        let sprCatalog = SprGesturesCatalog.all
        let gestureCatalog = GestureCatalog.factoryGestures + GestureCatalog.customGestures(withTitles: gestureNameList)

        stride(from: 0, to: min(sanitized.count, 48), by: 4).forEach { offset in
            let sprStart = sanitized.index(sanitized.startIndex, offsetBy: offset)
            guard let sprEnd = sanitized.index(sprStart, offsetBy: 2, limitedBy: sanitized.endIndex),
                  let boundEnd = sanitized.index(sprEnd, offsetBy: 2, limitedBy: sanitized.endIndex) else { return }

            let sprIdHex = sanitized[sprStart..<sprEnd]
            let boundIdHex = sanitized[sprEnd..<boundEnd]

            guard let sprId = Int(sprIdHex, radix: 16), sprId != 0 else { return }
            guard let sprGesture = sprCatalog.first(where: { $0.id == sprId }) else { return }

            let boundGestureId = Int(boundIdHex, radix: 16) ?? 0
            let boundGestureTitle = gestureCatalog.first(where: { $0.id == boundGestureId })?.title

            let displayItem = GesturesProvider.SprGestureDisplayItem(
                id: sprGesture.id,
                title: sprGesture.title,
                subtitle: boundGestureTitle,
                boundGestureId: boundGestureId == 0 ? nil : boundGestureId
            )

            items.append(displayItem)
        }

        return items
    }

    func requestRotationGroup() {
        print("Rotation requestRotationGroup")
        if isV3Widget {
            // Android parity: BLECommandsV3.request(PWCE_GET_GESTURE_GROUPE)
            let data = BLECommandsV3.shared.request(subcommand: Int32(ParameterCode.gestureGroupV3Get))
            sendBytes(data, useV3Channel: true)
            return
        }

        let parameterID = parameterID(forAnyDataCode: [ParameterCode.gestureGroupLegacy])
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestRotationGroup(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID)
        )
        sendBytes(data, useV3Channel: false)
    }
    
    private func requestGestureSettings(gestureId: Int) {
        if isV3Widget {
            let data = BLECommandsV3.shared.requestGestureInfo(gestureId: Int32(gestureId))
            sendBytes(data, useV3Channel: true)
            return
        }

        let parameterID = parameterID(forAnyDataCode: [ParameterCode.gestureSettingsLegacy])
        print("requestGestureSettings deviceAddress: \(widget.deviceAddress)    parameterID: \(parameterID)    gestureId: \(gestureId)")
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestGestureInfo(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            gestureId: Int32(gestureId)
        )
        sendBytes(data, useV3Channel: false)
    }

    private func sendRotationGroup(with gestures: [GesturesProvider.GestureDisplayItem]) {
        print("sendBytes sendRotationGroup gestures: \(gestures)")
        let rotationGroup = RotationGroup.make(from: gestures)
        print("sendBytes sendRotationGroup rotationGroup: \(rotationGroup)")
        if isV3Widget {
            let data = BLECommandsV3.shared.sendRotationGroup(rotationGroup: rotationGroup)
            sendBytes(data, useV3Channel: true)
            return
        }

        let parameterID = parameterID(forAnyDataCode: [ParameterCode.gestureGroupLegacy])
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.sendRotationGroupInfo(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            rotationGroup: rotationGroup
        )
        sendBytes(data, useV3Channel: false)
    }
    
    private func rotationGroupIds(from parameterData: String) -> [Int] {
        let sanitized = parameterData.trimmingCharacters(in: .whitespacesAndNewlines)
        var ids: [Int] = []

        stride(from: 0, to: min(sanitized.count, 32), by: 4).forEach { offset in
            let idStart = sanitized.index(sanitized.startIndex, offsetBy: offset)
            guard let idEnd = sanitized.index(idStart, offsetBy: 2, limitedBy: sanitized.endIndex) else { return }

            let idSubstring = sanitized[idStart..<idEnd]
            if let id = Int(idSubstring, radix: 16) {
                ids.append(id)
            }
        }

        return ids
    }

    func requestBindingGroup() {
        print("Binding requestBindingGroup")
        let parameterID = parameterID(forAnyDataCode: [ParameterCode.bindingGroupLegacy])
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestBindingGroup(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID)
        )
        sendBytes(data, useV3Channel: false)
    }
    
    func requestActiveGesture() {
        if isV3Widget {
            // Android parity: BLECommandsV3.request(PWCE_GET_CURRENT_GESTURE_NUM)
            let data = BLECommandsV3.shared.request(subcommand: Int32(ParameterCode.selectGestureV3Get))
            sendBytes(data, useV3Channel: true)
            return
        }

        let parameterID = parameterID(forAnyDataCode: [ParameterCode.selectGestureLegacy])
        print("ActiveGesture requestActiveGesture deviceAddress: \(widget.deviceAddress) parameterID: \(parameterID)")
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.requestActiveGesture(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID)
        )
        sendBytes(data, useV3Channel: false)
    }
    
    func updateBindingGroup(provider: GesturesProvider) {
        print("Binding updateBindingGroup")
        sendBindingGroup(with: provider.sprGestures)
    }

    private func sendBindingGroup(with gestures: [GesturesProvider.SprGestureDisplayItem]) {
        print("Binding sendBindingGroup")
        let bindingGroup = BindingGestureGroup.make(from: gestures)
        let parameterID = parameterID(forAnyDataCode: [ParameterCode.bindingGroupLegacy])
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.sendBindingGroupInfo(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            bindingGestureGroup: bindingGroup
        )
        sendBytes(data, useV3Channel: false)
    }

    private func sendActiveGesture(gestureId: Int) {
        print("sendBytes sendActiveGesture")
        if isV3Widget {
            // Android parity: BLECommandsV3.sendSubcommand(PWCE_SET_CURRENT_GESTURE_NUM, activeGesture)
            let data = BLECommandsV3.shared.sendSubcommand(
                subcommand: Int32(ParameterCode.selectGestureV3Set),
                parameter: Int32(gestureId)
            )
            sendBytes(data, useV3Channel: true)
            return
        }

        let parameterID = parameterID(forAnyDataCode: [ParameterCode.selectGestureLegacy])
        guard parameterID != 0 else { return }
        let data = BLECommands.shared.sendActiveGesture(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(parameterID),
            activeGesture: Int32(gestureId)
        )
        sendBytes(data, useV3Channel: false)
    }
    
    private func parameterID(forAnyDataCode dataCodes: [Int]) -> Int {
        parameterInfoSet.first(where: { dataCodes.contains($0.dataCode) })?.parameterID ?? 0
    }

    private func v3Binding(for dataCode: Int) -> WidgetV3BindingInfo? {
        bindings.first(where: { $0.dataCode == dataCode })
    }

    private func v3Binding(forAnyDataCode dataCodes: [Int]) -> WidgetV3BindingInfo? {
        bindings.first(where: { dataCodes.contains($0.dataCode) })
    }

    private func sendBytes(_ data: KotlinByteArray, useV3Channel: Bool) {
        let gatt = SampleGattAttributes()
        let command = useV3Channel ? gatt.SERIALPORTCHAR_UUID : gatt.MAIN_CHANNEL_CHARACTERISTIC
        print("sendBytes to \(data)")
        bleManager.sendBytesKmm(
            data: data,
            command: command,
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
    
    func contains(ref: ParameterRef) -> Bool {
        parameterInfoSet.contains { info in
            info.parameterID == ref.parameterID &&
            info.deviceAddress == ref.addressDevice
        }
    }

    func matchesActiveGesture(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard let binding = v3Binding(forAnyDataCode: [
            ParameterCode.selectGestureV3Get,
            ParameterCode.selectGestureV3Set
        ]) else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func matchesRotationGroup(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard let binding = v3Binding(forAnyDataCode: [
            ParameterCode.gestureGroupV3Get,
            ParameterCode.gestureGroupV3Set
        ]) else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func activeGestureId(from snapshot: ParameterSnapshotV3Bridge) -> Int? {
        guard snapshot.codecId == "CURRENT_GESTURE" else { return nil }
        return V3SnapshotParser.intField(from: snapshot.serializedValue, field: "currentGesture")
    }

    func rotationGroup(from snapshot: ParameterSnapshotV3Bridge, provider: GesturesProvider) -> [GesturesProvider.GestureDisplayItem]? {
        guard snapshot.codecId == "ROTATION_GROUP" else { return nil }
        let gestureIds = rotationGroupIds(fromSerializedSnapshot: snapshot.serializedValue)
        return buildRotationGroupItems(gestureIds: gestureIds)
    }

    func currentActiveGestureId() -> Int? {
        guard let binding = v3Binding(forAnyDataCode: [
            ParameterCode.selectGestureV3Get,
            ParameterCode.selectGestureV3Set
        ]) else { return nil }
        guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return nil }
        return activeGestureId(from: snapshot)
    }

    func currentRotationGroup(provider: GesturesProvider) -> [GesturesProvider.GestureDisplayItem]? {
        guard let binding = v3Binding(forAnyDataCode: [
            ParameterCode.gestureGroupV3Get,
            ParameterCode.gestureGroupV3Set
        ]) else { return nil }
        guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return nil }
        return rotationGroup(from: snapshot, provider: provider)
    }

    private func rotationGroupIds(fromSerializedSnapshot serialized: String) -> [Int] {
        guard
            let data = serialized.data(using: .utf8),
            let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return [] }

        return (1...8).compactMap { index in
            guard let value = json["gesture\(index)Id"] as? NSNumber else { return nil }
            return value.intValue
        }
    }

    private func buildRotationGroupItems(gestureIds: [Int]) -> [GesturesProvider.GestureDisplayItem] {
        let catalog = GestureCatalog.factoryGestures + GestureCatalog.customGestures(withTitles: gestureNameList)
        return gestureIds.compactMap { id in
            guard id != 0,
                  let gesture = catalog.first(where: { $0.id == id })
            else { return nil }

            return GesturesProvider.GestureDisplayItem(
                id: gesture.id,
                title: gesture.title,
                subtitle: gesture.subtitle,
                image: gesture.image
            )
        }
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
            title: SharedRes.strings().fist.desc().localized(),
            image: SharedRes.images().collection_fist_1.toUIImage()
        ),
        .init(
            id: 2,
            title: SharedRes.strings().gesture_point.desc().localized(),
            image: SharedRes.images().collection_point.toUIImage()
        ),
        .init(
            id: 3,
            title: SharedRes.strings().gesture_pinch.desc().localized(),
            image: SharedRes.images().collection_pinch.toUIImage()
        ),
        .init(
            id: 4,
            title: SharedRes.strings().gesture_fist_thumb_over.desc().localized(),
            image: SharedRes.images().collection_fist_2.toUIImage()
        ),
        .init(
            id: 5,
            title: SharedRes.strings().gesture_key.desc().localized(),
            image: SharedRes.images().collection_key.toUIImage()
        ),
        .init(
            id: 6,
            title: SharedRes.strings().gesture_rock.desc().localized(),
            image: SharedRes.images().collection_rock.toUIImage()
        ),
        .init(
            id: 7,
            title: SharedRes.strings().gesture_twizzers.desc().localized(),
            image: SharedRes.images().collection_twizzers.toUIImage()
        ),
        .init(
            id: 8,
            title: SharedRes.strings().gesture_cupholder.desc().localized(),
            image: SharedRes.images().collection_cupholder.toUIImage()
        ),
        .init(
            id: 9,
            title: SharedRes.strings().gesture_half_grab.desc().localized(),
            image: SharedRes.images().collect_half_grab.toUIImage()
        ),
        .init(
            id: 10,
            title: SharedRes.strings().gesture_ok.desc().localized(),
            image: SharedRes.images().collection_ok.toUIImage()
        ),
        .init(
            id: 11,
            title: SharedRes.strings().gesture_thumb_up.desc().localized(),
            image: SharedRes.images().collection_thumb_up.toUIImage()
        ),
//        .init(
//            id: 12,
//            title: SharedRes.strings().gesture_middle_finger.desc().localized(),
//            image: SharedRes.images().collection_middle_finger.toUIImage()
//        ),
        .init(
            id: 13,
            title: SharedRes.strings().gesture_double_point.desc().localized(),
            image: SharedRes.images().collection_double_point.toUIImage()
        ),
        .init(
            id: 14,
            title: SharedRes.strings().gesture_call_me.desc().localized(),
            image: SharedRes.images().collection_call_me.toUIImage()
        ),
        .init(
            id: 15,
            title: SharedRes.strings().gesture_natural_position.desc().localized(),
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
            gesture1Id: id(0), gesture1ImageId: id(0),
            gesture2Id: id(1), gesture2ImageId: id(1),
            gesture3Id: id(2), gesture3ImageId: id(2),
            gesture4Id: id(3), gesture4ImageId: id(3),
            gesture5Id: id(4), gesture5ImageId: id(4),
            gesture6Id: id(5), gesture6ImageId: id(5),
            gesture7Id: id(6), gesture7ImageId: id(6),
            gesture8Id: id(7), gesture8ImageId: id(7)
        )
    }
}
private extension BindingGestureGroup {
    static func make(from gestures: [GesturesProvider.SprGestureDisplayItem]) -> BindingGestureGroup {
        func sprId(_ index: Int) -> Int32 {
            guard gestures.indices.contains(index) else { return 0 }
            return Int32(gestures[index].id)
        }

        func boundGestureId(_ index: Int) -> Int32 {
            guard gestures.indices.contains(index), let boundGestureId = gestures[index].boundGestureId else { return 0 }
            return Int32(boundGestureId)
        }

        return BindingGestureGroup(
            gestureSpr1Id: sprId(0), gesture1Id: boundGestureId(0),
            gestureSpr2Id: sprId(1), gesture2Id: boundGestureId(1),
            gestureSpr3Id: sprId(2), gesture3Id: boundGestureId(2),
            gestureSpr4Id: sprId(3), gesture4Id: boundGestureId(3),
            gestureSpr5Id: sprId(4), gesture5Id: boundGestureId(4),
            gestureSpr6Id: sprId(5), gesture6Id: boundGestureId(5),
            gestureSpr7Id: sprId(6), gesture7Id: boundGestureId(6),
            gestureSpr8Id: sprId(7), gesture8Id: boundGestureId(7),
            gestureSpr9Id: sprId(8), gesture9Id: boundGestureId(8),
            gestureSpr10Id: sprId(9), gesture10Id: boundGestureId(9),
            gestureSpr11Id: sprId(10), gesture11Id: boundGestureId(10),
            gestureSpr12Id: sprId(11), gesture12Id: boundGestureId(11)
        )
    }
}
enum SprGesturesCatalog {
    static let all: [SprGestureSelectionOption] = [
        .init(id: 1, title: SharedRes.strings().thumb_finger.desc().localized()),
        .init(id: 2, title: SharedRes.strings().flexion.desc().localized()),
        .init(id: 3, title: SharedRes.strings().extension.desc().localized()),
        .init(id: 4, title: SharedRes.strings().palm_closing.desc().localized()),
        .init(id: 5, title: SharedRes.strings().palm_opening.desc().localized()),
        .init(id: 6, title: SharedRes.strings().ok_pinch.desc().localized()),
        .init(id: 7, title: SharedRes.strings().pistol_pointer_gesture.desc().localized()),
        .init(id: 8, title: SharedRes.strings().gesture_key.desc().localized()),
        .init(id: 9, title: SharedRes.strings().adduction.desc().localized()),
        .init(id: 10, title: SharedRes.strings().abduction.desc().localized()),
        .init(id: 11, title: SharedRes.strings().pronation.desc().localized()),
        .init(id: 12, title: SharedRes.strings().supination.desc().localized())
    ]
}
struct SprGestureSelectionOption: Identifiable, Hashable {
    let id: Int
    let title: String
}
private extension GestureListItemViewModel {
    static func makeGestureNames() -> [String] {
        GestureService.shared.loadNames()
    }
}
private enum ParameterCode {
    static let selectGestureLegacy = 0x01
    static let gestureSettingsLegacy = 0x1F
    static let gestureGroupLegacy = 0x20
    static let bindingGroupLegacy = 0x2B

    static let selectGestureV3Get = 0x24
    static let selectGestureV3Set = 0x25
    static let gestureSettingsV3 = 0x27
    static let gestureGroupV3Get = 0x35
    static let gestureGroupV3Set = 0x36
}
