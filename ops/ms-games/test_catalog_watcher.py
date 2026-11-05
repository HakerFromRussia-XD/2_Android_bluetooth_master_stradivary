import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from catalog_watcher import publish_manifest, validate_manifest


def android_game(game_id="sushidash", package="com.unity.trashdash"):
    return {"id": game_id, "title": "Sushi Dash", "android": {
        "packageName": package, "launcherActivity": "com.unity3d.player.UnityPlayerGameActivity",
        "versionName": "1.0", "versionCode": 350, "usesMotoricaControl": True,
    }}


def encode(games, **extra):
    return (json.dumps({"games": games, **extra}, ensure_ascii=False, indent=2) + "\n").encode()


class GameCatalogPublisherTest(unittest.TestCase):
    def test_platforms_and_legacy_fields_are_retained_byte_for_byte(self):
        legacy = {"id": "stk", "title": "Legacy game", "packageName": "com.motorica.games.stk",
                  "launcherActivity": "com.motorica.games.stk.Main", "versionName": "1.0.16", "versionCode": 1016}
        ios = {"id": "fluxara_drift", "title": "Fluxara Drift", "android": None, "ios": {
            "bundleId": "io.fluxara.drift", "urlScheme": "fluxara-drive", "versionName": "1.0",
            "versionCode": 1, "appStoreUrl": "",
        }}
        body = encode([legacy, android_game(), ios], custom={"retained": "да"})
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "upload.json"
            destination = Path(temporary) / "public" / "manifest.json"
            source.write_bytes(body)
            result = publish_manifest(source, destination)
            self.assertEqual("published", result["status"])
            self.assertEqual(3, result["games"])
            self.assertEqual(hashlib.sha256(body).hexdigest(), result["sha256"])
            self.assertEqual(body, destination.read_bytes())

    def test_incomplete_or_invalid_upload_keeps_previous_public_version(self):
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "upload.json"
            destination = Path(temporary) / "public" / "manifest.json"
            original = encode([android_game()])
            source.write_bytes(original)
            publish_manifest(source, destination)
            for invalid in [b'{"games":[', encode([android_game(), android_game()]),
                            encode([{"id": "missing", "title": "Missing platform"}])]:
                source.write_bytes(invalid)
                with self.assertRaises(ValueError):
                    publish_manifest(source, destination)
                self.assertEqual(original, destination.read_bytes())
                self.assertFalse((destination.parent / "history").exists())

    def test_replacement_preserves_previous_hash_and_unchanged_upload_adds_no_history(self):
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "upload.json"
            destination = Path(temporary) / "public" / "manifest.json"
            original = encode([android_game()])
            source.write_bytes(original)
            publish_manifest(source, destination)
            self.assertEqual("unchanged", publish_manifest(source, destination)["status"])
            changed_game = android_game()
            changed_game["android"]["versionCode"] = 351
            updated = encode([changed_game])
            source.write_bytes(updated)
            publish_manifest(source, destination)
            history = list((destination.parent / "history").glob("*.json"))
            self.assertEqual(1, len(history))
            self.assertIn(hashlib.sha256(original).hexdigest(), history[0].name)
            self.assertEqual(original, history[0].read_bytes())
            self.assertEqual(updated, destination.read_bytes())

    def test_duplicate_packages_boolean_versions_and_invalid_schemes_are_rejected(self):
        duplicate = android_game("other")
        bad_version = android_game()
        bad_version["android"]["versionCode"] = True
        bad_scheme = {"id": "ios", "title": "iOS", "ios": {
            "bundleId": "example.game", "urlScheme": "game://", "versionCode": 1,
        }}
        for games in [[android_game(), duplicate], [bad_version], [bad_scheme]]:
            with self.assertRaises(ValueError):
                validate_manifest(encode(games))

    def test_catalog_size_is_data_driven(self):
        games = [android_game(f"game{index}", f"example.game{index}") for index in range(500)]
        self.assertEqual(500, len(validate_manifest(encode(games))["games"]))

    def test_version_code_must_fit_the_clients_signed_64_bit_integer(self):
        for invalid in [-1, 1 << 63, True, 1.5]:
            game = android_game()
            game["android"]["versionCode"] = invalid
            with self.assertRaises(ValueError):
                validate_manifest(encode([game]))
        game = android_game()
        game["android"]["versionCode"] = (1 << 63) - 1
        self.assertEqual(1, len(validate_manifest(encode([game]))["games"]))

    def test_apple_bundle_characters_and_case_insensitive_uniqueness(self):
        def ios_game(game_id, bundle):
            return {"id": game_id, "title": "iOS game", "ios": {
                "bundleId": bundle, "urlScheme": "sushi-dash", "versionCode": 42,
            }}
        hyphenated = ios_game("sushidash", "com.Example.Sushi-Dash")
        self.assertEqual(1, len(validate_manifest(encode([hyphenated]))["games"]))
        with self.assertRaisesRegex(ValueError, "Duplicate iOS bundleId"):
            validate_manifest(encode([hyphenated, ios_game("duplicate", "COM.example.sushi-dash")]))
        for invalid in ["com.example.sushi_dash", "com.example.sushi dash", "com.example.суши"]:
            with self.assertRaisesRegex(ValueError, "Invalid iOS bundleId"):
                validate_manifest(encode([ios_game("invalid", invalid)]))

    def test_reserved_stk_alias_rejects_other_android_packages_in_both_formats(self):
        nested = android_game("stk", "com.example.other")
        flat = {"id": nested["id"], "title": nested["title"], **nested["android"]}
        for game in [nested, flat]:
            with self.assertRaisesRegex(ValueError, "reserved stk"):
                validate_manifest(encode([game]))
        expected = android_game("stk", "com.motorica.games.stk")
        self.assertEqual(1, len(validate_manifest(encode([expected]))["games"]))

    def test_utf8_bom_upload_is_rejected_and_keeps_the_public_catalog(self):
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "upload.json"
            destination = Path(temporary) / "public" / "manifest.json"
            original = encode([android_game()])
            source.write_bytes(original)
            publish_manifest(source, destination)
            source.write_bytes(b"\xef\xbb\xbf" + original)
            with self.assertRaises(ValueError):
                publish_manifest(source, destination)
            self.assertEqual(original, destination.read_bytes())
            self.assertFalse((destination.parent / "history").exists())


if __name__ == "__main__":
    unittest.main()
