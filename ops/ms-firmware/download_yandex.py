#!/usr/bin/env python3
import argparse
import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://cloud-api.yandex.net/v1/disk/public/resources"
DOWNLOAD_API = API + "/download"


def get_json(url, params):
    query = urllib.parse.urlencode(params)
    error = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url + "?" + query, timeout=60) as response:
                return json.load(response)
        except Exception as caught:
            error = caught
            if attempt < 2:
                time.sleep(1)
    raise error


def list_files(public_url, path="/"):
    resource = get_json(
        API,
        {"public_key": public_url, "path": path, "limit": 1000},
    )
    if resource.get("type") == "file":
        yield resource
        return
    for item in resource.get("_embedded", {}).get("items", []):
        if item.get("type") == "dir":
            yield from list_files(public_url, item["path"])
        elif item.get("type") == "file":
            yield item


def download(public_url, item, destination):
    relative = item["path"].lstrip("/")
    target = destination / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    error = None
    for attempt in range(3):
        try:
            link = get_json(
                DOWNLOAD_API,
                {"public_key": public_url, "path": item["path"]},
            )["href"]
            temporary = target.with_suffix(target.suffix + ".part")
            with urllib.request.urlopen(link, timeout=120) as response, temporary.open("wb") as output:
                while block := response.read(1024 * 1024):
                    output.write(block)
            temporary.replace(target)
            break
        except Exception as caught:
            error = caught
            temporary.unlink(missing_ok=True)
            if attempt < 2:
                time.sleep(1)
    else:
        raise error
    if item.get("size", -1) >= 0 and target.stat().st_size != item["size"]:
        raise ValueError(f"Size mismatch: {relative}")
    print(relative)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("destination")
    parser.add_argument(
        "--public-url",
        default="https://disk.yandex.ru/d/hOV0LPW05OAdWw",
    )
    args = parser.parse_args()
    destination = Path(args.destination)
    destination.mkdir(parents=True, exist_ok=True)
    for item in list_files(args.public_url):
        download(args.public_url, item, destination)


if __name__ == "__main__":
    main()
