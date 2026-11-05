# MS games catalog

Both Android and iOS read the same public catalog:

- `https://firmware-ms.157.22.203.6.nip.io/games/manifest.json`

Open the existing file manager using the existing firmware account:

- `https://firmware-admin-ms.157.22.203.6.nip.io/web/client/`

The account and the `firmware/` folder retain their current permissions. To
change the game catalog, upload or replace `games/manifest.json`. There is no
separate publish button. `ms-games-catalog.service` checks that file every second,
validates its entries, and atomically replaces the public copy. Incomplete JSON,
invalid platform metadata, and duplicate game IDs or platform packages leave the
previous valid catalog online. Deleting the uploaded file also keeps the last
published catalog online.

The JSON has one `games` array. Each game has `id`, `title`, and at least one
platform object. The Android object contains `packageName`, `launcherActivity`,
`versionName`, a non-negative signed 64-bit `versionCode`, and optional boolean `usesMotoricaControl`.
The iOS object contains `bundleId`, `urlScheme`, `versionName`, integer
`versionCode`, and `appStoreUrl`; leave that URL empty until the app is published.
An entry can support one platform or both. Existing flat Android fields remain
supported. Unknown fields and the original uploaded JSON bytes are preserved.
There is no fixed number of games.

Save the file as UTF-8 without a byte-order mark (BOM). The reserved Android
`stk` entry must retain `packageName: com.motorica.games.stk` for compatibility.
iOS bundle identifiers use ASCII letters, digits, periods, and hyphens; their
uniqueness is checked without regard to letter case.

For a game update, publish the game build in its store first, then raise that
platform's `versionCode` in this file. Android uses the APK's `versionCode`; iOS
uses the game's integer `CFBundleVersion`. The two platforms have independent
version sequences. `versionName` is descriptive and is not compared.

On iOS the game must also report its installed bundle/version metadata through
the existing signed App Group. Sushi Dash writes `installedGame.sushidash` at
cold start and foreground; older game binaries without this writer cannot
report their installed version. Motorica Start uses the marker only for the
version after the OS confirms installation, and never substitutes the remote
version for an unknown installed version. Without a store URL, an installed
iOS game remains playable even when a newer version is listed.

Runtime paths:

- Upload: `/srv/ms-firmware/staging/games/manifest.json`
- Public copy: `/srv/ms-firmware/games/manifest.json`
- Previous public versions, named with time and SHA-256:
  `/srv/ms-firmware/games/history/`
- Publisher: `/usr/local/lib/ms-games/catalog_watcher.py`
- Service: `/etc/systemd/system/ms-games-catalog.service`

Only the exact public `/games/manifest.json` URL is mapped by Nginx; history and
staging are not exposed. The public response uses `Cache-Control: no-store`.
Existing firmware daily backups also include the games data and publisher.

To validate and publish once on the server:

```sh
sudo -u ms-upload /usr/bin/python3 /usr/local/lib/ms-games/catalog_watcher.py --once
```

To run the publisher regression checks locally:

```sh
python3 -m unittest discover -s ops/ms-games -p 'test_*.py' -v
```
