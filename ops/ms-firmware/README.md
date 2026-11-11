# MS firmware hosting

## Motorica WebClient (тот же интерфейс, что на FVDS)

Локальная панель использует **SFTPGo 2.7.6**, как существующая панель FVDS.
Интерфейс и форма входа оригинальные, без отдельной реализации фронтенда.
Файлы читаются непосредственно из API Motorica через `motorica_httpfs.py`.

Запуск из корня проекта (Python 3.12+):

```bash
python3 ops/ms-firmware/firmware_panel.py
```

Открыть `http://127.0.0.1:8765/web/client/`. Логин `ms-firmware`, пароль тот же,
что на FVDS. Файл прежнего пароля находится в
`ops/ms-firmware/.panel-runtime/webclient.password` с правами `0600`.
Каталог `.panel-runtime` исключён из Git. Не добавляйте его в коммиты или архивы
исходников. Ключ Motorica остаётся в Python-процессе и не передаётся браузеру.
Используется `MOTORICA_WRITE_KEY` либо существующий `WRITE_KEY` из скрипта
миграции. При переносе на другой компьютер предпочтительна переменная окружения.

Возможности:

- просмотр рабочих файлов, поиск и скачивание с проверкой SHA-256;
- оригинальные `assembly.json` и `catalog.json` в корне `firmware`, без изменения
  байтов; JSON хранятся в API с техническим BoardModel `MS_FIRMWARE_METADATA`,
  адаптер показывает только последнюю редакцию каждого под исходным именем;
- техническая модель JSON и тестовая `TEST-DO-NOT-FLASH` скрыты из дерева;
- папки `firmware/<BoardModel>`; загрузка ZIP в папку выбирает модель платы;
- создание папки модели (пустая папка хранится в состоянии панели, после первой
  загрузки модель также появляется в API; вложенные подпапки не поддерживаются);
- версия определяется по `FW_ini.ini`, проверяются размер и CRC `UserFW.bin`;
- ZIP до 100 МиБ; распакованное содержимое до 64 МиБ согласно текущему валидатору;
- после загрузки архив скачивается из Motorica и проверяется по SHA-256;
- совпадающий файл не загружается повторно; ZIP нельзя перезаписать другим
  содержимым, загрузка новой редакции JSON сохраняет новую запись API;
- повторные имена из API не скрываются: старые записи показываются с суффиксом Id;
- удаление и переименование отключены; эти методы API ещё не предоставлены.

Для установки на другой машине:

```bash
python3 ops/ms-firmware/install_panel_sftpgo.py
```

Установщик загружает официальный пакет SFTPGo 2.7.6 для macOS/Linux и проверяет
SHA-256 по метаданным релиза GitHub. Пароль нужно передать локальным файлом через
`--password-file /path/to/webclient.password` (без пароля в аргументах команды).
Альтернатива: установленный SFTPGo с флагами `--binary /usr/bin/sftpgo`
и `--resources /usr/share/sftpgo`. Порт меняется через `--port`.

Панель и внутренний адаптер слушают только `127.0.0.1`. Для общего доступа
на сервере компании понадобится размещение этих файлов и HTTPS reverse proxy
перед портом панели. API Motorica и FVDS эта установка не перенастраивает.
Локальная панель после установки работает без доступа к FVDS.
Авторизацию/CSRF и сессии обеспечивает SFTPGo; отдельного входа в админку нет.
Временная копия учётной записи удаляется сразу после импорта в SFTPGo.
Папки и последний результат загрузки сохраняются в `.panel-runtime/state`.
JSON можно загружать в корень `firmware`; проверяется синтаксис и структура,
содержимое не переписывается. Исходный `catalog.json` описывает прежние пути;
сохранение его в API само по себе не переключает мобильное приложение.
Каталог игр здесь не редактируется.

Проверки (без загрузки тестовых прошивок в Motorica):

```bash
python3 -m unittest discover -s ops/ms-firmware -p 'test_*.py'
```

Интеграционный тест поднимает настоящий SFTPGo с отдельным временным паролем
и изолированным API, входит через HTML-форму и загружает ZIP через тот же
обработчик, что использует браузер. Если SFTPGo ещё не установлен, этот тест
пропускается.

## Migration to Motorica

Run from the repository root with Python 3.10+ (standard library only):

```bash
python3 ops/ms-firmware/migrate_to_motorica.py
```

This downloads and validates every ZIP in the public FVDS `catalog.json`, checks
size, SHA-256, internal firmware CRC and version, and backs up `assembly.json`
and `catalog.json`. Default output: `/tmp/motorica-firmware-migration`. Use
`--work-dir /path/to/persistent-backup` to keep it outside temporary storage.
Invalid ZIPs and archives absent from the source catalog are not migrated.
Pause changes to the FVDS firmware folder during migration for a consistent copy.

Set `WRITE_KEY = "..."` near the top of `migrate_to_motorica.py`, or preferably
provide `MOTORICA_WRITE_KEY` through the environment. Never commit a real key.
Then explicitly enable uploading:

```bash
python3 ops/ms-firmware/migrate_to_motorica.py --upload
```

Each ZIP is POSTed as `File` with `BoardModel`, `Version`, and a description
containing the original path. Board models default to existing folder names:
`FAM`, `GUI`, `EMG`, `BLDC_Driver`, `FINGER`. Agree these with Motorica first;
override with e.g. `--board-model FINGER=BLDC_Driver`. File names and bytes are
preserved, including BLDC address suffixes. The script does not decide whether
a firmware is approved for end users.

Existing records are matched by BoardModel + Version + Sha256, so interrupted
runs can be restarted without uploading those records again. Every uploaded or
reused record is downloaded and compared byte-for-byte by size and SHA-256.
POST requests are never automatically retried. After a lost POST response, wait
until the server list reflects the upload before restarting. Do not run another
uploader concurrently; the API does not document an idempotency key.

To copy the original JSON files after the ZIP migration:

```bash
python3 ops/ms-firmware/migrate_manifests.py --upload
```

This stores both files byte-for-byte in Motorica with the metadata BoardModel
used by the WebClient adapter, and verifies downloaded copies. The main ZIP
migration command by itself only backs them up locally.

`migration-report.json` records old paths, new IDs, hashes and verification
status. `migration-plan.json` records the prepared source. The source server
and applications are not modified. The API stores the manifests as generic
files; the adapter maps these records back to their original paths.
Games, server configuration and backups outside the firmware catalog are not
transferred.

The actual API response schema must be confirmed with a key. The script expects
a JSON array from GET, and `Id`, `BoardModel`, `Version`, `Sha256` on list
records (PascalCase/camelCase accepted). POST must return `Id` and `Sha256`.
Unexpected formats stop the run instead of guessing and creating duplicates.

Offline tests:

```bash
python3 -m unittest discover -s ops/ms-firmware -p 'test_migrate_to_motorica.py'
```

## Upload firmware

Open the WebClient:

- `https://firmware-admin-ms.157.22.203.6.nip.io/web/client/`

It contains the firmware folder and a separate games folder:

```text
firmware/
  assembly.json
  FAM/
  GUI/
  EMG/
  BLDC_Driver/
  FINGER/
games/
  manifest.json
```

Upload, replace, or delete `assembly.json` and firmware ZIP files directly in
this folder. There is no staging workflow and no publish button.

Upload game catalog changes in `games/manifest.json` using the same WebClient
account. The separate games publisher validates JSON and publishes the last
valid file atomically at `https://firmware-ms.157.22.203.6.nip.io/games/manifest.json`.
See [the games catalog workflow](../ms-games/README.md).

The mobile applications read the same directory through:

- `https://firmware-ms.157.22.203.6.nip.io/assembly.json`
- `https://firmware-ms.157.22.203.6.nip.io/catalog.json`
- `https://firmware-ms.157.22.203.6.nip.io/<family>/<archive>.zip`

## Automatic catalog

`ms-firmware-catalog.service` checks `firmware` every second. After a valid
ZIP upload it recreates `catalog.json` automatically. The ZIP must contain
`UserFW.bin` and `FW_ini.ini` with matching firmware size and CRC metadata.

An incomplete or invalid ZIP is not added to `catalog.json`; the previous
catalog remains available until the upload is complete. `assembly.json` is
served exactly as uploaded. ZIP responses use `Cache-Control: no-store`, so
the phone does not continue using an old archive after a replacement.

## Internal recovery copy

The working directory is `/srv/ms-firmware/staging/firmware`. Historic
release folders remain outside WebClient for emergency recovery only. The
retired Publisher service is disabled and `/publish/` redirects to WebClient.

`ms-firmware-backup.timer` makes a daily backup in
`/var/backups/ms-firmware` and retains 30 days.
