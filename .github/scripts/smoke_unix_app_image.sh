#!/usr/bin/env bash

set -euo pipefail

app_root="${1:?Application root is required}"
launcher="${2:?Application launcher is required}"
label="${3:-Unix app image}"
timeout_seconds="${4:-12}"
verify_sqlite="${5:-false}"

case "${verify_sqlite,,}" in
  true|1|yes|on) verify_sqlite=true ;;
  false|0|no|off) verify_sqlite=false ;;
  *)
    echo "Unsupported SQLite verification flag: $verify_sqlite"
    exit 2
    ;;
esac

if [[ ! -d "$app_root" ]]; then
  echo "$label root not found: $app_root"
  exit 1
fi
if [[ ! -x "$launcher" ]]; then
  echo "$label launcher is missing or not executable: $launcher"
  exit 1
fi

# The smoke test changes its working directory to the app root. Resolve both
# inputs first so a relative launcher does not become relative to app_root twice.
app_root="$(cd "$app_root" && pwd -P)"
launcher_dir="$(cd "$(dirname "$launcher")" && pwd -P)"
launcher="$launcher_dir/$(basename "$launcher")"

stdout_log="${RUNNER_TEMP:-/tmp}/seal-smoke-${RANDOM}-stdout.log"
stderr_log="${RUNNER_TEMP:-/tmp}/seal-smoke-${RANDOM}-stderr.log"
state_root=""
database_path=""

if [[ "$verify_sqlite" == "true" ]]; then
  state_root="$(mktemp -d "${RUNNER_TEMP:-/tmp}/seal-sqlite-smoke-XXXXXX")"
  database_path="$state_root/seal/seal.db"
fi

echo "$label root: $app_root"
echo "$label launcher: $launcher"
if [[ "$verify_sqlite" == "true" ]]; then
  echo "$label SQLite state root: $state_root"
fi

cd "$app_root"
if [[ "$verify_sqlite" == "true" ]]; then
  JPACKAGE_DEBUG=true \
    SEAL_DESKTOP_STORAGE_BACKEND=sqlite \
    SEAL_DESKTOP_STORAGE_STATE_DIR="$state_root" \
    "$launcher" >"$stdout_log" 2>"$stderr_log" &
else
  JPACKAGE_DEBUG=true "$launcher" >"$stdout_log" 2>"$stderr_log" &
fi
pid=$!

cleanup() {
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
  if [[ -n "$state_root" ]]; then
    rm -rf "$state_root"
  fi
}
trap cleanup EXIT

print_logs() {
  if [[ -s "$stdout_log" ]]; then
    echo "----- $label stdout -----"
    cat "$stdout_log"
  fi
  if [[ -s "$stderr_log" ]]; then
    echo "----- $label stderr -----"
    cat "$stderr_log"
  fi
}

sleep "$timeout_seconds"

if kill -0 "$pid" 2>/dev/null; then
  if [[ "$verify_sqlite" == "true" ]]; then
    if [[ ! -s "$database_path" ]]; then
      print_logs
      echo "$label stayed alive but did not create a non-empty SQLite database: $database_path"
      exit 1
    fi
    if grep -Eiq 'sqlite_storage_warning|No suitable driver|UnsatisfiedLinkError' "$stdout_log" "$stderr_log"; then
      print_logs
      echo "$label reported a SQLite driver or native-library failure."
      exit 1
    fi
    echo "$label created SQLite database: $database_path ($(wc -c < "$database_path") bytes)"
  fi
  echo "$label stayed alive for $timeout_seconds seconds; startup succeeded."
  exit 0
fi

set +e
wait "$pid"
exit_code=$?
set -e

print_logs

echo "$label exited before the ${timeout_seconds}-second smoke-test window completed with code $exit_code."
exit 1
