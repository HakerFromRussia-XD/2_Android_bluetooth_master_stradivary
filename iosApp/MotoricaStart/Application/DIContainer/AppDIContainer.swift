import Foundation
import shared

final class AppDIContainer {
    
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
    func makeWidgetsSceneDIContainer() -> WidgetsSceneDIContainer {
        let dependencies = WidgetsSceneDIContainer.Dependencies(
            apiDataTransferService: apiDataTransferService,
            imageDataTransferService: imageDataTransferService,
            bleManager: bleManager
        )
        return WidgetsSceneDIContainer(dependencies: dependencies)
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
