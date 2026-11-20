#!/usr/bin/env bash
# Runs the Maestro flows on the booted emulator.
#
# The emulator reports "booted" while its adb connection can still drop for a moment. Maestro then fails
# flows within seconds with "device 'emulator-5554' not found", or its helper app on the device crashes
# ("Device server died"), although the app and the emulator are fine. So: wait until the device answers
# several times in a row, and retry the flows once, but only for those two infrastructure errors. A real
# test failure (an assertion) is never retried, so it still fails the job. The first attempt's report and
# log are kept next to the retry's, so the evidence of the flake stays in the uploaded results.
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

# $1: a name for this attempt's report, log and debug folder.
run_flows() {
  maestro test app/maestro/ --format junit --output "build/maestro/$1.xml" --debug-output "build/maestro/$1-debug" \
    2>&1 | tee "build/maestro/$1.log"
}

INFRA_ERRORS="device '.*' not found|Device server died"

mkdir -p build/maestro
adb wait-for-device
wait_until_stable
adb install -r app/build/outputs/apk/debug/app-debug.apk

if run_flows report; then exit 0; fi
# The crash of Maestro's helper may only reach the JUnit report, not the console log.
if grep -qE "$INFRA_ERRORS" build/maestro/report.log build/maestro/report.xml; then
  echo "::warning::Maestro lost the emulator (connection or its helper app); retrying once."
  wait_until_stable
  run_flows retry
else
  exit 1
fi
