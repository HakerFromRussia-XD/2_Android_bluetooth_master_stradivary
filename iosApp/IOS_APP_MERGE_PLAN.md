# План объединения iOS-приложений Motorica Start

## Цель

Объединить два iOS-приложения в один итоговый проект, который остается в KMM-репозитории:

- новое приложение: `iosApp/MotoricaStart`, использует KMM/shared и текущий серо-белый UBI4/V3 интерфейс;
- старое приложение: `/Users/motoricallc/Documents/iOs/motorica_start_iOs-_3D`, работает со старыми протезами и содержит старую CoreBluetooth-протокольную часть;
- итоговый проект: `/Users/motoricallc/Documents/denis/2_Android_bluetooth_master_stradivary/iosApp`.

Код старого приложения при переносе не переписывать и не переименовывать. Исходную структуру старого приложения нужно положить внутрь `iosApp/OldMotoricaStart`, сохранив папки и подпапки. Интеграционная логика, адаптеры и маршрутизация должны жить отдельно от скопированного старого кода.

## Статус на 2026-06-01

- Старое приложение скопировано в `iosApp/OldMotoricaStart` без `Pods`, `build`, `.git`, `xcuserdata` и `.DS_Store`.
- В новый проект CocoaPods не подключались. Legacy target использует уже добавленную SPM-зависимость `DGCharts`; старые ссылки `Charts` адаптированы под `DGCharts`.
- Добавлен отдельный framework target `OldMotoricaStart`, который изолирует старые Swift-классы от основного target `MotoricaStart`.
- Старый `Main.storyboard` собирается как ресурс `OldMotoricaStart.framework`; для старого `UINib(..., bundle: nil)` в main bundle добавлена интеграционная копия `ScanDeviceNib.xib` с фиксированным `customModule="OldMotoricaStart"`, исходный xib в `iosApp/OldMotoricaStart` не изменяется.
- Добавлена интеграционная папка `iosApp/MotoricaStart/OldMotoricaStartIntegration` с классификацией устройств, storage-ключами объединенного scan flow и launcher-адаптером старой ветки.
- Экран сканирования нового приложения теперь выбирает внешний вид по `merged.hasConnectedNewProsthesis`: до первого нового подключения бело-голубой режим, после подключения к новому протезу серо-белый режим.
- Бело-голубой режим должен быть не стилизацией нового экрана, а копией старого scan UI: UIKit `UISegmentedControl` с порядком `Протезы | Все устройства`, top=80, height=60, старая белая таблица без modern-оформления и legacy-ячейка с черным названием и серым RSSI.
- Область status bar должна окрашиваться в цвет активного scan-экрана: верхний цвет старого градиента для бело-голубого режима и `ubi4_back` для серо-белого режима.
- Список устройств в обоих scan UI должен ограничиваться нижней safe area и скроллиться внутри своей рамки, не уходя под home indicator.
- Фильтр "Протезы" учитывает и новые UBI4/V3 маркеры из KMM, и старые маркеры legacy-протезов.
- Выбор нового протеза идет по текущему KMM-пути; выбор старого протеза открывает legacy-интерфейс через `OldMotoricaStartLauncher`.
- Проверки сборки пройдены:
  - `./gradlew :shared:compileKotlinIosArm64 :shared:compileDebugKotlinAndroid`;
  - `xcodebuild` старого исходного приложения из `/Users/motoricallc/Documents/iOs/motorica_start_iOs-_3D`;
  - `xcodebuild` `OldMotoricaStart` для iOS Simulator;
  - `xcodebuild` `MotoricaStart` для iOS Simulator;
  - `xcodebuild` `MotoricaStart` для реального устройства `iPhone` `00008110-0018148C029A401E`.
- Реальная device-проверка: `MotoricaStart.app` установлен и запущен на подключенном `iPhone` через `devicectl`; процесс `MotoricaStart` виден на устройстве.
- Реальная BLE-проверка 2026-06-01 на `iPhone` `00008110-0018148C029A401E`:
  - `FEST-H-04921` найден в scan list как `FEST-XFTHS04921`; после выбора открылся legacy root `OldMotoricaStart`, старый `ScanViewController` перешел в `SensorsViewController`, CoreBluetooth показал `Device ready` для `FEST-XFTHS04921`;
  - `111111` найден в scan list как `FTHS3-111111`; после выбора открылся новый `MotoricaStart.MainTabBarController`, legacy root не появился, CoreBluetooth показал `Device ready` для BLE-девайса `FTHS3-111111`.
- Для совместимости с неизмененным старым `DataManager.loadAll` добавлен adapter, который перед legacy flow временно убирает новую папку `Documents/Firmware` из корня `Documents`, а перед новым firmware UI возвращает ее обратно.

Осталось дополнительно проверить на живых BLE-протезах:

- первый запуск показывает бело-голубой scan screen;
- после первого нового подключения повторный запуск показывает серо-белый scan screen;
- из серо-белого режима старый протез все еще открывает legacy-ветку.

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
   - показать старый внешний вид экрана ios со всеми кнопками и списками

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
- Использовать текущий KMM scanner для новых UBI4/V3 устройств и для старых устройств.
- Старой протокольной части (в случае выбора старого устройства для подключения) передавать данные для подключения (возможно в ней потребуется повторное сканирование).
- Фильтр "Протезы" должен использовать объединенный список маркеров Android `checkOurLEName`.

### Этап 5. Реализовать маршрутизацию после выбора

- При выборе `.newKmm`:
  - остановить scan;
  - вызвать текущий `BluetoothListViewModel.connect(to:)` или вынести KMM connect в общий coordinator;
  - выставить `UiInterfaceModeBridgeV3.updateFromDeviceName`;
  - сохранить флаг нового интерфейса;
  - открыть `MainTabBarController`.
- При выборе `.oldLegacy`:
  - остановить scan;
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
- Выбор старого протеза: открывается старый интерфейс, читаются старые характеристики, работают настройки/жесты/3D. Реально проверено на `FEST-H-04921` до открытия старого `SensorsViewController`.
- Выбор нового UBI4/V3 протеза: открывается текущий интерфейс, работает KMM connect, widgets/status bar/sync. Реально проверено на `111111` до открытия `MainTabBarController`.
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
