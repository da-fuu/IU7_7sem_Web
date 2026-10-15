#!/usr/bin/env python3
import json
import re
import sys
import uuid
from datetime import datetime, timezone
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs


class TimeTrackerMockHandler(BaseHTTPRequestHandler):
    users = {}
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
    active_timer = None
    records = {}
    history = {}

    def log_message(self, format, *args):
        pass

    def _send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
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

    def _get_auth_user(self):
        auth_header = self.headers.get('Authorization', '')
        if not auth_header.startswith('Bearer '):
            return None
        token = auth_header[7:].strip()
        for u in self.users.values():
            if u.get('token') == token:
                return u
        if token.startswith("eyJ"):
            return {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "login": "alex_dev",
                "name": "Алексей Смирнов",
                "role": "USER"
            }
        return None

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        user = self._get_auth_user()

        if path == "/api/v1/health":
            self._send_json(200, {"status": "UP", "service": "timetracker-mock-api"})
            return

        if path == "/api/v1/users/me":
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            self._send_json(200, {
                "id": user["id"],
                "login": user["login"],
                "name": user["name"],
                "role": user["role"]
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
            self._send_json(200, user_cats)
            return

        m = re.match(r"^/api/v1/categories/([0-9a-fA-F-]+)$", path)
        if m:
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            cat_id = m.group(1)
            cat = self.categories.get(cat_id)
            if not cat:
                self._send_error(404, "CATEGORY_NOT_FOUND", f"Категория с ID '{cat_id}' не найдена")
                return
            self._send_json(200, cat)
            return

        if path == "/api/v1/timer/active":
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            if not TimeTrackerMockHandler.active_timer:
                self._send_json(200, None)
                return
            active = TimeTrackerMockHandler.active_timer
            cat = self.categories.get(active["categoryId"], {})
            start_dt = datetime.fromisoformat(active["startTime"].replace('Z', '+00:00'))
            now_dt = datetime.now(timezone.utc)
            elapsed = int((now_dt - start_dt).total_seconds())
            self._send_json(200, {
                "recordId": active["recordId"],
                "categoryId": active["categoryId"],
                "categoryName": cat.get("name", "Работа / Проекты"),
                "colorHex": cat.get("colorHex", "#3B82F6"),
                "startTime": active["startTime"],
                "elapsedSeconds": max(0, elapsed)
            })
            return

        if path == "/api/v1/records":
            if not user:
                self._send_error(401, "UNAUTHORIZED", "Требуется авторизация в системе")
                return
            user_records = [r for r in self.records.values() if r["userId"] == user["id"]]
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
            if not rec or rec["userId"] != user["id"]:
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
            if rec_id not in self.records:
                self._send_error(404, "RECORD_NOT_FOUND", f"Запись времени '{rec_id}' не найдена")
                return
            items = self.history.get(rec_id, [])
            self._send_json(200, items)
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
                self._send_error(400, "VALIDATION_ERROR", "Цвет должен быть в формате ^#[0-9A-Fa-f]{6}$")
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

        if path == "/api/v1/timer/start":
            cat_id = body.get("categoryId")
            if not cat_id or cat_id not in self.categories:
                self._send_error(404, "CATEGORY_NOT_FOUND", f"Категория '{cat_id}' не найдена")
                return
            if TimeTrackerMockHandler.active_timer:
                cat_name = self.categories.get(TimeTrackerMockHandler.active_timer["categoryId"], {}).get("name", "")
                self._send_error(409, "TIMER_ALREADY_ACTIVE", f"У вас уже запущена сессия таймера для категории '{cat_name}'")
                return
            rec_id = str(uuid.uuid4())
            now_iso = datetime.now(timezone.utc).isoformat()
            TimeTrackerMockHandler.active_timer = {
                "recordId": rec_id,
                "categoryId": cat_id,
                "startTime": now_iso
            }
            cat = self.categories[cat_id]
            self._send_json(201, {
                "recordId": rec_id,
                "categoryId": cat_id,
                "categoryName": cat["name"],
                "colorHex": cat["colorHex"],
                "startTime": now_iso,
                "elapsedSeconds": 0
            })
            return

        if path == "/api/v1/timer/stop":
            if not TimeTrackerMockHandler.active_timer:
                self._send_error(404, "NO_ACTIVE_TIMER", "У вас нет запущенного таймера")
                return
            simulate_short = body.get("simulateShort", False)
            if simulate_short:
                TimeTrackerMockHandler.active_timer = None
                self._send_error(422, "SESSION_TOO_SHORT", "Длительность сессии менее 1 минуты (зафиксировано 15 с). Запись отклонена.")
                return
            rec_id = TimeTrackerMockHandler.active_timer["recordId"]
            cat_id = TimeTrackerMockHandler.active_timer["categoryId"]
            start_iso = TimeTrackerMockHandler.active_timer["startTime"]
            end_iso = datetime.now(timezone.utc).isoformat()
            desc = body.get("description", "Завершенная сессия таймера")
            rec_obj = {
                "id": rec_id,
                "userId": user["id"],
                "categoryId": cat_id,
                "startTime": start_iso,
                "endTime": end_iso,
                "description": desc
            }
            self.records[rec_id] = rec_obj
            TimeTrackerMockHandler.active_timer = None
            self._send_json(200, rec_obj)
            return

        if path == "/api/v1/records":
            cat_id = body.get("categoryId")
            start_time = body.get("startTime")
            end_time = body.get("endTime")
            desc = body.get("description", "")
            if not cat_id or not start_time or not end_time or not desc:
                self._send_error(400, "VALIDATION_ERROR", "Все поля обязательны для TimeRecordCreateRequest")
                return
            if cat_id not in self.categories:
                self._send_error(404, "CATEGORY_NOT_FOUND", f"Категория '{cat_id}' не найдена")
                return
            try:
                dt_start = datetime.fromisoformat(start_time.replace('Z', '+00:00'))
                dt_end = datetime.fromisoformat(end_time.replace('Z', '+00:00'))
            except Exception:
                self._send_error(400, "BAD_REQUEST", "Некорректный ISO-8601 формат времени")
                return
            if dt_end <= dt_start:
                self._send_error(422, "INVALID_INTERVAL", "Время окончания должно быть строго позже времени начала")
                return
            if (dt_end - dt_start).total_seconds() < 60:
                self._send_error(422, "DURATION_TOO_SHORT", "Минимальная продолжительность записи составляет 1 минуту")
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
            if not rec or rec["userId"] != user["id"]:
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
            # Forward-only rollback: archive current state to history
            current_archive = {
                "id": str(uuid.uuid4()),
                "recordId": rec_id,
                "description": rec["description"],
                "modifiedAt": datetime.now(timezone.utc).isoformat()
            }
            hist_list.insert(0, current_archive)
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
            if not rec or rec["userId"] != user["id"]:
                self._send_error(404, "RECORD_NOT_FOUND", "Запись не найдена")
                return
            if "description" in body and body["description"] != rec["description"]:
                old_ver = {
                    "id": str(uuid.uuid4()),
                    "recordId": rec_id,
                    "description": rec["description"],
                    "modifiedAt": datetime.now(timezone.utc).isoformat()
                }
                if rec_id not in self.history:
                    self.history[rec_id] = []
                self.history[rec_id].insert(0, old_ver)
                rec["description"] = body["description"]
            if "categoryId" in body:
                if body["categoryId"] not in self.categories:
                    self._send_error(404, "CATEGORY_NOT_FOUND", "Категория не найдена")
                    return
                rec["categoryId"] = body["categoryId"]
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
            self.end_headers()
            return

        m_rec = re.match(r"^/api/v1/records/([0-9a-fA-F-]+)$", path)
        if m_rec:
            rec_id = m_rec.group(1)
            rec = self.records.get(rec_id)
            if not rec or rec["userId"] != user["id"]:
                self._send_error(404, "RECORD_NOT_FOUND", "Запись не найдена")
                return
            del self.records[rec_id]
            if rec_id in self.history:
                del self.history[rec_id]
            self.send_response(204)
            self.end_headers()
            return

        self._send_error(404, "ENDPOINT_NOT_FOUND", f"Маршрут DELETE {path} не поддерживается")


def run_mock_server(port=4010):
    server_address = ('127.0.0.1', port)
    httpd = HTTPServer(server_address, TimeTrackerMockHandler)
    print(f"TimeTracker OpenAPI Mock Server listening on http://127.0.0.1:{port}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping mock server.")
        httpd.server_close()


if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4010
    run_mock_server(port)
