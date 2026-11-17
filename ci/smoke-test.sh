#!/usr/bin/env bash
# Installs the APK on a running emulator, walks through the app, saves screenshots
# and fails if the app crashes. Usage: smoke-test.sh <apk> <out-dir>
set -uo pipefail
apk="$1"; out="$2"; pkg=io.github.akrishna87.policyvault
mkdir -p "$out"
adb wait-for-device
adb install -r "$apk" || exit 1
adb shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am start -n "$pkg/.MainActivity"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$out/$1.png"; }
crashed() { adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: .*$pkg" >/dev/null; }
# Taps the centre of the Nth (default first) view whose text or content description is $1.
# Matches the label exactly (fixed-string) so names with * or ₹ work.
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local line
  line=$(adb shell cat /sdcard/ui.xml | sed 's/></>\n</g' | grep -F "text=\"$1\"" | sed -n "${2:-1}p")
  if [ -z "$line" ]; then
    line=$(adb shell cat /sdcard/ui.xml | sed 's/></>\n</g' | grep -F "content-desc=\"$1\"" | sed -n "${2:-1}p")
  fi
  local b; b=$(echo "$line" | grep -o 'bounds="[^"]*"' | head -1)
  if [ -z "$b" ]; then echo "  (no '$1' on screen)"; return 1; fi
  read -r x1 y1 x2 y2 <<<"$(echo "$b" | grep -o '[0-9]\+' | tr '\n' ' ')"
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}
# Types into the field whose label is $1.
type_into() {
  tap "$1" || return 1
  sleep 1
  adb shell input text "$2"
}

fail() { echo "=== $1; failing so this build isn't published untested ==="; exit 1; }

# 1. Empty vault: the welcome state.
shot 01-empty 6
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | grep -q "No policies yet" || fail "The empty state never showed"
crashed && fail "The app crashed on launch"

# 2. Add a policy: tap the button on the empty state, fill the insurer, save.
tap "Add your first policy" || tap "Add policy" || fail "No add button on the empty state"
shot 02-add 3
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | grep -q "What kind of policy" || fail "The add form never showed"
type_into "Insurer *" "StarHealth" || fail "Could not tap the insurer field"
shot 02b-typed 1
# Save lives in the top bar, so it is reachable without scrolling past the keyboard.
tap "Save" || fail "No save button"
sleep 4
shot 03-saved 1

# 3. The saved policy is on the dashboard.
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | grep -q "StarHealth" || fail "The saved policy never showed on the dashboard"
adb shell cat /sdcard/ui.xml | grep -q "Total cover" || fail "The dashboard totals never showed"

# 4. Open the detail screen.
tap "StarHealth"
sleep 3
shot 04-detail 1
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | grep -q "Details" || fail "The detail screen never showed"

# 5. The policies tab, with search.
adb shell input keyevent 4
sleep 2
tap "Policies"
sleep 2
shot 05-list 1
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | grep -q "StarHealth" || fail "The policies list never showed the policy"

crashed && fail "The app crashed during the walkthrough"
echo "=== Smoke test passed ==="
