# Task Management API

A REST API for managing users and their tasks, built with Spring Boot and PostgreSQL.

## Features

- CRUD for users and tasks, with a one-to-many relationship between them
- Task filtering by status, priority and owner
- Request validation with Jakarta Bean Validation
- Consistent JSON error responses from a single `@RestControllerAdvice`
- DTOs at the HTTP boundary, so JPA entities are never serialized directly
- Unit tests for the service layer (Mockito) and the web layer (`@WebMvcTest`) that run without a database

## Tech stack

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Persistence | Hibernate, PostgreSQL 18 |
| Build | Maven (wrapper included) |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc |

## Project structure

```
src/main/java/com/sinan/taskmanager
├── controller   REST endpoints; map DTOs to and from entities
├── service      business rules and transactions
├── repository   Spring Data JPA repositories and task filter specifications
├── entity       JPA entities and enums
├── dto          request/response records and the error body
└── exception    domain exceptions and the global exception handler
```

## Getting started

### Prerequisites

- JDK 25 or newer
- PostgreSQL running on `localhost:5432`

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE taskmanager;"
```

Tables are created automatically on first startup (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Set the database password

The application connects as user `postgres` and reads the password from the `DB_PASSWORD` environment variable. It is never stored in the repository.

PowerShell:

```powershell
$env:DB_PASSWORD = "your-postgres-password"
```

bash:

```bash
export DB_PASSWORD="your-postgres-password"
```

### 3. Run the application

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

bash:

```bash
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080`.

## Running the tests

```powershell
.\mvnw.cmd test
```

The service and controller tests use mocks, so they need neither PostgreSQL nor `DB_PASSWORD`. The one exception is `TaskmanagerApplicationTests`, a smoke test that boots the whole application against the real database: it runs only when `DB_PASSWORD` is set and is skipped otherwise.

## API reference

### Users

| Method | Path | Description | Success |
|---|---|---|---|
| `POST` | `/api/users` | Create a user | `201 Created` |
| `GET` | `/api/users` | List all users | `200 OK` |
| `GET` | `/api/users/{id}` | Get a user | `200 OK` |
| `PUT` | `/api/users/{id}` | Update a user | `200 OK` |
| `DELETE` | `/api/users/{id}` | Delete a user and all of their tasks | `204 No Content` |

Request body for `POST` and `PUT`:

| Field | Type | Rules |
|---|---|---|
| `name` | string | required, max 100 characters |
| `email` | string | required, valid email, unique (case-insensitive) |

### Tasks

| Method | Path | Description | Success |
|---|---|---|---|
| `POST` | `/api/tasks` | Create a task | `201 Created` |
| `GET` | `/api/tasks` | List tasks, optionally filtered | `200 OK` |
| `GET` | `/api/tasks/{id}` | Get a task | `200 OK` |
| `PUT` | `/api/tasks/{id}` | Update a task | `200 OK` |
| `DELETE` | `/api/tasks/{id}` | Delete a task | `204 No Content` |

Request body for `POST` and `PUT`:

| Field | Type | Rules |
|---|---|---|
| `title` | string | required, max 150 characters |
| `description` | string | optional, max 2000 characters |
| `status` | `TODO` \| `IN_PROGRESS` \| `DONE` | optional; defaults to `TODO` on create, unchanged on update |
| `priority` | `LOW` \| `MEDIUM` \| `HIGH` | optional; defaults to `MEDIUM` on create, unchanged on update |
| `dueDate` | date (`YYYY-MM-DD`) | optional |
| `userId` | number | required; must be an existing user |

Query parameters for `GET /api/tasks` (all optional, combined with AND):

| Parameter | Values |
|---|---|
| `status` | `TODO`, `IN_PROGRESS`, `DONE` |
| `priority` | `LOW`, `MEDIUM`, `HIGH` |
| `userId` | id of the owning user |

## Example requests

The examples use `curl` from bash. In Windows PowerShell, call `curl.exe` instead of `curl`, or use `Invoke-RestMethod` as shown at the end.

### Create a user

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Ada Lovelace", "email": "ada@example.com"}'
```

```http
HTTP/1.1 201
Location: http://localhost:8080/api/users/1
Content-Type: application/json
```

```json
{
  "id": 1,
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "createdAt": "2026-10-07T09:15:30.123456Z"
}
```

### Create a task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Write quarterly report",
        "description": "Summarize Q3 results",
        "priority": "HIGH",
        "dueDate": "2026-12-31",
        "userId": 1
      }'
```

```json
{
  "id": 1,
  "title": "Write quarterly report",
  "description": "Summarize Q3 results",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-12-31",
  "createdAt": "2026-10-07T09:16:02.511204Z",
  "updatedAt": "2026-10-07T09:16:02.511204Z",
  "userId": 1
}
```

### List and filter tasks

```bash
curl http://localhost:8080/api/tasks
curl "http://localhost:8080/api/tasks?status=TODO"
curl "http://localhost:8080/api/tasks?priority=HIGH"
curl "http://localhost:8080/api/tasks?status=IN_PROGRESS&priority=HIGH&userId=1"
```

### Update a task

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Write quarterly report",
        "description": "Summarize Q3 results",
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "dueDate": "2026-12-31",
        "userId": 1
      }'
```

### Get and delete

```bash
curl http://localhost:8080/api/users/1
curl http://localhost:8080/api/tasks/1
curl -i -X DELETE http://localhost:8080/api/tasks/1
curl -i -X DELETE http://localhost:8080/api/users/1
```

### PowerShell

```powershell
$body = @{ name = "Ada Lovelace"; email = "ada@example.com" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/users -ContentType "application/json" -Body $body
```

## Error responses

Every error has the same shape:

```json
{
  "timestamp": "2026-10-07T09:20:11.482913Z",
  "status": 404,
  "error": "Not Found",
  "message": "Task with id 42 not found",
  "path": "/api/tasks/42",
  "fieldErrors": {}
}
```

`fieldErrors` is filled in for validation failures and empty otherwise:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": ""}'
```

```json
{
  "timestamp": "2026-10-07T09:21:45.027331Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": {
    "title": "Title is required",
    "userId": "User id is required"
  }
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure, malformed JSON, unknown enum value, invalid date, or a non-numeric id |
| `404 Not Found` | The user or task does not exist, or the path is unknown |
| `405 Method Not Allowed` | The HTTP method is not supported on that path |
| `409 Conflict` | The email is already used by another user |
| `500 Internal Server Error` | Unexpected failure; details are logged, not returned |

Values of the wrong type name the offending field and say what was expected:

```json
{
  "timestamp": "2026-10-07T09:22:30.118054Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value '2026-13-45' for field 'dueDate'. Expected a date in the format YYYY-MM-DD",
  "path": "/api/tasks",
  "fieldErrors": {}
}
```

## License

Released under the [MIT License](LICENSE).
