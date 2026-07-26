#!/bin/sh
set -eu

serial="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
test "$(printf '%s\n' "$serial" | sed '/^$/d' | wc -l | tr -d ' ')" = "1"
original_font="$(adb -s "$serial" shell settings get system font_scale | tr -d '\r')"
restore() {
    adb -s "$serial" shell wm size reset >/dev/null
    adb -s "$serial" shell wm density reset >/dev/null
    if test "$original_font" = "null"; then
        adb -s "$serial" shell settings delete system font_scale >/dev/null
    else
        adb -s "$serial" shell settings put system font_scale "$original_font" >/dev/null
    fi
}
trap restore EXIT HUP INT TERM
./gradlew assembleDebug assembleDebugAndroidTest
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null

for width_height in 1280x720 1920x1080 3840x2160; do
    adb -s "$serial" shell wm size "$width_height" >/dev/null
    adb -s "$serial" shell wm density 320 >/dev/null
    adb -s "$serial" shell settings put system font_scale 1.0 >/dev/null
    adb -s "$serial" shell am instrument -w \
        -e class org.kaloscope.tv.test.golden.P2GoldenScreenshotTest \
        org.kaloscope.tv.test/androidx.test.runner.AndroidJUnitRunner
done
