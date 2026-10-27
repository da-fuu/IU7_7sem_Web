# Лабораторная работа №2: интеграционное и E2E-тестирование

Интеграционные тесты в `web/data/src/test/kotlin/com/ppo/web/it` проверяют совместную работу сценариев `domain` и PostgreSQL-репозиториев. Тест пользовательских сценариев использует тестовый хешер, а не реализацию хеширования backend. Unit-тесты первой лабораторной и новые тесты запускаются раздельными этапами.

E2E-тест `MvpScenarioITCase` отправляет HTTP-запросы в отдельный экземпляр backend и проверяет регистрацию, настройки, категорию, запись времени и её историю. `scripts/run_e2e_scenario.py` воспроизводит сценарий для захвата трафика; `scripts/capture_e2e_traffic.sh` требует запущенный тестовый backend и доступ к tshark или tcpdump. Порт можно узнать командой `docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml port backend-test 8080` и передать как `TARGET_PORT`. Не публикуйте дамп: в нём могут содержаться токены и запросы.

GitHub Actions запускает тестовый контейнер в порядке unit → integration → E2E. Результаты Allure собираются даже при ошибке; не запущенные этапы обозначаются отдельными записями skipped. История сохраняется в кэше между запусками. Compose создаёт отдельный экземпляр PostgreSQL с начальной схемой и данными; `down -v` удаляет его после выполнения, включая ошибку. При принудительном завершении CI очистку может потребоваться выполнить вручную.

Для локальной изоляции указывайте разное имя проекта для каждого запуска и отдельный checkout: контейнеры используют исходники и каталоги сборки через bind mount. Перед запуском задайте `export TEST_UID=$(id -u) TEST_GID=$(id -g)`. Например:

```sh
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml build test-runner
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml run --rm test-runner ./gradlew test :web:backend:jar
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml run --rm test-runner ./gradlew integrationTest
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml up -d --build backend-test
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml run --rm test-runner ./gradlew e2eTest
docker compose -p lab2-alice -f deploy/test/docker-compose.test.yml down -v --remove-orphans
```

После запуска backend необходимо дождаться готовности `/health`, как это делает CI. Для повторного запуска интеграционных тестов используйте `--rerun-tasks`; для первоначального состояния базы удалите стенд и создайте заново. Проверка одновременных запусков и результаты CI требуют отдельного подтверждения на реальном окружении.

## Проверки при ревизии

27 октября 2026 года прошли `./gradlew test --offline`, два последовательных запуска `integrationTest` против отдельного PostgreSQL в Docker и `e2eTest` по HTTP к контейнеру backend. Стенд после проверки удалён. Проверка GitHub Actions, одновременной работы двух независимых checkout и сетевого захвата в этой ревизии не выполнялась.
