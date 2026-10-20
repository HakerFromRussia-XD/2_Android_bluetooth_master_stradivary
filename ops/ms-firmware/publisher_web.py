#!/usr/bin/env python3
import argparse
import html
import hmac
import os
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from publish_firmware import publish_release, rollback_release


class PublisherHandler(BaseHTTPRequestHandler):
    root = Path("/srv/ms-firmware")
    csrf_token = ""

    def do_GET(self):
        if self.path.rstrip("/") not in ("", "/publish"):
            self.send_error(404)
            return
        self.render()

    def do_POST(self):
        actions = {
            "/publish": publish_release,
            "/rollback": rollback_release,
        }
        action_name = self.path.rstrip("/")
        if action_name not in actions:
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_error(400)
            return
        if length < 1 or length > 4096:
            self.send_error(400)
            return
        values = urllib.parse.parse_qs(self.rfile.read(length).decode("utf-8"))
        token = values.get("csrf", [""])[0]
        release_id = values.get("release", [""])[0]
        if not hmac.compare_digest(token, self.csrf_token):
            self.send_error(403)
            return
        try:
            result = actions[action_name](self.root, release_id)
            self.render(message=f"{result['status']}: {result['release']}")
        except Exception as error:
            self.render(error=str(error), status=400)

    def render(self, message="", error="", status=200):
        staging_root = self.root / "staging"
        releases_root = self.root / "releases"
        staging = sorted(
            (path.name for path in staging_root.iterdir() if path.is_dir() and not path.is_symlink()),
            reverse=True,
        ) if staging_root.exists() else []
        releases = sorted(
            (path.name for path in releases_root.iterdir() if path.is_dir() and not path.is_symlink()),
            reverse=True,
        ) if releases_root.exists() else []
        current_link = self.root / "current"
        current = os.readlink(current_link) if current_link.is_symlink() else "-"

        def options(values):
            return "\n".join(
                f'<option value="{html.escape(value)}">{html.escape(value)}</option>'
                for value in values
            )

        token = html.escape(self.csrf_token)
        body = f"""<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>MS Firmware Publisher</title>
  <style>
    body {{ font: 16px system-ui, sans-serif; max-width: 760px; margin: 48px auto; padding: 0 20px; }}
    form {{ display: grid; gap: 14px; max-width: 520px; margin: 28px 0; }}
    select, button {{ font: inherit; padding: 10px; }}
    .ok {{ color: #176b35; }} .error {{ color: #9b1c1c; }}
  </style>
</head>
<body>
  <h1>MS Firmware Publisher</h1>
  <p>Текущий релиз: <strong>{html.escape(current)}</strong></p>
  <p class="ok">{html.escape(message)}</p>
  <p class="error">{html.escape(error)}</p>
  <form method="post" action="/publish/">
    <label>Подготовленный релиз
      <select name="release" required>{options(staging)}</select>
    </label>
    <input type="hidden" name="csrf" value="{token}">
    <button type="submit">Проверить и опубликовать</button>
  </form>
  <form method="post" action="/rollback/">
    <label>Откат на опубликованный релиз
      <select name="release" required>{options(releases)}</select>
    </label>
    <input type="hidden" name="csrf" value="{token}">
    <button type="submit">Выполнить откат</button>
  </form>
</body>
</html>"""
        encoded = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.send_header("Cache-Control", "no-store")
        self.send_header(
            "Content-Security-Policy",
            "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'",
        )
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format, *args):
        super().log_message(format, *args)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default="/srv/ms-firmware")
    parser.add_argument("--csrf-file", default="/etc/ms-firmware/publisher.csrf")
    parser.add_argument("--address", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8092)
    args = parser.parse_args()
    PublisherHandler.root = Path(args.root)
    PublisherHandler.csrf_token = Path(args.csrf_file).read_text(encoding="utf-8").strip()
    ThreadingHTTPServer((args.address, args.port), PublisherHandler).serve_forever()


if __name__ == "__main__":
    main()
