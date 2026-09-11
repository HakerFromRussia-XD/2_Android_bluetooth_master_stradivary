# SpecialSettings V3: контракт состояния и карта источников

Дата: 2026-09-09. Результат анализа A1 из
[основного плана](ubi4_v3_clean_architecture_plan.md).
Это контракт полного экрана для A2–A6; реализованная часть отмечена ниже.
Код приложения в A1 не менялся. Порядок этапов задаёт только основной план.
После проверки навигации исправлен целевой Fragment: обычное меню открывает
SpecialSettingsFragment. Первоначальная привязка описания к AdvancedFragment
была ошибочной. Подключение трёх Slider исправлено отдельно в A1.1;
результаты проверки записаны в основном плане.

Цель: `SpecialSettingsFragment` получает состав списка, значения и доступность из своей
`V3SpecialSettingsViewModel`. В A2 созданы этот класс, его Factory/UiState/Action
в пакете `presentation.specialsettings`; прежних V3AdvancedSettings в коде нет.
Fragment отображает состояние и передаёт действия;
ViewModel объединяет источники, domain выполняет правила и сценарии,
data обращается к существующим хранилищам и очереди BLE.

Реализовано в A2: SpecialSettingsFragment владеет ViewModel и подпиской
repeatOnLifecycle(STARTED); UiState содержит selectedSection и три Slider.
V3SliderSettingsController переиспользуется с отдельным scope каждого владельца.
Для Sensors/Service/Advanced сохранена привязка Base к V3SliderSettingsViewModel.
STOP и переход к APPLICATION отменяют отложенные записи Slider; PROSTHESIS
восстанавливает значения.

Реализовано в A3: deviceProfile и типизированный widgets входят в UiState.
DataFactory/updateFlow для V3 скрыты за DataFactoryV3SpecialSettingsWidgetsSource;
V3SpecialSettingsWidgetMapper преобразует Items в описания и обратно для текущих
адаптеров. Info содержит существующий ключ, подпись и позицию; варианты хранят
метаданные отображения. ParameterInfo восстанавливается по ключу в преобразователе,
поэтому ID и привязки адаптеров сохраняются без BLE-структур в UiState.
Options копируются на обеих границах. Повторный одинаковый список не сбрасывает
черновики; смена устройства или набора Slider отменяет ожидающие записи.
Fragment отображает состав из состояния; прямой путь DataFactory/updateFlow
сохраняется только для UBI4. Чтение/сохранение выбранной вкладки ещё находится
в Fragment до A6; остальные значения и обработчики переносим по A4–A6.

Реализовано в A4.1: ToggleSlider «Блокировка движения с ЕМГ» в SpecialSettings
для STANDARD_V3/INDY3. В toggleSliders хранится ToggleSliderUiStateV3:
значение V3ToggleSliderValue (timeTenths/isEnabled), domain-диапазон,
доступность и признак анимации. Кнопка функции доступна по общему допуску;
ползунок/± — также по флагу функции. Единственный владелец состояния и таймера —
экранная ViewModel. EditToggleSliderUseCaseV3 отвечает за изменение одного поля
с сохранением другого, SendToggleSliderValueUseCaseV3 — за отправку после задержки.
Пакеты и сериализация находятся в существующей реализации repository;
узкий V3ToggleSliderSettingsRepository не заставляет обычные Slider зависеть от
ToggleSlider-методов.

В A4.2 через тот же сценарий подключены «Переключение жестов сенсорами» и
«Время работы экрана». Теперь все три ToggleSlider SpecialSettings обычного V3
получают значения и доступность из состояния экрана; в INDY3 активен только
присутствующий в его составе EMG_MOVEMENT_LOCK. Таймеры отдельных ключей независимы,
удаление одного виджета отменяет только его отправку. Команды дополнительных
параметров — 0F/15 и 10/08, формат и диапазон совпадают с первым ToggleSlider.
ToggleSlider на остальных экранах пока сохраняют прежние обработчики.

В A5.1 (2026-09-10) подключён Spinner «Режим работы протеза» для STANDARD_V3/INDY3.
`spinners[P_KEY_HAND_CONTROL_MODE]` хранит отображаемый индекс и доступность;
варианты берутся из widgets. ViewModel получает значение через узкий
V3SpinnerSettingsRepository; SetSpinnerValueUseCaseV3 проверяет пять режимов 0–4
и текущий допуск перед немедленной записью. Реализация repository сохраняет порядок
store → профиль → кеш → очередь и команду 0F/3B. При программном выборе listener
PowerSpinner временно отключается; следующий пользовательский выбор обрабатывается,
включая явный повтор текущего пункта.

В A5.2 через тот же сценарий подключён «Действие при смене жеста» в STANDARD_V3:
ключ GESTURE_CHANGE_MODE, domain-диапазон 0–1 и прежняя команда 0F/2D.
В INDY3 его нет в составе, поэтому действие отклоняется. Оба обычных Spinner
SpecialSettings теперь получают значения из состояния экрана; SETTINGS_PROFILE
переносится отдельно в A5.3, Spinner других экранов — на прежнем пути.

В A5.3.1 список профилей и активный ID подключены к `settingsProfiles` через
V3SettingsProfilesRepository и GetSettingsProfilesUseCaseV3. Состояние содержит
профили с ID/необязательным customName, activeProfileId, canCreate, isEnabled,
isLoading и loadFailed (только чтение списка). Неприсутствующий виджет даёт null,
без запроса БД. Fragment передаёт состояние в адаптер; render только отображает.
В A5.3.1 операции выбора/создания/переименования оставались в адаптере.
В A5.3.2 выбор существующего профиля перенесён в SettingsProfileSelected →
ViewModel → SelectSettingsProfileUseCaseV3 → repository; повторный выбор активного
ID вновь применяет его значения. С A5.3.3 создание использует SettingsProfileCreateRequested
→ ViewModel → CreateSettingsProfileUseCaseV3 → repository. operation/failedOperation
(SELECT, CREATE или RENAME) описывают выполняемую/ошибочную операцию, заменяя поля
isSelecting/selectionFailed. С A5.3.4 переименование проходит через
SettingsProfileNameSubmitted → RenameSettingsProfileUseCaseV3 → repository.
nameEditor хранит запрос открытия диалога и профиль; Fragment показывает диалог из
состояния. Ввод текста остаётся локальным Compose-состоянием до сохранения.

## 1. Проверенный состав экрана

Раздел настроек протеза: `display = 2`. Позиции ниже — фактический `widgetPosition`, начиная с нуля.
`assignWidgetOrder()` назначает позиции отдельно внутри каждого display,
а `widgetId` — последовательно по всему набору. `DataFactory` сортирует по позиции.

| Ключ | Виджет / русская подпись | STANDARD_V3 | INDY3 |
| --- | --- | --- | --- |
| `P_KEY_EMG_CHANGE_GESTURE` | ToggleSlider / Переключение жестов сенсорами | 0 | Нет |
| `P_KEY_EMG_MOVEMENT_LOCK` | ToggleSlider / Блокировка движения с ЕМГ | 1 | 0 |
| `P_KEY_SCREEN_TIMEOUT` | ToggleSlider / Время работы экрана | 2 | Нет |
| `P_KEY_EMG_MAX_GAIN_VALUE` | Slider / Максимальная чувствительность датчиков | 3 | 1 |
| `P_KEY_FORCE_SETTINGS` | Slider / Настройка силы | 4 | 2 |
| `P_KEY_SPEED_SETTINGS` | Slider / Настройка скорости | 5 | 3 |
| `P_KEY_HAND_CONTROL_MODE` | Spinner / Режим работы протеза | 6 | 4 |
| `P_KEY_GESTURE_CHANGE_MODE` | Spinner / Действие при смене жеста | 7 | Нет |
| `P_KEY_SETTINGS_PROFILE` | Spinner / Профили настроек | Нет: выключен флаг | 5 |

Итого: **8 элементов STANDARD_V3, 6 элементов INDY3**. В стандартном генераторе
`SETTINGS_PROFILE_WIDGET_ENABLED = false`. Если его когда-либо включат, профиль
окажется перед «Действие при смене жеста»; в этом рефакторинге флаг не меняется.

Slider: максимальная чувствительность — `0..250`, сила и скорость — `0..100`.
ToggleSlider: значение времени `10..100`, шаг значения `1`, множитель отображения
`0.1`, то есть `1.0..10.0 сек` с шагом `0.1 сек`. Ползунок использует позиции `0..90`.

Обычные Spinner сохраняют порядок вариантов:

- Режим работы: Нормальный; Спортивный; Плавное управление силой;
  Плавное управление скоростью; Плавное управление силой и скоростью.
- Действие при смене жеста: Без действия; Перейти в открытое положение.

Подписи берём из уже подготовленных Items: генератор локализует строки из
SharedRes, затем DataFactory разделяет подпись по `%`, обрезает пробелы и берёт
первую часть для этих виджетов. Не создаём второй каталог подписей или порядка.
S-структуры V3 уже содержат текст; механизм `labelCode` для E-структур не заменяет его.

В разделе приложения DataFactory.mobileWidgets() возвращает один Switch
«Автоматический вход» / «Auto login» (`MobileSettingsKey.AUTO_LOGIN`).
Это существующий SwitchItem, не SwitchItemV3; адаптер читает
SET_MODE_SMART_CONNECTION из SharedPreferences (по умолчанию false) и сохраняет
пользовательское изменение через main.saveBoolean без отправки BLE.

Навигация: `page_4 → showSpecialScreen() → SpecialSettingsFragment`.
Удержание accountBtn переключает видимость `page_secret`; его выбор вызывает
`showSecretScreen() → ServiceFragment`. AdvancedFragment остаётся отдельным
классом с showAdvancedScreen и восстановлением из Help; это не маршрут page_4.

Источники: [генераторы и publishHardcodedWidgets][parser],
[DataFactory][factory], [русские ресурсы][strings].

## 2. Как экран работает сейчас

| Что | Источник и текущий потребитель | Граница при переносе |
| --- | --- | --- |
| Состав | BLEController выбирает генератор по `activeV3DeviceProfile`; парсер публикует `UiState.listWidgets` и `updateFlow` | Узкий presentation-источник состава и преобразователь вокруг DataFactory |
| Первый список | SpecialSettings восстанавливает LAST_ACTIVE_SETTINGS_FILTER; ViewModel читает снимок выбранной вкладки через source | Реализовано в A3; первый список доступен без события |
| Обновление списка | Source передаёт инвалидации updateFlow, ViewModel обновляет widgets; Fragment сравнивает состав перед swap | Реализовано в A3; при занятом layout отображается самое свежее состояние с проверкой существования view |
| Переключение вкладки | Кнопки сохраняют LAST_ACTIVE_SETTINGS_FILTER, меняют activeSettingsFragmentFilterFlow (1 — протез, 2 — приложение); в V3 посылают SettingsSectionSelected, состояние содержит выбранную вкладку и её список | Выбор в UiState с A2, состав с A3; сохранение/совместимый сигнал выносится за контракт в A6, анимация индикатора остаётся UI |
| Значения Slider | `V3DeviceSettingsRepository` → общий V3SliderSettingsController → V3SpecialSettingsViewModel → подписка SpecialSettingsFragment → пассивный Slider adapter; запись через UseCase | Реализовано в A2; Base предоставляет только render/callback к общему адаптеру и создание зависимости |
| Значения ToggleSlider/Spinner | Все применимые ToggleSlider и обычные Spinner SpecialSettings получают значения через repository → ViewModel → UiState | ToggleSlider завершены в A4, обычные Spinner — в A5.1–A5.2; профили настроек отдельно |
| Доступность | `UiState.v3WidgetsInteractionEnabled`; её меняет в том числе ControllerBleStatusConnection, есть включение для эмуляции | Поток доступности через repository-контракт; не выводить готовность только из GATT connect |
| Синхронизация | `startupInProgress`, `fullInitInProgress`, `widgetsLoadingProgressFlow`, событие `widgetsLoadingFlow` | Наблюдаемое состояние через Android-адаптер существующего источника |
| Профили настроек | V3SettingsProfilesRepository → GetSettingsProfilesUseCaseV3 → экранная ViewModel; выбор по ID через SelectSettingsProfileUseCaseV3, создание через CreateSettingsProfileUseCaseV3, имя через RenameSettingsProfileUseCaseV3 | Чтение/выбор/создание/переименование перенесены в A5.3.1–A5.3.4; диалог отображает Fragment |
| Анимации | `WidgetState.dbSnapshotAppliedWithCrc` подавляет анимации значений | Presentation получает политику анимаций; адаптер не читает глобальное состояние |

`updateFlow` — `SharedFlow<Int>(replay = 1)`, а не снимок данных и не подтверждение
завершения синхронизации. Например, обновление списка плат также эмитит `1`.
Первичная подписка должна читать текущий список даже при отсутствии нового события.
Повторное событие с тем же составом не сбрасывает черновики и не отправляет BLE.

`ParameterStoreV3.values` содержит снимок, а `updates` имеет `replay = 0`.
Новый источник значений использует снимок и наблюдение за ним, чтобы восстановиться
после паузы и заметить `clear()`, который не публикует отдельные ключи в `updates`.
Ключ store содержит адрес, parameterID и dataCode, но не dataOffsets. Для выбора
поля/параметра сохраняем полное соответствие из ParameterInfoRegistry.

Тип устройства `STANDARD_V3/INDY3` и выбранный пользователем **профиль настроек** —
разные понятия. `activeV3DeviceProfile` и serial менеджера сейчас обычные поля,
не StateFlow. Источник читает их при создании/возврате экрана и обновлении состава;
для смены устройства нужен явный сигнал Android-сессии, а не опрос в UI.
Ключи одинаковых параметров не должны переносить черновики между устройствами.

## 3. Контракт полного V3SpecialSettingsUiState

Далее — целевые поля. selectedSection и sliders реализованы в A2;
deviceProfile и widgets — в A3, все применимые ключи toggleSliders — в A4,
spinners для HAND_CONTROL_MODE — в A5.1 и GESTURE_CHANGE_MODE — в A5.2;
список/активный ID settingsProfiles — в A5.3.1. Остальные добавляем по A5–A6,
без пустого каркаса всех классов заранее.

| Поле | Содержание и правило |
| --- | --- |
| `deviceProfile` | Текущий тип устройства V3. Для NOT_V3 этот путь экрана не активируется |
| `selectedSection` | Настройки протеза или приложения; восстанавливается из LAST_ACTIVE_SETTINGS_FILTER |
| `widgets` | Упорядоченный неизменяемый список выбранной вкладки: `V3SpecialSettingsWidget` с вариантами `Slider`, `ToggleSlider`, `Spinner`, `SettingsProfile`, `Switch` для AUTO_LOGIN |
| `sliders` | Существующий `Map<String, SliderUiStateV3>`: значение/черновик, domain-диапазон, доступность и признак анимации |
| `toggleSliders` | Состояния по parameterKey: значение времени, включённость функции, диапазон, отдельно доступность переключателя и ползунка |
| `spinners` | `Map<String, SpinnerUiStateV3>`: отображаемый `selectedIndex` и `isEnabled`; варианты и подписи задаёт описание виджета. Подключены HAND_CONTROL_MODE и GESTURE_CHANGE_MODE; отсутствующий в составе ключ имеет null/false |
| `settingsProfiles` | Nullable: отсутствует, если нет виджета. Список `{profileId, customName}`, activeProfileId, canCreate, isEnabled, isLoading/loadFailed чтения; operation/failedOperation (SELECT, CREATE или RENAME), null означает отсутствие операции/ошибки. nameEditor: запрос диалога с requestId и профилем или null |
| `mobileSettings` | Значение «Автоматический вход» из настроек приложения; не подчиняется BLE-блокировке параметров протеза |
| `synchronization` | Снимок startup/fullInit и счётчиков current/total; неизвестный total не означает 100% или готовность |
| `isInteractionEnabled` | Текущий общий допуск к действиям, из существующего источника доступности |
| `operationError` | Nullable, только фактически полученная ошибка сценария экрана; не выдуманное подтверждение/ошибка BLE |

Описание виджета хранит `id`, `parameterKey`, `position`, `title` и нужные для
представления метаданные: единицу/масштаб ToggleSlider либо варианты Spinner.
Для мобильного Switch используется существующий mobileKey, не ParameterInfo V3.
Значения живут в соответствующих состояниях по ключам, не дублируются внутри
описаний. Ограничения записи определяются domain; presentation не передаёт
диапазон из XML или widget-структуры в UseCase как источник правил.

Сохраняем идентификаторы существующих адаптеров внутри состава данного экрана:
`slider-{address}-{parameterID}-{position}`,
`toggle-slider-{address}-{parameterID}-{position}`,
`spinner-{address}-{parameterID}-{dataCode}-{position}`.
Для действий используем parameterKey, а не подпись, индекс строки или widgetId.
Смена сессии устройства сбрасывает состояние и ожидающие записи отдельно от row ID.

Значение `null` означает отсутствие прочитанного значения, а не ноль или запрет
работы. Пока сохраняем текущие визуальные fallback: Slider показывает минимум,
ToggleSlider без данных отображает выключенное состояние и минимальное время,
Spinner начинает с индекса из структуры. Автоматическую запись fallback не делаем.
У Spinner `selectedIndex` уже учитывает этот fallback и ограничение по вариантам;
`null` здесь означает отсутствие виджета или вариантов. Исходный индекс хранится
отдельно внутри ViewModel и остаётся без изменений в repository: например, входящий
255 отображается как последний пункт, но не записывается обратно как 4.
Полученное из store значение тоже не объявляем подтверждённым устройством:
store обновляется и оптимистично после действий пользователя.

Все контейнеры — снимки; изменяемые shared-модели вроде ToggleV3 копируются
в скаляры. `Any`, widget-структуры, View/Context, BLE-пакеты и Room Entity
не входят в новый UiState или domain. Разбор текущих Items сосредоточен в одном
presentation-преобразователе. В A3 совместимость с ещё прежними адаптерами остаётся
за UI-границей; полный отказ этих адаптеров от shared-структур завершается в A4–A6.

## 4. Действия и существующие побочные эффекты

| Сценарий | Поведение, которое необходимо учесть |
| --- | --- |
| Slider | Drag меняет черновик; отпускание применяет значение; ±1 объединяются за 300 мс. Domain проверяет диапазон и доступность. Репозиторий обновляет store/профиль/кеш и ставит команду в очередь |
| ToggleSlider: время | Drag меняет отображаемый текст. Отпускание и ±1 сейчас сразу обновляют store/профиль/кеш, отправка в очередь отложена на 300 мс для конкретного виджета |
| ToggleSlider: включение | Переключает флаг, сохраняя время; та же локальная запись и отложенная отправка. Ползунок/± доступны только при включённой функции и общем допуске; переключатель — по общему допуску |
| Обычный Spinner | Пользовательский выбор сразу обновляет store, сохранение профиля и кеш, затем очередь. Программный выбор при render не должен вызывать запись |
| Профиль настроек | Выбор по profileId, создание копии активного профиля, переименование — разные действия. Выбор/создание может применить много BLE- и мобильных настроек через SettingsProfileApplierV3 |
| Выбор вкладки | Сохраняет выбор и меняет видимый состав. Возврат к протезу показывает актуальные значения; сам выбор не отправляет BLE |
| Автоматический вход | Сохраняет настройку приложения, программная отрисовка не записывает её и не отправляет BLE |
| Lifecycle | STARTED подключает отображение и читает актуальные значения; STOP/уничтожение view отменяет ожидающие UI-записи и освобождает представления |

Для новых действий сохраняем названия виджетов, например
`ToggleSliderValueChanged`, `ToggleSliderChangeCommitted`, `ToggleSliderStepClicked`,
`ToggleSliderEnabledChanged`, `SpinnerValueSelected`, `SettingsProfileSelected`,
`SettingsProfileCreateRequested`, `SettingsProfileRenameRequested`, `SettingsSectionSelected`,
`AutoLoginChanged`.
Это события presentation. UseCase выделяется под применение настройки или операцию
профиля, а не под каждое движение пальца/открытие диалога.

ToggleSlider передаёт в domain время и флаг раздельно. Data сохраняет существующий
формат байта: bit7 — включённость, bits0..6 — время (`0..127` транспортно).
Пример: время 25 и включённость true → `0x99`; UI показывает `2.5 сек`.
Ограничение пользовательского времени `10..100` не означает, что нужно переписать
полученный байт 0 при первом показе. Текущий код при выключении сохраняет его время.

Найденные особенности для проверки A4, без исправлений в A1:

- Сейчас таймер ToggleSlider на финише проверяет `isAttached`, но не живой допуск
  подключения. Перенос должен выполнить уже принятое правило отмены при блокировке
  и STOP; отдельно проверить рассогласование локального значения и отменённой отправки.
- В текущем S-bind `labelCodes` остаётся `-1`, поэтому ветка отображения `∞`
  не активируется для этих ToggleSlider. Не вводить бесконечность по предположению.
- `Fragment.hide()` не гарантирует STOP. Сохранить текущую навигацию и отдельно
  проверить её поведение, не приравнивать скрытие к уничтожению представления.
- Выбор вкладки приложения также не вызывает STOP. В A2 реализована отмена
  ожидающих записей Slider и восстановление значений при возврате. Для прочих
  типов виджетов это проверяется при их переносе. После A4 каждый ToggleSlider SpecialSettings
  отменяет ожидающую отправку при APPLICATION/STOP, блокировке подключения,
  исчезновении виджета и смене устройства. Уже выполненные локальное сохранение,
  обновление store и профиля не откатываются; возврат читает актуальный локальный снимок.

## 5. Профили настроек: отдельная граница

Список берётся из Room через V3SettingsProfilesRepository для явно переданного serial. Профили
сортируются по ID; пользовательское имя либо локализованное «Профиль №…»; максимум
3 профиля, затем пункт `+` отсутствует. ID профиля не равен индексу Spinner.
Имя обрезается по краям, должно быть непустым и не длиннее 30 символов.

Создание делает снимок текущих BLE-значений активного профиля, копирует его и
выбирает новый. При пропусках после импорта берётся свободный ID в 1..3
(например, [1, 3] → 2). Переключение выбирает существующий профиль и возвращает набор
значений для применения. Переименование меняет имя в БД без отправки BLE.
`P_KEY_SETTINGS_PROFILE` сохраняется в store/кеше как локальный индекс и исключён
из обычного сохранения параметров профиля. Нельзя заменить эти операции одним
`sendCommand(..., selectedIndex)`.

После V3 init BLEController отдельно скачивает серверные профили и применяет их
через тот же SettingsProfileApplierV3. Это происходит и без открытого SpecialSettings.
При подписке экрана повторять скачивание/применение нельзя.

В A5.3.1 добавлена Android-инвалидация V3SettingsProfilesUpdates: её вызывают
callback завершения локальных операций и Ubi4SettingsProfileReceiver после
существующего импорта/попытки применения. ViewModel перечитывает список; загрузка
с сервера и применение не повторяются. Обычный store-пакет этого не вызывает.
Shared API не менялся. Чтение существующего repository может создать профиль
по умолчанию в БД, как прежде. Запрос привязан к serial; поколение запроса и
проверка coroutine lifecycle отсекают поздний ответ перед обновлением UiState
и локального индекса. При возврате на экран список читается заново; одинаковое
событие состава не запускает повторное чтение. Миграция serial и очередность
операций изменения профилей этим шагом не переработаны.

Состояние выполняемой операции означает локальное чтение/создание/выбор/
переименование. «Значения поставлены в очередь» не означает «всё применено устройством».
С A5.3.2 для выбора и с A5.3.3 для создания отменяются таймеры Slider/ToggleSlider SpecialSettings и блокируются
новые действия параметров этого экрана. awaitPendingWrites(serial) общего менеджера
ожидает завершения уже зарегистрированных BLE- и мобильных сохранений этого serial;
другой serial не задерживает операцию, отмена ожидания не отменяет сохранения.
Сохранение остаётся асинхронным с прежней обработкой ошибок в логе. Data сверяет
serial/MAC/доступность и cancellation, меняет активный профиль, обновляет локальный
индекс и передаёт прежний набор значений существующему applier, внедрённому в конструктор.
STOP/уход с раздела/смена устройства/блокировка отменяют профильную операцию; завершённые изменения
БД не откатываются, после ошибки или возврата читается фактическое состояние.
Общей транзакции с серверным импортом, миграцией serial и операциями других экранов нет.

В A5.3.4 открытие редактора передаёт ID профиля, а сохранение/отмена — requestId
конкретного диалога. Старый callback не может изменить профиль или закрыть новый
диалог, в том числе повторно открытый для того же ID. Перечитывание списка
(например, после импорта), STOP, смена устройства/serial/раздела и блокировка
сбрасывают nameEditor; Fragment также удаляет диалог при STOP/destroyView.
Domain проверяет ID, наличие профиля и имя через V3SettingsProfileNameRules;
неизменное пользовательское имя не записывается повторно. Data фиксирует serial,
проверяет доступность/cancellation и вызывает прежний renameProfile. Активный
профиль, его значения и BLE не меняются. Ожидание сохранений и отмена таймеров
параметров нужны для выбора/создания; переименование их не вызывает.
Во время сохранения имени остальные профильные операции заблокированы.
Диалог закрывается при сохранении, как прежде; неуспех отражается в failedOperation,
список перечитывается, повторного сохранения при render/возврате нет.

## 6. Загрузка и ошибки

У SpecialSettingsFragment нет pull-to-refresh. Изначально анализ ошибочно включал
обработчик AdvancedFragment; переносить его сюда не нужно.
Существующий refresh других экранов через BaseWidgetsFragment устанавливает `fullInitInProgress`, подключает существующий
SyncProgressDialog и вызывает `refreshWidgetsV3BySwipe()`. Тот запускает
`initRequestsV3()` и снимает fullInit после постановки запросов в очередь.
Прогресс отдельно считает ожидаемые ответы; при завершении публикуется
`widgetsLoadingFlow` и вызывается обработчик подключения. Это разные моменты.

В SpecialSettings `updateFlow` только перестраивает текущий список и при необходимости
индикатор выбранной вкладки; завершение синхронизации из него не выводим.

SyncProgressDialog принадлежит Activity и уже наблюдает startup/fullInit, прогресс
и событие завершения. Контракт SpecialSettings отражает эти источники для экрана;
дублирующий модальный диалог или новая инициализация не нужны. При возврате берём
StateFlow-снимки: событие `widgetsLoadingFlow` не имеет replay и могло быть пропущено.

Сейчас нет общего потока ошибок SpecialSettings. Ошибки notify/запроса device data
показываются через toast из BLEController, ошибки загрузки/фонового сохранения
профиля пишутся в лог. С A5.3.4 ошибка выбора/создания/переименования отражается
в failedOperation=SELECT/CREATE/RENAME; общая презентация ошибок остаётся A6. В A5–A6 результат операции, если доступен, преобразуем в ошибку экрана;
не извлекаем ошибки из логов и не придумываем ACK, rollback или новые retries.
Сообщения существующего sync-сценария остаются у его владельца без дублирования.
Переименование показываем через существующий диалог; его состояние/закрытие
связаны с выбранным profileId и lifecycle представления.

## 7. Проверки для следующих шагов

Это перечень обязательных проверок реализации, не отчёт об уже выполненных тестах.

| Шаг | Что проверяем |
| --- | --- |
| A2 | Один владелец SpecialSettings VM и одна подписка render; Slider Sensors/Service продолжают работать; attach/STOP/recreate и переход к настройкам приложения не дублируют запись |
| A3 | Для обоих профилей таблица выше совпадает с `prepareData(2)` по типам, ключам, позициям, ID, подписям и вариантам RU/EN; строки других display не попадают в SpecialSettings |
| A3 | Первое чтение без события, replay и одинаковые обновления состава; значения до/после списка; `ParameterStore.clear()`; смена устройства; отсутствие BLE при наблюдении |
| A3/A6 | Восстановление LAST_ACTIVE_SETTINGS_FILTER, сохранение обеих вкладок, отсутствие V3 Slider в разделе приложения; AUTO_LOGIN меняет только существующую мобильную настройку |
| A4 | Упаковка флага/времени, 10/100 и входящее 0, шаг 0.1 сек, независимые таймеры, выключение функции, блокировка подключения, STOP и повторный bind |
| A5 | Обычный Spinner отправляет только пользовательский выбор; профиль выбирается по ID, максимум 3, создание/имя/применение/серверный импорт; нет повторной отправки при render |
| A6 | Startup, неизвестный total, progress/окончание, reconnect и возврат экрана; сообщения не дублируются; финальная проверка V3 и INDY3 на устройстве |

A1 выполнен чтением кода и сверкой путей данных, затем исправлен после проверки
фактического маршрута. Новые автоматические тесты, сборка и проверки на устройстве
в самом A1 не запускались. Проверки исправления привязки A1.1 учитываются отдельно
в основном плане. A2 завершён: Android-сборка и 77 app-тестов прошли
(2026-09-09 13:30:31–32 UTC), включая 9 проверок новой экранной модели.
После A3 сборка и 91 app-тест прошли (2026-09-09 14:01:12–13 UTC),
включая 12 новых проверок A3. Проверены неизменённые генераторы STANDARD_V3/INDY3
с ресурсами RU/EN, обе вкладки, метаданные адаптеров, первые/повторные события,
смена устройства, поздние значения/состав и очистка ParameterStore без отправки.
После A4.1 сборка и 111 app-тестов прошли (2026-09-09 14:25:18–19 UTC),
включая 20 новых интеграционных проверок первого ToggleSlider. Проверены
оба типа устройства, флаг/время/границы, кеш/store/clear, отсутствие записи
при наблюдении/drag, немедленное сохранение и пакет после 300 мс, отмена
таймера и восстановление после APPLICATION. Android view/устройство не
проверялись.
После A4.2 сборка и 118 app-тестов прошли (2026-09-09 14:40:24 UTC),
включая 7 новых проверок дополнительных параметров: свои команды/кеш/профильные
ключи, границы, переключение флага, независимые таймеры, удаление одного виджета,
общая отмена и переход в INDY3. Android view/устройство не проверялись.
После A5.1 сборка и 136 app-тестов прошли (2026-09-10 08:07:24 UTC),
включая 16 интеграционных проверок первого Spinner и 2 проверки реального адаптера
PowerSpinner с заглушками View/перерисовки на JVM. Проверены пять режимов, порядок
сохранения/команды, кеш/store, отсутствие записи при наблюдении/возврате/блокировке,
fallback/clamp и восстановление listener после программного выбора.
Android view/устройство не проверялись.
После A5.2 сборка и 146 app-тестов прошли (2026-09-10 08:20:00–02 UTC):
10 новых проверок второго Spinner, включая независимость ключей и блокировку
в INDY3; существующая проверка генераторов дополнена вариантами RU/EN.
Android view/устройство не проверялись. A5.3.1 подключает список/активный ID;
выбор существующего профиля перенесён в A5.3.2.
После A5.3.1 сборка и 164 app-теста прошли (2026-09-10 08:36:47–49 UTC):
поздние ответы/STOP/очистка ViewModel, ID и индекс, предел/профиль по умолчанию,
инвалидация после импорта, отсутствие повторного применения и программный выбор
в реальном SettingsProfileSpinnerAdapterV3 с заглушками Android View на JVM.
Реальный экран и выбор/создание/переименование на телефоне не проверялись.
После A5.3.2 сборка и 187 app-тестов прошли (2026-09-11 08:56:39–42 UTC):
выбор по ID, отмена таймеров/повторных действий, ожидание реальных фоновых сохранений
KMM-менеджера, отмена/другой serial, поздний ответ, ошибка и явный повтор.
На устройстве и iOS не проверялось.
После A5.3.3 сборка и 215 app-тестов в 25 suites прошли (2026-09-11 09:13:13–16 UTC),
0 ошибок/пропусков; `/tmp/ubi4-special-settings-a5-3-3-20260911.log`.
Добавлены проверки создания: копирование реальным KMM repository, предел 3,
пропуски ID после импорта, ожидание сохранений, блокировка повторных действий,
отмена и ошибка без автоповтора. На устройстве/iOS не проверялось; shared-тесты
по-прежнему отложены. На этом этапе далее оставалось переименование.
Виджет скрыт для обычного V3 существующим SETTINGS_PROFILE_WIDGET_ENABLED=false;
в INDY3 он включён. Пользователь подтвердил обычный V3; флаг в парсере не менялся.

## Основные исходники

- [SpecialSettingsFragment][fragment] и [временная связь в BaseWidgetsFragment][base].
- [Экранная ViewModel][viewmodel] и [общая логика Slider][slidercontroller].
- [Маршруты нижнего меню][navigation] и [открытие настроек в Activity][route].
- [ToggleSliderDelegateAdapterV3][toggle] и [SpinnerDelegateAdapterV3][spinner].
- [ParameterStoreV3][store] и [глобальные сигналы UiState][globalstate].
- [V3 init/refresh в BLEController][controller] и [SyncProgressDialog][sync].
- [SettingsProfileRepository и SettingsProfileManager][profiles],
  [применение набора значений][applier] и [Android-получатель серверного профиля][receiver].

[parser]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/data/parser/BLEParserV3.kt:790
[factory]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/data/DataFactory.kt:138
[strings]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/moko-resources/strings/ru/strings.xml:823
[fragment]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ui/fragments/SpecialSettingsFragment.kt:30
[base]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ui/fragments/base/BaseWidgetsFragment.kt:325
[toggle]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/adapters/widgetDelegateAdaptersV3/ToggleSliderDelegateAdapterV3.kt:68
[spinner]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/adapters/widgetDelegateAdaptersV3/SpinnerDelegateAdapterV3.kt:77
[store]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/data/state/ParameterStoreV3.kt:49
[globalstate]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/data/state/UiState.kt:11
[controller]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ble/BLEController.kt:881
[sync]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ui/dialog/SyncProgressDialog.kt:72
[profiles]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/shared/src/commonMain/kotlin/com/bailout/stickk/ubi4/data/local/repository/SettingsProfileRepository.kt:110
[applier]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/adapters/widgetDelegateAdaptersV3/SettingsProfileApplierV3.kt:22
[receiver]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/data/network/Ubi4SettingsProfileReceiver.kt:5
[navigation]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ui/bottom/BottomNavigationController.kt:54
[route]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/ui/main/MainActivityUBI4.kt:630
[viewmodel]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/versions/v3/presentation/specialsettings/V3SpecialSettingsViewModel.kt:18
[slidercontroller]: /Users/denisoshkin/StudioProjects/GitHub/2_Android_bluetooth_master_stradivary/app/src/main/java/com/bailout/stickk/ubi4/versions/v3/presentation/sliders/V3SliderSettingsController.kt:16

После A5.3.4 сборка и 252 app-теста в 25 suites прошли (2026-09-11 09:25:26–31 UTC),
0 ошибок/пропусков; `/tmp/ubi4-special-settings-a5-3-4-20260911.log`.
Проверены правила имени/ID, отмена редактора и старых callback, отсутствие apply,
повторные действия, ошибки, lifecycle и сохранение таймеров параметров при rename.
Реальный KMM repository проверен app-тестом на сохранность значений/активного профиля.
Shared-код, парсеры, iOS и флаг видимости не менялись в A5.3.4. Android view/телефон
и iOS-сборка не проверялись. Далее A6, начиная с «Автоматического входа».
