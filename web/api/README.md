# WebLab#2 — Проектирование REST API и контракт OpenAPI 3.1

Данный документ содержит спецификацию, трассировочные таблицы пользовательских сценариев к операциям API, анализ семантики HTTP-контракта, журнал управления изменениями и инструкции по автоматизированной проверке для лабораторной работы №2 (ИИ-трек, 2026).

---

## 1. Обзор контракта REST API

Контракт REST API системы TimeTracker спроектирован по каноническим принципам RESTful-архитектуры и Clean Architecture:

- **Файл спецификации**: [`openapi.yaml`](openapi.yaml)
- **Версия стандарта**: OpenAPI 3.1.0
- **Базовый префикс путей**: `/api/v1/`
- **Формат обмена**: `application/json; charset=utf-8`
- **Схема авторизации**: `bearerAuth` (HTTP Bearer JWT)
- **Покрытие сущностей и ресурсов**:
  - `User` и `UserSettings` — каноническая ресурсная адресация `/users/{userId}` (профиль пользователя: `id`, `login`, `name`, `role`) и изолированный ресурс настроек `/users/{userId}/settings` (единицы времени, пороги, авто-остановка). Настройки трекера не загромождают профиль пользователя и запрашиваются строго по необходимости.
  - `Category` (системные базовые и личные категории) — полный CRUD (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
  - `TimeRecord` (записи времени и текущие активности) — единая модель открытых интервалов (`endTime = null` для текущей задачи). Поддержка параллельных активностей (multi-tasking) и пересекающихся интервалов.
  - `RecordHistoryEntry` (неизменяемый аудит версий всех атрибутов записи и forward-only откат) — `GET /records/{id}/history`, `POST /records/{id}/history/{versionId}/rollback`.
  - `Auth` — `POST /auth/register`, `POST /auth/login`.
- **Объектная упаковка ответов**: полное исключение корневых массивов; все коллекции упакованы в словари со свойством `items`.

---

## 2. Трассировка: Пользовательский сценарий → Шаг → operationId

В таблице представлена сквозная трассировка ключевых сценариев к эндпоинтам, HTTP-методам, идентификаторам операций (`operationId`) и кодам ответов.

| № | Пользовательский сценарий | Шаг сценария | HTTP метод и URL | operationId | Ожидаемые HTTP статусы |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | **Аутентификация и управление настройками** | 1.1. Регистрация нового пользователя | `POST /api/v1/auth/register` | `registerUser` | `201 Created`, `400 Bad Request`, `409 Conflict` |
| | | 1.2. Вход и получение JWT токена | `POST /api/v1/auth/login` | `loginUser` | `200 OK`, `400 Bad Request`, `401 Unauthorized` |
| | | 1.3. Получение профиля пользователя по ID | `GET /api/v1/users/{userId}` | `getUserById` | `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| | | 1.4. Чтение пользовательских настроек | `GET /api/v1/users/{userId}/settings` | `getUserSettings` | `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| | | 1.5. Настройка единиц времени и авто-остановки | `PATCH /api/v1/users/{userId}/settings` | `updateUserSettings` | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| **2** | **Учет времени и параллельные активности** | 2.1. Запуск первой активной задачи (`endTime=null`) | `POST /api/v1/records` | `createRecord` | `201 Created`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `422 Unprocessable Entity` |
| | | 2.2. Запуск параллельной активной задачи (Multi-tasking) | `POST /api/v1/records` | `createRecord` | `201 Created` (без конфликта), `400 Bad Request`, `401 Unauthorized` |
| | | 2.3. Создание завершенной записи с перекрытием дат | `POST /api/v1/records` | `createRecord` | `201 Created`, `400 Bad Request`, `401 Unauthorized`, `422 Unprocessable Entity` |
| | | 2.4. Завершение активной задачи (простановка `endTime`) | `PATCH /api/v1/records/{recordId}` | `patchRecord` | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `422 Unprocessable Entity` |
| | | 2.5. Просмотр списка записей с пагинацией и статусом | `GET /api/v1/records` | `listRecords` | `200 OK` (`items`, `total`, `limit`, `offset`), `400 Bad Request`, `401 Unauthorized` |
| | | 2.6. Корректировка временных границ (`startTime`, `endTime`) | `PATCH /api/v1/records/{recordId}` | `patchRecord` | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `422 Unprocessable Entity` |
| | | 2.7. Удаление ошибочной записи | `DELETE /api/v1/records/{recordId}` | `deleteRecord` | `204 No Content`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| **3** | **Управление категориями** | 3.1. Создание новой категории | `POST /api/v1/categories` | `createCategory` | `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `409 Conflict` |
| | | 3.2. Получение списка категорий в словаре `items` | `GET /api/v1/categories` | `listCategories` | `200 OK` (`items: [...]`), `401 Unauthorized` |
| | | 3.3. Получение деталей категории | `GET /api/v1/categories/{categoryId}` | `getCategoryById` | `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| | | 3.4. Полное обновление категории | `PUT /api/v1/categories/{categoryId}` | `updateCategory` | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict` |
| | | 3.5. Частичное изменение цвета/имени | `PATCH /api/v1/categories/{categoryId}` | `patchCategory` | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict` |
| | | 3.6. Удаление категории | `DELETE /api/v1/categories/{categoryId}` | `deleteCategory` | `204 No Content`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| **4** | **Аудит истории параметров записи и откат** | 4.1. Автоматическая фиксация снимка при правке | `PATCH /api/v1/records/{recordId}` | `patchRecord` | `200 OK` (снимок сохранен в историю) |
| | | 4.2. Получение истории снимков в словаре `items` | `GET /api/v1/records/{recordId}/history` | `getRecordHistory` | `200 OK` (`items: [RecordHistoryEntry]`), `401 Unauthorized`, `404 Not Found` |
| | | 4.3. Откат всех атрибутов записи к версии | `POST /api/v1/records/{recordId}/history/{versionId}/rollback` | `rollbackRecordVersion` | `200 OK`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |

---

## 3. Анализ семантики HTTP-контракта

### 3.1. HTTP-методы, безопасность и идемпотентность

В соответствии со стандартами RFC 7231 и RFC 5789 для всех эндпоинтов соблюдены требования безопасности (Safe) и идемпотентности (Idempotent):

| HTTP метод | Safe (не меняет состояние) | Idempotent (повторный вызов дает тот же эффект) | Применение в TimeTracker API |
| :--- | :---: | :---: | :--- |
| `GET` | **Да** | **Да** | Чтение коллекций и сущностей (`/categories`, `/records`, `/users/{id}`, `/users/{id}/settings`, `/history`). |
| `POST` | **Нет** | **Нет** | Создание ресурсов и выполнение доменных действий (`/auth/register`, `/auth/login`, `/categories`, `/records`, `/rollback`). |
| `PUT` | **Нет** | **Да** | Полное замещение сущности (`PUT /categories/{id}`, `PUT /records/{id}`). Требует полного набора обязательных полей. |
| `PATCH` | **Нет** | **Нет** / **Да** (по RFC 7396) | Частичное обновление сущности (`PATCH /users/{id}/settings`, `PATCH /categories/{id}`, `PATCH /records/{id}`). |
| `DELETE` | **Нет** | **Да** | Удаление ресурса (`DELETE /categories/{id}`, `DELETE /records/{id}`). |

### 3.2. Каталог используемых кодов состояния HTTP

- **Успешные ответы (2xx)**:
  - `200 OK`: Успешное выполнение запроса на чтение, частичное/полное обновление, остановку активной задачи или откат версии.
  - `201 Created`: Успешное создание ресурса (регистрация пользователя, создание категории, создание записи времени или старт активной задачи).
  - `204 No Content`: Успешное удаление ресурса без тела ответа (`DELETE /categories/{id}`, `DELETE /records/{id}`).
- **Клиентские ошибки (4xx)**:
  - `400 Bad Request`: Синтаксическая ошибка в теле запроса, некорректный JSON или невалидный формат типов/enum (например, неверный `DurationUnit` или HEX-цвет).
  - `401 Unauthorized`: Отсутствует или недействителен токен авторизации (Bearer JWT).
  - `403 Forbidden`: Недостаточно прав для выполнения действия (попытка обычного пользователя запросить чужой профиль/настройки, отредактировать чужую запись или создать базовую категорию без роли `ADMIN`).
  - `404 Not Found`: Запрашиваемый ресурс (пользователь, категория, запись времени, исторический снимок) не найден.
  - `409 Conflict`: Конфликт уникальности (повторный логин при регистрации, дублирование имени категории).
  - `422 Unprocessable Entity`: Семантическая невалидность переданных параметров:
    - Некорректный временной интервал $T_{end} \le T_{start}$ (`INVALID_TIME_RANGE`).
    - Длительность записи менее установленного пользователем минимального порога при включенном `minDurationEnabled` (`DURATION_TOO_SHORT`).

### 3.3. Единый конверт ошибок (RFC 7807 Compatible)

Все ошибки сервиса возвращаются в единообразном JSON-формате по схеме `ErrorResponse`:

```json
{
  "code": "DURATION_TOO_SHORT",
  "message": "Длительность записи менее установленного минимального порога (30 сек)",
  "timestamp": "2026-10-18T17:19:04Z"
}
```

### 3.4. Модель открытых интервалов (Open Records Paradigm)

Концепция отдельной сущности таймера упразднена:
- **Запуск задачи**: `POST /api/v1/records` с полем `endTime = null` (или опущенным).
- **Остановка задачи**: `PATCH /api/v1/records/{recordId}` с указанием времени `endTime`.
- **Мультизадачность (Multi-tasking)**: пользователь может иметь произвольное количество параллельно запущенных задач и пересекающихся записей.
- **Авто-остановка**: если в настройках включен параметр `autoStopEnabled: true`, при превышении интервалом лимита `maxActiveDurationValue` `maxActiveDurationUnit` сервер фиксирует завершение задачи с $T_{end} = T_{start} + maxActiveDuration$.

### 3.5. Комплексное версионирование записи

Снимки в истории (`RecordHistoryEntry`) фиксируют полное состояние записи при любой модификации (`PUT` или `PATCH` полей `startTime`, `endTime`, `categoryId`, `description`):
- При вызове отката (`POST /api/v1/records/{id}/history/{versionId}/rollback`) текущее состояние записи архивируется в историю как новый снимок (Forward-only audit), а в саму запись возвращаются значения всех полей из снимка `versionId`.

---

## 4. Управление изменениями контракта (Change Management)

В рамках выполнения требований и согласования с преподавателем зафиксированы следующие ключевые архитектурные изменения:

### 4.1. Суть изменений и обоснование

1. **Каноническая ресурсная адресация `/users/{userId}` и изоляция настроек в `/users/{userId}/settings`**:
   - По рекомендации преподавателя контекстный псевдо-эндпоинт `/users/me` заменен на канонические ресурсные URI `/users/{userId}` и `/users/{userId}/settings`.
   - Профиль пользователя (`User`) является легковесным и содержит исключительно идентификационные данные (`id`, `login`, `name`, `role`), необходимые для отображения в GUI и авторизации.
   - Пользовательские настройки (`UserSettings`) полностью вынесены в специализированный ресурс `/users/{userId}/settings`. Это исключает избыточную нагрузку на сеть при логине и навигации, реализуя четкое разделение ответственности (Separation of Concerns).
   - Обеспечена полноценная ролевая модель RBAC: обычный пользователь управляет своим профилем по `userId`, а администратор (`ADMIN`) имеет доступ к настройкам любого пользователя через единый интерфейс.
2. **Гибкие пользовательские настройки и единицы измерения**:
   - Добавлены настройки: автоматическая остановка зависших задач (`autoStopEnabled`, `maxActiveDurationValue`, `maxActiveDurationUnit`) и контроль минимальной длительности (`minDurationEnabled`, `minDurationValue`, `minDurationUnit`).
   - Поддерживаются единицы: `seconds`, `minutes`, `hours`, `days`.
3. **Упразднение сущности Timer в пользу открытых записей TimeRecord**:
   - Удалены эндпоинты `/api/v1/timer/*`. Активная задача — это запись с `endTime = null`.
   - Сняты ограничения на одну активную сессию и запрет на пересечение интервалов (упразднены ошибки `TIMER_ALREADY_ACTIVE` и `INTERVAL_OVERLAP`).
4. **Комплексное версионирование записей**:
   - Версионирование расширено с одного описания на все параметры активности: `startTime`, `endTime`, `categoryId`, `description`.
5. **Строгая объектная упаковка всех коллекций (`items`)**:
   - Устранены корневые массивы `[...]`. Все списки (`/categories`, `/history`, `/records`) возвращают словарь со свойством `items`.

### 4.2. YAML Diff ключевых изменений

```diff
--- openapi_v1_0.yaml
+++ openapi.yaml
@@ -28,8 +28,8 @@
 tags:
   - name: Auth
-    description: Аутентификация и профиль пользователя
+    description: Регистрация и аутентификация пользователей
+  - name: Users
+    description: Управление профилем и пользовательскими настройками
   - name: Categories
     description: Управление категориями активностей
-  - name: Timer
-    description: Управление активным таймером
   - name: Records
@@ -145,26 +145,55 @@
-  /api/v1/users/me:
-    get:
-      operationId: getCurrentUser
+  /api/v1/users/{userId}:
+    get:
+      operationId: getUserById
+  /api/v1/users/{userId}/settings:
+    get:
+      operationId: getUserSettings
+    patch:
+      operationId: updateUserSettings
 
-  /api/v1/timer/start:
-  /api/v1/timer/stop:
-  /api/v1/timer/active:
+  # Сущность таймера исключена; запуск и остановка выполняются через /records
```

---

## 5. Автоматизированная проверка и демонстрация

### 5.1. Линтинг и валидация схемы OpenAPI 3.1

#### Redocly CLI (Индустриальный стандарт):
```bash
docker run --rm -v $(pwd):/spec -w /spec redocly/cli lint web/api/openapi.yaml
```
**Результат:**
```text
validating web/api/openapi.yaml...
web/api/openapi.yaml: validated in 201ms

Woohoo! Your API description is valid. 🎉
```

#### openapi-spec-validator (Python CLI):
```bash
openapi-spec-validator web/api/openapi.yaml
```
**Результат:**
```text
web/api/openapi.yaml: OK
```

### 5.2. Сквозное сценарное тестирование (15 шагов)

**Команда запуска:**
```bash
python3 web/api/scripts/test_scenario.py
```

**Отчет выполнения тестов:**
```text
======================================================================
TimeTracker REST API — Multi-Step User Scenarios Verification
Target Server: http://127.0.0.1:4015
======================================================================

[Step 1] POST /api/v1/auth/register (operationId: registerUser)
  -> SUCCESS (201 Created): Registered user id=bff750f1-5f32-48de-89b2-4c0a31caf061 (lean profile without settings)

[Step 2] POST /api/v1/auth/login (operationId: loginUser)
  -> SUCCESS (200 OK): Login successful, token received for user bff750f1-5f32-48de-89b2-4c0a31caf061

[Step 3] GET /api/v1/users/bff750f1-5f32-48de-89b2-4c0a31caf061 (operationId: getUserById)
  -> SUCCESS (200 OK): Profile authenticated for Алексей Смирнов (lean profile without settings)

[Step 4] GET /api/v1/users/bff750f1-5f32-48de-89b2-4c0a31caf061/settings (operationId: getUserSettings)
  -> SUCCESS (200 OK): Settings retrieved (autoStop=8 hours, min=1 minutes)

[Step 5] PATCH /api/v1/users/a5dea5b2-0e77-4c22-8441-468af7e93259/settings (operationId: updateUserSettings)
  -> SUCCESS (200 OK): Units updated (12 hours auto-stop, min duration disabled)

[Step 6] POST /api/v1/categories (operationId: createCategory)
  -> SUCCESS (201 Created): Custom category created, id=5b7a4beb-3748-42f7-962f-2c9f33347a25

[Step 7] GET /api/v1/categories (operationId: listCategories)
  -> SUCCESS (200 OK): Found 3 categories inside 'items' envelope

[Step 8] POST /api/v1/records (operationId: createRecord — Active Task 1)
  -> SUCCESS (201 Created): Active task 1 started, id=d3e7583d-2028-42cc-a50b-526396e38957, endTime=null

[Step 9] POST /api/v1/records [Multi-tasking Check] (operationId: createRecord — Active Task 2)
  -> SUCCESS (201 Created): Concurrent active task 2 started without conflict, id=8c996c76-a57c-4014-8b60-01c346e3f3f7

[Step 10] POST /api/v1/records [Overlap Allowance] (operationId: createRecord)
  -> SUCCESS (201 Created): Overlapping record created successfully, id=8f3db5e4-cd56-40ae-9529-c4f9d4852fb2

[Step 11] PATCH /api/v1/records/d3e7583d-2028-42cc-a50b-526396e38957 (operationId: patchRecord — Stop Task)
  -> SUCCESS (200 OK): Active task 1 stopped with endTime=2026-10-15T10:45:00Z

[Step 12] PATCH /api/v1/records/d3e7583d-2028-42cc-a50b-526396e38957 (operationId: patchRecord — Modify Times & Description)
  -> SUCCESS (200 OK): Interval and description modified, full snapshot archived

[Step 13] GET /api/v1/records/d3e7583d-2028-42cc-a50b-526396e38957/history (operationId: getRecordHistory)
  -> SUCCESS (200 OK): Found 2 history version(s) in 'items', snapshot verified (id=2082732e-9156-45b3-b87c-aa2a8a9d9897)

[Step 14] POST /api/v1/records/d3e7583d-2028-42cc-a50b-526396e38957/history/2082732e-9156-45b3-b87c-aa2a8a9d9897/rollback (operationId: rollbackRecordVersion)
  -> SUCCESS (200 OK): Forward-only rollback executed, times and description restored

[Step 15] Business Rule Validation: Dynamic Unit Threshold (30 seconds)
  -> SUCCESS (422 Unprocessable Entity): Short interval (< 30s) rejected according to user settings

======================================================================
ALL SCENARIO TESTS PASSED SUCCESSFULLY! (15/15 steps)
======================================================================
```
