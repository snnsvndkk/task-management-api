# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Task management REST API with CRUD for users and tasks (`/api/users`, `/api/tasks`) and task filtering by status, priority and owner. See `README.md` for the endpoint reference.

## Architecture

Layered packages under `com.sinan.taskmanager`: `controller`, `service`, `repository`, `entity`, `dto`, `exception`.

- Services take and return entities, never DTOs. Controllers own the mapping: request records build entities with `toEntity()`, response records are built with `from(entity)`. Entities must never be returned from a controller.
- For tasks the owner is passed to the service as a separate `userId` argument; `TaskRequest.toEntity()` leaves `user` unset and the service resolves it.
- `TaskRequest.toEntity()` passes a null status/priority through deliberately: the service treats null as "use the default" on create and "keep the current value" on update.
- Task filtering goes through `TaskSpecifications.matching(...)` (JPA Specifications), not derived query methods; add new filters there.
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and funnels everything, including Spring MVC's built-in exceptions, into the `ApiError` record. Services signal failures with `ResourceNotFoundException` (404) and `DuplicateResourceException` (409).
- No Lombok: entities use hand-written accessors, DTOs are records.

## Stack

- Spring Boot 4.1.1, Java 25, Maven (wrapper pinned to Maven 3.9.16)
- Spring Web MVC, Spring Data JPA (Hibernate), Jakarta Bean Validation
- PostgreSQL (the only database driver on the classpath — no H2 or Testcontainers)

Spring Boot 4 uses modular starters, which is why the POM has `spring-boot-starter-webmvc` rather than `spring-boot-starter-web`, and a per-module test starter (`-webmvc-test`, `-data-jpa-test`, `-validation-test`) instead of a single `spring-boot-starter-test`. Follow that pattern when adding dependencies, and expect Boot 4 package names to differ from Boot 3 examples. JSON is handled by Jackson 3, whose classes live under `tools.jackson`, not `com.fasterxml.jackson`.

## Commands

Use the Maven wrapper. In PowerShell it is `.\mvnw.cmd`; in bash it is `./mvnw`.

```powershell
.\mvnw.cmd spring-boot:run          # run the API (port 8080)
.\mvnw.cmd test                     # run all tests
.\mvnw.cmd test "-Dtest=TaskServiceTest"                            # one test class
.\mvnw.cmd test "-Dtest=TaskServiceTest#getTaskReturnsExistingTask"  # one test method
.\mvnw.cmd clean package            # build the jar into target/
.\mvnw.cmd clean package -DskipTests
```

Quote `-D` arguments in PowerShell, otherwise it mangles values containing `.` or `#`.

No linter, formatter, or static analysis plugin is configured.

## Database and environment

`src/main/resources/application.properties` points at `jdbc:postgresql://localhost:5432/taskmanager` as user `postgres`, with the password read from the `DB_PASSWORD` environment variable. There is no default value, so the app fails at startup if it is unset:

```powershell
$env:DB_PASSWORD = "..."
```

The `taskmanager` database must already exist; nothing creates it.

## Tests

`mvnw test` must keep passing with no database and no `DB_PASSWORD`. Service tests are plain Mockito (`@ExtendWith(MockitoExtension.class)`); controller tests are `@WebMvcTest` slices with the service replaced by `@MockitoBean`. Boot 4 moved `WebMvcTest` to `org.springframework.boot.webmvc.test.autoconfigure`.

The only test that touches PostgreSQL is `TaskmanagerApplicationTests.contextLoads`, a `@SpringBootTest` against the real properties (there is no `src/test/resources` override or test profile). It is gated with `@EnabledIfEnvironmentVariable` on `DB_PASSWORD` and skipped when that is unset.

## Schema

Schema is managed by Hibernate with `spring.jpa.hibernate.ddl-auto=update` — there is no Flyway or Liquibase. Entity changes alter the local schema on next startup, and `update` never drops or renames columns, so removed or renamed fields leave stale columns behind. `spring.jpa.show-sql=true` logs every SQL statement to the console.
