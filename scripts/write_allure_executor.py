#!/usr/bin/env python3
import json
import os
from pathlib import Path

run_number = int(os.environ["GITHUB_RUN_NUMBER"])
run_id = os.environ["GITHUB_RUN_ID"]
run_attempt = os.environ["GITHUB_RUN_ATTEMPT"]
server = os.environ["GITHUB_SERVER_URL"].rstrip("/")
repository = os.environ["GITHUB_REPOSITORY"]
entry = {
    "name": "GitHub Actions",
    "type": "github",
    "buildName": f"Run {run_number}, attempt {run_attempt}",
    "buildOrder": run_number,
    "buildUrl": f"{server}/{repository}/actions/runs/{run_id}/attempts/{run_attempt}",
}
results = Path("build/allure-results")
results.mkdir(parents=True, exist_ok=True)
(results / "executor.json").write_text(json.dumps(entry), encoding="utf-8")
