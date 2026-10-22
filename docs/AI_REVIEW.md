# AI_REVIEW.md — Журнал работы в ИИ-треке (Web-технологии 2026)

В данном документе фиксируются результаты выполнения лабораторных работ, используемые модели и инструменты, ссылки на проверки, а также ключевые принятые и отклоненные архитектурные решения.

---

## WebLab#1 — I'm a startuper (and/or a system architect)

- **Задача**: Провести комплексный системный анализ проекта TimeTracker, сформировать технические и архитектурные требования для Web-версии, подготовить Use-Case, BPMN, ER, C4 диаграммы, эскизы интерфейса, оформить ADR и подготовить контекст агента ([AGENTS.md](../AGENTS.md)).
- **Инструмент / Модель**: Antigravity Coding Agent (Gemini 3.8 Flash High).
- **Результаты и артефакты**:
  - Комплект системного анализа и спецификация: [README.md](../README.md)
  - Контекст и руководство для агентов: [AGENTS.md](../AGENTS.md)
  - Исходники диаграмм C4, BPMN, ER, Use-Case: каталог [docs/](.)
  - Архитектурные решения: [ADR-001](adr/ADR-001-backend-stack.md), [ADR-002](adr/ADR-002-database.md), [ADR-003](adr/ADR-003-frontend-stack.md)
- **Способ проверки**:
  - Проверка согласованности ролей (`USER`, `ADMIN`), сущностей (`User`, `Category`, `TimeRecord`, `DescriptionVersion`), бизнес-правил (минимальная длительность 1 мин, запрет пересечения интервалов) между Use-Case, BPMN, ER, C4 и эскизами экранов.
  - Проверка валидности синтаксиса PlantUML, DBML и Mermaid диаграмм.
- **Существенные принятые решения**:
  1. *Принято*: Переиспользование ядра `:domain` (чистый Kotlin) в веб-бэкенде на Ktor с сохранением архитектуры Clean Architecture и обеспечением независимости от СУБД.
  2. *Принято*: Использование PostgreSQL в качестве основной СУБД для web-контура с целевой поддержкой primary-replica репликации в Docker Compose для WebLab#5.
  3. *Принято*: Разработка веб-клиента в виде SPA на TypeScript + React + Vite с компонентной архитектурой и строгой типизацией на базе генерируемого из OpenAPI клиента (для WebLab#8).
- **Существенные отклоненные решения**:
  1. *Отклонено*: Переписывание бэкенда на Go или Python. Причина: приведет к дублированию валидации и бизнес-правил, уже реализованных и протестированных в модуле `:domain`.
  2. *Отклонено*: Использование встроенной SQLite для высоконагруженного web-сервера с балансировкой на чтение/запись. Причина: SQLite блокирует файл базы при записи и не подходит для масштабирования в Docker с раздельными репликами (WebLab#5).

---

## WebLab#2 — REST API System Analyst & OpenAPI 3.1

- **Задача**: Спроектировать контракт REST API по стандарту OpenAPI 3.1.0 для системы TimeTracker, реализовать каноническую ресурсную адресацию пользователей и настроек (`/users/{userId}/settings`) с динамическими единицами измерения (секунды, минуты, часы, дни), модель открытых записей (`endTime = null`) с поддержкой параллельных сессий (multi-tasking) и перекрытий интервалов, комплексный аудит истории параметров записи, устранить корневые массивы в пользу словарей с полем `items`, подготовить автоматизированную валидацию, mock-сервер и сквозные тесты с фиксацией управления изменениями (Change Management).
- **Инструмент / Модель**: Antigravity Coding Agent (Gemini 3.8 Flash High).
- **Результаты и артефакты**:
  - Контракт OpenAPI 3.1: [web/api/openapi.yaml](../web/api/openapi.yaml)
  - Документация и анализ контракта: [web/api/README.md](../web/api/README.md)
  - Мок-сервер REST API: [web/api/scripts/mock_server.py](../web/api/scripts/mock_server.py)
  - Сквозной тест пользовательских сценариев (15 шагов): [web/api/scripts/test_scenario.py](../web/api/scripts/test_scenario.py)
- **Способ проверки**:
  - Индустриальный линтинг схемы через **Redocly CLI** (`docker run --rm -v $(pwd):/spec -w /spec redocly/cli lint web/api/openapi.yaml`): валидация синтаксиса OpenAPI 3.1.0, структурных правил и безопасности (0 ошибок, 0 предупреждений).
  - Валидация через **openapi-spec-validator** (`openapi-spec-validator web/api/openapi.yaml`): полное соответствие официальной спецификации OpenAPI 3.1 (статус OK).
  - Запуск mock-сервера и сценарного теста `python3 web/api/scripts/test_scenario.py`: успешное выполнение 15/15 шагов, включая CRUD категорий (обертка `items`), ресурсные пути `/users/{userId}` и `/users/{userId}/settings`, динамическую настройку единиц измерения, параллельный запуск нескольких активных задач (`endTime=null`), создание пересекающихся записей, завершение активности через `PATCH`, модификацию интервалов и категории с фиксацией снимка в истории, просмотр истории (`items`), forward-only откат всех параметров записи и валидацию настраиваемого порога минимальной длительности (`422 Unprocessable Entity`).
- **Существенные принятые решения**:
  1. *Принято*: Каноническая ресурсная адресация `/users/{userId}` и `/users/{userId}/settings` (согласовано по рекомендации преподавателя) с полным отказом от псевдо-эндпоинта `/users/me`. Сущность `User` сделана легковесной (только профиль: `id`, `login`, `name`, `role`), а настройки вынесены в специализированный ресурс `/users/{userId}/settings`, что оптимизирует трафик в GUI и обеспечивает чистый Separation of Concerns и RBAC.
  2. *Принято*: Введение гибких пользовательских настроек `UserSettings` с поддержкой единиц времени (`seconds`, `minutes`, `hours`, `days`), автоматической остановки зависших задач (`autoStopEnabled`, `maxActiveDuration`) и переключателя минимальной длительности (`minDurationEnabled`, `minDuration`).
  3. *Принято*: Парадигма открытых интервалов (`TimeRecord` с `endTime = null`) вместо выделенной сущности таймера. Это исключило дублирование моделей и обеспечило естественную поддержку многозадачности (multi-tasking) и перекрывающихся интервалов.
  4. *Принято*: Комплексное версионирование записи (`RecordHistoryEntry`): фиксация снимков всех ключевых параметров (`startTime`, `endTime`, `categoryId`, `description`) при любых изменениях с возможностью полного forward-only отката.
  5. *Принято*: Строгая объектная упаковка коллекций: все списочные ответы возвращают словарь со свойством `items`.
  6. *Принято*: Полноценная поддержка CORS и preflight `OPTIONS`-запросов в мок-сервере (включая `Access-Control-Allow-Private-Network: true` для Chromium) совместно с регистрацией `http://localhost:4010` в списке серверов спецификации `openapi.yaml`. Это обеспечивает моментальное интерактивное тестирование контракта прямо через web-интерфейс Swagger Editor ([editor.swagger.io](https://editor.swagger.io)).
  7. *Принято*: Автоматическая инициализация (auto-provisioning) учетных записей из примеров документации (`alex_dev`, `admin`) при логине в мок-сервере, что устраняет ошибку `401 Unauthorized` при попытке тестирования логина в Swagger UI без предварительного ручного вызова регистрации.
  8. *Принято*: Архитектурная реорганизация репозитория: унаследованные компоненты курса ППО (`app`, `data`, `data_objectbox`, `migration`, `tech_ui`, `config.properties`) изолированы в подкаталоге `ppo/`, в то время как корневой Gradle (`gradlew`, `settings.gradle.kts`) и чистое доменное ядро `:domain` сохранены на верхнем уровне для прямого использования в новом веб-бэкенде (WebLab#3).
  9. *Принято*: Надежное завершение процесса мок-сервера по `Ctrl+C`: заголовок `Connection: close` (предотвращает блокировку сокета браузерным Keep-Alive), перехват `SIGINT`/`SIGTERM` с вызовом `os._exit(0)` и опция сокета `allow_reuse_address = True`.
- **Существенные отклоненные решения**:
  1. *Отклонено*: Реализация GraphQL (Bonus #1) или RPC-эндпоинтов. Причина: строгое ограничение проекта CORE PROGRAM ONLY (NO BONUSES); стандарт REST + OpenAPI 3.1 полностью покрывает требования системы.
  2. *Отклонено*: Автогенерация OpenAPI контракта из кода бэкенда (Code-First). Причина: принцип CONTRACT FIRST — контракт является независимым источником истины для параллельной разработки бэкенда (Ktor, WebLab#3) и фронтенда (React SPA, WebLab#8).
  3. *Отклонено*: Использование контекстного псевдонима `/users/me`. Причина: рекомендация преподавателя по соблюдению канонического RESTful-дизайна; ресурсно-ориентированная адресация `/users/{userId}` исключает неявный контекст сессии и масштабируется на администрирование пользователей.
  4. *Отклонено*: Выделенная сущность и пути таймера (`/timer/*`). Причина: таймер концептуально представляет собой незавершенную запись времени (`endTime = null`); выделенная сущность блокировала одновременное ведение нескольких параллельных задач.

---

## WebLab#3 — REST API Backend & Data Layer Separation (Ktor / PostgreSQL / Docker Compose)

- **Задача**: Разработать масштабируемый серверный REST API сервис на базе Kotlin и Ktor в строгом соответствии с контрактом OpenAPI 3.1 (`web/api/openapi.yaml`), реализовать архитектуру Clean Architecture с выделением слоя данных в отдельный Gradle-подпроект `:web:data`, контейнеризовать сервис и PostgreSQL в Docker Compose, реализовать **Бонус 1** (JWT-аутентификация и авторизация по ролям USER/ADMIN) и **Бонус 2** (перенаправление существующих клиентских интерфейсов TUI `:tech_ui` и GUI Android `:app` на новый REST API сервис без потери функционала), написать модульные тесты Ktor, продемонстрировать автоматическое выявление намеренного локального нарушения контракта с его восстановлением и подтвердить работоспособность живого сервиса в контейнере сквозным скриптом.
- **Инструмент / Модель**: Antigravity Coding Agent (Gemini 3.8 Flash High / Claude 3.7 Sonnet).
- **Результаты и артефакты**:
  - Подпроект слоя данных `:web:data` ([web/data/build.gradle.kts](../web/data/build.gradle.kts), [DatabaseFactory.kt](../web/data/src/main/kotlin/com/ppo/web/data/db/DatabaseFactory.kt), ORM таблицы [Tables.kt](../web/data/src/main/kotlin/com/ppo/web/data/orm/Tables.kt), DAO сущности [Entities.kt](../web/data/src/main/kotlin/com/ppo/web/data/orm/Entities.kt), репозитории PostgreSQL на базе JetBrains Exposed ORM).
  - Подпроект веб-сервиса `:web:backend` ([web/backend/build.gradle.kts](../web/backend/build.gradle.kts), [Application.kt](../web/backend/src/main/kotlin/com/ppo/web/Application.kt), маршруты `AuthRoutes`, `UserRoutes`, `CategoryRoutes`, `RecordRoutes`, `HistoryRoutes`, `SwaggerRoutes`, DTO, JWT-безопасность).
  - Docker Compose и контейнеризация: [deploy/docker-compose.yml](../deploy/docker-compose.yml), [docker-compose.yml](../docker-compose.yml), [deploy/postgres/init.sql](../deploy/postgres/init.sql), [web/backend/Dockerfile](../web/backend/Dockerfile), [.env.example](../.env.example).
  - Клиентский слой перенаправления: [ppo/data/src/main/java/com/ppo/data/api/RestApiClient.kt](../ppo/data/src/main/java/com/ppo/data/api/RestApiClient.kt), CLI `--api` в `:tech_ui`, автопереключение в `:app`.
  - Модульные тесты Ktor (`AuthApiTest`, `CategoryApiTest`, `RecordApiTest`, `HistoryApiTest`, `UserApiTest`, `ContractViolationDemonstrationTest`).
  - Скрипт верификации живого сервиса: [web/api/scripts/verify_live_api.py](../web/api/scripts/verify_live_api.py).
  - Документация сервиса: [web/backend/README.md](../web/backend/README.md).
- **Способ проверки**:
  - Модульные тесты Ktor: `./gradlew :web:backend:test` — все тесты завершились успешно (`BUILD SUCCESSFUL`).
  - Демонстрация нарушения контракта (`ContractViolationDemonstrationTest`): тест подтверждает гарантированное обнаружение искажения схемы ("голый" массив `[...]` вместо структуры `{"items": [...]}`) и прохождение при контрактном ответе.
  - Сборка и тесты модулей `:domain:test`, `:web:data:compileKotlin`, `:tech_ui:jar`, `:app:compileDebugKotlin` — полная обратная совместимость сохранена.
  - Сквозной тест живого контейнеризованного API: `python3 web/api/scripts/verify_live_api.py http://localhost:8080` — успешное прохождение всех 15 шагов.
  - Интерактивная проверка консольного интерфейса TUI в режиме API (`java -jar dist/tech_ui.jar --api ...`): проверены регистрация, вход, получение системных категорий, создание пользовательской категории, запуск/остановка таймера, отсечение коротких интервалов по бизнес-правилу, добавление ручных записей, изменение описания и откат версий.
  - Проверка Swagger UI: [http://localhost:8080/api/v1/docs](http://localhost:8080/api/v1/docs) возвращает статус 200 OK.
- **Существенные принятые решения**:
  1. *Принято*: Внедрение ORM-фреймворка **JetBrains Exposed** (`exposed-core`, `exposed-dao`, `exposed-jdbc`, `exposed-java-time`) в слой данных `:web:data`. Все операции с БД (CRUD, внешние ключи, сортировки, пагинация, фильтрация) переведены на типизированные ORM-таблицы и DAO-сущности (`UserEntity`, `UserSettingsEntity`, `CategoryEntity`, `TimeRecordEntity`, `RecordHistoryEntity`), что исключило ручные SQL-строки и сделало слой данных строго типизированным.
  2. *Принято*: Выделение слоя данных в отдельный Gradle-подпроект `:web:data` по прямому требованию чистой архитектуры. Это позволило изолировать зависимости PostgreSQL/HikariCP/Exposed от Ktor и оставить `:web:backend` ответственным исключительно за транспортный уровень HTTP/REST.
  3. *Принято*: Использование Jackson (`ktor-serialization-jackson` + `jackson-module-kotlin` + `JavaTimeModule`) вместо Kotlinx Serialization для гарантированной совместимости версий компилятора Kotlin.
  4. *Принято*: Создание легковесного `RestApiClient` с нулевыми внешними зависимостями (на базе `HttpURLConnection`) в `:data`. Это позволило перенаправить как JVM CLI, так и Android GUI на новый REST API без подтягивания тяжелых JVM-клиентов и без поломок существующих сборок Room/ObjectBox.
  5. *Принято*: Автоматическое управление токеном сессии в CLI через локальный файл сессии, что дало бесшовный пользовательский опыт при выполнении консольных команд через API.
  6. *Принято*: Фоновый сервис автоматической остановки зависших задач (`AutoStopWorker`). В Ktor-сервисе развернут периодический фоновый процесс (по умолчанию раз в 10 минут, настраивается через `AUTO_STOP_INTERVAL_MS`), проверяющий все активные записи (`endTime == null`) пользователей с `autoStopEnabled = true`. При превышении `maxActiveDuration` задача автоматически завершается с фиксацией аудиторного снимка в `RecordHistoryEntry`. Также при чтении и создании записей вызывается локальная синхронизация просроченных задач пользователя.
  7. *Принято*: Поддержка параллельного трекинга (multi-tasking) и пересекающихся интервалов: отменено ограничение на 1 активную задачу, удалена выделенная сущность таймера из API в пользу модели открытых интервалов (`endTime == null`), добавлена фильтрация категорий по параметру `scope` (`all`, `default`, `custom`) и версионирование изменений временных рамок (`startTime`, `endTime`) в `RecordHistoryEntry`.
  8. *Принято*: Модернизация доменного ядра `:domain` (Clean Architecture): перенос всей чистой бизнес-логики нового ТЗ в доменные Use Cases (`CreateTimeRecordUseCase`, `UpdateTimeRecordUseCase`, `RollbackRecordHistoryUseCase`, `GetRecordHistoryUseCase`, `AutoStopExpiredRecordsUseCase`, `ManageUserSettingsUseCase`, `DeleteRecordUseCase`). Модель `DescriptionVersion` заменена на `RecordHistoryEntry` по всему домену с сохранением typealias и адаптированных фасадов для легаси-модулей `ppo/*`, благодаря чему архитектура осталась строгой, а легаси-код и тесты продолжают успешно компилироваться и проходить.
- **Существенные отклоненные решения**:
  1. *Отклонено*: Использование тяжеловесного Hibernate / Spring Data JPA. Причина: избыточный оверхед, несовместимость с легким стеком Kotlin Ktor; Exposed является официальным идиоматичным решением от JetBrains для Kotlin.
  2. *Отклонено*: Размещение DDL и кода репозиториев PostgreSQL непосредственно внутри `:web:backend`. Причина: строгое требование пользователя и принципов Clean Architecture по обособлению слоя хранения в отдельный модуль `:web:data`.
  3. *Отклонено*: Реализация бизнес-логики (авто-остановка, аудит истории, валидация по настройкам) внутри маршрутов `:web:backend` в обход `:domain`. Причина: нарушение Clean Architecture; вся логика инкапсулирована в Use Cases модуля `:domain`.
  4. *Отклонено*: Написание inline-комментариев в исходном коде Kotlin/Java/Python. Причина: жесткое правило проекта NO INLINE COMMENTS (самодокументируемый код).

