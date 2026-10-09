import UIKit
import SwiftUI
import shared

final class WidgetsSceneDIContainer {
    struct Dependencies {
        let apiDataTransferService: DataTransferService
        let imageDataTransferService: DataTransferService
        let bleManager: BleManagerKmm
    }
    
    private let dependencies: Dependencies
    private let requestAccountStatisticsUseCaseV3: RequestAccountStatisticsUseCaseV3
    private let observeAccountStatisticsUseCaseV3: ObserveAccountStatisticsUseCaseV3
    private let getCustomGestureNamesUseCaseV3: GetCustomGestureNamesUseCaseV3
    private let getDeviceSessionUseCaseV3: GetDeviceSessionUseCaseV3

    // MARK: - Persistent Storage
    lazy var widgetsQueriesStorage: WidgetsQueriesStorage = CoreDataWidgetsQueriesStorage(maxStorageLimit: 10)
    lazy var widgetsResponseCache: WidgetsResponseStorage = CoreDataWidgetsResponseStorage()


    init(
        dependencies: Dependencies,
        requestAccountStatisticsUseCaseV3: RequestAccountStatisticsUseCaseV3,
        observeAccountStatisticsUseCaseV3: ObserveAccountStatisticsUseCaseV3,
        getCustomGestureNamesUseCaseV3: GetCustomGestureNamesUseCaseV3,
        getDeviceSessionUseCaseV3: GetDeviceSessionUseCaseV3
    ) {
        self.dependencies = dependencies
        self.requestAccountStatisticsUseCaseV3 = requestAccountStatisticsUseCaseV3
        self.observeAccountStatisticsUseCaseV3 = observeAccountStatisticsUseCaseV3
        self.getCustomGestureNamesUseCaseV3 = getCustomGestureNamesUseCaseV3
        self.getDeviceSessionUseCaseV3 = getDeviceSessionUseCaseV3
    }
    
    // MARK: - Use Cases
    private lazy var autoLoginRepositoryV3 = V3AutoLoginSettingsRepositoryImpl()
    private lazy var getAutoLoginEnabledUseCaseV3 = GetAutoLoginEnabledUseCaseV3(repository: autoLoginRepositoryV3)
    private lazy var setAutoLoginEnabledUseCaseV3 = SetAutoLoginEnabledUseCaseV3(repository: autoLoginRepositoryV3, skipUnchanged: false)
    private lazy var enqueueV3Packet: (KotlinByteArray) -> Void = { [bleManager = dependencies.bleManager] packet in
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: packet,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }

    private lazy var deviceSettingsRepositoryV3 = V3DeviceSettingsRepositoryImpl(
        enqueuePacket: enqueueV3Packet,
        saveBleValue: { parameterInfo, value in
            SettingsProfileManager.shared.saveBleValue(parameterInfo: parameterInfo, typedValue: value)
        },
        // iOS keeps an unknown value until a device response populates the typed store.
        readCachedValues: false,
        // iOS waits for the device response before updating the store, cache and profile.
        saveValueBeforeSending: false,
        // Each EMG command contains both gains; preserve the other slider's queued edit.
        retainEmgGainDrafts: true,
        beforeSpinnerValueSent: { parameterInfo, value in
            guard parameterInfo.parameterID?.intValue == 0x10,
                  parameterInfo.dataCode?.intValue == 0x0E,
                  parameterInfo.deviceAddress?.intValue == 1,
                  parameterInfo.dataOffsets?.intValue == 0 else { return }
            let index = Int(value.intValue)
            NSLog("[V3HandSide] source=widget selectedIndex=%d address=%d parameter=0x%02X dataCode=0x%02X",
                  index, 1, 0x10, 0x0E)
            // Preserve the native hand-side store update and 3D notification before queueing SET.
            V3HandSideProvider.shared.applyWidgetValue(index)
        }
    )

    private lazy var getSliderSettingsUseCaseV3 = GetSliderSettingsUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var observeSliderResponsesUseCaseV3 = ObserveSliderResponsesUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var requestSliderValueUseCaseV3 = RequestSliderValueUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var setSliderValueUseCaseV3 = SetSliderValueUseCaseV3(
        repository: deviceSettingsRepositoryV3,
        // Preserve the existing iOS queueing of commands while disconnected.
        requireInteractionEnabled: false
    )

    private lazy var getToggleSliderSettingsUseCaseV3 = GetToggleSliderSettingsUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var requestToggleSliderValueUseCaseV3 = RequestToggleSliderValueUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var sendToggleSliderValueUseCaseV3 = SendToggleSliderValueUseCaseV3(
        repository: deviceSettingsRepositoryV3,
        requireInteractionEnabled: false
    )

    private lazy var getSpinnerSettingsUseCaseV3 = GetSpinnerSettingsUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var requestSpinnerValueUseCaseV3 = RequestSpinnerValueUseCaseV3(
        repository: deviceSettingsRepositoryV3
    )

    private lazy var setSpinnerValueUseCaseV3 = SetSpinnerValueUseCaseV3(
        repository: deviceSettingsRepositoryV3,
        requireInteractionEnabled: false,
        allowedValues: { key in
            // iOS sends the row index for INDY3 profiles, including its existing "+Add" row.
            key == V3ParameterKeys.shared.P_KEY_SETTINGS_PROFILE
                ? KotlinIntRange(start: Int32.min, endInclusive: Int32.max)
                : V3SpinnerSettingsRules.shared.allowedValues(parameterKey: key)
        }
    )

    private lazy var deviceRoleRepositoryV3 = V3DeviceRoleRepositoryImpl(
        readSavedRole: { KotlinInt(int: Int32(UserFirmwareRoleAccess.selectedRole)) },
        saveRole: { value in UserDefaults.standard.set(Int(value.intValue), forKey: UserFirmwareRoleAccess.key) },
        settings: deviceSettingsRepositoryV3,
        allowProsthetist: true,
        updateSharedAccess: false
    )
    private lazy var getDeviceRoleUseCaseV3 = GetDeviceRoleUseCaseV3(repository: deviceRoleRepositoryV3)
    private lazy var changeDeviceRoleUseCaseV3 = ChangeDeviceRoleUseCaseV3(
        repository: deviceRoleRepositoryV3,
        requireInteractionEnabled: false,
        skipUnchanged: false
    )

    private lazy var sensorsCommandsRepositoryV3 = V3SensorsCommandsRepositoryImpl(
        enqueuePacket: enqueueV3Packet,
        refreshWidgets: { [bleManager = dependencies.bleManager] in
            bleManager.restartV3Synchronization()
        },
        validateCommandDeviceContext: false
    )

    private lazy var startProsthesisMovementUseCaseV3 = StartProsthesisMovementUseCaseV3(
        repository: sensorsCommandsRepositoryV3,
        requireInteractionEnabled: false
    )
    private lazy var stopProsthesisMovementUseCaseV3 = StopProsthesisMovementUseCaseV3(
        repository: sensorsCommandsRepositoryV3
    )

    private lazy var calibrationRepositoryV3 = V3ProsthesisCalibrationRepositoryImpl(
        enqueuePacket: enqueueV3Packet,
        validateCommandDeviceContext: false
    )
    private lazy var startProsthesisCalibrationUseCaseV3 = StartProsthesisCalibrationUseCaseV3(
        repository: calibrationRepositoryV3,
        requireInteractionEnabled: false
    )
    private lazy var releaseProsthesisCalibrationButtonUseCaseV3 = ReleaseProsthesisCalibrationButtonUseCaseV3(
        repository: calibrationRepositoryV3
    )

    private let deviceNameStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private lazy var deviceInfoRepositoryV3 = V3DeviceInfoRepositoryImpl(
        readDeviceNameInput: { [storage = deviceNameStorage] in
            (try? storage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)) ?? ""
        },
        enqueuePacket: { [bleManager = dependencies.bleManager] packet, completion in
            let gatt = SampleGattAttributes()
            bleManager.sendBytesKmm(
                data: packet,
                command: gatt.SERIALPORTCHAR_UUID,
                typeCommand: gatt.WRITE,
                onChunkSent: { _ = completion() }
            )
        },
        onDeviceNameQueued: { [storage = deviceNameStorage] transportText in
            try? storage.save(transportText, for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
            NotificationCenter.default.post(
                name: .v3DeviceNameDidUpdate,
                object: DeviceNameBridgeV3.shared.displayName(deviceName: transportText)
            )
        }
    )
    private lazy var getDeviceInfoTextUseCaseV3 = GetDeviceInfoTextUseCaseV3(repository: deviceInfoRepositoryV3)
    private lazy var observeMainStatusUseCaseV3 = ObserveMainStatusUseCaseV3(repository: V3MainStatusRepositoryImpl())

    private func makeStatusBarViewModelV3(output: StatusBarViewModel) -> V3StatusBarViewModel {
        V3StatusBarViewModel(output: output, getDeviceInfoText: getDeviceInfoTextUseCaseV3,
                             observeMainStatus: observeMainStatusUseCaseV3)
    }
    private lazy var editDeviceInfoTextUseCaseV3 = EditDeviceInfoTextUseCaseV3(
        trimDeviceName: TextInputListItemViewModelV3.trimDeviceName
    )
    private lazy var setDeviceInfoTextUseCaseV3 = SetDeviceInfoTextUseCaseV3(
        repository: deviceInfoRepositoryV3,
        requireInteractionEnabled: false
    )

    static func makeGestureSettingsViewModelV3() -> GestureSettingsViewModelV3 {
        let repository = V3GestureEditorRepositoryImpl(
            readSavedHandSide: { KotlinInt(int: Int32(V3HandSideProvider.shared.currentSide)) },
            subscribeSettingsUpdates: { callback in
                let token = NotificationCenter.default.addObserver(
                    forName: .gestureSettingsDidUpdateV3, object: nil, queue: nil
                ) { notification in
                    guard let info = notification.userInfo?["dataV3"] as? ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject>,
                          let parameterID = gestureSettingsIntValue(from: info.parameterID),
                          let dataCode = gestureSettingsIntValue(from: info.dataCode),
                          let addressDevice = gestureSettingsIntValue(from: info.deviceAddress) else {
                        return
                    }
                    _ = callback(ParameterInfo(
                        parameterID: KotlinInt(int: Int32(parameterID)),
                        dataCode: KotlinInt(int: Int32(dataCode)),
                        deviceAddress: KotlinInt(int: Int32(addressDevice)),
                        dataOffsets: info.dataOffsets as? KotlinInt
                    ))
                }
                return {
                    NotificationCenter.default.removeObserver(token)
                    return KotlinUnit()
                }
            },
            enqueuePacket: { packet in GestureService.shared.sendDataToFestV3(dataForWrite: packet) },
            saveProfile: { info, value in
                SettingsProfileManager.shared.saveBleValue(parameterInfo: info, typedValue: value)
            },
            // iOS keeps runtime edits in the renderer and waits for RX before persistence.
            saveSettingsBeforeSending: false,
            // The native source preserves unknown (-1) and model-test priority over stored values.
            readTypedHandSideFirst: false
        )
        return GestureSettingsViewModelV3(
            requestGestureSettingsUseCase: RequestGestureSettingsUseCaseV3(repository: repository),
            writeGestureSettingsUseCase: WriteGestureSettingsUseCaseV3(repository: repository, clampPositions: false),
            observeGestureSettingsUseCase: ObserveGestureSettingsUseCaseV3(repository: repository),
            getGestureEditorHandSideUseCase: GetGestureEditorHandSideUseCaseV3(repository: repository),
            getCustomGestureNamesUseCase: GetCustomGestureNamesUseCaseV3(repository: V3CustomGestureNamesRepositoryImpl.shared),
            renameCustomGestureUseCase: RenameCustomGestureUseCaseV3(repository: V3CustomGestureNamesRepositoryImpl.shared)
        )
    }

    private static func makeGestureViewModel(
        widget: Widget,
        bleManager: BleManagerKmm,
        enqueuePacket: @escaping (KotlinByteArray) -> Void,
        openSettings: ((Int, Bool) -> Void)?
    ) -> GestureListItemViewModel {
        let target = GestureListItemViewModel.activeGestureTarget(for: widget)
        let rotationTarget = GestureListItemViewModel.rotationGroupTarget(for: widget)
        let preferencesRepository = V3GesturesPreferencesRepositoryImpl(
            readUiTestRotationMode: { GesturesProvider.isUiTestDefaultRotationEnabled }
        )
        let repository = V3GesturesRepositoryImpl(
            enqueuePacket: enqueuePacket,
            saveBleValue: { parameterInfo, value in
                SettingsProfileManager.shared.saveBleValue(parameterInfo: parameterInfo, typedValue: value)
            },
            // Preserve provider-only optimism, offline commands and the widget's exact RX binding.
            saveActiveGestureBeforeSending: false,
            validateActiveGestureDeviceContext: false,
            useActiveGestureSnapshots: true,
            activeGestureTarget: target.map {
                ParameterRef(addressDevice: Int32($0.deviceAddress), parameterID: Int32($0.parameterID), dataCode: Int32($0.dataCode))
            },
            // iOS queues one rotation packet and keeps edits in the provider until RX.
            saveRotationGroupBeforeSending: false,
            validateRotationGroupBeforeSending: false,
            observeRotationGroupSnapshots: true,
            rotationGroupTarget: rotationTarget.map {
                ParameterRef(addressDevice: Int32($0.deviceAddress), parameterID: Int32($0.parameterID), dataCode: Int32($0.dataCode))
            }
        )
        return GestureListItemViewModel(
            widget: widget,
            bleManager: bleManager,
            gesturesViewModel: V3GesturesViewModel(
                getActiveGestureUseCase: GetActiveGestureUseCaseV3(repository: repository),
                observeGesturesChangesUseCase: ObserveGesturesChangesUseCaseV3(repository: repository),
                requestActiveGestureUseCase: RequestActiveGestureUseCaseV3(repository: repository, requireInteractionEnabled: false),
                selectGestureUseCase: SelectGestureUseCaseV3(repository: repository, requireInteractionEnabled: false, validateGestureId: false),
                requestRotationGroupUseCase: RequestRotationGroupUseCaseV3(repository: repository, requireInteractionEnabled: false),
                setRotationGroupUseCase: SetRotationGroupUseCaseV3(repository: repository, requireInteractionEnabled: false),
                getGesturesPreferencesUseCase: GetGesturesPreferencesUseCaseV3(repository: preferencesRepository),
                setGesturesSectionUseCase: SetGesturesSectionUseCaseV3(repository: preferencesRepository),
                setFactoryGestureCollectionExpandedUseCase: SetFactoryGestureCollectionExpandedUseCaseV3(repository: preferencesRepository)
            ),
            openCustomGestureSettings: openSettings
        )
    }

    private static func makePlotViewModel(
        widget: Widget, enqueuePacket: @escaping (KotlinByteArray) -> Void
    ) -> PlotListItemViewModelV3 {
        let configuration = PlotListItemViewModelV3.thresholdConfiguration(for: widget)
        let repository = V3SensorsPlotRepositoryImpl(
            enqueuePacket: enqueuePacket,
            saveBleValue: { parameterInfo, value in
                SettingsProfileManager.shared.saveBleValue(parameterInfo: parameterInfo, typedValue: value)
            },
            readThresholds: {
                // Preserve iOS typed snapshots: aliases, missing fields and no serialized cache fallback.
                guard let target = configuration.target else { return nil }
                for code in PlotListItemViewModelV3.thresholdDataCodeCandidates(for: target.dataCode) {
                    if let snapshot = WidgetStateBridgeV3.shared.getCurrent(
                        addressDevice: Int32(target.deviceAddress),
                        parameterID: Int32(target.parameterID),
                        dataCode: Int32(code)
                    ) {
                        guard let open = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openThreshold"),
                              let close = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeThreshold") else { return nil }
                        return V3PlotThresholds(open: Int32(clamping: open), close: Int32(clamping: close))
                    }
                }
                return nil
            },
            saveValueAfterSending: false,
            sampleTargets: configuration.parameters.map {
                ParameterRef(addressDevice: Int32($0.deviceAddress), parameterID: Int32($0.parameterID), dataCode: Int32($0.dataCode))
            },
            observeThresholdSnapshots: true,
            thresholdTarget: configuration.target.map {
                ParameterRef(addressDevice: Int32($0.deviceAddress), parameterID: Int32($0.parameterID), dataCode: Int32($0.dataCode))
            }
        )
        return PlotListItemViewModelV3(
            widget: widget,
            thresholdConfiguration: configuration,
            getPlotSettingsUseCase: GetPlotSettingsUseCaseV3(repository: repository),
            requestPlotThresholdsUseCase: RequestPlotThresholdsUseCaseV3(repository: repository),
            observePlotThresholdsUseCase: ObservePlotThresholdsUseCaseV3(repository: repository),
            editPlotThresholdUseCase: EditPlotThresholdUseCaseV3(),
            setPlotThresholdsUseCase: SetPlotThresholdsUseCaseV3(
                repository: repository, requireInteractionEnabled: false
            ),
            observePlotSamplesUseCase: ObservePlotSamplesUseCaseV3(repository: repository)
        )
    }

    func makeSearchWidgetsUseCase() -> SearchWidgetsUseCase {
        DefaultSearchWidgetsUseCase(
            widgetsRepository: makeWidgetsRepository(),
            widgetsQueriesRepository: makeWidgetsQueriesRepository()
        )
    }
    
    func makeFetchRecentWidgetQueriesUseCase(
        requestValue: FetchRecentWidgetQueriesUseCase.RequestValue,
        completion: @escaping (FetchRecentWidgetQueriesUseCase.ResultValue) -> Void
    ) -> UseCase {
        FetchRecentWidgetQueriesUseCase(
            requestValue: requestValue,
            completion: completion,
            widgetsQueriesRepository: makeWidgetsQueriesRepository()
        )
    }
    
    // MARK: - Repositories
    func makeWidgetsRepository() -> WidgetsRepository {
        DefaultWidgetsRepository(
            dataTransferService: dependencies.apiDataTransferService,
            cache: widgetsResponseCache
        )
    }
    
    func makeWidgetsQueriesRepository() -> WidgetsQueriesRepository {
        DefaultWidgetsQueriesRepository(
            widgetsQueriesPersistentStorage: widgetsQueriesStorage
        )
    }
    
    // MARK: - Widgets List
    func makeWidgetsListViewController(
        actions: WidgetsListViewModelActions,
        screenTitle: String? = nil
    ) -> WidgetsListViewController {
        WidgetsListViewController.create(
            with: makeWidgetsListViewModel(actions: actions)
        )
    }
    
    func makeGesturesTabViewController(actions: WidgetsListViewModelActions, makeAccountScreen: @escaping () -> AccountViewController) -> GesturesTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 0
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().title_home)
        return GesturesTabViewController(contentViewController: controller, makeAccountScreen: makeAccountScreen,
                                         makeStatusBarViewModelV3: makeStatusBarViewModelV3)
    }

    func makeSensorsTabViewController(actions: WidgetsListViewModelActions, makeAccountScreen: @escaping () -> AccountViewController) -> SensorsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 1
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().title_dashboard)
        return SensorsTabViewController(contentViewController: controller, makeAccountScreen: makeAccountScreen,
                                        makeStatusBarViewModelV3: makeStatusBarViewModelV3)
    }

    func makeTrainingTabViewController(actions: WidgetsListViewModelActions, makeAccountScreen: @escaping () -> AccountViewController) -> TrainingTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 3
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().training)
        return TrainingTabViewController(contentViewController: controller, makeAccountScreen: makeAccountScreen,
                                         makeStatusBarViewModelV3: makeStatusBarViewModelV3)
    }

    func makeSpecialSettingsTabViewController(actions: WidgetsListViewModelActions, makeAccountScreen: @escaping () -> AccountViewController) -> SpecialSettingsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 2
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().special_settings)
        return SpecialSettingsTabViewController(contentViewController: controller, makeAccountScreen: makeAccountScreen,
                                                makeStatusBarViewModelV3: makeStatusBarViewModelV3)
    }

    func makeServiceSettingsTabViewController(actions: WidgetsListViewModelActions, makeAccountScreen: @escaping () -> AccountViewController) -> ServiceSettingsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 4
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().service_settings)
        return ServiceSettingsTabViewController(contentViewController: controller, makeAccountScreen: makeAccountScreen,
                                                makeStatusBarViewModelV3: makeStatusBarViewModelV3)
    }
    
    func makeWidgetsListViewModel(actions: WidgetsListViewModelActions) -> WidgetsListViewModel {
        DefaultWidgetsListViewModel(
            searchMWidgetsUseCase: makeSearchWidgetsUseCase(),
            bleManager: dependencies.bleManager,
            makeSliderViewModel: { [
                get = getSliderSettingsUseCaseV3,
                observe = observeSliderResponsesUseCaseV3,
                request = requestSliderValueUseCaseV3,
                set = setSliderValueUseCaseV3
            ] widget in
                SliderListItemViewModelV3(
                    widget: widget,
                    getSliderSettingsUseCase: get,
                    observeSliderResponsesUseCase: observe,
                    requestSliderValueUseCase: request,
                    setSliderValueUseCase: set
                )
            },
            makeToggleSliderViewModel: { [
                bleManager = dependencies.bleManager,
                get = getToggleSliderSettingsUseCaseV3,
                request = requestToggleSliderValueUseCaseV3,
                send = sendToggleSliderValueUseCaseV3
            ] widget in
                ToggleSliderListItemViewModelV3(
                    widget: widget,
                    bleManager: bleManager,
                    getToggleSliderSettingsUseCase: get,
                    requestToggleSliderValueUseCase: request,
                    sendToggleSliderValueUseCase: send
                )
            },
            makeSpinnerViewModel: { [
                bleManager = dependencies.bleManager,
                get = getSpinnerSettingsUseCaseV3,
                request = requestSpinnerValueUseCaseV3,
                set = setSpinnerValueUseCaseV3,
                getRole = getDeviceRoleUseCaseV3,
                changeRole = changeDeviceRoleUseCaseV3
            ] widget in
                SpinnerListItemViewModelV3(
                    widget: widget,
                    bleManager: bleManager,
                    getSpinnerSettingsUseCase: get,
                    requestSpinnerValueUseCase: request,
                    setSpinnerValueUseCase: set,
                    getDeviceRoleUseCase: getRole,
                    changeDeviceRoleUseCase: changeRole
                )
            },
            makeCommandViewModel: { [
                bleManager = dependencies.bleManager,
                session = getDeviceSessionUseCaseV3,
                start = startProsthesisMovementUseCaseV3,
                stop = stopProsthesisMovementUseCaseV3,
                calibrate = startProsthesisCalibrationUseCaseV3,
                release = releaseProsthesisCalibrationButtonUseCaseV3
            ] widget in
                CommandListItemViewModelV3(
                    widget: widget,
                    bleManager: bleManager,
                    getDeviceSessionUseCase: session,
                    startMovementUseCase: start,
                    stopMovementUseCase: stop,
                    startCalibrationUseCase: calibrate,
                    releaseCalibrationButtonUseCase: release
                )
            },
            makePlotViewModel: { [enqueue = enqueueV3Packet] widget in
                Self.makePlotViewModel(widget: widget, enqueuePacket: enqueue)
            },
            makeTextInputViewModel: { [
                bleManager = dependencies.bleManager,
                get = getDeviceInfoTextUseCaseV3,
                edit = editDeviceInfoTextUseCaseV3,
                set = setDeviceInfoTextUseCaseV3
            ] widget in
                TextInputListItemViewModelV3(
                    widget: widget,
                    bleManager: bleManager,
                    getDeviceInfoTextUseCase: get,
                    editDeviceInfoTextUseCase: edit,
                    setDeviceInfoTextUseCase: set
                )
            },
            makeGestureViewModel: { [
                bleManager = dependencies.bleManager,
                enqueue = enqueueV3Packet
            ] widget, openSettings in
                Self.makeGestureViewModel(
                    widget: widget,
                    bleManager: bleManager,
                    enqueuePacket: enqueue,
                    openSettings: openSettings
                )
            },
            requestAccountStatisticsUseCaseV3: requestAccountStatisticsUseCaseV3,
            observeAccountStatisticsUseCaseV3: observeAccountStatisticsUseCaseV3,
            getCustomGestureNamesUseCaseV3: getCustomGestureNamesUseCaseV3,
            observeSyncUseCaseV3: ObserveSyncUseCaseV3(repository: V3SyncRepositoryImpl()),
            requestSyncInitializationUseCaseV3: RequestSyncInitializationUseCaseV3(
                repository: V3SyncInitializationRepositoryImpl(initialize: { [bleManager = dependencies.bleManager] in
                    bleManager.restartV3Synchronization()
                })
            ),
            makeSwitcherViewModel: { [bleManager = dependencies.bleManager,
                                     get = getAutoLoginEnabledUseCaseV3,
                                     set = setAutoLoginEnabledUseCaseV3] widget in
                SwitcherListItemViewModelV3(widget: widget, bleManager: bleManager,
                                          getAutoLoginEnabledUseCase: get,
                                          setAutoLoginEnabledUseCase: set)
            },
            actions: actions
        )
    }
}
