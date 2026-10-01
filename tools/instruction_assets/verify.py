#!/usr/bin/env python3
"""Validate the localized instruction asset budget and optional APK comparison."""
import argparse
import hashlib
import json
import re
import struct
from io import BytesIO
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
BASELINE_IMAGE_BYTES = 480967


def verify_runtime_r_classes(archive):
    definitions, referenced = set(), set()
    for name in archive.namelist():
        if not name.endswith(".dex"):
            continue
        data = archive.read(name)
        assert data.startswith(b"dex\n"), f"Unsupported DEX file: {name}"
        string_count, string_offset = struct.unpack_from("<II", data, 56)
        type_count, type_offset = struct.unpack_from("<II", data, 64)
        class_count, class_offset = struct.unpack_from("<II", data, 96)
        strings = []
        for index in range(string_count):
            offset = struct.unpack_from("<I", data, string_offset + index * 4)[0]
            while data[offset] & 128:
                offset += 1
            offset += 1
            strings.append(data[offset:data.index(b"\0", offset)].decode("utf8", "replace"))
        types = [strings[struct.unpack_from("<I", data, type_offset + index * 4)[0]] for index in range(type_count)]
        referenced.update(types)
        definitions.update(types[struct.unpack_from("<I", data, class_offset + index * 32)[0]] for index in range(class_count))
    missing = sorted(name for name in referenced - definitions
                     if name.startswith("L") and ("/R$" in name or name.endswith("/R;"))
                     and not name.startswith(("Landroid/", "Lcom/android/")))
    assert not missing, f"Runtime R classes missing from APK: {missing[:20]}"
    assert "Landroidx/startup/R$string;" in definitions, "App startup R class missing from APK"
    return {"class_count": len(definitions), "startup_r_string_defined": True, "missing_runtime_r_classes": []}


def verify():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline-apk", type=Path)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--apk-resources", type=Path, help="aapt2 dump resources output, for optimized APK paths")
    parser.add_argument("--report", type=Path)
    parser.add_argument("--figma-exports", type=Path)
    args = parser.parse_args()
    data = json.loads((Path(__file__).with_name("advanced-settings.json")).read_text())
    cards = data["cards"]
    for locale, index in (("base", 1), ("ru", 0)):
        resources = ET.parse(ROOT / f"shared/src/commonMain/moko-resources/strings/{locale}/strings.xml")
        strings = {node.attrib["name"]: node.text for node in resources.getroot().findall("string")}
        for card in cards:
            for item in [card] + card.get("sections", []):
                for field in ("title", "body"):
                    key = f"help_advanced_{item['key']}_{field}"
                    assert strings[key] == item[field][index], (locale, key)
    assets = ROOT / "shared/src/commonMain/moko-resources/images"
    assert not list(assets.glob("ubi4_help_image_advanced_settings_*")), "Obsolete full-card PNGs still packaged"
    records = []
    for card in cards:
        for locale in ("ru", "en"):
            file = assets / f"ubi4_help_widget_{card['index']}_{locale}@3x.png"
            with Image.open(file) as image:
                assert image.width == 888, (file, image.size)
                assert image.height == {7: 433, 8: 237}.get(card["index"], 141), (file, image.size)
                if args.figma_exports:
                    source = args.figma_exports / file.name.replace("@3x", "")
                    with Image.open(source) as exported:
                        assert exported.size == image.size, (source, exported.size, image.size)
                        assert exported.convert("RGBA").tobytes() == image.convert("RGBA").tobytes(), f"Pixels changed: {file}"
                records.append({"name": file.name, "bytes": file.stat().st_size, "pixels": list(image.size),
                                "rgba_sha256": hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest()})
    total = sum(record["bytes"] for record in records)
    assert total <= BASELINE_IMAGE_BYTES, "Combined RU/EN image budget grew"
    report = {"figma_file": data["fileKey"], "baseline_image_bytes": BASELINE_IMAGE_BYTES,
              "ru_en_image_bytes": total, "image_bytes_saved": BASELINE_IMAGE_BYTES - total, "assets": records}
    if args.figma_exports:
        report["lossless_from_figma"] = True
    if args.apk:
        with zipfile.ZipFile(args.apk) as archive:
            report["runtime_r_class_verification"] = verify_runtime_r_classes(archive)
            names = archive.namelist()
            assert not any("ubi4_help_image_advanced_settings_" in name for name in names)
            if args.apk_resources:
                dump = args.apk_resources.read_text()
                assert "drawable/ubi4_help_image_advanced_settings_" not in dump
                blocks = dict(re.findall(r"resource 0x[0-9a-f]+ drawable/(\S+)\n(.*?)(?=\s+resource 0x|\Z)", dump, re.S))
                packaged = []
                for index in range(1, 9):
                    alias = blocks[f"ubi4_help_widget_{index}"]
                    assert f"() @drawable/ubi4_help_widget_{index}_en" in alias
                    assert f"(ru) @drawable/ubi4_help_widget_{index}_ru" in alias
                    for locale in ("ru", "en"):
                        name = f"ubi4_help_widget_{index}_{locale}"
                        paths = re.findall(r"\(file\) (\S+\.png) type=PNG", blocks[name])
                        assert len(paths) == 1, (name, paths)
                        with Image.open(BytesIO(archive.read(paths[0]))) as image, Image.open(assets / f"{name}@3x.png") as source:
                            assert image.size == source.size
                            assert image.convert("RGBA").tobytes() == source.convert("RGBA").tobytes(), f"APK pixels changed: {name}"
                        packaged.append({"resource": name, "apk_path": paths[0]})
                report["packaged_widget_assets"] = packaged
                report["apk_widget_pixels_match_sources"] = True
                report["apk_widget_locale_aliases_verified"] = True
            else:
                assert len([name for name in names if "ubi4_help_widget_" in name and name.endswith(".png")]) == 16
        report["apk_bytes"] = args.apk.stat().st_size
        if args.baseline_apk:
            report["baseline_apk_bytes"] = args.baseline_apk.stat().st_size
            report["apk_bytes_saved"] = args.baseline_apk.stat().st_size - args.apk.stat().st_size
            assert report["apk_bytes_saved"] >= 0, "APK size increased"
    if args.report:
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({key: value for key, value in report.items() if key != "assets"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    verify()
