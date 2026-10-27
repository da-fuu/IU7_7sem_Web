#!/usr/bin/env bash
set -euo pipefail

report_root="${ALLURE_LOCAL_REPORT_DIR:?}"
report_name="${GITHUB_RUN_ID:?}-${GITHUB_RUN_ATTEMPT:?}"
mkdir -p "$report_root/$report_name"
rsync -a --delete "${GITHUB_WORKSPACE:?}/build/reports/allure-report/" "$report_root/$report_name/"
printf '\n[Открыть Allure на машине runner](http://127.0.0.1:18765/%s/)\n' "$report_name" >> "${GITHUB_STEP_SUMMARY:?}"
