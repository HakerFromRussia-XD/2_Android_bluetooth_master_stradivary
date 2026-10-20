# MS firmware hosting

## Upload firmware

Open the WebClient:

- `https://firmware-admin-ms.157.22.203.6.nip.io/web/client/`

It contains one working folder:

```text
firmware/
  assembly.json
  FAM/
  GUI/
  EMG/
  BLDC_Driver/
  FINGER/
```

Upload, replace, or delete `assembly.json` and firmware ZIP files directly in
this folder. There is no staging workflow and no publish button.

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
