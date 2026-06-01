# План объединения iOS-приложений Motorica Start

Дата: 2026-06-01

## Цель

Объединить два iOS-приложения в один итоговый проект, который остается в KMM-репозитории:

- новое приложение: `iosApp/MotoricaStart`, использует KMM/shared и текущий серо-белый UBI4/V3 интерфейс;
- старое приложение: `/Users/motoricallc/Documents/iOs/motorica_start_iOs-_3D`, работает со старыми протезами и содержит старую CoreBluetooth-протокольную часть;
- итоговый проект: `/Users/motoricallc/Documents/denis/2_Android_bluetooth_master_stradivary/iosApp`.

Код старого приложения при переносе не переписывать и не переименовывать. Исходную структуру старого приложения нужно положить внутрь `iosApp/OldMotoricaStart`, сохранив папки и подпапки. Интеграционная логика, адаптеры и маршрутизация должны жить отдельно от скопированного старого кода.

## Текущее устройство

Android-эталон:

- `app/src/main/java/com/bailout/stickk/scan/view/ScanActivity.java` всегда стартует общий экран сканирования на `activity_scan_new` с бело-голубым оформлением по умолчанию.
- `app/src/main/java/com/bailout/stickk/intro/StartActivity.kt` выбирает ветку после выбора устройства: если имя содержит `UBIv4` или один из V3-маркеров (`FTFS3`, `FTFO3`, `FTHS3`, `FTHO3`, `FTEP3`, `FTEB3`), открывается UBI4/V3 ветка; иначе открывается старая ветка.
- `app/src/main/java/com/bailout/stickk/intro/SplashScreen.kt` читает флаг `UBI4_MODE_ACTIVATED` и меняет стартовый цветовой режим. После подключения к новому протезу следующий запуск уже идет в новом серо-белом режиме.

Новое iOS-приложение:

- стартует через `iosApp/MotoricaStart/Application/AppDelegate.swift`;
- корневой поток задан в `iosApp/MotoricaStart/Application/AppFlowCoordinator.swift`;
- экран сканирования: `iosApp/MotoricaStart/Presentation/BluetoothScene/BluetoothList`;
- сканирование и подключение нового протокола идут через `BleManagerKmm` в `BluetoothListViewModel`;
- после выбора устройства открывается `MainTabBarController`;
- распознавание новой V3-ветки уже есть в `shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/bridges/UiInterfaceModeBridgeV3.kt`.

Старое iOS-приложение:

- основной экран сканирования: `MotoricaStart/ViewControllers/Main/ScanViewController.swift`;
- старая протокольная часть, CoreBluetooth-соединение, нотификации и запись характеристик находятся прямо в `ScanViewController.swift`;
- маршрутизация в старый интерфейс идет через segue `goSensorsSettings` в `Main.storyboard`;
- разделение серийников и протоколов завязано на `Utility/SampleGattAttributes.swift`, `Utility/NameUtil.swift`, `USE_MULTIGRAB_FESTH`, `USE_MULTIGRAB_FESTX`, `FEST-H`, `FEST-X`, `FTFS`, `FTHS`, `FTFO`, `FTHO`, `FTEP`, `FTEB`;
- состояние последнего подключения хранится через `DataManager`/`SaveObjectString` и ключи `DEVICE_NAME`, `DEVICE_MAC`, `LAST_CONNECTION`, `SMART_CONNECTION`.

## Ключевой риск

Нельзя просто добавить все файлы старого приложения в основной target `MotoricaStart`.

В новом и старом приложениях уже есть совпадающие имена классов и расширений: `DeviceCell`, `ScanItem`, `SampleGattAttributes`, `AAPLOpenGLRenderer`, `AAPLOpenGLViewController`, `UIImage.init(color:)`, `String.hexDecodedData()` и другие. Если собрать все в одном Swift-модуле, будут конфликты имен и риск поломать уже работающую UBI4/KMM-ветку.

Безопасная стратегия: старое приложение переносится как изолированный legacy-модуль, а основной app target общается с ним через тонкий адаптер.

## Целевая архитектура

1. `iosApp/OldMotoricaStart`
   - Содержит исходники старого приложения в исходной структуре.
   - Скопированные файлы не редактируются.
   - В target основного приложения напрямую не добавляются.

2. `OldMotoricaStart` Xcode target или framework
   - Компилирует старый код как отдельный Swift/Objective-C модуль.
   - Скрывает внутренние дубли имен от основного `MotoricaStart` target.
   - Подключает старые ресурсы: `Main.storyboard`, `ScanDeviceNib.xib`, `Res/Assets.xcassets`, `OpenGL/shaders`, fonts.
   - Если storyboard/xib ожидают `customModule="MotoricaStart"`, исходные файлы не менять; решить это либо настройкой отдельного module name, либо generated-копиями ресурсов на build phase.

3. `iosApp/MotoricaStart/OldMotoricaStartIntegration`
   - Новая интеграционная папка в основном приложении.
   - Содержит только адаптеры и маршрутизаторы, например:
     - `ProsthesisFamilyClassifier`;
     - `UnifiedScanCoordinator`;
     - `OldMotoricaStartLauncher`;
     - `MergedScanAppearanceStore`;
     - `LegacyConnectionStateBridge`.
   - Эти файлы можно менять; старый код внутри `iosApp/OldMotoricaStart` остается как есть.

4. Единый стартовый поток
   - `AppFlowCoordinator.start()` больше не должен безусловно открывать текущий `BluetoothListCoordinator`.
   - Он должен открывать объединенный экран сканирования.
   - Внешний вид сканирования выбирается из persisted state:
     - первый запуск или последняя старая ветка: бело-голубой режим;
     - после первого успешного подключения к UBI4/V3: серо-белый режим нового приложения.

5. Единая классификация устройств
   - Новый протез: имя содержит `UBIv4` или V3-маркеры `FTFS3`, `FTFO3`, `FTHS3`, `FTHO3`, `FTEP3`, `FTEB3`.
   - Старый протез: `FEST-H`, `FEST-X` без V3-маркера, `FEST-F`, `FEST-EP`, `FEST-EB`, `INDY`, `HRSTM` и остальные старые имена из старого `checkOurLEName`.
   - Неизвестное устройство показывать только в фильтре "Все устройства"; по нажатию не ломать текущую ветку, а показывать безопасную ошибку/ничего не подключать до явного правила.

## Поведение экрана сканирования

Нужно повторить Android-логику:

1. Первый запуск приложения:
   - открыть единый экран сканирования в бело-голубом режиме;
   - показать фильтр "Протезы / Все устройства";
   - сканировать и старые, и новые BLE-устройства.

2. Выбор старого протеза:
   - маршрутизатор определяет старую ветку;
   - открывается интерфейс старого приложения;
   - используется старая CoreBluetooth-протокольная часть;
   - состояние старой ветки сохраняется отдельно, чтобы не сломать smart connection старого приложения.

3. Выбор нового протеза:
   - маршрутизатор определяет UBI4/V3 ветку;
   - вызывается текущий KMM-путь `BleManagerKmm.connectToDevice`;
   - открывается текущий `MainTabBarController`;
   - сохраняется флаг, что пользователь уже подключался к новому протезу.

4. Повторный запуск после первого подключения к новому протезу:
   - стартовый экран сканирования открывается в серо-белом режиме нового приложения;
   - протокольная часть нового приложения остается KMM/shared;
   - старые устройства все еще доступны из списка и должны уводить в старую ветку.

## Пошаговый план работ

### Этап 0. Зафиксировать baseline

- Собрать текущий KMM iOS target без старого кода.
- Собрать старое iOS-приложение из `/Users/motoricallc/Documents/iOs/motorica_start_iOs-_3D`.
- Зафиксировать скриншоты текущего iOS-сканирования, старого iOS-сканирования и Android `ScanActivity`.
- Проверить, что в рабочем дереве нет чужих правок, которые будут смешаны с интеграцией.

### Этап 1. Перенести исходники старого приложения

- Скопировать исходную структуру старого приложения в `iosApp/OldMotoricaStart`.
- Сохранять внутренние пути: `MotoricaStart`, `Common`, `Nibs`, `OpenGL`, `Res`, `ViewControllers`, `Utility`, `Base.lproj`, `ru.lproj`, `en.lproj`.
- Не редактировать скопированные классы.
- Не переносить generated/cache артефакты: `Pods`, `DerivedData`, `build`, `xcuserdata`, `.DS_Store`.
- `Podfile` и `.xcodeproj` старого приложения можно хранить как reference-артефакты, но основной проект должен использовать управляемую интеграцию зависимостей.

### Этап 2. Изолировать старый код в отдельный модуль

- Создать отдельный target/framework `OldMotoricaStart`.
- Подключить к нему старые Swift, Objective-C, storyboard/xib, assets, fonts и shaders.
- Настроить bridging header для старого OpenGL-кода внутри legacy target.
- Разрулить зависимость Charts: старый проект использовал CocoaPods `Charts ~> 4.1.0`, новый уже использует SPM `DGCharts`. Предпочтительно не возвращать CocoaPods в KMM-проект; использовать текущую SPM-зависимость или отдельную настройку legacy target.
- Проверить, что основной target не видит старые дублирующиеся классы как свои исходники.

### Этап 3. Создать адаптер старой ветки

- В legacy target добавить новый public wrapper, не меняя исходные старые классы.
- Wrapper должен уметь:
  - создать стартовый контроллер старого интерфейса из старого storyboard;
  - принять выбранное устройство или данные устройства от общего сканера;
  - сохранить старые ключи `DEVICE_NAME`, `DEVICE_MAC`, `LAST_CONNECTION`, `SMART_CONNECTION`;
  - вернуть `UIViewController` для показа из основного приложения.
- Если старый `ScanViewController` невозможно безопасно использовать как часть общего сканера, оставить его внутри legacy target и вынести наружу только старый post-connect UI.

### Этап 4. Объединить сканирование

- Создать общий scan model:
  - `id`;
  - `name`;
  - `displayName`;
  - `rssi`;
  - `family`: `.newKmm`, `.oldLegacy`, `.unknown`;
  - `source`: `.kmm` или `.legacyCoreBluetooth`.
- Сохранить текущий KMM scanner для новых UBI4/V3 устройств.
- Для старых устройств использовать CoreBluetooth-путь, который сохраняет доступ к `CBPeripheral`, иначе старая протокольная часть не сможет подключиться без повторного поиска.
- На уровне UI объединить результаты двух сканеров в один список, убирая дубли по UUID/name.
- Фильтр "Протезы" должен использовать объединенный список маркеров Android `checkOurLEName`.

### Этап 5. Реализовать маршрутизацию после выбора

- При выборе `.newKmm`:
  - остановить legacy scan;
  - вызвать текущий `BluetoothListViewModel.connect(to:)` или вынести KMM connect в общий coordinator;
  - выставить `UiInterfaceModeBridgeV3.updateFromDeviceName`;
  - сохранить флаг нового интерфейса;
  - открыть `MainTabBarController`.
- При выборе `.oldLegacy`:
  - остановить KMM scan;
  - передать устройство в legacy wrapper;
  - открыть старый интерфейс через navigation/root transition;
  - не менять старые протокольные методы.
- При выборе `.unknown`:
  - не открывать ни одну ветку без явного правила.

### Этап 6. Синхронизировать persisted state

- Ввести отдельные ключи основного приложения:
  - `merged.lastConnectedFamily`;
  - `merged.hasConnectedNewProsthesis`;
  - `merged.lastNewDeviceName`;
  - `merged.lastNewDeviceUUID`;
  - `merged.lastOldDeviceName`;
  - `merged.lastOldDeviceUUID`.
- Не переиспользовать старые ключи как основные, чтобы не сломать старую smart connection.
- При подключении к новому протезу выставлять `merged.hasConnectedNewProsthesis = true`.
- При подключении к старому протезу не стирать историю нового подключения, но текущую активную ветку сохранять как old.
- Для визуального режима scan screen использовать `merged.hasConnectedNewProsthesis`, как Android использует `UBI4_MODE_ACTIVATED`.

### Этап 7. Проверка и регрессия

Минимальный набор проверок перед тем, как считать интеграцию готовой:

- `./gradlew :shared:compileKotlinIosArm64 :shared:compileDebugKotlinAndroid`
- `xcodebuild` текущего app target для iOS Simulator.
- `xcodebuild` для device/generic iOS.
- Запуск приложения на устройстве с Bluetooth.
- Первый запуск: бело-голубой scan screen.
- Выбор старого протеза: открывается старый интерфейс, читаются старые характеристики, работают настройки/жесты/3D.
- Выбор нового UBI4/V3 протеза: открывается текущий интерфейс, работает KMM connect, widgets/status bar/sync.
- Повторный запуск после нового протеза: scan screen серо-белый.
- После серо-белого scan screen выбор старого протеза все еще открывает старую ветку.
- Smart connection старой ветки не ломает новый auto/manual connect.
- Проверить локализации и assets старого storyboard.
- Проверить, что в Xcode нет дублей классов в одном target.

## Что не делать

- Не копировать старые файлы прямо в `iosApp/MotoricaStart` с переименованием классов.
- Не редактировать старые классы только ради Swift 5-стиля или форматирования.
- Не смешивать старые `DataManager`/`SaveObjectString` ключи с новым `UserDefaultsKeyValueStorage` без адаптера.
- Не заменять KMM BLE-путь новым CoreBluetooth-кодом для UBI4/V3.
- Не переносить CocoaPods в основной проект, если можно обойтись текущей SPM-зависимостью.
- Не считать работу завершенной без проверки старого и нового протокольного подключения на реальном Bluetooth-устройстве.

## Открытые технические вопросы

- Нужно решить, будет ли `OldMotoricaStart` dynamic framework, static framework или отдельный Xcode target внутри workspace.
- Нужно проверить, компилируется ли старый Swift 4-код в текущем Xcode/Swift 5 без изменения исходников. Если нет, правки должны идти через adapter/build settings, а не через переписывание старых файлов.
- Нужно определить, можно ли один общий scan screen вести двумя BLE-сканерами одновременно, или старый CoreBluetooth scanner должен быть единственным владельцем старых `CBPeripheral`.
- Нужно проверить, какие storyboard/xib ресурсы старого приложения требуют module name `MotoricaStart`, и выбрать способ не менять исходные файлы.
- Нужно уточнить полный список серийников "новой" ветки, если он шире Android V3-маркеров `FTFS3`, `FTFO3`, `FTHS3`, `FTHO3`, `FTEP3`, `FTEB3` и `UBIv4`.

## Критерий готовности

Интеграция считается готовой только когда один app target из KMM-проекта:

- собирается без конфликтов старых и новых классов;
- на первом запуске показывает бело-голубой scan screen;
- открывает старую ветку для старых протезов;
- открывает новую KMM/UBI4/V3 ветку для новых протезов;
- после первого нового подключения показывает серо-белый scan screen;
- сохраняет работоспособность обоих приложений в их исходных сценариях.
