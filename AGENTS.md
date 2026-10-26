# AGENTS.md — Контекст и руководство для ИИ-агентов (TimeTracker Web 2026)

Данный документ является единым источником контекста, правил и инструкций для кодирующих ИИ-агентов в рамках выполнения лабораторных работ по курсу «Web-технологии» (ИИ-трек, 2026).

---

## 1. Цель проекта и роль агента

- **Продукт**: TimeTracker — модульная распределенная веб-система для персонального хронометража, категоризации и аналитики продуктивности пользователей.
- **Цель ИИ-трека**: Разработка web-приложения на базе архитектурных наработок курса ППО, включая REST API (OpenAPI 3.1), развертывание с Nginx и Docker Compose, и клиентское SPA (TypeScript, React, Vite).
- **Роль агента**: Генерация и доработка проектных артефактов (требования, схемы, контракты, код, конфигурации, тесты) в строгом соответствии с критериями приемки студента.
- **Рабочий цикл**:
  $$\text{Входные данные и критерии} \longrightarrow \text{План} \longrightarrow \text{Генерация/Правка} \longrightarrow \text{Запуск проверок} \longrightarrow \text{Ревью} \longrightarrow \text{Исправление}$$
- **Фиксация решений**: По каждой лабораторной работе ведется журнал в [docs/AI_REVIEW.md](docs/AI_REVIEW.md).

---

## 2. Глоссарий предметной области

| Термин | Описание |
| :--- | :--- |
| **User (Пользователь)** | Актор системы. Профиль содержит только идентификационные данные (`id`, `login`, `name`, `role: USER / ADMIN`). |
| **UserSettings (Настройки пользователя)** | Изолированный ресурс предпочтений (`/api/v1/users/{userId}/settings`): единицы измерения (`seconds`, `minutes`, `hours`, `days`), лимит авто-остановки (`maxActiveDuration`, `autoStopEnabled`) и минимальный порог активности (`minDuration`, `minDurationEnabled`). |
| **Category (Категория)** | Классификатор деятельности (название, цвет в формате HEX). Бывает системной (базовой, `isDefault = true`, видна всем) и пользовательской (`userId != null`). |
| **TimeRecord (Запись времени)** | Интервал активности. Модель открытых интервалов: `endTime == null` обозначает текущую активную задачу. Поддерживается параллельный трекинг задач и пересечение интервалов. |
| **RecordHistoryEntry (Снимок истории записи)** | Неизменяемый снимок всех ключевых параметров записи (`startTime`, `endTime`, `categoryId`, `description`, `modifiedAt`) для аудита и forward-only отката. |
| **Collection Envelope (Обертка коллекций)** | Объектная упаковка всех списочных ответов: массивы возвращаются внутри словаря в поле `items: [...]` во избежание уязвимостей и для расширяемости. |
| **Clean Architecture** | Разделение системы на слои: Domain (чистая логика), Data (хранилище), Presentation / Web API (интерфейсы). |

---

## 3. Границы системы и ограничения

### 3.1. Внутренний контур (Система)
- **Web Backend**: REST API сервис (`:web:backend`), реализующий Use Cases предметной области и отдающий Swagger UI.
- **Web Frontend**: Клиентское SPA-приложение (TypeScript + React + Vite).
- **База данных**: Реляционная СУБД PostgreSQL (слой доступа `:web:data` на JetBrains Exposed).
- **Инфраструктурный слой**: Nginx (Reverse Proxy, раздача статики), Docker Compose.

### 3.2. Внешний контур
- Браузеры пользователей (десктоп, мобильные).
- Внешние потребители публичного REST API.

### 3.3. Критические ограничения для агентов
1. **CORE PROGRAM ONLY (NO BONUSES)**: Проект реализуется строго в рамках обязательной базовой программы лабораторных работ (WebLab#1–#8). Никакие факультативные доп. задания и расширения (Telegram Web App, GraphQL, Canvas, мобильные обертки Electron/Capacitor и т.д.) не реализуются и исключены из скоупа.
2. **NO SECRETS**: Никаких реальных паролей, токенов, приватных ключей и персональных данных в коде, конфигурациях и запросах к ИИ. Использовать переменные окружения и шаблоны `.env.example`.
3. **NO INLINE COMMENTS**: В программном коде комментарии любого вида строго запрещены (правило проекта: самодокументируемый код с говорящими именами). В конфигурационных файлах (docker-compose, nginx.conf) и markdown-документации пояснения разрешены.
4. **CONTRACT FIRST**: OpenAPI-спецификация является единым источником истины. Изменение логики или моделей данных начинается с обновления OpenAPI контракта в YAML.
5. **DOMAIN INTEGRITY**: Модуль `:domain` должен оставаться чистым Kotlin-модулем без зависимостей от внешних фреймворков и БД.

---

## 4. Ссылки на требования, контракты и ADR

- **Требования к WebLab#1**: [README.md (WebLab#1)](README.md)
- **Спецификация REST API и контракт (WebLab#2)**: [web/api/README.md](web/api/README.md) и [web/api/openapi.yaml](web/api/openapi.yaml)
- **Анализ исходного проекта ППО**: [docs/AI_ANALYSIS.md](docs/AI_ANALYSIS.md)
- **Архитектурные решения (ADR)**:
  - [ADR-001: Выбор стека веб-бэкенда и интеграция с ядром domain](docs/adr/ADR-001-backend-stack.md)
  - [ADR-002: Выбор архитектуры хранения данных и СУБД](docs/adr/ADR-002-database.md)
  - [ADR-003: Выбор клиентского стека для Web SPA](docs/adr/ADR-003-frontend-stack.md)
- **Журнал ревью и решений ИИ**: [docs/AI_REVIEW.md](docs/AI_REVIEW.md)
- **Графические и архитектурные схемы**: каталог [docs/](docs/)

---

## 5. Структура репозитория

```
IU7_7sem_web/
├── AGENTS.md                  # Данное руководство для агентов
├── README.md                  # Проектные материалы WebLab#1
├── gradle/ + gradlew          # Корневая система сборки Gradle
├── build.gradle.kts           # Корневой скрипт сборки
├── settings.gradle.kts        # Конфигурация модулей Gradle
├── docs/                      # Исходники диаграмм, ADR и проектные отчеты
│   ├── AI_ANALYSIS.md         # Системный анализ кодовой базы ППО
│   ├── AI_REVIEW.md           # Журнал решений и ревью в ИИ-треке
│   ├── adr/                   # Архитектурные решения (ADR-001..003)
│   ├── wireframes/            # Эскизы экранов веб-приложения
│   └── ...                    # C4, BPMN, ER, DBML схемы
│
├── domain/                    # [Ядро] Чистая бизнес-логика (Kotlin JVM, Clean Architecture)
│   └── src/main/java/com/ppo/domain/
│       ├── model/             # Сущности (User, Category, TimeRecord)
│       ├── repository/        # Интерфейсы репозиториев
│       └── usecase/           # Сценарии бизнес-логики
│
├── web/                       # [Web-трек] Веб-компоненты
│   ├── api/                   # Спецификация OpenAPI 3.1, мок-сервер, тесты (WebLab#2)
│   ├── data/                  # Слой хранения PostgreSQL и сущности БД (WebLab#3, TestingLab#1)
│   ├── backend/               # REST API сервис на Ktor (WebLab#3)
│   └── frontend/              # SPA приложение React+Vite (WebLab#8)
│
└── deploy/                    # Конфигурации Docker Compose, Nginx, мониторинга (WebLab#4)
```

---

## 6. Команды сборки, запуска и проверок

| Лабораторная | Команда | Назначение |
| :--- | :--- | :--- |
| **TestingLab#1** | `./gradlew :domain:test :web:data:test` | Запуск полного набора unit-тестов проекта (domain + web:data) |
| **TestingLab#1** | `./gradlew jacocoRootReport` | Генерация объединенного отчета покрытия JaCoCo для всего проекта (`build/reports/jacoco/root/html`) |
| **TestingLab#1** | `python3 scripts/coverage_report.py` | Сводный отчет покрытия кода JaCoCo (Lines & Branches) |
| **TestingLab#1** | `./gradlew allureReport` | Генерация интерактивного HTML-отчета Allure (`build/reports/allure-report`) |
| **TestingLab#1** | `./gradlew allureServe` | Локальный просмотр Allure-отчета в веб-сервере |
| **WebLab#2** | `docker run --rm -v $(pwd):/spec -w /spec redocly/cli lint web/api/openapi.yaml` | Индустриальный линтинг схемы (Redocly CLI) |
| **WebLab#2** | `openapi-spec-validator web/api/openapi.yaml` | Валидация спецификации (OpenAPI Spec Validator) |
| **WebLab#2** | `python3 web/api/scripts/mock_server.py 4010` | Запуск Mock-сервера по контракту |
| **WebLab#2** | `python3 web/api/scripts/test_scenario.py` | Сквозной тест пользовательских сценариев (15 шагов) |
| **WebLab#3** | `./gradlew :web:backend:test` | Запуск модульных и контрактных тестов сервиса |
| **WebLab#3** | `./gradlew :web:backend:jar && docker compose up --build -d` | Сборка fat-jar и запуск PostgreSQL + Backend в Docker Compose |
| **WebLab#3** | `python3 web/api/scripts/verify_live_api.py http://localhost:8080` | Сквозная верификация живого API (14 шагов) |
| **WebLab#3** | `java -jar dist/tech_ui.jar --api categories` | Проверка работы консольного клиента через REST API |
| **WebLab#4** | `docker compose -f deploy/docker-compose.yml up --build -d` | Развертывание Nginx + Web App + DB |
| **WebLab#4** | `./deploy/scripts/healthcheck.sh` | HTTP-проверки маршрутизации и статуса |
| **WebLab#5** | `k6 run deploy/tests/load_test.js` | Нагрузочное тестирование балансировки |
| **WebLab#8** | `cd web/frontend && npm test` | Запуск тестов компонентов frontend (Vitest) |
| **WebLab#8** | `cd web/frontend && npm run build` | Сборка frontend SPA (Vite) |

---

## 7. Границы изменений (Scope Control)

1. При выполнении **WebLab#1**:
   - Разрешено: редактирование `README.md`, `AGENTS.md`, `AI_REVIEW.md`, файлов в `docs/`.
   - Запрещено: модификация рабочего кода модулей `:domain`, `:data`, `:tech_ui`, ломающая легаси-функционал.
2. При выполнении **WebLab#2**:
   - Разрешено: `web/api/openapi.yaml`, `web/api/README.md`, `web/api/scripts/*`, `AI_REVIEW.md`, `AGENTS.md`.
   - Контракт проектируется в файле `web/api/openapi.yaml` на основе согласованных сущностей и сценариев из `README.md`.
   - Запрещено: реализация факультативных допов (GraphQL и т.д.), коммиты с невалидными примерами схемы.
3. При выполнении **WebLab#3**:
   - Реализован REST API сервис в модуле `:web:backend` на Kotlin/Ktor строго по контракту `web/api/openapi.yaml`.
   - Слой данных вынесен в отдельный подпроект `:web:data` (PostgreSQL, HikariCP, репозитории).
   - Реализован Бонус 1 (JWT аутентификация и авторизация по ролям USER/ADMIN).
   - Реализован Бонус 2 (перенаправление существующих клиентских интерфейсов `:tech_ui` и `:app` на REST API через `RestApiClient`).
   - Контейнеризованы сервис и СУБД в Docker Compose.
4. При переходе к **WebLab#4**:
   - Настройка Nginx reverse proxy, маршрутизация запросов к backend, отдача документации Swagger UI и статики.

