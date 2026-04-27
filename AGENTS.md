# AGENTS.md

Single-module Gradle Spring Boot auction backend.

## Build & run

```bash
./gradlew bootRun          # dev server on http://localhost:8080
./gradlew test             # only AuctionApplicationTests exists
./gradlew build
```

- Java 26 toolchain (see `build.gradle`).
- Spring Boot 4.0.5, Gradle 9.4.1 wrapper.
- Unusual test starters: `spring-boot-starter-webmvc-test` and `spring-boot-starter-websocket-test` instead of the usual `spring-boot-starter-test`.

## Database

- **SQLite** file DB: `auctiondb.db` in project root (gitignored).
- Dialect: `org.hibernate.community.dialect.SQLiteDialect` (`hibernate-community-dialects`).
- `spring.jpa.hibernate.ddl-auto=update` manages schema automatically.
- `spring.datasource.hikari.maximum-pool-size=1` is required for SQLite.
- `src/main/resources/schema.sql` exists but is **not** auto-executed by Spring Boot (no `spring.sql.init.mode=always`). Treat it as reference/backup only.

## Architecture

Layered: `Controller -> Service -> Repository`.

| Package | Responsibility |
|---------|----------------|
| `users` | Auth (register/login), JWT generation, `User` entity |
| `items` | Item publishing, retrieval, deletion |
| `bids/bidsdetails` | Placing bids |
| `itemstatus` | Tracks current price, highest bidder, end time, increments |
| `security` | JWT filter (`JwtSecurityFilter`), util, user details |
| `config` | `SecurityConfig`, `SwaggerCustomConfig` |
| `common` | `BaseResponse`, `BaseEntity`, shared validators |

Notable:
- `AuctionApplication` is also a `@RestController` with a `/hello` endpoint.
- `BaseEntity` (UUID) exists but is **not** extended by any current entity; `User` uses `String` PK, `Item`/`Bid`/`ItemStatus` use `Long` identity.
- `ItemStatus` uses `@MapsId` to share PK with `Item`.

## Security

JWT bearer token. Public endpoints (no auth):
- `POST /users/register`
- `POST /users/login`
- Swagger: `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`

All other endpoints require `Authorization: Bearer <token>`.

## API docs

Swagger UI available at `/swagger-ui.html` (springdoc-openapi 3.0.3). Controllers use `@SecurityRequirement(name = "bearerAuth")`.

## Formatting

- Eclipse formatter config: `eclipse-formatter.xml` (2-space indent, 120-char line). Referenced by `.vscode/settings.json`.
- `eclipse-formatter.xml` is gitignored; changes to it won't be committed unless removed from `.gitignore`.

## Testing

Test suite is minimal (only `AuctionApplicationTests` with a context-load smoke test). Add new tests under `src/test/java/com/auction/...`.
