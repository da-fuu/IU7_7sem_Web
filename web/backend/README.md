# TimeTracker REST API Backend (WebLab#3)

Сервис REST API на базе Kotlin и Ktor для системы учета времени и продуктивности **TimeTracker**.
Реализует контракт OpenAPI 3.1.0 (`web/api/openapi.yaml`), чистую архитектуру с отдельным слоем данных `:web:data`, JWT-аутентификацию (Бонус 1) и поддержку перенаправления существующих клиентских интерфейсов TUI и GUI (Бонус 2).

---

## 1. Архитектура и стек технологий

- **Язык**: Kotlin 2.3.x (JVM 17+)
- **HTTP / Web Engine**: [Ktor 3.1.1](https://ktor.io/) (Netty engine)
- **Сериализация**: Jackson (`ktor-serialization-jackson` + `jackson-module-kotlin` + `JavaTimeModule`)
- **Безопасность**: JWT (`com.auth0:java-jwt`), SHA-256 хеширование паролей с солью
- **База данных**: PostgreSQL 16 (контейнеризована в Docker Compose)
- **Пул соединений**: [HikariCP 6.2.1](https://github.com/brettwooldridge/HikariCP)
- **ORM / Слой данных**: [JetBrains Exposed 0.58.0](https://github.com/JetBrains/Exposed) (модули `exposed-core`, `exposed-dao`, `exposed-jdbc`, `exposed-java-time`) в отдельном Gradle-подпроекте `:web:data`. Все операции с базой производятся через типизированные ORM-таблицы, DAO-сущности и транзакции Exposed.
- **Ядро бизнес-логики**: модуль `:domain` (чистый Kotlin без внешних фреймворков)

---

## 2. Структура модулей

```
web/
├── api/
│   ├── openapi.yaml           # Спецификация контракта REST API (OpenAPI 3.1.0)
│   ├── README.md              # Документация контракта и mock-сервера
│   └── scripts/
│       ├── mock_server.py     # Локальный Mock-сервер
│       ├── test_scenario.py   # Тест сквозных пользовательских сценариев
│       └── verify_live_api.py # Автоматический тест живого API сервиса
├── data/                      # Подпроект :web:data (слой хранения Exposed ORM + PostgreSQL)
│   └── src/main/kotlin/com/ppo/web/data/
│       ├── db/
│       │   └── DatabaseFactory.kt # Пул HikariCP и инициализация Exposed ORM
│       ├── orm/
│       │   ├── Tables.kt          # Схемы ORM-таблиц Exposed (UsersTable, etc.)
│       │   └── Entities.kt        # DAO-сущности Exposed и доменные мапперы
│       └── repository/            # Реализации интерфейсов :domain на Exposed DAO
└── backend/                   # Подпроект :web:backend (REST API сервис)
    ├── Dockerfile             # Контейнеризация сервиса
    └── src/
        ├── main/kotlin/com/ppo/web/
        │   ├── Application.kt      # Точка входа Ktor Netty сервера
        │   ├── dto/                # DTO запросов и ответов (*Dto)
        │   ├── security/           # JWT генерация и проверка токенов
        │   └── routes/             # Обработчики маршрутов REST API
        └── test/kotlin/com/ppo/web/ # Модульные и контрактные тесты
```

---

## 3. Конфигурация и переменные окружения

| Переменная | Описание | Значение по умолчанию |
| :--- | :--- | :--- |
| `PORT` | Порт HTTP-сервера | `8080` |
| `DB_HOST` | Хост СУБД PostgreSQL | `localhost` (в Docker: `db`) |
| `DB_PORT` | Порт СУБД PostgreSQL | `5432` |
| `DB_NAME` | Имя базы данных | `timetracker` |
| `DB_USER` | Пользователь БД | `timetracker` |
| `DB_PASSWORD` | Пароль пользователя БД | `timetracker_pass` |
| `JWT_SECRET` | Секретный ключ подписи JWT токенов | `timetracker_super_secret_jwt_key_2026` |
| `TIMETRACKER_API_URL` | URL для легаси клиентов TUI/GUI | `http://localhost:8080/api/v1` |

---

## 4. Запуск сервиса

### 4.1. В контейнерах Docker Compose (Рекомендуемый способ)

Из корневой директории проекта:

```bash
# Сборка fat-jar и запуск PostgreSQL + Web Backend
./gradlew :web:backend:jar
docker compose up --build -d
```

Проверка состояния контейнеров:

```bash
docker compose ps
docker compose logs -f backend
```

Остановка:

```bash
docker compose down
```

### 4.2. Локальный запуск (Standalone)

Требуется работающий PostgreSQL (например, `docker compose up -d db`):

```bash
# Запуск через Gradle
./gradlew :web:backend:run

# Или запуск собранного fat-jar
./gradlew :web:backend:jar
java -jar web/backend/build/libs/backend.jar
```

---

## 5. Документация API (Swagger UI)

После запуска backend сервиса интерактивная документация и спецификация доступны по адресам:

- **Swagger UI**: [http://localhost:8080/api/v1/docs](http://localhost:8080/api/v1/docs)
- **OpenAPI 3.1 Contract**: [http://localhost:8080/api/v1/openapi.yaml](http://localhost:8080/api/v1/openapi.yaml)
- **Health Check**: [http://localhost:8080/health](http://localhost:8080/health)

---

## 6. Запуск проверок и тестов

### 6.1. Модульные тесты Ktor

Запуск набора юнит- и интеграционных тестов в Ktor Test Host (тестируются регистрация, логин, ролевой доступ, настройки, категории, интервалы времени, снимки истории, валидация 422 и откат):

```bash
./gradlew :web:backend:test
```

### 6.2. Демонстрация фиксации нарушения контракта

В наборе тестов присутствует специализированный тест `ContractViolationDemonstrationTest`:
- Проверяет сценарий намеренного искажения контракта (возврат "голого" JSON-массива `[...]` вместо специфицированного объекта `{"items": [...]}`).
- Демонстрирует автоматическое обнаружение регрессии и прохождение теста на корректной схеме.

### 6.3. Сквозной тест живого сервиса

При запущенных контейнерах (`docker compose up -d`):

```bash
python3 web/api/scripts/verify_live_api.py http://localhost:8080
```

Скрипт проверяет 14 последовательных шагов реального взаимодействия:
1. Health check `/health`
2. Раздачу Swagger UI и `openapi.yaml`
3. Регистрацию пользователей и администратора
4. Валидацию полей (400) и конфликт логина (409)
5. Аутентификацию и выдачу JWT токена
6. Защиту профиля и разграничение доступа (401 / 403)
7. Получение и частичное обновление настроек пользователя
8. Создание системных и пользовательских категорий с контролем прав
9. Старт непрерывной сессии таймера
10. Разрешение параллельных активных задач
11. Завершение сессии через PATCH
12. Бизнес-правила валидации времени (422 `INVALID_TIME_RANGE`, 422 `DURATION_TOO_SHORT`)
13. Снимки описаний и откат к предыдущей версии
14. Пагинацию записей (`limit`, `offset`, `total`)

---

## 7. Переключение интерфейсов TUI и GUI на REST API (Бонус 2)

Реализованы адаптеры API в `:data` (`RestApiClient`, `ApiUserRepository`, `ApiCategoryRepository`, `ApiTimeRecordRepository`).

### 7.1. Запуск консольного клиента (:tech_ui) в режиме API

Сборка JAR:
```bash
./gradlew :tech_ui:jar
```

Запуск команд через REST API:
```bash
# Флаг --api активирует работу через REST API сервер
java -jar dist/tech_ui.jar --api register alex "Алексей" pass123 USER
java -jar dist/tech_ui.jar --api login alex pass123
java -jar dist/tech_ui.jar --api categories
java -jar dist/tech_ui.jar --api create-category "Проекты" "#3B82F6"
java -jar dist/tech_ui.jar --api start <category_uuid>
java -jar dist/tech_ui.jar --api stop
java -jar dist/tech_ui.jar --api records
```

Также режим API можно включить через файл `config.properties`:
```properties
api.url=http://localhost:8080/api/v1
```
или через переменную окружения `TIMETRACKER_API_URL`.

### 7.2. Мобильное приложение Android (:app) в режиме API

В `config.properties` или через аргументы сборки задается адрес сервиса (для эмулятора Android по умолчанию используется `http://10.0.2.2:8080/api/v1`).
Репозитории Koin автоматически переключаются с локальной Room БД на `RestApiClient`.
