#!/usr/bin/env python3
import json
import os
import re
import signal
import sys
import uuid
from datetime import datetime, timezone, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs


UNIT_MULTIPLIERS = {
    "seconds": 1,
    "minutes": 60,
    "hours": 3600,
    "days": 86400,
}


def to_seconds(value: int, unit: str) -> int:
    return int(value) * UNIT_MULTIPLIERS.get(unit, 60)


class TimeTrackerMockHandler(BaseHTTPRequestHandler):
    users = {}
    user_settings = {}
    categories = {
        "11111111-1111-1111-1111-111111111111": {
            "id": "11111111-1111-1111-1111-111111111111",
            "userId": None,
            "name": "Работа / Проекты",
            "colorHex": "#3B82F6",
            "isDefault": True
        },
        "22222222-2222-2222-2222-222222222222": {
            "id": "22222222-2222-2222-2222-222222222222",
            "userId": None,
            "name": "Учеба",
            "colorHex": "#10B981",
            "isDefault": True
        }
    }
    records = {}
    history = {}

    def log_message(self, format, *args):
        pass

    def _send_cors_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With')
        self.send_header('Access-Control-Allow-Private-Network', 'true')
        self.send_header('Connection', 'close')

    def do_OPTIONS(self):
        self.send_response(204)
        self._send_cors_headers()
        self.end_headers()

    def _send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self._send_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))

    def _send_error(self, status_code, code, message, details=None):
        payload = {
            "code": code,
            "message": message,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        if details:
            payload["details"] = details
        self._send_json(status_code, payload)

    def _read_body(self):
        content_length = int(self.headers.get('Content-Length', 0))
        if content_length == 0:
            return {}
        body = self.rfile.read(content_length)
        try:
            return json.loads(body.decode('utf-8'))
        except Exception:
            return None

    def _get_default_settings(self):
        return {
            "autoStopEnabled": True,
            "maxActiveDurationValue": 8,
            "maxActiveDurationUnit": "hours",
            "minDurationEnabled": True,
            "minDurationValue": 1,
            "minDurationUnit": "minutes"
        }

    def _get_user_settings(self, user_id):
        if user_id not in self.user_settings:
            self.user_settings[user_id] = self._get_default_settings()
        return self.user_settings[user_id]

    def _get_auth_user(self):
        auth_header = self.headers.get('Authorization', '')
        if not auth_header.startswith('Bearer '):
            return None
        token = auth_header[7:].strip()
        for u in self.users.values():
            if u.get('token') == token:
                return u
        if token.startswith("eyJ"):
            default_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            if default_id not in self.users:
                self.users[default_id] = {
                    "id": default_id,
                    "login": "alex_dev",
                    "name": "Алексей Смирнов",
                    "role": "USER",
                    "token": token
                }
            return self.users[default_id]
        return None

    def _check_auto_stop(self, user_id):
        settings = self._get_user_settings(user_id)
        if not settings.get("autoStopEnabled", True):
            return
        max_sec = to_seconds(
            settings.get("maxActiveDurationValue", 8),
            settings.get("maxActiveDurationUnit", "hours")
        )
        now_dt = datetime.now(timezone.utc)
        for r in self.records.values():
            if r["userId"] == user_id and r["endTime"] is None:
                start_dt = datetime.fromisoformat(r["startTime"].replace('Z', '+00:00'))
                if (now_dt - start_dt).total_seconds() > max_sec:
                    r["endTime"] = (start_dt + timedelta(seconds=max_sec)).isoformat()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)
        user = self._get_auth_user()

        if path == "/api/v1/health":
            self._send_json(200, {"status": "UP", "service": "timetracker-mock-api"})
            return

        m_settings = re.match(r"^/api/v1/users/([0-9a-fA-F-]+)/settings$", path)
        if m_settings:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            target_user_id = m_settings.group(1)
            if user["role"] != "ADMIN" and user["id"] != target_user_id:
                self._send_error(403, "FORBIDDEN", "Доступ к чужим настройкам запрещен")
                return
            settings = self._get_user_settings(target_user_id)
            self._send_json(200, settings)
            return

        m_user = re.match(r"^/api/v1/users/([0-9a-fA-F-]+)$", path)
        if m_user:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            target_user_id = m_user.group(1)
            if user["role"] != "ADMIN" and user["id"] != target_user_id:
                self._send_error(403, "FORBIDDEN", "Доступ к чужому профилю запрещен")
                return
            target_user = self.users.get(target_user_id)
            if not target_user:
                self._send_error(404, "USER_NOT_FOUND", f"Пользователь '{target_user_id}' не найден")
                return
            self._send_json(200, {
                "id": target_user["id"],
                "login": target_user["login"],
                "name": target_user["name"],
                "role": target_user["role"]
            })
            return

        if path == "/api/v1/categories":
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            user_cats = [
                c for c in self.categories.values()
                if c["isDefault"] or c.get("userId") == user["id"]
            ]
            self._send_json(200, {"items": user_cats})
            return

        m_cat = re.match(r"^/api/v1/categories/([0-9a-fA-F-]+)$", path)
        if m_cat:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            cat_id = m_cat.group(1)
            cat = self.categories.get(cat_id)
            if not cat:
                self._send_error(404, "CATEGORY_NOT_FOUND", f"Категория с ID '{cat_id}' не найдена")
                return
            if not cat["isDefault"] and cat.get("userId") != user["id"] and user["role"] != "ADMIN":
                self._send_error(403, "FORBIDDEN", "Доступ к чужой категории запрещен")
                return
            self._send_json(200, cat)
            return

        if path == "/api/v1/records":
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            self._check_auto_stop(user["id"])
            user_records = [r for r in self.records.values() if r["userId"] == user["id"]]

            status = query.get("status", ["all"])[0]
            if status == "active":
                user_records = [r for r in user_records if r["endTime"] is None]
            elif status == "completed":
                user_records = [r for r in user_records if r["endTime"] is not None]

            cat_id = query.get("categoryId", [None])[0]
            if cat_id:
                user_records = [r for r in user_records if r["categoryId"] == cat_id]

            limit = int(query.get("limit", [20])[0])
            offset = int(query.get("offset", [0])[0])
            paginated = user_records[offset:offset + limit]
            self._send_json(200, {
                "items": paginated,
                "total": len(user_records),
                "limit": limit,
                "offset": offset
            })
            return

        m_rec = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)$", path)
        if m_rec:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            rec_id = m_rec.group(1)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", f"Запись времени '{rec_id}' не найдена")
                return
            self._send_json(200, rec)
            return

        m_hist = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)/history$", path)
        if m_hist:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            rec_id = m_hist.group(1)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", f"Запись времени '{rec_id}' не найдена")
                return
            items = self.history.get(rec_id, [])
            self._send_json(200, {"items": items})
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут GET {path} не поддерживается")

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        body = self._read_body()
        if body is None:
            self._send_error(400, "BAD_REQUEST", "Некорректный JSON в теле запроса")
            return

        if path == "/api/v1/auth/register":
            login = body.get("login")
            name = body.get("name")
            role = body.get("role", "USER")
            if not login or not name:
                self._send_error(400, "VALIDATION_ERROR", "Поля login и name обязательны")
                return
            if login in [u["login"] for u in self.users.values()]:
                self._send_error(409, "USER_ALREADY_EXISTS", f"Пользователь с логином '{login}' уже существует")
                return
            user_id = str(uuid.uuid4())
            token = f"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.{user_id}"
            user_obj = {
                "id": user_id,
                "login": login,
                "name": name,
                "role": role,
                "token": token
            }
            self.users[user_id] = user_obj
            self._send_json(201, {
                "token": token,
                "user": {
                    "id": user_id,
                    "login": login,
                    "name": name,
                    "role": role
                }
            })
            return

        if path == "/api/v1/auth/login":
            login = body.get("login")
            for u in self.users.values():
                if u["login"] == login:
                    self._send_json(200, {
                        "token": u["token"],
                        "user": {
                            "id": u["id"],
                            "login": u["login"],
                            "name": u["name"],
                            "role": u["role"]
                        }
                    })
                    return
            if login == "alex_dev":
                user_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                token = f"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.{user_id}"
                user_obj = {
                    "id": user_id,
                    "login": "alex_dev",
                    "name": "Алексей Смирнов",
                    "role": "USER",
                    "token": token
                }
                self.users[user_id] = user_obj
                self._send_json(200, {
                    "token": token,
                    "user": {
                        "id": user_id,
                        "login": "alex_dev",
                        "name": "Алексей Смирнов",
                        "role": "USER"
                    }
                })
                return
            if login == "admin":
                user_id = "00000000-0000-0000-0000-000000000001"
                token = f"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.{user_id}"
                user_obj = {
                    "id": user_id,
                    "login": "admin",
                    "name": "Администратор",
                    "role": "ADMIN",
                    "token": token
                }
                self.users[user_id] = user_obj
                self._send_json(200, {
                    "token": token,
                    "user": {
                        "id": user_id,
                        "login": "admin",
                        "name": "Администратор",
                        "role": "ADMIN"
                    }
                })
                return
            self._send_error(401, "INVALID_CREDENTIALS", "Неверный логин или пароль")
            return

        user = self._get_auth_user()
        if not user:
            self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
            return

        if path == "/api/v1/categories":
            name = body.get("name")
            color_hex = body.get("colorHex")
            is_default = body.get("isDefault", False)
            if not name or not color_hex:
                self._send_error(400, "VALIDATION_ERROR", "Поля name и colorHex обязательны")
                return
            if not re.match(r"^#[0-9A-Fa-f]{6}$", color_hex):
                self._send_error(400, "VALIDATION_ERROR", "Цвет должен соответствовать формату ^#[0-9A-Fa-f]{6}$")
                return
            if is_default and user["role"] != "ADMIN":
                self._send_error(403, "FORBIDDEN", "Только администратор может создавать базовые категории")
                return
            cat_id = str(uuid.uuid4())
            cat_obj = {
                "id": cat_id,
                "userId": None if is_default else user["id"],
                "name": name,
                "colorHex": color_hex,
                "isDefault": is_default
            }
            self.categories[cat_id] = cat_obj
            self._send_json(201, cat_obj)
            return

        if path == "/api/v1/records":
            cat_id = body.get("categoryId")
            if not cat_id:
                self._send_error(400, "VALIDATION_ERROR", "Поле categoryId обязательно")
                return
            if cat_id not in self.categories:
                self._send_error(404, "CATEGORY_NOT_FOUND", f"Категория '{cat_id}' не найдена")
                return

            start_time = body.get("startTime") or datetime.now(timezone.utc).isoformat()
            end_time = body.get("endTime", None)
            desc = body.get("description", "")

            try:
                dt_start = datetime.fromisoformat(start_time.replace('Z', '+00:00'))
            except Exception:
                self._send_error(400, "BAD_REQUEST", "Некорректный ISO-8601 формат startTime")
                return

            if end_time is not None:
                try:
                    dt_end = datetime.fromisoformat(end_time.replace('Z', '+00:00'))
                except Exception:
                    self._send_error(400, "BAD_REQUEST", "Некорректный ISO-8601 формат endTime")
                    return
                if dt_end <= dt_start:
                    self._send_error(422, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                    return

                settings = self._get_user_settings(user["id"])
                if settings.get("minDurationEnabled", True):
                    min_sec = to_seconds(
                        settings.get("minDurationValue", 1),
                        settings.get("minDurationUnit", "minutes")
                    )
                    if (dt_end - dt_start).total_seconds() < min_sec:
                        self._send_error(422, "DURATION_TOO_SHORT", f"Длительность записи менее минимального порога ({min_sec} сек)")
                        return

            rec_id = str(uuid.uuid4())
            rec_obj = {
                "id": rec_id,
                "userId": user["id"],
                "categoryId": cat_id,
                "startTime": start_time,
                "endTime": end_time,
                "description": desc
            }
            self.records[rec_id] = rec_obj
            self._send_json(201, rec_obj)
            return

        m_rb = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)/history/([0-9a-fA-F-]+)/rollback$", path)
        if m_rb:
            rec_id, ver_id = m_rb.group(1), m_rb.group(2)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", f"Запись '{rec_id}' не найдена")
                return
            hist_list = self.history.get(rec_id, [])
            target_version = None
            for v in hist_list:
                if v["id"] == ver_id:
                    target_version = v
                    break
            if not target_version:
                self._send_error(404, "VERSION_NOT_FOUND", f"Историческая версия '{ver_id}' не найдена")
                return

            # Forward-only rollback: save complete current state before rollback
            current_archive = {
                "id": str(uuid.uuid4()),
                "recordId": rec_id,
                "categoryId": rec["categoryId"],
                "startTime": rec["startTime"],
                "endTime": rec["endTime"],
                "description": rec["description"],
                "modifiedAt": datetime.now(timezone.utc).isoformat()
            }
            hist_list.insert(0, current_archive)

            rec["categoryId"] = target_version["categoryId"]
            rec["startTime"] = target_version["startTime"]
            rec["endTime"] = target_version["endTime"]
            rec["description"] = target_version["description"]
            self._send_json(200, rec)
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут POST {path} не поддерживается")

    def do_PATCH(self):
        parsed = urlparse(self.path)
        path = parsed.path
        body = self._read_body()
        if body is None:
            self._send_error(400, "BAD_REQUEST", "Некорректный JSON")
            return

        user = self._get_auth_user()
        if not user:
            self._send_error(401, "UNAUTHORIZED", "Требуется авторизация")
            return

        m_settings = re.match(r"^/api/v1/users/([0-9a-fA-F-]+)/settings$", path)
        if m_settings:
            target_user_id = m_settings.group(1)
            if user["role"] != "ADMIN" and user["id"] != target_user_id:
                self._send_error(403, "FORBIDDEN", "Доступ к чужим настройкам запрещен")
                return
            settings = self._get_user_settings(target_user_id)
            if "autoStopEnabled" in body:
                settings["autoStopEnabled"] = bool(body["autoStopEnabled"])
            if "maxActiveDurationValue" in body:
                val = int(body["maxActiveDurationValue"])
                if val < 1:
                    self._send_error(400, "VALIDATION_ERROR", "maxActiveDurationValue должно быть >= 1")
                    return
                settings["maxActiveDurationValue"] = val
            if "maxActiveDurationUnit" in body:
                unit = body["maxActiveDurationUnit"]
                if unit not in UNIT_MULTIPLIERS:
                    self._send_error(400, "VALIDATION_ERROR", f"Некорректная единица: {unit}")
                    return
                settings["maxActiveDurationUnit"] = unit
            if "minDurationEnabled" in body:
                settings["minDurationEnabled"] = bool(body["minDurationEnabled"])
            if "minDurationValue" in body:
                val = int(body["minDurationValue"])
                if val < 1:
                    self._send_error(400, "VALIDATION_ERROR", "minDurationValue должно быть >= 1")
                    return
                settings["minDurationValue"] = val
            if "minDurationUnit" in body:
                unit = body["minDurationUnit"]
                if unit not in UNIT_MULTIPLIERS:
                    self._send_error(400, "VALIDATION_ERROR", f"Некорректная единица: {unit}")
                    return
                settings["minDurationUnit"] = unit
            self._send_json(200, settings)
            return

        m_cat = re.match(r"^/api/v1/categories/([0-9a-fA-F-]+)$", path)
        if m_cat:
            cat_id = m_cat.group(1)
            cat = self.categories.get(cat_id)
            if not cat:
                self._send_error(404, "CATEGORY_NOT_FOUND", "Категория не найдена")
                return
            if cat["isDefault"] and user["role"] != "ADMIN":
                self._send_error(403, "FORBIDDEN", "Нельзя редактировать базовую категорию")
                return
            if "name" in body:
                cat["name"] = body["name"]
            if "colorHex" in body:
                if not re.match(r"^#[0-9A-Fa-f]{6}$", body["colorHex"]):
                    self._send_error(400, "VALIDATION_ERROR", "Некорректный HEX-цвет")
                    return
                cat["colorHex"] = body["colorHex"]
            self._send_json(200, cat)
            return

        m_rec = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)$", path)
        if m_rec:
            rec_id = m_rec.group(1)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", "Запись не найдена")
                return

            new_start = body.get("startTime", rec["startTime"])
            new_end = body.get("endTime", rec["endTime"])
            new_cat = body.get("categoryId", rec["categoryId"])
            new_desc = body.get("description", rec["description"])

            if new_cat not in self.categories:
                self._send_error(404, "CATEGORY_NOT_FOUND", "Указанная категория не найдена")
                return

            try:
                dt_start = datetime.fromisoformat(new_start.replace('Z', '+00:00'))
            except Exception:
                self._send_error(400, "BAD_REQUEST", "Некорректный формат startTime")
                return

            if new_end is not None:
                try:
                    dt_end = datetime.fromisoformat(new_end.replace('Z', '+00:00'))
                except Exception:
                    self._send_error(400, "BAD_REQUEST", "Некорректный формат endTime")
                    return
                if dt_end <= dt_start:
                    self._send_error(422, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                    return

                settings = self._get_user_settings(rec["userId"])
                if settings.get("minDurationEnabled", True):
                    min_sec = to_seconds(
                        settings.get("minDurationValue", 1),
                        settings.get("minDurationUnit", "minutes")
                    )
                    if (dt_end - dt_start).total_seconds() < min_sec:
                        self._send_error(422, "DURATION_TOO_SHORT", f"Длительность менее минимального порога ({min_sec} сек)")
                        return

            # Check if any attributes modified -> create snapshot
            has_changes = (
                new_start != rec["startTime"] or
                new_end != rec["endTime"] or
                new_cat != rec["categoryId"] or
                new_desc != rec["description"]
            )
            if has_changes:
                snapshot = {
                    "id": str(uuid.uuid4()),
                    "recordId": rec_id,
                    "categoryId": rec["categoryId"],
                    "startTime": rec["startTime"],
                    "endTime": rec["endTime"],
                    "description": rec["description"],
                    "modifiedAt": datetime.now(timezone.utc).isoformat()
                }
                if rec_id not in self.history:
                    self.history[rec_id] = []
                self.history[rec_id].insert(0, snapshot)

            rec["startTime"] = new_start
            rec["endTime"] = new_end
            rec["categoryId"] = new_cat
            rec["description"] = new_desc
            self._send_json(200, rec)
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут PATCH {path} не поддерживается")

    def do_PUT(self):
        parsed = urlparse(self.path)
        path = parsed.path
        body = self._read_body()
        if body is None:
            self._send_error(400, "BAD_REQUEST", "Некорректный JSON")
            return

        user = self._get_auth_user()
        if not user:
            self._send_error(401, "UNAUTHORIZED", "Требуется авторизация")
            return

        m_cat = re.match(r"^/api/v1/categories/([0-9a-fA-F-]+)$", path)
        if m_cat:
            cat_id = m_cat.group(1)
            cat = self.categories.get(cat_id)
            if not cat:
                self._send_error(404, "CATEGORY_NOT_FOUND", "Категория не найдена")
                return
            if cat["isDefault"] and user["role"] != "ADMIN":
                self._send_error(403, "FORBIDDEN", "Нельзя модифицировать базовую категорию")
                return
            cat["name"] = body.get("name", cat["name"])
            cat["colorHex"] = body.get("colorHex", cat["colorHex"])
            self._send_json(200, cat)
            return

        m_rec = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)$", path)
        if m_rec:
            rec_id = m_rec.group(1)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", "Запись не найдена")
                return

            new_cat = body.get("categoryId")
            new_start = body.get("startTime")
            new_end = body.get("endTime")
            new_desc = body.get("description")
            if not new_cat or not new_start or new_desc is None:
                self._send_error(400, "VALIDATION_ERROR", "Поля categoryId, startTime и description обязательны для PUT")
                return

            if new_cat not in self.categories:
                self._send_error(404, "CATEGORY_NOT_FOUND", "Категория не найдена")
                return

            try:
                dt_start = datetime.fromisoformat(new_start.replace('Z', '+00:00'))
            except Exception:
                self._send_error(400, "BAD_REQUEST", "Некорректный формат startTime")
                return

            if new_end is not None:
                try:
                    dt_end = datetime.fromisoformat(new_end.replace('Z', '+00:00'))
                except Exception:
                    self._send_error(400, "BAD_REQUEST", "Некорректный формат endTime")
                    return
                if dt_end <= dt_start:
                    self._send_error(422, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                    return
                settings = self._get_user_settings(rec["userId"])
                if settings.get("minDurationEnabled", True):
                    min_sec = to_seconds(
                        settings.get("minDurationValue", 1),
                        settings.get("minDurationUnit", "minutes")
                    )
                    if (dt_end - dt_start).total_seconds() < min_sec:
                        self._send_error(422, "DURATION_TOO_SHORT", f"Длительность менее минимального порога ({min_sec} сек)")
                        return

            snapshot = {
                "id": str(uuid.uuid4()),
                "recordId": rec_id,
                "categoryId": rec["categoryId"],
                "startTime": rec["startTime"],
                "endTime": rec["endTime"],
                "description": rec["description"],
                "modifiedAt": datetime.now(timezone.utc).isoformat()
            }
            if rec_id not in self.history:
                self.history[rec_id] = []
            self.history[rec_id].insert(0, snapshot)

            rec["categoryId"] = new_cat
            rec["startTime"] = new_start
            rec["endTime"] = new_end
            rec["description"] = new_desc
            self._send_json(200, rec)
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут PUT {path} не поддерживается")

    def do_DELETE(self):
        parsed = urlparse(self.path)
        path = parsed.path
        user = self._get_auth_user()
        if not user:
            self._send_error(401, "UNAUTHORIZED", "Требуется авторизация")
            return

        m_cat = re.match(r"^/api/v1/categories/([0-9a-fA-F-]+)$", path)
        if m_cat:
            cat_id = m_cat.group(1)
            cat = self.categories.get(cat_id)
            if not cat:
                self._send_error(404, "CATEGORY_NOT_FOUND", "Категория не найдена")
                return
            if cat["isDefault"] and user["role"] != "ADMIN":
                self._send_error(403, "FORBIDDEN", "Нельзя удалить базовую категорию")
                return
            del self.categories[cat_id]
            self.send_response(204)
            self._send_cors_headers()
            self.end_headers()
            return

        m_rec = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)$", path)
        if m_rec:
            rec_id = m_rec.group(1)
            rec = self.records.get(rec_id)
            if not rec or (rec["userId"] != user["id"] and user["role"] != "ADMIN"):
                self._send_error(404, "RECORD_NOT_FOUND", "Запись не найдена")
                return
            del self.records[rec_id]
            if rec_id in self.history:
                del self.history[rec_id]
            self.send_response(204)
            self._send_cors_headers()
            self.end_headers()
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут DELETE {path} не поддерживается")


def run_mock_server(port=4010):
    server_address = ('127.0.0.1', port)
    HTTPServer.allow_reuse_address = True
    httpd = HTTPServer(server_address, TimeTrackerMockHandler)
    print(f"TimeTracker OpenAPI Mock Server listening on http://127.0.0.1:{port}")

    def shutdown_handler(signum, frame):
        print("\nStopping mock server.")
        try:
            httpd.server_close()
        except Exception:
            pass
        os._exit(0)

    signal.signal(signal.SIGINT, shutdown_handler)
    signal.signal(signal.SIGTERM, shutdown_handler)

    try:
        httpd.serve_forever()
    except (KeyboardInterrupt, SystemExit):
        shutdown_handler(signal.SIGINT, None)


if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4010
    run_mock_server(port)
