import Foundation
import UIKit
import shared

final class AppDIContainer {
    static func makeInitialStatusBarViewModel() -> StatusBarViewModel {
        if UiInterfaceModeBridgeV3.shared.isEnabled() {
            let observeStatus = ObserveMainStatusUseCaseV3(repository: V3MainStatusRepositoryImpl())
            return StatusBarViewModel(isConnected: observeStatus.currentConnectionReady())
        }
        let initialState = Int(truncating: BLEStateBridge.shared.currentStateOrdinal() as NSNumber)
        return StatusBarViewModel(isConnected: initialState == 2)
    }

    
    lazy var appConfiguration = AppConfiguration()
    
    // MARK: - Network
    lazy var apiDataTransferService: DataTransferService = {
        let config = ApiDataNetworkConfig(
            baseURL: URL(string: appConfiguration.apiBaseURL)!,
            queryParameters: [
                "api_key": appConfiguration.apiKey,
                "language": NSLocale.preferredLanguages.first ?? "en"
            ]
        )
        
        let apiDataNetwork = DefaultNetworkService(config: config)
        return DefaultDataTransferService(with: apiDataNetwork)
    }()
    lazy var imageDataTransferService: DataTransferService = {
        let config = ApiDataNetworkConfig(
            baseURL: URL(string: appConfiguration.imagesBaseURL)!
        )
        let imagesDataNetwork = DefaultNetworkService(config: config)
        return DefaultDataTransferService(with: imagesDataNetwork)
    }()
    
    // MARK: - DIContainers of scenes
    private lazy var deviceSessionRepositoryV3 = V3DeviceSessionRepositoryImpl()
    private lazy var getDeviceSessionUseCaseV3 = GetDeviceSessionUseCaseV3(repository: deviceSessionRepositoryV3)
    private lazy var deviceInteractionViewModelV3: V3DeviceInteractionViewModel = {
        V3DeviceInteractionViewModel(
            updateInteraction: UpdateDeviceInteractionUseCaseV3(repository: deviceSessionRepositoryV3),
            observeConnection: { callback in
                [
                    BLEStateBridge.shared.observeState { state in
                        callback(.connectionChanged(isReady: Int(truncating: state) == Int(BLEState.State.ready.ordinal)))
                    },
                    UiStateBridge.shared.observeWidgetsLoadingProgress { _ in
                        callback(.progressReceived)
                    },
                    UiStateBridge.shared.observeWidgetsLoadCompletion {
                        callback(.synchronizationFinished)
                    }
                ]
            }
        )
    }()

    func makeDeviceInteractionViewModelV3() -> V3DeviceInteractionViewModel {
        deviceInteractionViewModelV3
    }

    func makeMainTabsViewModelV3() -> V3MainTabsViewModel {
        V3MainTabsViewModel(getVisibleDisplays: GetMainVisibleDisplaysUseCaseV3(repository: V3MainDisplaysRepositoryImpl()))
    }

    func makeUserFirmwareViewModelV3(updates: UserFirmwareUpdates) -> V3UserFirmwareViewModel {
        let repository = V3UserFirmwareActionsRepositoryImpl(updates: updates)
        return V3UserFirmwareViewModel(
            startUpdate: StartUserFirmwareUpdateUseCaseV3(repository: repository),
            postponeUpdate: PostponeUserFirmwareUpdateUseCaseV3(repository: repository),
            acknowledgeCompletion: AcknowledgeUserFirmwareCompletionUseCaseV3(repository: repository)
        )
    }

    func makeAccountViewController() -> AccountViewController {
        AccountViewController(
            viewModelV3: makeAccountViewModelV3(),
            firmwareViewModelV3: makeServiceFirmwareViewModelV3(),
            makeStatisticsScreen: { [self] in
                AccountStatisticsViewController(viewModel: makeAccountStatisticsViewModelV3())
            },
            makeCustomerServiceScreen: { [self] profile, title in
                makeCustomerServiceScreen(profile: profile, topTitle: title)
            },
            makeProsthesisInfoScreen: { [self] profile, title in
                makeProsthesisInfoScreen(profile: profile, topTitle: title)
            }
        )
    }

    private func makeAccountViewModelV3() -> V3AccountViewModel? {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else { return nil }
        let repository = V3AccountBoardsRepositoryImpl(useAddressNameFallback: false)
        return V3AccountViewModel(
            getBoards: GetAccountBoardsUseCaseV3(repository: repository, isZeroVersion: { version in
                KotlinBoolean(bool: FirmwareVersionCatalog.shared.isZeroVersion(version: version))
            }),
            observeBoards: ObserveAccountBoardsUseCaseV3(repository: repository),
            loadProfile: LoadAccountProfileSnapshotUseCaseV3(repository: V3AccountProfileSnapshotRepositoryImpl())
        )
    }

    private func makeServiceFirmwareViewModelV3() -> V3ServiceFirmwareViewModel? {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else { return nil }
        let repository = V3ServiceFirmwareCatalogRepositoryImpl(cacheDirectory: {
            FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first?.path
                ?? NSTemporaryDirectory()
        })
        let filesForBoard = GetServiceFirmwareForBoardUseCaseV3(
            familyForAddress: { address in
                FirmwareBoardFamily.companion.fromDeviceAddress(address: address.int32Value).name
            },
            isCompatible: { address, name in
                KotlinBoolean(bool: FirmwareCompatibility.shared.isCompatible(deviceAddress: address.int32Value,
                                                                               fileName: name))
            },
            versionForDevice: { address, name in
                FirmwareCompatibility.shared.versionForDevice(deviceAddress: address.int32Value, fileName: name)
            },
            isVersionNewer: { installed, candidate in
                KotlinBoolean(bool: FirmwareVersionCatalog.shared.isLocalVersionNewer(deviceVersion: installed,
                                                                                     localVersion: candidate))
            },
            isUpdateAvailable: { address, installed, names in
                KotlinBoolean(bool: FirmwareCompatibility.shared.isUpdateAvailable(deviceAddress: address.int32Value,
                                                                                    installedVersion: installed,
                                                                                    fileNames: names))
            }
        )
        return V3ServiceFirmwareViewModel(
            loadCatalog: LoadServiceFirmwareCatalogUseCaseV3(repository: repository),
            filesForBoard: filesForBoard,
            downloadFiles: DownloadServiceFirmwareFilesUseCaseV3(repository: repository)
        )
    }

    private func makeCustomerServiceScreen(profile: AccountBridgeProfile, topTitle: String) -> UIViewController {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else {
            return AccountCustomerServiceViewController(profile: profile, topTitle: topTitle)
        }
        let repository = V3AccountDetailsSnapshotRepository(profile: profile)
        let viewModel = V3CustomerServiceViewModel(
            getInfo: GetCustomerServiceInfoUseCaseV3(repository: repository,
                                                   warrantyExpirationDate: { _ in profile.warrantyExpirationDate }),
            getManagerPhone: GetCustomerServiceManagerPhoneUseCaseV3(repository: repository)
        )
        return AccountCustomerServiceViewController(viewModel: viewModel, topTitle: topTitle)
    }

    private func makeProsthesisInfoScreen(profile: AccountBridgeProfile, topTitle: String) -> UIViewController {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else {
            return AccountProsthesisInfoViewController(profile: profile, topTitle: topTitle)
        }
        let repository = V3AccountDetailsSnapshotRepository(profile: profile)
        return AccountProsthesisInfoViewController(
            viewModel: V3ProsthesisInformationViewModel(getInfo: GetProsthesisInformationUseCaseV3(repository: repository)),
            topTitle: topTitle
        )
    }

    func makeBleLogViewController() -> BleLogViewController {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else {
            return BleLogViewController()
        }
        let repository = V3BleLogRepositoryImpl(
            readGraphStreamHidden: { KotlinBoolean(bool: BleLogSettings.hidesGraphStream) },
            saveGraphStreamHidden: { hidden in
                BleLogSettings.saveGraphStreamHiddenPreference(hidden.boolValue)
            }
        )
        return BleLogViewController(
            viewModelV3: BleLogViewModelV3(
                filter: ManageBleLogFilterUseCaseV3(repository: repository),
                observeLog: ObserveBleLogUseCaseV3(repository: repository)
            )
        )
    }

    private lazy var accountStatisticsRepositoryV3: V3AccountStatisticsRepositoryImpl = {
        V3AccountStatisticsRepositoryImpl(
            readCustomGestureName: { index in
                let names = V3CustomGestureNamesRepositoryImpl.shared.loadNames()
                let index = Int(index.intValue)
                return names.indices.contains(index) ? names[index] : nil
            },
            requestTelemetry: { [bleManager = self.bleManager] in
                let gatt = SampleGattAttributes()
                bleManager.sendBytesKmm(
                    data: BLECommandsV3.shared.requestTelemetryData(),
                    command: gatt.SERIALPORTCHAR_UUID,
                    typeCommand: gatt.WRITE,
                    onChunkSent: {}
                )
            },
            isV3: { KotlinBoolean(bool: UiInterfaceModeBridgeV3.shared.isEnabled()) },
            subscribeTelemetryCounters: { callback in
                WidgetStateBridge.shared.observeTelemetryGestureCounters { counters in
                    // Preserve the native read/aggregation point inside its queued main callback.
                    DispatchQueue.main.async { _ = callback(counters) }
                }
            }
        )
    }()

    private lazy var requestAccountStatisticsUseCaseV3 = RequestAccountStatisticsUseCaseV3(repository: accountStatisticsRepositoryV3)
    private lazy var observeAccountStatisticsUseCaseV3 = ObserveAccountStatisticsUseCaseV3(repository: accountStatisticsRepositoryV3)
    private lazy var getCustomGestureNamesUseCaseV3 = GetCustomGestureNamesUseCaseV3(repository: V3CustomGestureNamesRepositoryImpl.shared)

    private func makeAccountStatisticsViewModelV3() -> AccountStatisticsViewModelV3 {
        AccountStatisticsViewModelV3(
            requestStatistics: requestAccountStatisticsUseCaseV3,
            observeStatistics: observeAccountStatisticsUseCaseV3,
            getCustomGestureNames: getCustomGestureNamesUseCaseV3
        )
    }

    func makeWidgetsSceneDIContainer() -> WidgetsSceneDIContainer {
        let dependencies = WidgetsSceneDIContainer.Dependencies(
            apiDataTransferService: apiDataTransferService,
            imageDataTransferService: imageDataTransferService,
            bleManager: bleManager
        )
        return WidgetsSceneDIContainer(
            dependencies: dependencies,
            requestAccountStatisticsUseCaseV3: requestAccountStatisticsUseCaseV3,
            observeAccountStatisticsUseCaseV3: observeAccountStatisticsUseCaseV3,
            getCustomGestureNamesUseCaseV3: getCustomGestureNamesUseCaseV3,
            getDeviceSessionUseCaseV3: getDeviceSessionUseCaseV3
        )
    }
    
    // MARK: - Bluetooth
    lazy var bleManager: BleManagerKmm = {
        _ = BLEComponents.shared
        return BleEnvironment.shared.getBleManager()
    }()
    lazy var bluetoothRepository: BluetoothRepository = BluetoothRepositoryImpl()
    private lazy var keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    func makeBluetoothSceneDIContainer() -> BluetoothSceneDIContainer {
        let deps = BluetoothSceneDIContainer.Dependencies(
            bleManager: bleManager,
            bluetoothRepository: bluetoothRepository,
            keyValueStorage: keyValueStorage
        )
        return BluetoothSceneDIContainer(dependencies: deps)
    }
}
