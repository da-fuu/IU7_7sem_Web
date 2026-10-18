#!/usr/bin/env python3
import sys
import time
import threading
import requests
from pathlib import Path

# Add scripts directory to path to import mock handler
sys.path.insert(0, str(Path(__file__).resolve().parent))
from mock_server import TimeTrackerMockHandler
from http.server import HTTPServer

MOCK_PORT = 4015
BASE_URL = f"http://127.0.0.1:{MOCK_PORT}"


def start_server():
    server = HTTPServer(('127.0.0.1', MOCK_PORT), TimeTrackerMockHandler)
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    return server


def run_tests():
    print("=" * 70)
    print("TimeTracker REST API — Multi-Step User Scenarios Verification")
    print(f"Target Server: {BASE_URL}")
    print("=" * 70)

    server = start_server()
    time.sleep(0.5)

    passed_steps = 0
    total_steps = 15

    try:
        # Step 1: Register User
        print("\n[Step 1] POST /api/v1/auth/register (operationId: registerUser)")
        reg_payload = {
            "login": "alex_dev",
            "name": "Алексей Смирнов",
            "password": "securePassword123",
            "role": "USER"
        }
        res = requests.post(f"{BASE_URL}/api/v1/auth/register", json=reg_payload)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        data = res.json()
        assert "token" in data and "user" in data
        assert data["user"]["login"] == "alex_dev"
        assert "settings" not in data["user"], "Settings should not be returned in User object"
        token = data["token"]
        user_id = data["user"]["id"]
        headers = {"Authorization": f"Bearer {token}"}
        print(f"  -> SUCCESS (201 Created): Registered user id={user_id} (lean profile without settings)")
        passed_steps += 1

        # Step 2: Login User
        print("\n[Step 2] POST /api/v1/auth/login (operationId: loginUser)")
        login_payload = {"login": "alex_dev", "password": "securePassword123"}
        res = requests.post(f"{BASE_URL}/api/v1/auth/login", json=login_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        data = res.json()
        assert data["token"] == token
        assert data["user"]["id"] == user_id
        assert "settings" not in data["user"], "Settings should not be returned in User object"
        print(f"  -> SUCCESS (200 OK): Login successful, token received for user {user_id}")
        passed_steps += 1

        # Step 3: Get User Profile by ID (Canonical Resource)
        print(f"\n[Step 3] GET /api/v1/users/{user_id} (operationId: getUserById)")
        res = requests.get(f"{BASE_URL}/api/v1/users/{user_id}", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        data = res.json()
        assert data["id"] == user_id
        assert "settings" not in data, "Settings should not be returned in User profile"
        print(f"  -> SUCCESS (200 OK): Profile authenticated for {data['name']} (lean profile without settings)")
        passed_steps += 1

        # Step 4: Get User Settings
        print(f"\n[Step 4] GET /api/v1/users/{user_id}/settings (operationId: getUserSettings)")
        res = requests.get(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        settings = res.json()
        assert settings["autoStopEnabled"] is True
        assert settings["maxActiveDurationUnit"] == "hours"
        assert settings["minDurationUnit"] == "minutes"
        print(f"  -> SUCCESS (200 OK): Settings retrieved (autoStop={settings['maxActiveDurationValue']} {settings['maxActiveDurationUnit']}, min={settings['minDurationValue']} {settings['minDurationUnit']})")
        passed_steps += 1

        # Step 5: Update Settings with Custom Units
        print(f"\n[Step 5] PATCH /api/v1/users/{user_id}/settings (operationId: updateUserSettings)")
        patch_settings = {
            "autoStopEnabled": True,
            "maxActiveDurationValue": 12,
            "maxActiveDurationUnit": "hours",
            "minDurationEnabled": False,
            "minDurationValue": 30,
            "minDurationUnit": "seconds"
        }
        res = requests.patch(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=headers, json=patch_settings)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        updated_settings = res.json()
        assert updated_settings["minDurationEnabled"] is False
        assert updated_settings["maxActiveDurationValue"] == 12
        assert updated_settings["minDurationUnit"] == "seconds"
        print(f"  -> SUCCESS (200 OK): Units updated (12 hours auto-stop, min duration disabled)")
        passed_steps += 1

        # Step 6: Create Custom Category
        print("\n[Step 6] POST /api/v1/categories (operationId: createCategory)")
        cat_payload = {
            "name": "Спорт и здоровье",
            "colorHex": "#EC4899",
            "isDefault": False
        }
        res = requests.post(f"{BASE_URL}/api/v1/categories", headers=headers, json=cat_payload)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        custom_cat = res.json()
        custom_cat_id = custom_cat["id"]
        assert custom_cat["name"] == "Спорт и здоровье"
        assert custom_cat["colorHex"] == "#EC4899"
        print(f"  -> SUCCESS (201 Created): Custom category created, id={custom_cat_id}")
        passed_steps += 1

        # Step 7: List Categories (Verify items envelope)
        print("\n[Step 7] GET /api/v1/categories (operationId: listCategories)")
        res = requests.get(f"{BASE_URL}/api/v1/categories", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        cat_resp = res.json()
        assert "items" in cat_resp, "Expected dictionary with 'items' key!"
        cats = cat_resp["items"]
        cat_ids = [c["id"] for c in cats]
        assert custom_cat_id in cat_ids
        print(f"  -> SUCCESS (200 OK): Found {len(cats)} categories inside 'items' envelope")
        passed_steps += 1

        # Step 8: Start First Active Task (Record with endTime=null)
        print("\n[Step 8] POST /api/v1/records (operationId: createRecord — Active Task 1)")
        active_payload_1 = {
            "categoryId": custom_cat_id,
            "startTime": "2026-10-15T10:00:00Z",
            "description": "Разработка REST API контракта"
        }
        res = requests.post(f"{BASE_URL}/api/v1/records", headers=headers, json=active_payload_1)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        active_rec_1 = res.json()
        rec_id_1 = active_rec_1["id"]
        assert active_rec_1["endTime"] is None
        print(f"  -> SUCCESS (201 Created): Active task 1 started, id={rec_id_1}, endTime=null")
        passed_steps += 1

        # Step 9: Multi-tasking — Start Second Concurrent Active Task
        print("\n[Step 9] POST /api/v1/records [Multi-tasking Check] (operationId: createRecord — Active Task 2)")
        active_payload_2 = {
            "categoryId": "11111111-1111-1111-1111-111111111111",
            "startTime": "2026-10-15T10:15:00Z",
            "description": "Фоновое прослушивание технического доклада"
        }
        res = requests.post(f"{BASE_URL}/api/v1/records", headers=headers, json=active_payload_2)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        active_rec_2 = res.json()
        rec_id_2 = active_rec_2["id"]
        assert active_rec_2["endTime"] is None
        print(f"  -> SUCCESS (201 Created): Concurrent active task 2 started without conflict, id={rec_id_2}")
        passed_steps += 1

        # Step 10: Overlap Allowance — Create Overlapping Completed Record
        print("\n[Step 10] POST /api/v1/records [Overlap Allowance] (operationId: createRecord)")
        overlap_payload = {
            "categoryId": custom_cat_id,
            "startTime": "2026-10-15T14:00:00Z",
            "endTime": "2026-10-15T15:30:00Z",
            "description": "Параллельная работа над задачами"
        }
        res = requests.post(f"{BASE_URL}/api/v1/records", headers=headers, json=overlap_payload)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        overlap_rec = res.json()
        overlap_rec_id = overlap_rec["id"]
        print(f"  -> SUCCESS (201 Created): Overlapping record created successfully, id={overlap_rec_id}")
        passed_steps += 1

        # Step 11: Stop Active Task via PATCH
        print(f"\n[Step 11] PATCH /api/v1/records/{rec_id_1} (operationId: patchRecord — Stop Task)")
        stop_payload = {"endTime": "2026-10-15T10:45:00Z"}
        res = requests.patch(f"{BASE_URL}/api/v1/records/{rec_id_1}", headers=headers, json=stop_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        stopped_rec = res.json()
        assert stopped_rec["endTime"] == stop_payload["endTime"]
        print(f"  -> SUCCESS (200 OK): Active task 1 stopped with endTime={stopped_rec['endTime']}")
        passed_steps += 1

        # Step 12: Modify Time Interval and Description via PATCH
        print(f"\n[Step 12] PATCH /api/v1/records/{rec_id_1} (operationId: patchRecord — Modify Times & Description)")
        update_payload = {
            "startTime": "2026-10-15T10:05:00Z",
            "endTime": "2026-10-15T11:00:00Z",
            "description": "Разработка REST API контракта и ревью схем"
        }
        res = requests.patch(f"{BASE_URL}/api/v1/records/{rec_id_1}", headers=headers, json=update_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        modified_rec = res.json()
        assert modified_rec["startTime"] == update_payload["startTime"]
        assert modified_rec["endTime"] == update_payload["endTime"]
        assert modified_rec["description"] == update_payload["description"]
        print(f"  -> SUCCESS (200 OK): Interval and description modified, full snapshot archived")
        passed_steps += 1

        # Step 13: Inspect Record History (Verify items envelope and full snapshot)
        print(f"\n[Step 13] GET /api/v1/records/{rec_id_1}/history (operationId: getRecordHistory)")
        res = requests.get(f"{BASE_URL}/api/v1/records/{rec_id_1}/history", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        history_resp = res.json()
        assert "items" in history_resp, "Expected dictionary with 'items' key!"
        history_list = history_resp["items"]
        assert len(history_list) >= 1
        archived_entry = history_list[0]
        archived_ver_id = archived_entry["id"]
        assert "startTime" in archived_entry and "endTime" in archived_entry and "categoryId" in archived_entry
        assert archived_entry["description"] == "Разработка REST API контракта"
        print(f"  -> SUCCESS (200 OK): Found {len(history_list)} history version(s) in 'items', snapshot verified (id={archived_ver_id})")
        passed_steps += 1

        # Step 14: Rollback Complete Record State from History
        print(f"\n[Step 14] POST /api/v1/records/{rec_id_1}/history/{archived_ver_id}/rollback (operationId: rollbackRecordVersion)")
        res = requests.post(f"{BASE_URL}/api/v1/records/{rec_id_1}/history/{archived_ver_id}/rollback", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        restored_rec = res.json()
        assert restored_rec["description"] == "Разработка REST API контракта"
        assert restored_rec["endTime"] == "2026-10-15T10:45:00Z"
        print(f"  -> SUCCESS (200 OK): Forward-only rollback executed, times and description restored")
        passed_steps += 1

        # Step 15: Business Rule Validation with Custom Unit (30 seconds threshold)
        print(f"\n[Step 15] Business Rule Validation: Dynamic Unit Threshold (30 seconds)")
        # Enable minDuration: 30 seconds
        requests.patch(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=headers, json={
            "minDurationEnabled": True,
            "minDurationValue": 30,
            "minDurationUnit": "seconds"
        })
        # Try creating a 10-second completed record
        short_payload = {
            "categoryId": custom_cat_id,
            "startTime": "2026-10-15T12:00:00Z",
            "endTime": "2026-10-15T12:00:10Z",
            "description": "Случайный клик"
        }
        res = requests.post(f"{BASE_URL}/api/v1/records", headers=headers, json=short_payload)
        assert res.status_code == 422, f"Expected 422 Unprocessable Entity, got {res.status_code}: {res.text}"
        err_data = res.json()
        assert err_data["code"] == "DURATION_TOO_SHORT"
        print(f"  -> SUCCESS (422 Unprocessable Entity): Short interval (< 30s) rejected according to user settings")
        passed_steps += 1

        print("\n" + "=" * 70)
        print(f"ALL SCENARIO TESTS PASSED SUCCESSFULLY! ({passed_steps}/{total_steps} steps)")
        print("=" * 70)
        return 0

    except Exception as e:
        import traceback
        print(f"\n[FAILED] Test failed at step: {e}", file=sys.stderr)
        traceback.print_exc()
        return 1
    finally:
        server.shutdown()


if __name__ == '__main__':
    sys.exit(run_tests())
