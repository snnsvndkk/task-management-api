# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Task management REST API. Currently a bare Spring Initializr scaffold: only `TaskmanagerApplication` and a `contextLoads` test exist. No entities, repositories, services, or controllers have been written yet, so there are no established package or layering conventions to follow — new code goes under the base package `com.sinan.taskmanager` (component scanning starts there).

## Stack

- Spring Boot 4.1.1, Java 25, Maven (wrapper pinned to Maven 3.9.16)
- Spring Web MVC, Spring Data JPA (Hibernate), Jakarta Bean Validation
- PostgreSQL (the only database driver on the classpath — no H2 or Testcontainers)

Spring Boot 4 uses modular starters, which is why the POM has `spring-boot-starter-webmvc` rather than `spring-boot-starter-web`, and a per-module test starter (`-webmvc-test`, `-data-jpa-test`, `-validation-test`) instead of a single `spring-boot-starter-test`. Follow that pattern when adding dependencies, and expect Boot 4 package names to differ from Boot 3 examples.

## Commands

Use the Maven wrapper. In PowerShell it is `.\mvnw.cmd`; in bash it is `./mvnw`.

```powershell
.\mvnw.cmd spring-boot:run          # run the API (port 8080)
.\mvnw.cmd test                     # run all tests
.\mvnw.cmd test "-Dtest=TaskmanagerApplicationTests"                # one test class
.\mvnw.cmd test "-Dtest=TaskmanagerApplicationTests#contextLoads"   # one test method
.\mvnw.cmd clean package            # build the jar into target/
.\mvnw.cmd clean package -DskipTests
```

Quote `-D` arguments in PowerShell, otherwise it mangles values containing `.` or `#`.

No linter, formatter, or static analysis plugin is configured.

## Database and environment

`src/main/resources/application.properties` points at `jdbc:postgresql://localhost:5432/taskmanager` as user `postgres`, with the password read from the `DB_PASSWORD` environment variable. There is no default value, so both the app and the tests fail at startup if it is unset:

```powershell
$env:DB_PASSWORD = "..."
```

The `taskmanager` database must already exist; nothing creates it.

Tests are not isolated from this setup. `@SpringBootTest` loads the full context against the same properties (there is no `src/test/resources` override or test profile), so `mvnw test` needs a running local PostgreSQL and `DB_PASSWORD` set.

Schema is managed by Hibernate with `spring.jpa.hibernate.ddl-auto=update` — there is no Flyway or Liquibase. Entity changes alter the local schema on next startup, and `update` never drops or renames columns, so removed or renamed fields leave stale columns behind. `spring.jpa.show-sql=true` logs every SQL statement to the console.
