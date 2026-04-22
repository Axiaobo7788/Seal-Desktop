#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_BASE="$(mktemp -d)"
trap 'rm -rf "$TMP_BASE"' EXIT

run_backend() {
  local backend="$1"
  local state_dir="$TMP_BASE/$backend"
  mkdir -p "$state_dir"

  echo "[selfcheck] backend=$backend stateDir=$state_dir"
  (
    cd "$ROOT_DIR"
    ./gradlew :desktop:desktopStorageSelfCheck \
      -PstorageBackend="$backend" \
      -PstorageStateDir="$state_dir"
  )
}

run_backend json
run_backend dual
run_backend sqlite

echo "[selfcheck] all backends passed"
