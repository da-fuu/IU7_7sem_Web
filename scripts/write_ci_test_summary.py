#!/usr/bin/env python3
import json
import os
from collections import Counter
from pathlib import Path

results = Path("build/allure-results")
counts = Counter()
failures = []
for path in results.glob("*-result.json"):
    result = json.loads(path.read_text(encoding="utf-8"))
    status = result.get("status", "unknown")
    counts[status] += 1
    if status in {"failed", "broken"}:
        failures.append(result.get("fullName", result.get("name", "unknown")))

lines = ["## Результаты тестов", "", "| Статус | Количество |", "| --- | ---: |"]
for status in ("passed", "failed", "broken", "skipped", "unknown"):
    lines.append(f"| {status} | {counts[status]} |")
if failures:
    lines.extend(["", "Не прошли:", ""])
    lines.extend(f"- `{name.replace('`', '')}`" for name in failures)
lines.extend(["", "Полный Allure-отчёт сохраняется как артефакт и публикуется для просмотра на машине runner."])
with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
    summary.write("\n".join(lines) + "\n")
