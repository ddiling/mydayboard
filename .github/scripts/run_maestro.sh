#!/usr/bin/env bash
# Runs the Maestro flows on the booted emulator.
#
# The emulator reports "booted" while its adb connection can still drop for a moment. Maestro then fails
# every flow within seconds with "device 'emulator-5554' not found", although the emulator is alive.
# So: wait until the device answers several times in a row, and retry the flows once, but only for that
# connection error. A real test failure (an assertion) is never retried, so it still fails the job.
set -euo pipefail

wait_until_stable() {
  local ok=0
  for _ in $(seq 1 60); do
    if adb shell pm path android >/dev/null 2>&1; then ok=$((ok + 1)); else ok=0; fi
    if [ "$ok" -ge 5 ]; then return 0; fi
    sleep 2
  done
  echo "Emulator never answered 5 times in a row." >&2
  return 1
}

run_flows() {
  maestro test app/maestro/ --format junit --output build/maestro/report.xml --debug-output build/maestro/debug \
    2>&1 | tee build/maestro/output.log
}

mkdir -p build/maestro
adb wait-for-device
wait_until_stable
adb install -r app/build/outputs/apk/debug/app-debug.apk

if run_flows; then exit 0; fi
if grep -q "device '.*' not found" build/maestro/output.log; then
  echo "::warning::Maestro lost the emulator connection; retrying once."
  wait_until_stable
  run_flows
else
  exit 1
fi
