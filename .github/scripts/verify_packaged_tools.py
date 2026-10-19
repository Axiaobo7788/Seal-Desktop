#!/usr/bin/env python3

"""Check the packaged payload, not merely the presence of tool filenames."""

import argparse
import configparser
import pathlib

from write_tool_provenance import macos_unsigned_sha256, sha256, tool_version


def payload_matches(path: pathlib.Path, recorded: configparser.SectionProxy, platform: str) -> tuple[bool, str]:
    actual_hash = sha256(path)
    expected_hash = recorded["sha256"]
    expected_size = int(recorded["size_bytes"])
    if path.stat().st_size == expected_size and actual_hash == expected_hash:
        return True, actual_hash

    # macOS PKG construction may ad-hoc sign nested Mach-O tools. That changes
    # the raw file size/hash without changing the unsigned executable payload.
    # Only accept this explicitly recorded, platform-specific transformation.
    if platform == "macos":
        expected_unsigned_hash = recorded.get("unsigned_sha256", "").strip()
        actual_unsigned_hash = macos_unsigned_sha256(path)
        if expected_unsigned_hash and actual_unsigned_hash == expected_unsigned_hash:
            return True, f"{actual_hash} (macOS signed; unsigned SHA256 {actual_unsigned_hash})"

    return False, actual_hash


def verify(app_root: pathlib.Path, flavor: str, expected_commit: str) -> None:
    manifests = list(app_root.rglob("THIRD_PARTY_VERSIONS.txt"))
    names = {"yt-dlp", "yt-dlp.exe", "yt-dlp_macos", "ffmpeg", "ffmpeg.exe", "ffprobe", "ffprobe.exe"}
    payloads = [path for path in app_root.rglob("*") if path.is_file() and path.name in names]
    if flavor == "Lite":
        if manifests or payloads:
            raise ValueError("Lite package unexpectedly contains downloaded tools or provenance")
        print("Lite package contains no downloaded tool payload")
        return
    if len(manifests) != 1:
        raise ValueError(f"Expected one tool provenance manifest, found {len(manifests)}")
    manifest = configparser.ConfigParser(interpolation=None)
    manifest.read_string("[build]\n" + manifests[0].read_text(encoding="utf-8"))
    if manifest["build"]["format_version"] != "1":
        raise ValueError("Unsupported tool provenance format")
    if not expected_commit or manifest["build"]["build_commit"] != expected_commit:
        raise ValueError("Tool provenance does not match the workflow build commit")
    platform = manifest["build"].get("platform", "").strip().lower()
    for name, arguments in [("yt-dlp", ["--version"]), ("ffmpeg", ["-version"]), ("ffprobe", ["-version"])]:
        candidates = [path for path in payloads if path.name in {name, f"{name}.exe", f"{name}_macos"}]
        if len(candidates) != 1:
            raise ValueError(f"Expected one packaged {name}, found {len(candidates)}")
        path = candidates[0]
        recorded = manifest[name]
        matches, actual_hash_description = payload_matches(path, recorded, platform)
        if not matches:
            raise ValueError(f"Packaged {name} does not match its recorded payload hash")
        actual_version = tool_version(path, arguments)
        if actual_version != recorded["version"]:
            raise ValueError(f"Packaged {name} version differs from the provenance manifest")
        if not recorded["source_type"] or not recorded["source_url"].startswith("https://"):
            raise ValueError(f"Missing source provenance for {name}")
        print(f"Verified packaged {name}: {actual_version}; SHA256 {actual_hash_description}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-root", required=True, type=pathlib.Path)
    parser.add_argument("--flavor", choices=["Lite", "Full"], required=True)
    parser.add_argument("--expected-commit", required=True)
    args = parser.parse_args()
    verify(args.app_root, args.flavor, args.expected_commit)


if __name__ == "__main__":
    main()
