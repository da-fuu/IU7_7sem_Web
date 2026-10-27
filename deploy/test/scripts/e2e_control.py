#!/usr/bin/env python3
import argparse
from contextlib import closing
import os
import signal
import subprocess
import sys
import time
import uuid

import psycopg2


def connect():
    return psycopg2.connect(
        host=os.environ.get("POSTGRES_TEST_HOST", "127.0.0.1"),
        port=os.environ.get("POSTGRES_TEST_PORT", "55432"),
        dbname=os.environ.get("POSTGRES_TEST_DB", "timetracker_test"),
        user=os.environ.get("POSTGRES_TEST_USER", "timetracker_test"),
        password=os.environ.get("POSTGRES_TEST_PASSWORD", "timetracker_test_pass"),
        connect_timeout=5,
        keepalives=1,
        keepalives_idle=10,
        keepalives_interval=5,
        keepalives_count=3,
    )


def cleanup_run(connection, run_id):
    with connection.cursor() as cursor:
        cursor.execute("SELECT pg_try_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-run:" + run_id,))
        if not cursor.fetchone()[0]:
            return False
        try:
            cursor.execute("BEGIN")
            cursor.execute("SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))", ("lab2-e2e-data:" + run_id,))
            cursor.execute("SELECT user_id FROM lab2_test.e2e_users WHERE run_id = %s ORDER BY user_id", (run_id,))
            user_ids = [row[0] for row in cursor.fetchall()]
            for user_id in user_ids:
                cursor.execute("DELETE FROM public.users WHERE id = %s", (user_id,))
            cursor.execute("DELETE FROM lab2_test.e2e_runs WHERE run_id = %s", (run_id,))
            cursor.execute("COMMIT")
        except BaseException:
            cursor.execute("ROLLBACK")
            raise
        finally:
            cursor.execute("SELECT pg_advisory_unlock(hashtextextended(%s, 0))", ("lab2-e2e-run:" + run_id,))
    return True


def cleanup_all(connection):
    with connection.cursor() as cursor:
        cursor.execute("SELECT run_id FROM lab2_test.e2e_runs")
        runs = [row[0] for row in cursor.fetchall()]
    for run_id in runs:
        cleanup_run(connection, run_id)


def watch():
    while True:
        try:
            with closing(connect()) as connection:
                connection.autocommit = True
                with connection.cursor() as cursor:
                    cursor.execute("SELECT pg_try_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-cleaner",))
                    if not cursor.fetchone()[0]:
                        time.sleep(2)
                        continue
                while True:
                    cleanup_all(connection)
                    time.sleep(2)
        except Exception as error:
            print(f"E2E cleanup failed: {error}", file=sys.stderr, flush=True)
            time.sleep(2)


def run(command):
    if not command:
        raise ValueError("A test command is required")
    run_id = uuid.uuid4().hex
    lease = connect()
    lease.autocommit = True
    process = None
    try:
        with lease.cursor() as cursor:
            cursor.execute("SELECT pg_try_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-cleaner",))
            if cursor.fetchone()[0]:
                cursor.execute("SELECT pg_advisory_unlock(hashtextextended(%s, 0))", ("lab2-e2e-cleaner",))
                raise RuntimeError("E2E cleanup service is not connected")
            cursor.execute("SELECT pg_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-run:" + run_id,))
            cursor.execute("INSERT INTO lab2_test.e2e_runs (run_id) VALUES (%s)", (run_id,))
        environment = dict(os.environ, E2E_RUN_ID=run_id)
        process = subprocess.Popen(command, env=environment, start_new_session=True)
        def stop_child(signum, _frame):
            if process.poll() is None:
                os.killpg(process.pid, signum)
        signal.signal(signal.SIGTERM, stop_child)
        signal.signal(signal.SIGINT, stop_child)
        status = process.wait()
    finally:
        lease.close()
    with closing(connect()) as connection:
        connection.autocommit = True
        for _ in range(30):
            cleanup_run(connection, run_id)
            with connection.cursor() as cursor:
                cursor.execute("SELECT EXISTS (SELECT 1 FROM lab2_test.e2e_runs WHERE run_id = %s)", (run_id,))
                if not cursor.fetchone()[0]:
                    break
            time.sleep(1)
        else:
            raise RuntimeError("E2E run cleanup did not complete")
    return status if status >= 0 else 128 - status


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("run", "watch", "cleanup"))
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.mode == "watch":
        watch()
    elif args.mode == "cleanup":
        with closing(connect()) as connection:
            connection.autocommit = True
            cleanup_all(connection)
    else:
        command = args.command[1:] if args.command[:1] == ["--"] else args.command
        return run(command)
    return 0


if __name__ == "__main__":
    sys.exit(main())
