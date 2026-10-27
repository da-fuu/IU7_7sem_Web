#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys
import uuid

results = Path(sys.argv[1] if len(sys.argv) > 1 else "build/allure-results")
results.mkdir(parents=True, exist_ok=True)
for stage, title in (("UNIT", "Unit tests"), ("INTEGRATION", "Integration tests"), ("E2E", "E2E tests")):
    if os.environ.get(f"{stage}_OUTCOME") != "skipped":
        continue
    identifier = str(uuid.uuid4())
    result = {
        "uuid": identifier,
        "historyId": f"unexecuted-{stage.lower()}",
        "fullName": f"unexecuted.{stage.lower()}",
        "name": title,
        "status": "skipped",
        "statusDetails": {"message": "Stage was not executed because a previous step failed"},
        "stage": "finished",
    }
    (results / f"{identifier}-result.json").write_text(json.dumps(result), encoding="utf-8")
