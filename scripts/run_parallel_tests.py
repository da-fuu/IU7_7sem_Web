#!/usr/bin/env python3
import argparse
import concurrent.futures
import datetime
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import threading


def main():
    parser = argparse.ArgumentParser(description="Run independent developers' tests against the shared stand")
    parser.add_argument("developers", nargs="?", type=int, default=5)
    parser.add_argument("--suite", choices=("all", "integration", "e2e"), default="all")
    args = parser.parse_args()
    if not 1 <= args.developers <= 20:
        parser.error("developers must be between 1 and 20")
    root = Path(__file__).resolve().parent.parent
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    output = root / "build" / "reports" / "parallel" / stamp
    output.mkdir(parents=True)
    files = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=root
    ).decode().split("\0")
    workspaces = []
    for number in range(1, args.developers + 1):
        workspace = output / f"dev{number}" / "workspace"
        workspace.mkdir(parents=True)
        for name in files:
            source = root / name
            if not name or not source.is_file() or source.suffix in (".pdf", ".zip"):
                continue
            target = workspace / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
        workspaces.append(workspace)
    processes = set()
    lock = threading.Lock()
    stopped = threading.Event()
    start = threading.Barrier(args.developers)

    def stop(signum, _frame):
        stopped.set()
        with lock:
            for process in processes:
                if process.poll() is None:
                    process.send_signal(signum)

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)

    def run_developer(number, workspace):
        client = f"dev{number}"
        environment = dict(os.environ, TEST_CLIENT_ID=client)
        commands = [[sys.executable, "scripts/run_tests.py", args.suite, "--client", client,
                     "--report-root", str(root / "build/reports/local"),
                     "--publish-root", str(root / ".local/actions-runner/reports")]]
        logfile = workspace.parent / "tests.log"
        start.wait()
        print(f"START {client}: {logfile}", flush=True)
        status = 0
        with logfile.open("w") as log:
            for command in commands:
                with lock:
                    if stopped.is_set():
                        status = 130
                        break
                    process = subprocess.Popen(command, cwd=workspace, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
                    processes.add(process)
                for line in process.stdout:
                    log.write(line)
                    log.flush()
                    print(f"[{client}] {line.rstrip()}", flush=True)
                process.stdout.close()
                status = process.wait()
                with lock:
                    processes.discard(process)
                if status:
                    break
        print(f"{'PASS' if status == 0 else 'FAIL'} {client}: exit={status}", flush=True)
        return {"client": client, "exit_code": status, "log": str(logfile)}

    print(f"Shared stand: {args.developers} developers, suite={args.suite}", flush=True)
    print(f"Results: {output}", flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.developers) as executor:
        futures = [executor.submit(run_developer, number, workspace) for number, workspace in enumerate(workspaces, 1)]
        results = [future.result() for future in futures]
    (output / "summary.json").write_text(json.dumps(results, indent=2) + "\n")
    return 1 if any(result["exit_code"] for result in results) else 0


if __name__ == "__main__":
    sys.exit(main())
