#!/usr/bin/env python3
from contextlib import closing
import json
import os
from pathlib import Path
import time
import threading
import unittest
import uuid

import psycopg2

from e2e_control import cleanup_all, connect


class E2ECleanupTest(unittest.TestCase):
    def test_active_runs_are_preserved_and_abandoned_runs_are_cleaned(self):
        runs = [uuid.uuid4().hex for _ in range(2)]
        users = [str(uuid.uuid4()) for _ in runs]
        leases = []
        try:
            for run_id, user_id in zip(runs, users):
                lease = connect()
                lease.autocommit = True
                leases.append(lease)
                with lease.cursor() as cursor:
                    cursor.execute("SELECT pg_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-run:" + run_id,))
                    cursor.execute("INSERT INTO lab2_test.e2e_runs (run_id) VALUES (%s)", (run_id,))
                    cursor.execute(
                        "INSERT INTO users (id, login, name, password_hash, role) VALUES (%s, %s, %s, %s, %s)",
                        (user_id, f"lab2e2e_{run_id}_1", "cleanup test", "hash", "USER"),
                    )
            with closing(connect()) as cleaner:
                cleaner.autocommit = True
                cleanup_all(cleaner)
                self.assertEqual(self.count_users(cleaner, users), 2)
                leases[0].close()
                cleanup_all(cleaner)
                self.assertEqual(self.count_users(cleaner, users), 1)
                with cleaner.cursor() as cursor:
                    with self.assertRaises(psycopg2.Error):
                        cursor.execute(
                            "INSERT INTO users (id, login, name, password_hash, role) VALUES (%s, %s, %s, %s, %s)",
                            (str(uuid.uuid4()), f"lab2e2e_{runs[0]}_2", "late request", "hash", "USER"),
                        )
        finally:
            for lease in leases:
                lease.close()
            with closing(connect()) as cleaner:
                cleaner.autocommit = True
                cleanup_all(cleaner)
                self.assertEqual(self.count_users(cleaner, users), 0)

    def test_cleanup_waits_for_registration_transaction(self):
        run_id = uuid.uuid4().hex
        user_id = str(uuid.uuid4())
        lease = connect()
        lease.autocommit = True
        writer = connect()
        errors = []
        def clean():
            try:
                with closing(connect()) as connection:
                    connection.autocommit = True
                    cleanup_all(connection)
            except Exception as error:
                errors.append(error)
        cleaner_thread = None
        try:
            with lease.cursor() as cursor:
                cursor.execute("SELECT pg_advisory_lock(hashtextextended(%s, 0))", ("lab2-e2e-run:" + run_id,))
                cursor.execute("INSERT INTO lab2_test.e2e_runs (run_id) VALUES (%s)", (run_id,))
            with writer.cursor() as cursor:
                cursor.execute(
                    "INSERT INTO users (id, login, name, password_hash, role) VALUES (%s, %s, %s, %s, %s)",
                    (user_id, f"lab2e2e_{run_id}_1", "in-flight registration", "hash", "USER"),
                )
            lease.close()
            cleaner_thread = threading.Thread(target=clean)
            cleaner_thread.start()
            time.sleep(0.2)
            writer.commit()
            cleaner_thread.join(timeout=10)
            self.assertFalse(cleaner_thread.is_alive())
            self.assertEqual(errors, [])
            with closing(connect()) as connection:
                connection.autocommit = True
                for _ in range(30):
                    cleanup_all(connection)
                    with connection.cursor() as cursor:
                        cursor.execute("SELECT EXISTS (SELECT 1 FROM lab2_test.e2e_runs WHERE run_id = %s)", (run_id,))
                        if not cursor.fetchone()[0]:
                            break
                    time.sleep(0.1)
                self.assertEqual(self.count_users(connection, [user_id]), 0)
        finally:
            writer.rollback()
            writer.close()
            lease.close()
            if cleaner_thread:
                cleaner_thread.join(timeout=10)
            with closing(connect()) as connection:
                connection.autocommit = True
                cleanup_all(connection)

    @staticmethod
    def count_users(connection, users):
        with connection.cursor() as cursor:
            cursor.execute("SELECT count(*) FROM users WHERE id = ANY(%s)", (users,))
            return cursor.fetchone()[0]


if __name__ == "__main__":
    started = int(time.time() * 1000)
    result = unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(E2ECleanupTest))
    results_dir = os.environ.get("ALLURE_RESULTS_DIR")
    if results_dir:
        identifier = str(uuid.uuid4())
        entry = {
            "uuid": identifier,
            "historyId": "lab2.e2e_cleanup",
            "fullName": "lab2.e2e_cleanup",
            "name": "E2E cleanup isolation",
            "status": "passed" if result.wasSuccessful() else "failed",
            "stage": "finished",
            "start": started,
            "stop": int(time.time() * 1000),
            "labels": [{"name": "suite", "value": "E2E cleanup"}],
        }
        if not result.wasSuccessful():
            entry["statusDetails"] = {"message": "Cleanup isolation verification failed; see CI logs"}
        destination = Path(results_dir)
        destination.mkdir(parents=True, exist_ok=True)
        (destination / f"{identifier}-result.json").write_text(json.dumps(entry), encoding="utf-8")
    raise SystemExit(0 if result.wasSuccessful() else 1)
