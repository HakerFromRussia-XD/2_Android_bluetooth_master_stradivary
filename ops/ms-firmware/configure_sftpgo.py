#!/usr/bin/env python3
import argparse
import json
import os
from pathlib import Path


def configure(config_path: Path, signing_passphrase_file: str, finalize: bool) -> None:
    config = json.loads(config_path.read_text(encoding="utf-8"))
    config["common"]["max_total_connections"] = 20
    config["common"]["max_per_host_connections"] = 10
    config["common"]["event_manager"]["enabled_commands"] = []

    config["sftpd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]
    config["ftpd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]
    config["webdavd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]

    config["data_provider"]["users_base_dir"] = "/srv/ms-firmware/staging"
    config["data_provider"]["create_default_admin"] = not finalize
    config["data_provider"]["password_validation"]["admins"]["min_entropy"] = 60
    config["data_provider"]["password_validation"]["users"]["min_entropy"] = 50

    binding_common = {
        "address": "127.0.0.1",
        "enable_https": False,
        "certificate_file": "",
        "certificate_key_file": "",
        "min_tls_version": 12,
        "client_auth_type": 0,
        "tls_cipher_suites": [],
        "tls_protocols": [],
        "proxy_mode": 0,
        "proxy_allowed": [],
        "client_ip_proxy_header": "",
        "client_ip_header_depth": 0,
        "hide_login_url": 0,
        "render_openapi": False,
        "languages": ["en"],
        "oidc": {
            "client_id": "",
            "client_secret": "",
            "client_secret_file": "",
            "config_url": "",
            "redirect_base_url": "",
            "scopes": ["openid", "profile", "email"],
            "username_field": "",
            "role_field": "",
            "implicit_roles": False,
            "custom_fields": [],
            "insecure_skip_signature_check": False,
            "debug": False,
        },
        "security": {
            "enabled": True,
            "allowed_hosts": [],
            "allowed_hosts_are_regex": False,
            "hosts_proxy_headers": [],
            "https_redirect": False,
            "https_host": "",
            "https_proxy_headers": ["X-Forwarded-Proto"],
            "sts_seconds": 0,
            "sts_include_subdomains": False,
            "sts_preload": False,
            "content_type_nosniff": True,
            "content_security_policy": "",
            "permissions_policy": "",
            "cross_origin_opener_policy": "same-origin",
            "cross_origin_resource_policy": "same-origin",
            "cross_origin_embedder_policy": "",
            "referrer_policy": "no-referrer",
            "cache_control": "no-store",
        },
        "branding": {
            "web_admin": {
                "name": "MS Firmware Admin",
                "short_name": "MS Firmware",
                "favicon_path": "",
                "logo_path": "",
                "disclaimer_name": "",
                "disclaimer_path": "",
                "default_css": [],
                "extra_css": [],
            },
            "web_client": {
                "name": "MS Firmware Files",
                "short_name": "MS Firmware",
                "favicon_path": "",
                "logo_path": "",
                "disclaimer_name": "",
                "disclaimer_path": "",
                "default_css": [],
                "extra_css": [],
            },
        },
    }
    client_binding = dict(binding_common)
    client_binding.update(
        {
            "port": 8090,
            "enable_web_admin": False,
            "enable_web_client": True,
            "enable_rest_api": False,
            "enabled_login_methods": 0,
            "disabled_login_methods": 0,
        }
    )
    admin_binding = dict(binding_common)
    admin_binding.update(
        {
            "port": 8091,
            "enable_web_admin": True,
            "enable_web_client": False,
            "enable_rest_api": True,
            "enabled_login_methods": 0,
            "disabled_login_methods": 0,
        }
    )
    config["httpd"]["bindings"] = [client_binding, admin_binding]
    config["httpd"]["max_upload_file_size"] = 25 * 1024 * 1024
    config["httpd"]["signing_passphrase_file"] = signing_passphrase_file
    config["httpd"]["cookie_lifetime"] = 20
    config["httpd"]["share_cookie_lifetime"] = 20
    config["httpd"]["jwt_lifetime"] = 10
    config["httpd"]["hide_support_link"] = True

    temporary = config_path.with_suffix(".json.tmp")
    temporary.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
    os.chmod(temporary, 0o640)
    os.replace(temporary, config_path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="/etc/sftpgo/sftpgo.json")
    parser.add_argument(
        "--signing-passphrase-file",
        default="/etc/sftpgo/signing-passphrase",
    )
    parser.add_argument("--finalize", action="store_true")
    args = parser.parse_args()
    configure(
        Path(args.config),
        args.signing_passphrase_file,
        args.finalize,
    )


if __name__ == "__main__":
    main()
