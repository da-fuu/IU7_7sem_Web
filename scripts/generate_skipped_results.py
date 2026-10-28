#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys
import uuid

results = Path(sys.argv[1] if len(sys.argv) > 1 else "build/allure-results")
results.mkdir(parents=True, exist_ok=True)


def stage_of(result):
    labels = {label.get("name"): label.get("value", "") for label in result.get("labels", [])}
    class_name = labels.get("testClass", "")
    if result.get("fullName") == "lab2.e2e.infrastructure":
        return "E2E"
    if result.get("fullName") == "lab2.e2e_cleanup":
        return "CLEANUP"
    if class_name.endswith("MvpScenarioITCase"):
        return "E2E"
    if class_name.endswith("ITCase"):
        return "INTEGRATION"
    return "UNIT" if class_name else None


failed_stages = set()
for path in results.glob("*-result.json"):
    entry = json.loads(path.read_text(encoding="utf-8"))
    if entry.get("status") in {"failed", "broken"}:
        failed_stages.add(stage_of(entry))

stages = (
    ("UNIT", "Unit tests", True),
    ("INTEGRATION", "Integration tests", True),
    ("BACKEND", "Shared backend and cleanup service", False),
    ("CLEANUP", "E2E cleanup isolation", False),
    ("E2E", "E2E tests", True),
)
for stage, title, mark_skipped in stages:
    outcome = os.environ.get(f"{stage}_OUTCOME")
    if outcome == "skipped" and mark_skipped:
        status = "skipped"
        reason = "Stage was not executed because a previous step failed"
    elif outcome == "failure" and stage not in failed_stages:
        status = "broken"
        reason = "Stage failed before producing a failed test result; see CI logs"
    else:
        continue
    identifier = str(uuid.uuid4())
    entry = {
        "uuid": identifier,
        "historyId": f"stage-{stage.lower()}",
        "fullName": f"stage.{stage.lower()}",
        "name": title,
        "status": status,
        "statusDetails": {"message": reason},
        "stage": "finished",
    }
    (results / f"{identifier}-result.json").write_text(json.dumps(entry), encoding="utf-8")
