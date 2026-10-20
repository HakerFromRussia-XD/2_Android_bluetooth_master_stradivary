#!/bin/sh
set -eu
umask 077

root=/srv/ms-firmware
backup_root=/var/backups/ms-firmware
stamp=$(date -u +%Y%m%dT%H%M%SZ)
archive="$backup_root/ms-firmware-$stamp.tar.gz"

install -d -m 0750 -o root -g root "$backup_root"
exec 9>"$root/.publish.lock"
flock -x 9
tar -C / -czf "$archive" \
  srv/ms-firmware/staging \
  srv/ms-firmware/releases \
  srv/ms-firmware/current \
  srv/ms-firmware/publication-log \
  etc/ms-firmware \
  usr/local/lib/ms-firmware \
  usr/local/sbin/ms-firmware-backup \
  etc/nginx/ms-firmware-publisher.htpasswd \
  etc/systemd/system/ms-firmware-files.service \
  etc/systemd/system/ms-firmware-catalog.service \
  etc/systemd/system/ms-firmware-publisher.service \
  etc/systemd/system/ms-firmware-backup.service \
  etc/systemd/system/ms-firmware-backup.timer \
  etc/nginx/sites-available/ms-firmware-public \
  etc/nginx/sites-available/ms-firmware-admin
gzip -t "$archive"
sha256sum "$archive" > "$archive.sha256"
find "$backup_root" -type f -name 'ms-firmware-*.tar.gz' -mtime +30 -delete
find "$backup_root" -type f -name 'ms-firmware-*.tar.gz.sha256' -mtime +30 -delete
