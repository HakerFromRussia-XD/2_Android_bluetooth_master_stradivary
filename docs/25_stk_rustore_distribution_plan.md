# План переноса установки и обновления STK в RuStore

## Статус реализации — 29 июля 2026

Реализовано в Motorica Start `3.3.1787 (17)`:

- удалено `android.permission.REQUEST_INSTALL_PACKAGES`;
- удалены загрузка APK, проверка SHA-256 и установка через `PackageInstaller`;
- удалены игровой `FileProvider`, install-status receiver и их ресурсы;
- сохранены проверка установленной версии STK, запуск игры и системное удаление;
- подписи кнопки остаются «Установить», «Обновить» и «Играть»;
- «Установить» сначала проверяет публикацию карточки STK: HTTP 404 показывает
  тост «Игра пока недоступна в RuStore», опубликованная карточка открывает RuStore;
- «Обновить» сразу открывает RuStore, поскольку манифест повышается только после
  публикации соответствующей версии STK;
- при отсутствии приложения RuStore используется HTTPS-карточка;
- при возврате на экран повторно проверяются установленная версия и серверный манифест;
- серверный манифест остаётся единственным источником актуального `versionCode`;
- `apkUrl`, `sha256` и дополнительные поля игнорируются новым клиентом;
- серверные данные принимаются только для `com.motorica.games.stk`;
- добавлены unit-тесты разбора манифеста и состояний `INSTALL`, `UPDATE`, `PLAY`,
  `UNAVAILABLE`.

Собран и проверен релиз:

- файл: `app/release/app-release v3.3.1787.apk`;
- SHA-256: `1895924434b027986a724a3483ba759032031422be8cdc73efa723f3837f69f4`;
- сертификат SHA-256:
  `d84450b7734071976b89bfb6f0c79a2765934bf4c6f7e5eecc74ecb5700f76d8`;
- разрешение `REQUEST_INSTALL_PACKAGES` в итоговом APK отсутствует.

Формат STK-манифеста не меняется. Поля `versionName` и `versionCode` управляют
состоянием кнопки, а старые `apkUrl` и `sha256` сохраняются только для
переходного периода и не читаются новым клиентом.

Проверка на реальном устройстве `23028RNCAG`:

- прежняя debug-сборка с несовместимой подписью удалена по запросу пользователя;
- release `3.3.1787 (17)` установлен успешно;
- cold start выполнен, аварийного завершения процесса не обнаружено;
- приложение дошло до штатного системного запроса разрешений первого запуска;
- приложение RuStore `ru.vk.store` установлено;
- deeplink `rustore://apps.rustore.ru/app/com.motorica.games.stk` разрешается в
  `ru.vk.store/.app.MainActivity`;
- STK на устройстве пока не установлена, поэтому сценарии возврата после
  установки, запуска игры и обновления требуют отдельного прогона после
  публикации карточки STK и загрузки нового серверного манифеста.

## 1. Цель

Выпустить новую Android-сборку Motorica Start, которая:

- не содержит разрешение `android.permission.REQUEST_INSTALL_PACKAGES`;
- больше не скачивает и не устанавливает APK STK самостоятельно;
- продолжает получать с Яндекс Диска актуальные метаданные STK и определять наличие обновления;
- передаёт скачивание, установку и обновление `com.motorica.games.stk` приложению RuStore;
- сохраняет запуск уже установленной игры и пользовательское удаление игры через системный интерфейс Android.

## 2. Текущее состояние

### Motorica Start

Текущая цепочка находится в:

- `app/src/main/java/com/bailout/stickk/ubi4/ui/fragments/account/games/AccountGamesFragment.kt`;
- `app/src/main/java/com/bailout/stickk/ubi4/ui/fragments/account/games/GamePackageInstaller.kt`;
- `app/src/main/AndroidManifest.xml`;
- `app/src/main/res/xml/motorica_games_file_paths.xml`;
- `gradle.properties` и `BuildConfig.MOTORICA_GAMES_MANIFEST_URL`.

Сейчас приложение:

1. загружает `manifest.json` с Яндекс Диска;
2. сравнивает `versionCode` STK из манифеста с установленной версией;
3. скачивает `apkUrl`;
4. проверяет `sha256`;
5. запрашивает разрешение на установку из неизвестного источника;
6. устанавливает APK через `PackageInstaller`.

Шаги 3–6 должны быть полностью удалены из новой сборки.

### STK

- package name: `com.motorica.games.stk`;
- launcher activity: `com.motorica.games.stk.SuperTuxKartActivity`;
- текущий серверный манифест: `/Users/motoricallc/Downloads/stk-code-master/android/release/manifest.json`;
- обнаруженная версия в текущем локальном манифесте: `1.0.13`, `versionCode = 1013`;
- в Android-манифесте STK разрешение `REQUEST_INSTALL_PACKAGES` не обнаружено.

## 3. Целевая архитектура

```text
Motorica Start
    |
    | HTTPS: manifest.json
    v
Яндекс Диск
    |
    | versionName, versionCode, packageName, launcherActivity
    v
Определение состояния
    |
    +-- STK не установлена ------> «Установить»
    |
    +-- версия устарела ---------> «Обновить»
    |
    +-- версия актуальна --------> «Играть»
                                      |
                                      v
                     com.motorica.games.stk.SuperTuxKartActivity
```

Motorica Start не получает APK и не участвует в установочной сессии. Для установки или обновления она открывает карточку STK:

1. основной deeplink: `rustore://apps.rustore.ru/app/com.motorica.games.stk`;
2. fallback: `https://www.rustore.ru/catalog/app/com.motorica.games.stk`.

Согласно документации RuStore, HTTPS-ссылка открывает приложение RuStore, если оно установлено, иначе — веб-карточку приложения.

RuStore In-App Updates SDK в эту задачу не включается. Он обновляет само приложение, в которое интегрирован, и не предназначен для управления обновлением другого package name.

## 4. Изменение серверного манифеста

Рекомендуемая Android-секция:

```json
{
  "packageName": "com.motorica.games.stk",
  "launcherActivity": "com.motorica.games.stk.SuperTuxKartActivity",
  "versionName": "1.0.13",
  "versionCode": 1013
}
```

Правила:

- `versionName` и `versionCode` остаются источником проверки актуальности;
- `packageName` используется для проверки установки, запуска и формирования ссылки RuStore;
- новый клиент не должен читать или использовать `apkUrl` и `sha256`;
- ссылки RuStore лучше формировать из проверенного `packageName`, а не принимать произвольный URL из удалённого манифеста.

### Переходный период

Чтобы уже выпущенные версии Motorica Start не сломались сразу после изменения JSON:

1. выпустить Motorica Start без sideload-механизма;
2. временно сохранить `apkUrl` и `sha256` для старых клиентов;
3. для каждой новой версии сначала дождаться публикации STK в RuStore;
4. только после публикации повысить `versionCode` в манифесте Яндекс Диска;
5. после выбранного периода миграции удалить `apkUrl` и `sha256`.

Компромисс: пока старые поля существуют, старые версии Motorica Start всё ещё способны использовать старый способ установки. Если требуется немедленно отключить sideload для всех версий, поля можно удалить сразу, но экран игр в старых клиентах перестанет загружать карточку STK из-за обязательного `getString("apkUrl")`.

## 5. Публикация STK в RuStore

До изменения Motorica Start:

1. Создать карточку STK в RuStore для `com.motorica.games.stk`.
2. Проверить итоговый APK:
   - package name;
   - `versionName` и `versionCode`;
   - ABI и минимальную версию Android;
   - отсутствие нежелательных разрешений;
   - корректность подписи.
3. Загрузить подписанный APK/AAB и пройти модерацию.
4. Проверить карточку STK по обоим deeplink-вариантам на реальном устройстве.
5. Проверить обновление поверх ранее установленного STK.

Критично использовать сертификат, совместимый с уже распространявшимися APK STK. Android не позволит RuStore обновить установленную игру, если подписи отличаются. Перед публикацией нужно сравнить SHA-256 сертификатов:

- релизного STK, загружаемого в RuStore;
- APK STK, который ранее устанавливался через Motorica Start;
- при наличии — версии из другого магазина.

## 6. Изменения Motorica Start

### 6.1. AndroidManifest

В `app/src/main/AndroidManifest.xml`:

- удалить `android.permission.REQUEST_INSTALL_PACKAGES`;
- сохранить `<queries><package android:name="com.motorica.games.stk" /></queries>`, так как приложение проверяет установленную версию и запускает STK;
- отдельно проверить необходимость `REQUEST_DELETE_PACKAGES`; не удалять его автоматически, чтобы не сломать подтверждаемое пользователем удаление STK;
- удалить `GameInstallStatusReceiver`, если после удаления установщика он больше нигде не используется;
- удалить `FileProvider` и `motorica_games_file_paths.xml`, если итоговый поиск подтвердит отсутствие других потребителей.

### 6.2. AccountGamesFragment

В `AccountGamesFragment.kt`:

- оставить загрузку манифеста через HTTPS/Яндекс Disk API;
- оставить сравнение удалённого и установленного `versionCode`;
- удалить:
  - `downloadJob`;
  - `downloadApk`;
  - `verifySha256`;
  - `gameApkFile`;
  - `installApk`;
  - `canInstallDownloadedGames`;
  - `requestInstallDownloadedGamesPermission`;
  - регистрацию install-status receiver;
  - импорты `PackageInstaller`, `Settings`, `File`, `FileOutputStream`, `MessageDigest`;
- заменить обработку `DOWNLOAD` и `UPDATE` на открытие карточки STK в RuStore;
- при возврате в `onResume()` повторно проверять установленную версию и перерисовывать состояние;
- при ошибке основного deeplink открывать HTTPS-карточку;
- если ни RuStore, ни браузер не могут обработать ссылку, показывать локализованную ошибку;
- перед формированием deeplink проверять, что `packageName == BuildConfig.MOTORICA_STK_PACKAGE`, чтобы удалённый манифест не мог перенаправить пользователя на другой пакет.

### 6.3. Установщик

Удалить после подтверждения отсутствия других вызовов:

- `GamePackageInstaller.kt`;
- `GameInstallStatusReceiver`;
- связанные action/extra;
- локальный broadcast установки.

### 6.4. UI и локализация

Обновить тексты в базовых и русских ресурсах:

- использовать подписи `Install` / `Установить`, `Update` / `Обновить`,
  `Play` / `Играть`;
- добавить тост об ещё не опубликованной карточке STK;
- добавить ошибку открытия RuStore;
- удалить тексты прогресса скачивания APK, checksum и разрешения установки, если они больше нигде не используются.

Прогресс загрузки APK на экране больше не показывается: после нажатия пользователь переходит в UI RuStore.

## 7. Минимальная структура кода

Не проводить широкий рефакторинг экрана. Для тестируемости достаточно вынести:

- разбор записи STK из JSON в отдельный parser/repository;
- формирование и запуск deeplink в `RuStoreGameLauncher`;
- вычисление UI-состояния `INSTALL`, `UPDATE`, `PLAY`, `UNAVAILABLE` в чистую функцию.

Это позволит протестировать изменение без зависимости от `Fragment` и реального RuStore.

## 8. Тесты

### Unit-тесты

1. STK не установлена, манифест доступен → `INSTALL`.
2. Установленная версия ниже `versionCode` → `UPDATE`.
3. Версии равны или локальная выше → `PLAY`.
4. Манифест недоступен, STK установлена → `PLAY`.
5. Манифест недоступен, STK отсутствует → `UNAVAILABLE`.
6. Package name отличается от `MOTORICA_STK_PACKAGE` → безопасный отказ.
7. JSON как с `apkUrl`/`sha256`, так и без них успешно разбирается новым клиентом.

### Проверка сборки

1. Выполнить минимальную Kotlin-компиляцию:

   ```bash
   ./gradlew :app:compileDebugKotlinAndroid
   ```

2. Собрать релизный APK/AAB.
3. Проверить итоговый merged manifest и сам APK:
   - `REQUEST_INSTALL_PACKAGES` отсутствует;
   - install-status receiver отсутствует;
   - ненужный FileProvider отсутствует;
   - запрос видимости `com.motorica.games.stk` сохранён.
4. Проверить, что транзитивная зависимость не возвращает `REQUEST_INSTALL_PACKAGES`.

### Проверка на реальном устройстве

| Сценарий | Ожидаемый результат |
|---|---|
| RuStore установлен, STK отсутствует | Кнопка открывает карточку STK в RuStore |
| RuStore установлен, STK устарела | Кнопка открывает карточку с доступным обновлением |
| RuStore не установлен | Открывается веб-карточка STK |
| STK актуальна | Кнопка запускает игру |
| Возврат после установки | Экран меняется на состояние «Установлена»/«Играть» |
| Возврат после обновления | Исчезает состояние «Доступно обновление» |
| Пользователь отменил установку | Motorica Start остаётся работоспособной |
| Нет сети | Уже установленная STK по-прежнему запускается |
| Удаление STK | Открывается системное подтверждение удаления |

## 9. Порядок внедрения

1. Подготовить и проверить подписанный релиз STK.
2. Реализовать переход Motorica Start на RuStore.
3. При отсутствии карточки проверить тост после нажатия «Установить».
4. Опубликовать STK в RuStore и получить рабочую карточку.
5. Проверить установку и обновление STK через RuStore на реальном устройстве.
6. Удалить sideload-код и `REQUEST_INSTALL_PACKAGES`.
7. Обновить локализацию и тесты.
8. Собрать APK/AAB Motorica Start и проверить фактические разрешения итогового артефакта.
9. Прогнать матрицу реальных сценариев.
10. Отправить новую сборку Motorica Start на повторную модерацию.
11. После периода миграции удалить `apkUrl` и `sha256` с Яндекс Диска.

## 10. Критерии готовности

- итоговый APK/AAB Motorica Start не содержит `REQUEST_INSTALL_PACKAGES`;
- в коде Motorica Start нет скачивания APK STK и вызовов `PackageInstaller`;
- версия STK по-прежнему определяется по манифесту Яндекс Диска;
- установка и обновление выполняются только через карточку STK в RuStore;
- установленная STK запускается из Motorica Start;
- после возврата из RuStore состояние экрана обновляется;
- при отсутствии RuStore есть безопасный переход на официальную веб-карточку;
- сертификат STK в RuStore совместим с ранее распространявшейся версией;
- проверены итоговый merged manifest и релизный артефакт, а не только исходный `AndroidManifest.xml`.

## 11. Официальные источники

- Deeplinks RuStore: <https://www.rustore.ru/help/sdk/rustore-deeplinks>
- Публикация приложения: <https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication>
- Подписи APK/AAB: <https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication/apk-signature>
- SDK обновлений RuStore: <https://www.rustore.ru/help/guides/sdk-in-app-updates>
