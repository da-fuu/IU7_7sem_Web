#!/usr/bin/env python3
import sys
import uuid
import requests

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"

def run_live_verification():
    print("=" * 70)
    print("TimeTracker REST API — Live Verification Script")
    print(f"Target Server: {BASE_URL}")
    print("=" * 70)

    session = requests.Session()
    passed = 0
    total = 14

    suffix = uuid.uuid4().hex[:6]
    test_user_login = f"user_{suffix}"
    test_admin_login = f"admin_{suffix}"
    password = "SecurePassword123"

    print("\n[Step 1] Health Check GET /health")
    res = session.get(f"{BASE_URL}/health")
    assert res.status_code == 200 and res.json().get("status") == "UP"
    print("  -> SUCCESS: Service is UP")
    passed += 1

    print("\n[Step 2] Documentation Endpoints GET /api/v1/docs & GET /api/v1/openapi.yaml")
    docs_res = session.get(f"{BASE_URL}/api/v1/docs")
    spec_res = session.get(f"{BASE_URL}/api/v1/openapi.yaml")
    assert docs_res.status_code == 200 and "Swagger UI" in docs_res.text
    assert spec_res.status_code == 200 and "openapi: 3.1.0" in spec_res.text
    print("  -> SUCCESS: Swagger UI and OpenAPI contract delivered")
    passed += 1

    print("\n[Step 3] Register User & Admin POST /api/v1/auth/register")
    reg_user = session.post(f"{BASE_URL}/api/v1/auth/register", json={
        "login": test_user_login,
        "name": "Тестовый Пользователь",
        "password": password,
        "role": "USER"
    })
    assert reg_user.status_code == 201
    user_token = reg_user.json()["token"]
    user_id = reg_user.json()["user"]["id"]
    user_headers = {"Authorization": f"Bearer {user_token}"}

    reg_admin = session.post(f"{BASE_URL}/api/v1/auth/register", json={
        "login": test_admin_login,
        "name": "Тестовый Администратор",
        "password": password,
        "role": "ADMIN"
    })
    assert reg_admin.status_code == 201
    admin_token = reg_admin.json()["token"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    print(f"  -> SUCCESS: User ({user_id}) and Admin registered")
    passed += 1

    print("\n[Step 4] Validation and Conflict Tests on Register")
    dup_res = session.post(f"{BASE_URL}/api/v1/auth/register", json={
        "login": test_user_login,
        "name": "Дубликат",
        "password": password,
        "role": "USER"
    })
    assert dup_res.status_code == 409
    invalid_res = session.post(f"{BASE_URL}/api/v1/auth/register", json={
        "login": "ab",
        "name": "Иван",
        "password": password
    })
    assert invalid_res.status_code == 400
    print("  -> SUCCESS: Duplicate login rejected with 409, short login rejected with 400")
    passed += 1

    print("\n[Step 5] Authentication & Security POST /api/v1/auth/login")
    login_ok = session.post(f"{BASE_URL}/api/v1/auth/login", json={
        "login": test_user_login,
        "password": password
    })
    assert login_ok.status_code == 200 and "token" in login_ok.json()
    login_bad = session.post(f"{BASE_URL}/api/v1/auth/login", json={
        "login": test_user_login,
        "password": "WrongPassword"
    })
    assert login_bad.status_code == 401
    print("  -> SUCCESS: Login successful with valid password, rejected with 401 on invalid")
    passed += 1

    print("\n[Step 6] User Profile & Access Control GET /api/v1/users/{userId}")
    prof_res = session.get(f"{BASE_URL}/api/v1/users/{user_id}", headers=user_headers)
    assert prof_res.status_code == 200 and prof_res.json()["login"] == test_user_login
    random_user_uuid = str(uuid.uuid4())
    prof_forbidden = session.get(f"{BASE_URL}/api/v1/users/{random_user_uuid}", headers=user_headers)
    assert prof_forbidden.status_code == 403
    prof_unauth = session.get(f"{BASE_URL}/api/v1/users/{user_id}")
    assert prof_unauth.status_code == 401
    print("  -> SUCCESS: Profile accessed, unauthorized rejected with 401, cross-user access rejected with 403")
    passed += 1

    print("\n[Step 7] User Settings GET & PATCH /api/v1/users/{userId}/settings")
    settings_res = session.get(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=user_headers)
    assert settings_res.status_code == 200
    patch_settings = session.patch(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=user_headers, json={
        "maxActiveDurationValue": 10,
        "maxActiveDurationUnit": "hours",
        "minDurationEnabled": False
    })
    assert patch_settings.status_code == 200
    assert patch_settings.json()["maxActiveDurationValue"] == 10
    assert patch_settings.json()["minDurationEnabled"] is False
    print("  -> SUCCESS: Settings retrieved and partially updated")
    passed += 1

    print("\n[Step 8] Category Contract & RBAC (Base Category Creation)")
    user_base_cat = session.post(f"{BASE_URL}/api/v1/categories", headers=user_headers, json={
        "name": "Нелегальная Базовая",
        "colorHex": "#FF0000",
        "isDefault": True
    })
    assert user_base_cat.status_code == 403

    admin_base_cat = session.post(f"{BASE_URL}/api/v1/categories", headers=admin_headers, json={
        "name": f"Системная {suffix}",
        "colorHex": "#3B82F6",
        "isDefault": True
    })
    assert admin_base_cat.status_code == 201
    base_cat_id = admin_base_cat.json()["id"]

    user_cat = session.post(f"{BASE_URL}/api/v1/categories", headers=user_headers, json={
        "name": f"Пользовательская {suffix}",
        "colorHex": "#10B981"
    })
    assert user_cat.status_code == 201
    user_cat_id = user_cat.json()["id"]

    cats_res = session.get(f"{BASE_URL}/api/v1/categories", headers=user_headers)
    assert cats_res.status_code == 200
    assert "items" in cats_res.json()
    print("  -> SUCCESS: RBAC enforced for base categories, items object returned")
    passed += 1

    print("\n[Step 9] Start Active Time Record POST /api/v1/records")
    rec_start = session.post(f"{BASE_URL}/api/v1/records", headers=user_headers, json={
        "categoryId": user_cat_id,
        "description": "Активная сессия разработки"
    })
    assert rec_start.status_code == 201
    active_rec = rec_start.json()
    assert active_rec["endTime"] is None
    record_id = active_rec["id"]
    print(f"  -> SUCCESS: Active record created with id={record_id}")
    passed += 1

    print("\n[Step 10] Parallel Active Records Allowed")
    rec2_start = session.post(f"{BASE_URL}/api/v1/records", headers=user_headers, json={
        "categoryId": base_cat_id,
        "description": "Вторая параллельная активность"
    })
    assert rec2_start.status_code == 201
    rec2_id = rec2_start.json()["id"]
    print("  -> SUCCESS: Parallel active records successfully permitted")
    passed += 1

    print("\n[Step 11] Stop Active Session by PATCH /api/v1/records/{id}")
    stop_res = session.patch(f"{BASE_URL}/api/v1/records/{record_id}", headers=user_headers, json={
        "endTime": "2026-10-19T12:00:00Z"
    })
    assert stop_res.status_code == 200
    assert stop_res.json()["endTime"] is not None
    session.delete(f"{BASE_URL}/api/v1/records/{rec2_id}", headers=user_headers)
    print("  -> SUCCESS: Active session stopped and second record cleaned up")
    passed += 1

    print("\n[Step 12] Validation on Records (Invalid Range & Short Duration)")
    inv_range = session.post(f"{BASE_URL}/api/v1/records", headers=user_headers, json={
        "categoryId": user_cat_id,
        "startTime": "2026-10-19T15:00:00Z",
        "endTime": "2026-10-19T14:00:00Z"
    })
    assert inv_range.status_code == 422
    assert inv_range.json()["code"] == "INVALID_TIME_RANGE"

    session.patch(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=user_headers, json={
        "minDurationEnabled": True,
        "minDurationValue": 5
    })
    short_dur = session.post(f"{BASE_URL}/api/v1/records", headers=user_headers, json={
        "categoryId": user_cat_id,
        "startTime": "2026-10-19T15:00:00Z",
        "endTime": "2026-10-19T15:01:00Z"
    })
    assert short_dur.status_code == 422
    assert short_dur.json()["code"] == "DURATION_TOO_SHORT"
    print("  -> SUCCESS: 422 INVALID_TIME_RANGE and DURATION_TOO_SHORT verified")
    passed += 1

    print("\n[Step 13] Description History Snapshots and Rollback")
    manual_rec = session.post(f"{BASE_URL}/api/v1/records", headers=user_headers, json={
        "categoryId": user_cat_id,
        "startTime": "2026-10-19T10:00:00Z",
        "endTime": "2026-10-19T11:00:00Z",
        "description": "Первоначальное описание задачи"
    })
    assert manual_rec.status_code == 201
    hist_rec_id = manual_rec.json()["id"]

    session.patch(f"{BASE_URL}/api/v1/records/{hist_rec_id}", headers=user_headers, json={
        "description": "Измененное описание 1"
    })
    session.patch(f"{BASE_URL}/api/v1/records/{hist_rec_id}", headers=user_headers, json={
        "description": "Измененное описание 2"
    })

    history_res = session.get(f"{BASE_URL}/api/v1/records/{hist_rec_id}/history", headers=user_headers)
    assert history_res.status_code == 200
    hist_items = history_res.json()["items"]
    assert len(hist_items) == 2
    initial_version_id = hist_items[1]["id"]

    rollback_res = session.post(f"{BASE_URL}/api/v1/records/{hist_rec_id}/history/{initial_version_id}/rollback", headers=user_headers)
    assert rollback_res.status_code == 200
    assert rollback_res.json()["description"] == "Первоначальное описание задачи"
    print("  -> SUCCESS: Version snapshots recorded and rollback verified")
    passed += 1

    print("\n[Step 14] Pagination on Records GET /api/v1/records?limit=1&offset=0")
    page_res = session.get(f"{BASE_URL}/api/v1/records?limit=1&offset=0", headers=user_headers)
    assert page_res.status_code == 200
    page_data = page_res.json()
    assert len(page_data["items"]) == 1
    assert page_data["limit"] == 1
    assert page_data["total"] >= 2
    print(f"  -> SUCCESS: Pagination verified (items={len(page_data['items'])}, total={page_data['total']})")
    passed += 1

    print("\n" + "=" * 70)
    print(f"ALL {passed}/{total} LIVE API VERIFICATION STEPS PASSED SUCCESSFULLY!")
    print("=" * 70)

if __name__ == "__main__":
    run_live_verification()
