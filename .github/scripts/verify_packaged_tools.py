#!/usr/bin/env python3

"""Check packaged tool identity, provenance, and executable behavior."""

import argparse
import configparser
import pathlib

from write_tool_provenance import sha256, tool_version

TOOL_NAMES = {"yt-dlp", "yt-dlp.exe", "yt-dlp_macos", "ffmpeg", "ffmpeg.exe", "ffprobe", "ffprobe.exe"}


def tool_candidates(root: pathlib.Path, name: str) -> list[pathlib.Path]:
    names = {name, f"{name}.exe", f"{name}_macos"}
    return [path for path in root.rglob("*") if path.is_file() and path.name in names]


def read_manifest(root: pathlib.Path, expected_commit: str) -> tuple[configparser.ConfigParser, pathlib.Path]:
    manifests = list(root.rglob("THIRD_PARTY_VERSIONS.txt"))
    if len(manifests) != 1:
        raise ValueError(f"Expected one tool provenance manifest, found {len(manifests)}")
    manifest = configparser.ConfigParser(interpolation=None)
    manifest.read_string("[build]\n" + manifests[0].read_text(encoding="utf-8"))
    if manifest["build"]["format_version"] != "1":
        raise ValueError("Unsupported tool provenance format")
    if not expected_commit or manifest["build"]["build_commit"] != expected_commit:
        raise ValueError("Tool provenance does not match the workflow build commit")
    return manifest, manifests[0]


def reference_payload_hash(reference_root: pathlib.Path, name: str) -> tuple[int, str]:
    candidates = tool_candidates(reference_root, name)
    if len(candidates) != 1:
        raise ValueError(f"Expected one reference {name}, found {len(candidates)}")
    path = candidates[0]
    return path.stat().st_size, sha256(path)


def verify(
    app_root: pathlib.Path,
    flavor: str,
    expected_commit: str,
    reference_root: pathlib.Path | None = None,
) -> None:
    manifests = list(app_root.rglob("THIRD_PARTY_VERSIONS.txt"))
    payloads = [path for path in app_root.rglob("*") if path.is_file() and path.name in TOOL_NAMES]
    if flavor == "Lite":
        if manifests or payloads:
            raise ValueError("Lite package unexpectedly contains downloaded tools or provenance")
        print("Lite package contains no downloaded tool payload")
        return

    manifest, _ = read_manifest(app_root, expected_commit)
    platform = manifest["build"].get("platform", "").strip().lower()

    if reference_root is not None:
        reference_manifest, _ = read_manifest(reference_root, expected_commit)
        if reference_manifest["build"].get("platform", "").strip().lower() != platform:
            raise ValueError("Reference package platform differs from installed application provenance")

    for name, arguments in [("yt-dlp", ["--version"]), ("ffmpeg", ["-version"]), ("ffprobe", ["-version"])]:
        candidates = tool_candidates(app_root, name)
        if len(candidates) != 1:
            raise ValueError(f"Expected one packaged {name}, found {len(candidates)}")
        path = candidates[0]
        recorded = manifest[name]
        actual_size = path.stat().st_size
        actual_hash = sha256(path)
        recorded_size = int(recorded["size_bytes"])
        recorded_hash = recorded["sha256"]

        if actual_size != recorded_size or actual_hash != recorded_hash:
            if platform != "macos" or reference_root is None:
                raise ValueError(f"Packaged {name} does not match its recorded size/SHA256")
            reference_size, reference_hash = reference_payload_hash(reference_root, name)
            if actual_size != reference_size or actual_hash != reference_hash:
                raise ValueError(f"Installed macOS {name} differs from the PKG payload")
            hash_note = f"{actual_hash} (matches macOS PKG payload; source SHA256 {recorded_hash})"
        else:
            hash_note = actual_hash

        actual_version = tool_version(path, arguments)
        if actual_version != recorded["version"]:
            raise ValueError(f"Packaged {name} version differs from the provenance manifest")
        if not recorded["source_type"] or not recorded["source_url"].startswith("https://"):
            raise ValueError(f"Missing source provenance for {name}")
        print(f"Verified packaged {name}: {actual_version}; SHA256 {hash_note}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-root", required=True, type=pathlib.Path)
    parser.add_argument("--flavor", choices=["Lite", "Full"], required=True)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--reference-root", type=pathlib.Path)
    args = parser.parse_args()
    verify(args.app_root, args.flavor, args.expected_commit, args.reference_root)


if __name__ == "__main__":
    main()
