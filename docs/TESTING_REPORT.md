# Отчет по тестированию (Лабораторная работа №1: Unit-тестирование)
## Проект: TimeTracker Web (Модули `:domain` и `:web:data`)

---

## 1. Введение и цели работы

В рамках Лабораторной работы №1 разработана и внедрена комплексная система автоматизированного unit-тестирования для ключевых модулей распределенной веб-системы **TimeTracker** в строгом соответствии с заданием лабораторной работы (тестирование компонентов доступа к данным и бизнес-логики):
1. Чистого Kotlin-модуля бизнес-логики **`:domain`** (Use Cases, Domain Entities, валидации открытых интервалов, бизнес-правила порогов, аудит и forward-only откат версий).
2. Модуля доступа к данным **`:web:data`** (PostgreSQL/Exposed ORM репозитории: пользователи, настройки, категории, записи времени, история изменений, DDL-схема и DatabaseFactory).

Все тесты разработаны в соответствии с рекомендациями лекционного курса:
- Защита от регрессий, устойчивость к рефакторингу (Refactoring Resistance) и минимизация ошибок I и II рода (false positive / false negative).
- Тестирование наблюдаемого поведения (**Observable Behavior**), а не деталей реализации.
- Соблюдение принципа **Command-Query Separation (CQS)**.
- Строгое соблюдение структуры **Arrange-Act-Assert (AAA)** с правилом **ровно одной строки в секции Act**.
- Отсутствие inline-комментариев в коде (правило проекта `NO INLINE COMMENTS`).
- 100% покрытие бизнес-процессов BPMN со всеми альтернативными ветками шлюзов.

---

## 2. Паттерны генерации тестовых данных

Для исключения хардкода, читаемости и переиспользования данных реализованы два взаимодополняющих паттерна:

### 2.1. Object Mother (Фабрика канонических сущностей)
Расположена в `domain/src/test/java/com/ppo/domain/fixtures/TestMothers.kt`:
- **`TestUserMother`**:
  - `regularUser()`: стандартный пользователь (`role = USER`, `login = "alex_dev"`).
  - `adminUser()`: администратор (`role = ADMIN`, `login = "admin_boss"`).
  - `otherUser()`: сторонний пользователь (`login = "bob_tester"`).
- **`TestUserSettingsMother`**:
  - `defaultSettings()`: базовые настройки (авто-остановка и минимальный порог выключены).
  - `strictSettings()`: `minDurationEnabled = true` (300 сек), `autoStopEnabled = true` (120 мин).
- **`TestCategoryMother`**:
  - `baseWorkCategory()`: базовая категория (`isDefault = true`, `userId = null`, цвет `#3B82F6`).
  - `userCustomCategory()`: пользовательская категория (`isDefault = false`, привязана к пользователю).
- **`TestTimeRecordMother`**:
  - `activeTimerRecord()`: активная задача с открытым интервалом (`endTime = null`).
  - `completedRecord()`: завершенная задача длительностью 1 час ($T_{start} < T_{end}$).

### 2.2. Test Data Builder (Конструктор с гибким переопределением полей)
Расположен в `domain/src/test/java/com/ppo/domain/builders/Builders.kt`:
- **`TimeRecordBuilder`**: методы `.withUserId()`, `.withCategoryId()`, `.withStartTime()`, `.withEndTime()`, `.asActive()`, `.withDurationMinutes()`, `.withDurationSeconds()`, `.withDescription()`, `.build()`.
- **`UserSettingsBuilder`**: методы `.withUserId()`, `.withAutoStop(enabled, value, unit)`, `.withMinDuration(enabled, value, unit)`, `.build()`.
- **`CategoryBuilder`**: методы `.withName()`, `.withColorHex()`, `.asDefault()`, `.withUserId()`, `.build()`.
- **`UserBuilder`**: методы `.withLogin()`, `.withName()`, `.withPasswordHash()`, `.withRole()`, `.build()`.

### 2.3. Декларативные кастомные фикстуры через аннотации (@-синтаксис)
Для уменьшения шаблонного кода инициализации в блоках Arrange реализован механизм декларативных кастомных фикстур через аннотации перед тестовыми и вспомогательными методами (`com.ppo.domain.fixtures.*` и `com.ppo.web.data.fixtures.*`):
- **Аннотации перед тестовыми методами**:
  - `@WithRegularUser`: автоматическая инициализация профиля обычного пользователя.
  - `@WithAdminUser`: автоматическая инициализация профиля администратора.
  - `@WithStrictSettings(minDurationValue, minDurationUnit)`: создание и связывание строгих настроек с минимальным порогом и авто-остановкой.
  - `@WithDefaultSettings`: базовые дефолтные настройки.
  - `@WithBaseCategory`: системная базовая категория (`TestCategoryMother.baseWorkCategory()`).
  - `@WithUserCategory`: пользовательская категория текущего пользователя (`TestCategoryMother.userCustomCategory(userId)`).
  - `@WithCustomCategory(name, colorHex, isDefault)`: подготовка произвольной категории с заданными параметрами.
  - `@WithActiveTimeRecord` / `@WithCompletedTimeRecord`: автоматическая генерация активной или завершенной записи времени.
  - `@WithInMemoryDatabase`: автоматический подъем изолированной базы данных H2 (`MODE=PostgreSQL`) и накат схемы DDL.
- **Аннотация перед методами фикстур**:
  - `@CustomFixture(name = "...")`: маркировка кастомных методов подготовки окружения внутри тестовых классов, автоматически вызываемых раннером `RandomOrderRunner` перед прогоном каждого теста.
- **Потокобезопасный доступ**: значения созданных через аннотации фикстур передаются в тест через потокобезопасный контекст `FixtureContext.user`, `FixtureContext.settings`, `DatabaseFixtureContext.dataSource` и автоматически очищаются в блоке `finally` раннера.

---

## 3. Методология: Лондонская школа vs Классическая (Детройтская)

В соответствии с материалами лекций и заданием лабораторной работы, в проекте представлены обе школы unit-тестирования:

### 3.1. Лондонская школа (Mockist / Interaction-based)
- **Суть**: Объект тестирования изолируется от всех коллабораторов. Зависимости заменяются на Mock / Stub через библиотеку `MockK`.
- **Применение**:
  - Большинство Use Case классов в модуле `:domain` (`CreateTimeRecordUseCaseTest`, `UpdateTimeRecordUseCaseTest`, `DeleteRecordUseCaseTest`, `AutoStopExpiredRecordsUseCaseTest`, `RollbackRecordHistoryUseCaseTest`, `RegisterUserUseCaseTest`, `AdminManageBaseCategoryUseCaseTest` и др.).
- **Проверка по CQS**:
  - Для **Query** (запросы данных, входящие взаимодействия) настраиваются `coEvery { ... } returns ...`, а проверка осуществляется через `assertEquals` на результат SUT (без проверки вызова стабов).
  - Для **Command** (команды изменения состояния, исходящие взаимодействия) проверяются вызовы сохранения/удаления: `coVerify(exactly = 1) { repository.saveRecord(...) }`.

### 3.2. Классическая Детройтская школа (Classical / State-based)
- **Суть**: Модуль тестируется без моков и стабов. Для зависимостей используются легковесные in-memory реализации (`InMemoryTimeRecordRepository`, `InMemoryCategoryRepository`, `InMemoryUserRepository`, `InMemoryUserSettingsRepository`, `InMemoryRecordHistoryRepository`), либо in-memory СУБД H2 в режиме эмуляции PostgreSQL (`MODE=PostgreSQL`) для слоя данных.
- **Применение**:
  - Все репозитории слоя данных `:web:data` (`PostgresTimeRecordRepositoryTest`, `PostgresUserRepositoryTest`, `PostgresCategoryRepositoryTest`, `PostgresUserSettingsRepositoryTest`, `PostgresRecordHistoryRepositoryTest`, `DatabaseFactoryTest`).
  - Доменная логика единиц времени `DurationUnitTest` (`toSeconds`).
  - Комплексные сценарные тесты с фиксацией изменения реального внутреннего состояния.

### 3.3. Сравнительный анализ (класс `ComparisonApproachTest`)
В классе `domain/src/test/java/com/ppo/domain/usecase/ComparisonApproachTest.kt` реализован один и тот же сценарий (`CreateTimeRecordUseCase`) в двух парадигмах:
1. `createRecord_londonSchool_verifiesCollaboratorInteractions`: мокируются все репозитории; проверяется исходящий вызов `saveRecord(match { ... })`.
2. `createRecord_classicalSchool_verifiesFinalStateInFakeStorage`: используются `InMemoryTimeRecordRepository` и `InMemoryCategoryRepository`; проверяется сохраненное состояние объекта в коллекции репозитория через `fakeRepo.getRecordById(...)`.

**Вывод из сравнения**: Лондонский подход дает максимальную изоляцию и сразу локализует место ошибки при сбое конкретного класса. Классический подход более устойчив к внутреннему рефакторингу связей между классами и лучше проверяет реальную бизнес-целостность состояния.

---

## 4. Примененные техники тест-дизайна

Каждый тест ассоциирован с конкретной техникой подготовки тестовых данных:

1. **BVA (Boundary Value Analysis — Анализ граничных значений)**:
   - `shouldFailWithIllegalState_whenDurationBelowConfiguredMinThreshold`: проверка длительности на 1 секунду меньше порога (299 сек при пороге 300 сек $\to$ ошибка).
   - `shouldSucceed_whenDurationEqualsMinThresholdExactly`: проверка длительности ровно на границе порога (300 сек при пороге 300 сек $\to$ успех).
   - `shouldFailWithIllegalArgument_whenEndTimeBeforeStartTime`: проверка $T_{end} < T_{start}$.
   - `shouldFailWithIllegalArgument_whenPasswordTooShort`: проверка длины пароля 5 символов при минимуме 6.
   - `shouldFailWithIllegalArgument_whenMaxDurationValueLessThanOne`: проверка значения 0 при допустимых $\ge 1$.
   - `shouldConvertUnitsToSecondsCorrectly_forAllEnumValues`: граничные значения 0 и отрицательные числа.
2. **EP (Equivalence Partitioning — Классы эквивалентности)**:
   - `shouldSaveCategory_whenValidNameAndHexColor`: валидный HEX-цвет (`#10B981`) vs невалидный (`invalid-hex`).
   - `shouldConvertUnitsToSecondsCorrectly_forAllEnumValues`: классы единиц времени `seconds`, `minutes`, `hours`, `days`.
   - `testListCategoriesWithScopeFilter`: классы фильтрации `scope`: `all`, `default`, `custom`.
   - `testUnauthorizedAccess`: класс неавторизованных запросов без Bearer токена.
3. **State Transition (Тестирование переходов состояний)**:
   - `shouldCreateActiveRecord_whenEndTimeIsNull`: переход задачи в активное состояние ($endTime = null$).
   - `shouldAutoStopOverdueRecordAndSaveHistory_whenDurationExceedsMaxLimit`: переход активной задачи в завершенную по таймауту с фиксацией аудиторного снимка в `RecordHistoryEntry`.
   - `shouldRollbackToSnapshotAndCreateNewAuditEntry_whenOwnerRequestsRollback`: переход текущего состояния записи назад к исторической версии с созданием нового снимка в аудите (forward-only откат).
4. **Combinatorial / Pairwise (Комбинаторное тестирование)**:
   - Матрица ролевого доступа: `(Роль: USER / ADMIN) × (Категория: Пользовательская / Базовая)`. Пользователь может создавать только пользовательские, администратор — базовые (`isDefault = true`).
   - Матрица владения ресурсом: `(Запрос от: Владельца / Стороннего пользователя / Администратора) × (Операция: GET / PUT / PATCH / DELETE)`.
5. **Error Guessing (Предугадывание ошибок)**:
   - Регистрация пользователя с уже занятым логином (`USER_ALREADY_EXISTS` / 409 Conflict).
   - Создание категории с дублирующимся именем у одного пользователя.
   - Запрос несуществующего ресурса по случайному UUID (`NoSuchElementException` / 404 Not Found).
   - Подделка или повреждение подписи JWT-токена.
   - Попытка отката к историческому снимку от другой записи (`IllegalArgumentException`).

---

## 5. Покрытие бизнес-процессов BPMN (100% ветвей)

В соответствии с требованиями лабораторной работы к объектам тестирования (компоненты доступа к данным `:web:data` и бизнес-логики `:domain`), проверка всех трех BPMN-диаграмм проекта и всех их ветвей реализована непосредственно в этих модулях:

### 5.1. BPMN «Добавление записи вручную» (`docs/addtask.bpmn`)
- **Элементы**: `StartEvent_User` $\to$ запрос категорий `Activity_Request_Map` / `Activity_Get_Map_Data` $\to$ выбор категории и ввод интервала `Activity_Select_Spot` $\to$ отправка `Activity_Submit_Booking` / `Activity_Process_Booking` $\to$ `Gateway_Check_Result`.
- **Запрос категорий**:
  - `GetCategoriesUseCaseTest.kt` (`shouldReturnCategories_whenRequestedForUser`)
  - `PostgresCategoryRepositoryTest.kt` (`getCategoriesFlow_filtersUserAndDefaultCategories`)
- **Ветка «Да» (Happy Path)**: $T_{start} < T_{end} \to$ успешное сохранение записи с интервалом:
  - `CreateTimeRecordUseCaseTest.kt` (`shouldCreateCompletedRecord_whenIntervalIsValid`, `shouldCreateRecordSuccessfully_whenValidParametersGiven`)
  - `AddManualRecordUseCaseTest.kt` (`shouldAddRecordSuccessfully_whenValidDataProvided`)
  - `PostgresTimeRecordRepositoryTest.kt` (`saveRecord_completedTask_persistsWithEndTimestamp`)
- **Ветка «Нет» (Error Loop)**: $T_{end} \le T_{start} \to$ исключение `IllegalArgumentException`:
  - `CreateTimeRecordUseCaseTest.kt` (`shouldFailWithIllegalArgument_whenEndTimeBeforeStartTime`)
  - `AddManualRecordUseCaseTest.kt` (`shouldFailManualRecord_whenEndTimeBeforeStartTime`)

### 5.2. BPMN «Запуск и остановка таймера» (`docs/withtimer.bpmn`)
- **Элементы**: `StartEvent_User` $\to$ опрос активных сессий `Activity_Request_Screen` / `Activity_Get_Init_Data` $\to$ старт открытой сессии `Activity_Select_Spot` $\to$ остановка `Activity_Stop_Timer` / `Activity_Process_Timer` $\to$ валидация порога `minDuration` $\to$ `Gateway_Check_Result`.
- **Опрос активной задачи**:
  - `StartTimerUseCaseTest.kt` (`shouldStartTimer_whenNoActiveSessionExists`)
  - `PostgresTimeRecordRepositoryTest.kt` (`getActiveSession_whenActiveExists_returnsActiveRecord`, `getActiveSession_whenNoActiveExists_returnsNull`)
- **Старт таймера (открытый интервал $endTime = null$)**:
  - `CreateTimeRecordUseCaseTest.kt` (`shouldCreateActiveRecord_whenEndTimeIsNull`)
  - `StartTimerUseCaseTest.kt` (`shouldStartTimer_whenNoActiveSessionExists`)
  - `PostgresTimeRecordRepositoryTest.kt` (`saveRecord_activeTask_persistsWithNullEndTime`)
- **Ветка «Да» (Happy Path)**: $\Delta T \ge minDuration \to$ успех, фиксация $endTime \ne null$:
  - `CreateTimeRecordUseCaseTest.kt` (`shouldSucceed_whenDurationEqualsMinThresholdExactly`)
  - `UpdateTimeRecordUseCaseTest.kt` (`shouldUpdateTimeRecord_whenValidParameters`)
  - `StopTimerUseCaseTest.kt` (`shouldStopTimer_whenActiveSessionExists`)
  - `PostgresTimeRecordRepositoryTest.kt` (`saveRecord_existingRecord_updatesFields`)
- **Ветка «Нет» (Error Loop)**: $\Delta T < minDuration \to$ исключение `IllegalStateException`:
  - `CreateTimeRecordUseCaseTest.kt` (`shouldFailWithIllegalState_whenDurationBelowConfiguredMinThreshold`)

### 5.3. BPMN «Создание новой базовой категории» (`docs/addcategory.bpmn`)
- **Элементы**: `StartEvent_User` $\to$ экран категорий `Activity_Request_Screen` / `Activity_Get_Init_Data` $\to$ ввод названия и HEX `Activity_Select_Spot` $\to$ отправка `Activity_Submit_Data` / `Activity_Process_Data` $\to$ `Gateway_Check_Result`.
- **Запрос списка базовых категорий**:
  - `GetCategoriesUseCaseTest.kt` (`shouldReturnCategories_whenRequestedForUser`)
  - `PostgresCategoryRepositoryTest.kt` (`getCategoriesFlow_filtersUserAndDefaultCategories`)
- **Ветка «Да» (Happy Path)**: Администратор создает категорию с `isDefault = true` $\to$ успешное сохранение:
  - `AdminManageBaseCategoryUseCaseTest.kt` (`shouldSaveBaseCategory_whenAdminProvidesValidData`)
  - `PostgresCategoryRepositoryTest.kt` (`saveCategory_newBaseAndCustomCategories_persistsSuccessfully`)
- **Ветка «Нет» (Error Loop)**: Дубликат названия среди категорий $\to$ исключение `IllegalStateException`:
  - `AdminManageBaseCategoryUseCaseTest.kt` (`shouldFailWithIllegalState_whenDuplicateBaseCategoryNameExists`)
  - `ManageUserCategoryUseCaseTest.kt` (`shouldFailWithIllegalState_whenCategoryNameAlreadyExistsForUser`)
- **Ветка безопасности (RBAC)**: Обычный пользователь (`role = USER`) пытается создать/модифицировать базовую $\to$ отказ доступа `IllegalStateException`:
  - `AdminManageBaseCategoryUseCaseTest.kt` (`shouldFailWithIllegalState_whenRequesterIsNotAdmin`, `shouldFailWithIllegalState_whenBaseCategoryHasUserIdAttached`)

---

## 6. Инфраструктура и запуск тестов

### 6.1. Запуск из командной строки
Все тесты запускаются локально из корня репозитория стандартной командой Gradle Wrapper:
```bash
# Запуск unit-тестов требуемых модулей (:domain, :web:data)
./gradlew :domain:test :web:data:test
```

### 6.2. Запуск в режиме без доступа к интернету (Offline Mode)
Все тесты полностью автономны:
```bash
./gradlew :domain:test :web:data:test --offline
```
Тестовые наборы используют MockK, in-memory хранилища и встроенный H2 DataSource в режиме PostgreSQL (`MODE=PostgreSQL`), сетевые вызовы к внешним сервисам отсутствуют.

### 6.3. Запуск тестов в случайном порядке (Random Order)
Для предотвращения скрытых зависимостей между тестами и разделяемого мутабельного состояния создан и применен рандомизатор `RandomOrderRunner`:
```bash
# Запуск со случайным перемешиванием методов (псевдослучайный seed)
./gradlew :domain:test :web:data:test -Dtest.random.seed=1718000000000
```
Тестовые методы внутри каждого класса перемешиваются с использованием указанного `seed`.

### 6.4. Анализ количества процессов JVM при тестировании (п. 12 задания)
В Gradle количество процессов при выполнении тестов управляется параметрами таски `Test`:
1. **`maxParallelForks`**: максимальное количество параллельных JVM-процессов воркеров.
   - По умолчанию: `1` (все тестовые классы модуля выполняются последовательно в одном процессе воркера).
   - При `maxParallelForks = Runtime.getRuntime().availableProcessors()` Gradle поднимает пул параллельных процессов по числу ядер процессора, распределяя тестовые классы между ними.
2. **`forkEvery`**: количество тест-классов, выполняемых в рамках одного JVM-процесса до его перезапуска.
   - По умолчанию: `0` (бесконечность) — все тест-классы прогоняются в одном и том же долгоживущем JVM-процессе без накладных расходов на перезапуск JVM.
   - При `forkEvery = 1` Gradle порождает отдельный чистый JVM-процесс для каждого отдельного тест-класса. Это дает 100% изоляцию статических переменных и памяти, но существенно увеличивает время прогона из-за инициализации JVM на каждый класс.

В конфигурации проекта по умолчанию используется `forkEvery = 0, maxParallelForks = 1` для мгновенной скорости выполнения в миллисекундах.

---

## 7. Отчет о результатах покрытия кода (JaCoCo)

### 7.1. Команда генерации отчета
```bash
# Генерация единого сводного отчета JaCoCo для всего проекта
./gradlew jacocoRootReport

# Вывод сводной таблицы покрытия в терминал
python3 scripts/coverage_report.py

# Для детализации по отдельным модулям
python3 scripts/coverage_report.py --modules
```

### 7.2. Итоговые показатели покрытия всего проекта

```
=========================================================================================================
 MODULE: WHOLE PROJECT AGGREGATE (:DOMAIN + :WEB:DATA) (JaCoCo Code Coverage)
=========================================================================================================
Package / Class                                    | Lines                    | Branches                
---------------------------------------------------------------------------------------------------------
model.BusinessConfig                               | 2/2 (100.0%)             | N/A (no branches)       
model.DurationUnit                                 | 10/10 (100.0%)           | 4/4 (100.0%)            
model.UserSettings                                 | 9/9 (100.0%)             | N/A (no branches)       
model.TimeRecord                                   | 8/11 ( 72.7%)            | 0/2 (  0.0%)            
model.Category                                     | 6/6 (100.0%)             | N/A (no branches)       
model.UserRole                                     | 1/1 (100.0%)             | N/A (no branches)       
model.User                                         | 6/6 (100.0%)             | N/A (no branches)       
model.RecordHistoryEntry                           | 9/9 (100.0%)             | N/A (no branches)       
db.DatabaseFactory                                 | 26/48 ( 54.2%)           | 2/22 (  9.1%)           
orm.UserSettingsTable                              | 9/9 (100.0%)             | N/A (no branches)       
orm.UserSettingsEntity.Companion                   | 1/1 (100.0%)             | N/A (no branches)       
orm.UsersTable                                     | 7/7 (100.0%)             | N/A (no branches)       
orm.CategoryEntity.Companion                       | 1/1 (100.0%)             | N/A (no branches)       
orm.UserEntity                                     | 12/12 (100.0%)           | N/A (no branches)       
orm.UserEntity.Companion                           | 1/1 (100.0%)             | N/A (no branches)       
orm.TimeRecordEntity.Companion                     | 1/1 (100.0%)             | N/A (no branches)       
orm.RecordHistoryEntity                            | 16/16 (100.0%)           | N/A (no branches)       
orm.UserSettingsEntity                             | 16/16 (100.0%)           | N/A (no branches)       
orm.TimeRecordEntity                               | 14/14 (100.0%)           | N/A (no branches)       
orm.TimeRecordsTable                               | 8/8 (100.0%)             | N/A (no branches)       
orm.CategoryEntity                                 | 12/12 (100.0%)           | 6/6 (100.0%)            
orm.RecordHistoryEntity.Companion                  | 1/1 (100.0%)             | N/A (no branches)       
orm.CategoriesTable                                | 7/7 (100.0%)             | N/A (no branches)       
orm.RecordHistoryTable                             | 9/9 (100.0%)             | N/A (no branches)       
repository.ICategoryRepository.DefaultImpls        | 0/1 (  0.0%)             | N/A (no branches)       
repository.ICategoryRepository                     | 0/1 (  0.0%)             | N/A (no branches)       
repository.ITimeRecordRepository.DefaultImpls      | 0/8 (  0.0%)             | N/A (no branches)       
repository.ITimeRecordRepository                   | 0/11 (  0.0%)            | 0/6 (  0.0%)            
repository.PostgresUserRepository                  | 29/29 (100.0%)           | 7/8 ( 87.5%)            
repository.PostgresCategoryRepository.getCategoriesFlow.new Function2() {...} | 0/2 (  0.0%)             | N/A (no branches)       
repository.PostgresUserSettingsRepository          | 21/29 ( 72.4%)           | 6/8 ( 75.0%)            
repository.PostgresTimeRecordRepository            | 105/122 ( 86.1%)         | 24/40 ( 60.0%)          
repository.PostgresRecordHistoryRepository         | 30/30 (100.0%)           | 4/4 (100.0%)            
repository.PostgresCategoryRepository              | 51/51 (100.0%)           | 13/14 ( 92.9%)          
usecase.AutoStopExpiredRecordsUseCase              | 26/26 (100.0%)           | 6/6 (100.0%)            
usecase.RollbackRecordDescriptionUseCase           | 20/21 ( 95.2%)           | 9/12 ( 75.0%)           
usecase.RegisterUserUseCase                        | 23/27 ( 85.2%)           | 7/12 ( 58.3%)           
usecase.ManageUserCategoryUseCase                  | 19/22 ( 86.4%)           | 9/14 ( 64.3%)           
usecase.AdminDeleteBaseCategoryUseCase             | 13/14 ( 92.9%)           | 3/6 ( 50.0%)            
usecase.LoginUserUseCase                           | 12/12 (100.0%)           | 4/4 (100.0%)            
usecase.StartTimerUseCase                          | 14/14 (100.0%)           | 3/4 ( 75.0%)            
usecase.UpdateRecordDescriptionUseCase             | 18/19 ( 94.7%)           | 6/8 ( 75.0%)            
usecase.StopTimerUseCase                           | 10/12 ( 83.3%)           | 5/6 ( 83.3%)            
usecase.RollbackRecordHistoryUseCase               | 26/29 ( 89.7%)           | 7/12 ( 58.3%)           
usecase.AddManualRecordUseCase                     | 16/18 ( 88.9%)           | 9/12 ( 75.0%)           
usecase.AdminManageBaseCategoryUseCase             | 25/30 ( 83.3%)           | 12/20 ( 60.0%)          
usecase.DeleteCategoryUseCase                      | 8/8 (100.0%)             | 3/4 ( 75.0%)            
usecase.GetDescriptionHistoryUseCase               | 5/5 (100.0%)             | 2/2 (100.0%)            
usecase.DeleteRecordUseCase                        | 7/9 ( 77.8%)             | 6/6 (100.0%)            
usecase.UpdateTimeRecordUseCase                    | 41/46 ( 89.1%)           | 17/28 ( 60.7%)          
usecase.ManageUserSettingsUseCase                  | 8/8 (100.0%)             | 5/6 ( 83.3%)            
usecase.GetRecordHistoryUseCase                    | 8/10 ( 80.0%)            | 5/6 ( 83.3%)            
usecase.CreateTimeRecordUseCase                    | 28/29 ( 96.6%)           | 15/18 ( 83.3%)          
usecase.GetUserInfoUseCase                         | 3/3 (100.0%)             | N/A (no branches)       
usecase.GetCategoriesUseCase                       | 3/3 (100.0%)             | N/A (no branches)       
usecase.GetTimeRecordsUseCase                      | 3/3 (100.0%)             | N/A (no branches)       
---------------------------------------------------------------------------------------------------------
OVERALL TOTAL (Весь проект)                        | 764/869 ( 87.9%)         | 199/300 ( 66.3%)        
=========================================================================================================
```

Единый интерактивный HTML-отчет для всего проекта с детализацией по классам и подсветкой кода сформирован в:
- `build/reports/jacoco/root/html/index.html`

---

## 8. Интеграция с Allure Report

Для наглядной визуализации результатов прогона, истории и группировки по бизнес-требованиям в проект подключен фреймворк **Allure 2**:

### 8.1. Архитектура подключения
1. **Адаптер JUnit 4**:
   - В зависимости подключена библиотека `io.qameta.allure:allure-junit4:2.27.0`.
   - Регистрация слушателя выполнена в кастомном рандомизаторе `RandomOrderRunner`: при запуске каждого тестового класса `notifier.addListener(AllureJunit4())` перехватывает жизненный цикл тестов (старт, завершение, падение, игнорирование) и автоматически отключается в блоке `finally`.
   - Это гарантирует 100% совместимость с Gradle 9+ и отсутствие привязки к устаревшим внутренним API Gradle Conventions.
2. **Централизация результатов**:
   - Все тестовые модули (`:domain`, `:web:data`) настроены на единую директорию через системное свойство:
     `-Dallure.results.directory=<rootProject>/build/allure-results`.
3. **Генерация отчета через Allure CLI**:
   - В корневом `build.gradle.kts` настроена автоматическая загрузка и распаковка официального пакета `io.qameta.allure:allure-commandline:2.27.0` (конфигурация `allureCommandLine`).
   - Добавлены Gradle-таски:
     - `./gradlew allureReport`: компиляция и генерация единого статического HTML-отчета в `build/reports/allure-report`.
     - `./gradlew allureServe`: локальный запуск встроенного веб-сервера Allure для интерактивного просмотра в браузере.
     - `./gradlew allureOpen`: открытие ранее сгенерированного отчета `build/reports/allure-report`.

### 8.2. Визуализация и статусы Allure
Отчет Allure собирает метаданные и метрики прогона по всем 109 тестам проекта:
- **Suite Overview**: Полная карта выполнения тестов по пакетам и классам `:domain` и `:web:data`.
- **Execution Timeline**: Временная шкала прогона каждого отдельного тестового метода.
- **Randomization Verification**: Фиксация рандомизированного порядка выполнения методов через `RandomOrderRunner`.

В интерактивном отчете Allure доступна полная статистика со 100% успехом (109 тестов, 0 failed, 0 broken, 0 skipped).
