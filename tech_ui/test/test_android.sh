#!/bin/bash
set -e

./gradlew :tech_ui:dex

adb push dist/timetracker-dex.jar /data/local/tmp/timetracker/
adb push dist/lib /data/local/tmp/timetracker/
adb push tech_ui/test/test_cli_android.sh /data/local/tmp/timetracker/

adb shell "cd /data/local/tmp/timetracker && chmod +x test_cli_android.sh && ./test_cli_android.sh"
