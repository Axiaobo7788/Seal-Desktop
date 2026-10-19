#!/usr/bin/env python3

import argparse
import hashlib
import pathlib
import subprocess


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Write bundled Desktop tool provenance")
    parser.add_argument("--output", required=True)
    parser.add_argument("--build-commit", required=True)
    parser.add_argument("--build-workflow", required=True)
    parser.add_argument("--build-run-id", required=True)
    parser.add_argument("--platform", required=True)
    parser.add_argument("--architecture", required=True)
    parser.add_argument("--yt-dlp", required=True)
    parser.add_argument("--yt-dlp-url", required=True)
    parser.add_argument("--yt-dlp-source-type", required=True)
    parser.add_argument("--ffmpeg", required=True)
    parser.add_argument("--ffmpeg-url", required=True)
    parser.add_argument("--ffmpeg-source-type", required=True)
    parser.add_argument("--ffprobe", required=True)
    parser.add_argument("--ffprobe-url", required=True)
    parser.add_argument("--ffprobe-source-type", required=True)
    return parser.parse_args()


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def tool_version(path: pathlib.Path, arguments: list[str]) -> str:
    result = subprocess.run(
        [str(path), *arguments],
        check=True,
        capture_output=True,
        text=True,
        timeout=30,
    )
    output = result.stdout.strip() or result.stderr.strip()
    return next((line.strip() for line in output.splitlines() if line.strip()), "unknown")


def clean(value: str) -> str:
    return " ".join(value.splitlines()).strip()


def tool_section(
    name: str,
    path: pathlib.Path,
    source_url: str,
    source_type: str,
    version_arguments: list[str],
) -> list[str]:
    if not path.is_file():
        raise FileNotFoundError(f"Bundled tool not found: {path}")
    return [
        f"[{name}]",
        f"version={clean(tool_version(path, version_arguments))}",
        f"sha256={sha256(path)}",
        f"size_bytes={path.stat().st_size}",
        f"source_type={clean(source_type)}",
        f"source_url={clean(source_url)}",
        "",
    ]


def main() -> None:
    args = parse_args()
    output = pathlib.Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    lines = [
        "format_version=1",
        f"build_commit={clean(args.build_commit)}",
        f"build_workflow={clean(args.build_workflow)}",
        f"build_run_id={clean(args.build_run_id)}",
        f"platform={clean(args.platform)}",
        f"architecture={clean(args.architecture)}",
        "",
    ]
    lines += tool_section(
        "yt-dlp",
        pathlib.Path(args.yt_dlp),
        args.yt_dlp_url,
        args.yt_dlp_source_type,
        ["--version"],
    )
    lines += tool_section(
        "ffmpeg",
        pathlib.Path(args.ffmpeg),
        args.ffmpeg_url,
        args.ffmpeg_source_type,
        ["-version"],
    )
    lines += tool_section(
        "ffprobe",
        pathlib.Path(args.ffprobe),
        args.ffprobe_url,
        args.ffprobe_source_type,
        ["-version"],
    )
    output.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    main()
