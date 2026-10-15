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
        token = data["token"]
        user_id = data["user"]["id"]
        headers = {"Authorization": f"Bearer {token}"}
        print(f"  -> SUCCESS (201 Created): Registered user id={user_id}")
        passed_steps += 1

        # Step 2: Login User
        print("\n[Step 2] POST /api/v1/auth/login (operationId: loginUser)")
        login_payload = {"login": "alex_dev", "password": "securePassword123"}
        res = requests.post(f"{BASE_URL}/api/v1/auth/login", json=login_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        data = res.json()
        assert data["token"] == token
        print(f"  -> SUCCESS (200 OK): Login successful, token received")
        passed_steps += 1

        # Step 3: Get Current User Profile
        print("\n[Step 3] GET /api/v1/users/me (operationId: getCurrentUser)")
        res = requests.get(f"{BASE_URL}/api/v1/users/me", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        data = res.json()
        assert data["id"] == user_id
        print(f"  -> SUCCESS (200 OK): Profile authenticated for {data['name']}")
        passed_steps += 1

        # Step 4: Create Custom Category
        print("\n[Step 4] POST /api/v1/categories (operationId: createCategory)")
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

        # Step 5: List Categories
        print("\n[Step 5] GET /api/v1/categories (operationId: listCategories)")
        res = requests.get(f"{BASE_URL}/api/v1/categories", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        cats = res.json()
        cat_ids = [c["id"] for c in cats]
        assert custom_cat_id in cat_ids
        print(f"  -> SUCCESS (200 OK): Found {len(cats)} categories (including default and custom)")
        passed_steps += 1

        # Step 6: Start Timer Session
        print("\n[Step 6] POST /api/v1/timer/start (operationId: startTimer)")
        start_payload = {"categoryId": custom_cat_id}
        res = requests.post(f"{BASE_URL}/api/v1/timer/start", headers=headers, json=start_payload)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        timer_data = res.json()
        active_rec_id = timer_data["recordId"]
        assert timer_data["categoryId"] == custom_cat_id
        print(f"  -> SUCCESS (201 Created): Timer session started, recordId={active_rec_id}")
        passed_steps += 1

        # Step 7: Check Active Timer
        print("\n[Step 7] GET /api/v1/timer/active (operationId: getActiveTimer)")
        res = requests.get(f"{BASE_URL}/api/v1/timer/active", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        active_status = res.json()
        assert active_status is not None
        assert active_status["recordId"] == active_rec_id
        print(f"  -> SUCCESS (200 OK): Active session verified (elapsedSeconds={active_status['elapsedSeconds']})")
        passed_steps += 1

        # Step 8: Conflict Rule Test — Try to Start Second Timer
        print("\n[Step 8] POST /api/v1/timer/start [Conflict Check] (operationId: startTimer)")
        res = requests.post(f"{BASE_URL}/api/v1/timer/start", headers=headers, json=start_payload)
        assert res.status_code == 409, f"Expected 409 Conflict, got {res.status_code}: {res.text}"
        conflict_err = res.json()
        assert conflict_err["code"] == "TIMER_ALREADY_ACTIVE"
        print(f"  -> SUCCESS (409 Conflict): Business rule enforced ('{conflict_err['code']}')")
        passed_steps += 1

        # Step 9: Stop Active Timer
        print("\n[Step 9] POST /api/v1/timer/stop (operationId: stopTimer)")
        stop_payload = {"description": "Утренняя тренировка и разминка"}
        res = requests.post(f"{BASE_URL}/api/v1/timer/stop", headers=headers, json=stop_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        saved_rec = res.json()
        assert saved_rec["id"] == active_rec_id
        assert saved_rec["description"] == stop_payload["description"]
        assert saved_rec["endTime"] is not None
        print(f"  -> SUCCESS (200 OK): Timer stopped, record saved in database")
        passed_steps += 1

        # Step 10: Create Manual Record
        print("\n[Step 10] POST /api/v1/records (operationId: createRecord)")
        manual_payload = {
            "categoryId": custom_cat_id,
            "startTime": "2026-10-15T14:00:00Z",
            "endTime": "2026-10-15T15:30:00Z",
            "description": "Проектирование диаграммы классов и API контракта"
        }
        res = requests.post(f"{BASE_URL}/api/v1/records", headers=headers, json=manual_payload)
        assert res.status_code == 201, f"Expected 201, got {res.status_code}: {res.text}"
        manual_rec = res.json()
        manual_rec_id = manual_rec["id"]
        print(f"  -> SUCCESS (201 Created): Manual record created, id={manual_rec_id}")
        passed_steps += 1

        # Step 11: Partial Update (PATCH) Record Description
        print("\n[Step 11] PATCH /api/v1/records/{recordId} (operationId: patchRecord)")
        patch_payload = {
            "description": "Проектирование диаграммы классов, API контракта и ревью"
        }
        res = requests.patch(f"{BASE_URL}/api/v1/records/{manual_rec_id}", headers=headers, json=patch_payload)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        patched_rec = res.json()
        assert patched_rec["description"] == patch_payload["description"]
        print(f"  -> SUCCESS (200 OK): Description updated, previous version archived to audit")
        passed_steps += 1

        # Step 12: Inspect Description History
        print("\n[Step 12] GET /api/v1/records/{recordId}/history (operationId: getRecordHistory)")
        res = requests.get(f"{BASE_URL}/api/v1/records/{manual_rec_id}/history", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        history_list = res.json()
        assert len(history_list) >= 1
        archived_ver_id = history_list[0]["id"]
        assert history_list[0]["description"] == "Проектирование диаграммы классов и API контракта"
        print(f"  -> SUCCESS (200 OK): Found {len(history_list)} history version(s), target id={archived_ver_id}")
        passed_steps += 1

        # Step 13: Rollback Description to Previous Version
        print(f"\n[Step 13] POST /api/v1/records/{manual_rec_id}/history/{archived_ver_id}/rollback (operationId: rollbackRecordDescription)")
        res = requests.post(f"{BASE_URL}/api/v1/records/{manual_rec_id}/history/{archived_ver_id}/rollback", headers=headers)
        assert res.status_code == 200, f"Expected 200, got {res.status_code}: {res.text}"
        restored_rec = res.json()
        assert restored_rec["description"] == "Проектирование диаграммы классов и API контракта"
        print(f"  -> SUCCESS (200 OK): Forward-only rollback executed, description successfully restored")
        passed_steps += 1

        # Step 14: Delete Custom Category
        print(f"\n[Step 14] DELETE /api/v1/categories/{custom_cat_id} (operationId: deleteCategory)")
        res = requests.delete(f"{BASE_URL}/api/v1/categories/{custom_cat_id}", headers=headers)
        assert res.status_code == 204, f"Expected 204 No Content, got {res.status_code}"
        print(f"  -> SUCCESS (204 No Content): Custom category deleted")
        passed_steps += 1

        # Step 15: Business Rule Check — Minimum Duration Rejection (422 Unprocessable Entity)
        print("\n[Step 15] Business Rule Validation: Min Duration (< 1 min) Rejection")
        # Start a quick session
        requests.post(f"{BASE_URL}/api/v1/timer/start", headers=headers, json={"categoryId": "11111111-1111-1111-1111-111111111111"})
        # Stop with simulateShort
        res = requests.post(f"{BASE_URL}/api/v1/timer/stop", headers=headers, json={"simulateShort": True})
        assert res.status_code == 422, f"Expected 422 Unprocessable Entity, got {res.status_code}: {res.text}"
        short_err = res.json()
        assert short_err["code"] == "SESSION_TOO_SHORT"
        print(f"  -> SUCCESS (422 Unprocessable Entity): Short session rejected as random click ('{short_err['code']}')")
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
