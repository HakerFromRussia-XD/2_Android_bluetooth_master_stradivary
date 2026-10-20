import Foundation
import shared

enum GesturesPreferencesStorageKeys {
    static let selectedSegment = "GesturesWidgetSelectedSegment"
    static let factoryExpanded = "GesturesProvider.isFactoryExpanded"
}

final class V3GesturesPreferencesRepositoryImpl: NSObject, V3GesturesPreferencesRepository {
    private let userDefaults: UserDefaults
    private let readUiTestRotationMode: () -> Bool

    init(userDefaults: UserDefaults = .standard, readUiTestRotationMode: @escaping () -> Bool) {
        self.userDefaults = userDefaults
        self.readUiTestRotationMode = readUiTestRotationMode
        super.init()
    }

    func getGesturesPreferences() -> V3GesturesPreferences {
        let expanded = userDefaults.object(forKey: GesturesPreferencesStorageKeys.factoryExpanded) as? Bool ?? true
        let section = readUiTestRotationMode()
            ? 1
            : userDefaults.integer(forKey: GesturesPreferencesStorageKeys.selectedSegment)
        return V3GesturesPreferences(selectedSection: Int32(clamping: section), isFactoryCollectionExpanded: expanded)
    }

    func setGesturesSection(section: Int32) {
        userDefaults.set(Int(section), forKey: GesturesPreferencesStorageKeys.selectedSegment)
    }

    func setFactoryGestureCollectionExpanded(expanded: Bool) {
        userDefaults.set(expanded, forKey: GesturesPreferencesStorageKeys.factoryExpanded)
    }
}
