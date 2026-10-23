# Ktor Tasks API

Учебный REST API на Kotlin + Ktor. Проект демонстрирует Swagger UI/OpenAPI, документирование маршрутов, автоматические тесты, централизованную обработку ошибок через `StatusPages` и логирование запросов через `CallLogging`.

## Требования

- JDK 17 или новее
- Gradle Wrapper (если wrapper отсутствует, установите Gradle 8.14+ или создайте wrapper командой `gradle wrapper`)

## Запуск

```bash
.\gradlew.bat run
```

Сервис запускается на `http://localhost:8080`.

- Swagger UI: http://localhost:8080/swagger
- OpenAPI YAML: http://localhost:8080/openapi.yaml
- Health check: http://localhost:8080/health

Swagger UI загружает библиотеку с unpkg CDN, поэтому для отображения интерфейса требуется интернет-соединение. Спецификация OpenAPI хранится локально в `src/main/resources/openapi.yaml`.

## Тестирование

```bash
.\gradlew.bat test
```

Если в проект добавлен Gradle Wrapper, используйте `./gradlew test` (Linux/macOS) или `gradlew.bat test` (Windows).

Тесты проверяют health endpoint, создание и получение задач, валидацию, ответ 404, удаление, а также доступность Swagger UI и OpenAPI.

## Маршруты

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/` | Информация об API |
| GET | `/health` | Проверка состояния |
| GET | `/tasks` | Список задач |
| POST | `/tasks` | Создать задачу |
| GET | `/tasks/{id}` | Получить задачу |
| DELETE | `/tasks/{id}` | Удалить задачу |
| GET | `/swagger` | Swagger UI |
| GET | `/openapi.yaml` | OpenAPI спецификация |

## Примеры запросов

Создать задачу:

```bash
curl -i -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Подготовить проект"}'
```

Получить список:

```bash
curl http://localhost:8080/tasks
```

Получить задачу по ID:

```bash
curl http://localhost:8080/tasks/1
```

Удалить задачу:

```bash
curl -i -X DELETE http://localhost:8080/tasks/1
```

## Централизованные ошибки и логирование

`StatusPages` возвращает единый JSON-формат:

```json
{
  "code": "bad_request",
  "message": "title must not be blank"
}
```

`CallLogging` записывает входящие вызовы в журнал приложения. Не логируйте токены, пароли и чувствительные данные.

## Git

```bash
git init
git add .
git commit -m "Initial Ktor API with Swagger, tests and error handling"
git branch -M main
git remote add origin <URL-ВАШЕГО-РЕПОЗИТОРИЯ>
git push -u origin main
```

## Примечания

- Хранилище задач находится в памяти и очищается при перезапуске сервиса.
- Это стартовый учебный проект, а не production-конфигурация.
- Для воспроизводимой сборки рекомендуется добавить Gradle Wrapper и закоммитить его в Git.
