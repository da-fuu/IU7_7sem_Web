#!/usr/bin/env python3
import argparse
import fcntl
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid


def main():
    parser = argparse.ArgumentParser(description="Run tests and always generate a local Allure report with history")
    parser.add_argument("suite", nargs="?", choices=("all", "unit", "integration", "e2e"), default="all")
    parser.add_argument("--client", default=os.environ.get("TEST_CLIENT_ID", "local"))
    parser.add_argument("--report-root", type=Path)
    parser.add_argument("--publish-root", type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,40}", args.client):
        parser.error("client must contain 1-40 letters, digits, underscores or hyphens")
    root = Path(__file__).resolve().parent.parent
    report_root = (args.report_root or root / "build/reports/local").resolve()
    series = report_root / args.client / args.suite
    series.mkdir(parents=True, exist_ok=True)
    with (series / "run.lock").open("w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            parser.error("this client and suite are already running; use a different --client")
        counter = series / "build-order"
        order = int(counter.read_text()) + 1 if counter.exists() else 1
        counter.write_text(str(order))
        run_dir = series / "runs" / f"{order:04d}-{uuid.uuid4().hex[:8]}"
        results = run_dir / "results"
        results.mkdir(parents=True)
        report = series / "report"
        if (report / "history").is_dir():
            shutil.copytree(report / "history", results / "history")
        (results / "executor.json").write_text(json.dumps({
            "name": "Local tests", "type": "local", "buildOrder": order,
            "buildName": f"{args.client} / {args.suite} / run {order}",
        }))
        environment = dict(os.environ, ALLURE_RESULTS_DIR=str(results), TEST_CLIENT_ID=args.client)
        gradle = ["./gradlew", "--console=plain", "--no-daemon", "--max-workers=1",
                  "-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8",
                  "-Pkotlin.compiler.execution.strategy=in-process"]
        commands = {
            "unit": gradle + ["test", "--rerun-tasks"],
            "integration": gradle + ["integrationTest"],
            "e2e": [sys.executable, "deploy/test/scripts/e2e_control.py", "run", "--"] + gradle + ["e2eTest"],
        }
        stages = list(commands) if args.suite == "all" else [args.suite]
        status = 0
        outcomes = dict(environment)
        with (run_dir / "tests.log").open("w") as log:
            for stage in stages:
                key = stage.upper() + "_OUTCOME"
                if status:
                    outcomes[key] = "skipped"
                    continue
                print(f"Starting {stage}: client={args.client}, run={order}", flush=True)
                with subprocess.Popen(commands[stage], cwd=root, env=environment, stdout=subprocess.PIPE,
                                      stderr=subprocess.STDOUT, text=True, bufsize=1) as process:
                    for line in process.stdout:
                        print(line, end="", flush=True)
                        log.write(line)
                        log.flush()
                    status = process.wait()
                outcomes[key] = "success" if status == 0 else "failure"
        subprocess.run([sys.executable, "scripts/generate_skipped_results.py", str(results)], cwd=root, env=outcomes, check=True)
        cli = root / "build/allure-cli/allure-2.27.0/bin/allure"
        if not cli.exists():
            subprocess.run(gradle + ["installAllureCli"], cwd=root, check=True)
        subprocess.run([str(cli), "generate", str(results), "-o", str(report), "--clean"], cwd=root, check=True)
        print(f"Allure report: {report}", flush=True)
        publish = args.publish_root or root / ".local/actions-runner/reports"
        if publish.is_dir():
            link = publish / (f"local-{args.suite}" if args.client == "local" else f"local-{args.client}-{args.suite}")
            if link.is_symlink():
                link.unlink()
            if not link.exists():
                link.symlink_to(report, target_is_directory=True)
                print(f"Open: http://127.0.0.1:18765/{link.name}/", flush=True)
        print(f"Test exit code: {status}", flush=True)
        return status if status >= 0 else 128 - status


if __name__ == "__main__":
    sys.exit(main())
