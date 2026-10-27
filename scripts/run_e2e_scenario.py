#!/usr/bin/env python3
import datetime
import json
import sys
import time
import uuid
import requests

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"

def run():
    print(f"[E2E] Starting simulation against {BASE_URL}")
    session = requests.Session()

    suffix = uuid.uuid4().hex[:6]
    login = f"e2e_user_{suffix}"
    password = "E2EPassword2026"

    print("[Step 1] Health check")
    r = session.get(f"{BASE_URL}/health")
    assert r.status_code == 200, f"Health check failed: {r.status_code}"

    print("[Step 2] Registration")
    r = session.post(f"{BASE_URL}/api/v1/auth/register", json={
        "login": login,
        "name": "E2E Scenario Presenter",
        "password": password,
        "role": "USER"
    })
    assert r.status_code == 201, f"Registration failed: {r.status_code} {r.text}"
    data = r.json()
    token = data["token"]
    user_id = data["user"]["id"]
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    print("[Step 3] Configure user settings")
    r = session.patch(f"{BASE_URL}/api/v1/users/{user_id}/settings", headers=headers, json={
        "autoStopEnabled": True,
        "maxActiveDurationValue": 4,
        "maxActiveDurationUnit": "hours",
        "minDurationEnabled": False,
        "minDurationValue": 1,
        "minDurationUnit": "minutes"
    })
    assert r.status_code == 200, f"Settings failed: {r.status_code} {r.text}"

    print("[Step 4] Create custom category")
    r = session.post(f"{BASE_URL}/api/v1/categories", headers=headers, json={
        "name": "Демонстрация MVP",
        "colorHex": "#3B82F6"
    })
    assert r.status_code == 201, f"Create category failed: {r.status_code} {r.text}"
    category_id = r.json()["id"]

    print("[Step 5] Start active timer")
    start_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    r = session.post(f"{BASE_URL}/api/v1/records", headers=headers, json={
        "categoryId": category_id,
        "startTime": start_iso,
        "endTime": None,
        "description": "Первичная запись презентации"
    })
    assert r.status_code == 201, f"Start timer failed: {r.status_code} {r.text}"
    record_id = r.json()["id"]

    print("[Step 6] Query active records")
    r = session.get(f"{BASE_URL}/api/v1/records?status=active", headers=headers)
    assert r.status_code == 200, f"Query active records failed: {r.status_code} {r.text}"
    assert any(rec["id"] == record_id for rec in r.json()["items"])

    print("[Step 7] Stop timer")
    time.sleep(1)
    end_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    r = session.patch(f"{BASE_URL}/api/v1/records/{record_id}", headers=headers, json={
        "endTime": end_iso
    })
    assert r.status_code == 200, f"Stop timer failed: {r.status_code} {r.text}"

    print("[Step 8] Update description")
    r = session.put(f"{BASE_URL}/api/v1/records/{record_id}", headers=headers, json={
        "categoryId": category_id,
        "startTime": start_iso,
        "endTime": end_iso,
        "description": "Обновленное описание задачи"
    })
    assert r.status_code == 200, f"Update record failed: {r.status_code} {r.text}"

    print("[Step 9] Verify history audit")
    r = session.get(f"{BASE_URL}/api/v1/records/{record_id}/history", headers=headers)
    assert r.status_code == 200, f"Get history failed: {r.status_code} {r.text}"
    history_items = r.json()["items"]
    assert len(history_items) > 0
    history_id = history_items[0]["id"]

    print("[Step 10] Rollback history")
    r = session.post(f"{BASE_URL}/api/v1/records/{record_id}/history/{history_id}/rollback", headers=headers)
    assert r.status_code == 200, f"Rollback failed: {r.status_code} {r.text}"
    assert r.json()["description"] == "Первичная запись презентации"

    print("[Step 11] Cleanup record")
    r = session.delete(f"{BASE_URL}/api/v1/records/{record_id}", headers=headers)
    assert r.status_code == 204, f"Delete record failed: {r.status_code} {r.text}"

    print("[E2E] Simulation completed successfully!")

if __name__ == "__main__":
    run()
