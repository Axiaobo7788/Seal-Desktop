#!/usr/bin/env bash

set -euo pipefail

app_root="${1:?Application root is required}"
launcher="${2:?Application launcher is required}"
label="${3:-Unix app image}"
timeout_seconds="${4:-12}"

if [[ ! -d "$app_root" ]]; then
  echo "$label root not found: $app_root"
  exit 1
fi
if [[ ! -x "$launcher" ]]; then
  echo "$label launcher is missing or not executable: $launcher"
  exit 1
fi

stdout_log="${RUNNER_TEMP:-/tmp}/seal-smoke-${RANDOM}-stdout.log"
stderr_log="${RUNNER_TEMP:-/tmp}/seal-smoke-${RANDOM}-stderr.log"

echo "$label root: $app_root"
echo "$label launcher: $launcher"

cd "$app_root"
JPACKAGE_DEBUG=true "$launcher" >"$stdout_log" 2>"$stderr_log" &
pid=$!

cleanup() {
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

sleep "$timeout_seconds"

if kill -0 "$pid" 2>/dev/null; then
  echo "$label stayed alive for $timeout_seconds seconds; startup succeeded."
  exit 0
fi

set +e
wait "$pid"
exit_code=$?
set -e

if [[ -s "$stdout_log" ]]; then
  echo "----- $label stdout -----"
  cat "$stdout_log"
fi
if [[ -s "$stderr_log" ]]; then
  echo "----- $label stderr -----"
  cat "$stderr_log"
fi

echo "$label exited before the ${timeout_seconds}-second smoke-test window completed with code $exit_code."
exit 1
