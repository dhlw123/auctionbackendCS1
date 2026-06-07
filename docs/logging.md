# Logging System

## Overview

The auction backend uses **SLF4J** as the logging facade with **Logback** as the underlying implementation. Logback is provided transitively by Spring Boot and requires no additional dependencies.

Logs are emitted to two destinations simultaneously:
- **Console** — colorized output for development
- **File** — `logs/auction.log` with daily rotation and 7-day retention

## Configuration

### `src/main/resources/logback-spring.xml`

The Logback configuration defines two appenders and reads log levels from Spring properties:

| Element | Purpose |
|---------|---------|
| `springProperty rootLevel` | Mirrors `logging.level.root` from `application.properties` (default: `WARN`) |
| `springProperty auctionLevel` | Mirrors `logging.level.com.auction` from `application.properties` (default: `INFO`) |
| `CONSOLE` appender | Colorized output with pattern `HH:mm:ss.SSS [thread] LEVEL logger - message` |
| `FILE` appender | Rolling file at `logs/auction.log`, rotates daily, keeps 7 days of history |
| Root logger | Route to both CONSOLE and FILE |
| `com.auction` logger | Inherits from root but allows independent level control |

### `application.properties`

```
logging.level.root=WARN
logging.level.com.auction=INFO
```

- **root=WARN** — third-party libraries (Spring, Hibernate, Tomcat) are quiet unless something goes wrong
- **com.auction=INFO** — all application code logs at INFO and above

To temporarily enable debug logging for all application code:

```properties
logging.level.com.auction=DEBUG
```

To enable Hibernate SQL logging for debugging:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### `.gitignore`

The `logs/` directory is ignored — log files are never committed to version control.

## Instrumented Files

Every instrumented class follows the same pattern:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);
```

### Application Startup

**File:** `AuctionApplication.java`

| Event | Level | Message |
|-------|-------|---------|
| Application starting | INFO | `Starting AuctionApplication` |

### Authentication

**File:** `AuthService.java`

| Event | Level | Message |
|-------|-------|---------|
| User registered | INFO | `User registered: {username}` |
| Login success | INFO | `User logged in: {username}` |
| Login — banned user | WARN | `Banned user attempted login: {username}` |
| Login — wrong password | WARN | `Failed login attempt for user: {username}` |
| Logout | INFO | `User logged out: {username}` |

### JWT Token Processing

**File:** `JwtUtil.java`

| Event | Level | Message |
|-------|-------|---------|
| Token validation failure | WARN | `JWT validation error: {error detail}` |

This covers expired tokens, malformed tokens, and signature mismatches.

### Domain Logic

**File:** `User.java`

| Event | Level | Message |
|-------|-------|---------|
| Negative balance add | WARN | `Must not add negative value` |
| Negative balance deduct | WARN | `Must not deduct negative value` |

### Exception Handling

**File:** `GlobalExceptionHandler.java`

Every exception caught by the global handler is logged before the HTTP response is sent:

| Exception type | HTTP | Level | Message |
|----------------|------|-------|---------|
| `MethodArgumentNotValidException` | 400 | WARN | `Validation failed: {field → message map}` |
| `BaseException` | 400 | WARN | `Business error: {message}` |
| `EntityNotFoundException` | 404 | INFO | `Entity not found: {message}` |
| `JwtExpiredException` | 498 | WARN | `JWT expired or invalid token used` |

Exceptions not matched by any handler fall through to Spring Boot's default error handling (returns appropriate HTTP status codes automatically).

## Log Output Examples

### Console output

```
15:06:46.920 [http-nio-8080-exec-2] INFO  c.a.AuctionApplication - Starting AuctionApplication
15:06:47.123 [http-nio-8080-exec-3] INFO  c.a.auth.AuthService - User registered: alice
15:06:47.456 [http-nio-8080-exec-4] INFO  c.a.auth.AuthService - User logged in: bob
15:06:47.789 [http-nio-8080-exec-5] WARN  c.a.auth.AuthService - Failed login attempt for user: eve
15:06:48.012 [http-nio-8080-exec-6] WARN  c.a.auth.jwtools.JwtUtil - JWT validation error: JWT expired
15:06:48.234 [http-nio-8080-exec-7] WARN  c.a.common.GlobalExceptionHandler - Business error: User not found
15:07:01.567 [http-nio-8080-exec-8] WARN  c.a.common.GlobalExceptionHandler - Validation failed: {password=size must be between 1 and 72}
15:07:15.890 [http-nio-8080-exec-9] WARN  c.a.users.User - Must not deduct negative value
```

### File output (`logs/auction.log`)

The file appender uses a plain (non-colorized) format:

```
2026-06-07 15:06:46.920 [http-nio-0.0.0.0-8080-exec-2] INFO  c.a.AuctionApplication - Starting AuctionApplication
2026-06-07 15:06:47.123 [http-nio-0.0.0.0-8080-exec-3] INFO  c.a.auth.AuthService - User registered: alice
```

Daily rotation produces files like:

```
logs/auction.log               ← current
logs/auction.2026-06-06.log    ← yesterday
logs/auction.2026-06-05.log    ← 2 days ago
...
```

Files older than 7 days are automatically deleted.

## Log Levels

| Level | Usage |
|-------|-------|
| **ERROR** | Reserved for unexpected failures (none currently — unhandled exceptions are logged by Spring Boot's default error handling) |
| **WARN** | Recoverable failures: bad credentials, invalid tokens, validation errors, business rule violations |
| **INFO** | Normal operational events: startup, user registration, login, logout |
| **DEBUG** | Not used by default — enable via `logging.level.com.auction=DEBUG` for detailed tracing |

## Design Decisions

1. **SLF4J over direct Logback** — SLF4J is the standard Java logging facade. If the logging backend ever changes, no code changes are needed.

2. **No request/response filter** — kept minimal to avoid log noise. Request failures are visible through exception handler logs.

3. **No catch-all Exception handler** — Spring Boot's built-in `BasicErrorController` handles unmapped exceptions and returns proper HTTP status codes (400 for malformed requests, 405 for wrong methods, etc.). Adding a blanket `@ExceptionHandler(Exception.class)` would override this and incorrectly turn framework-level exceptions into 500 errors.

4. **File logging optional via config** — the file appender is always present in the config but can be removed or reconfigured without touching Java code.

5. **Level control via properties** — log levels can be changed at runtime by editing `application.properties` and restarting, or through Spring Boot's actuator endpoints if `spring-boot-starter-actuator` is added in the future.
