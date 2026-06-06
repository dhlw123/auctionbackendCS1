# Auction API Test Suite

Black-box integration tests for the Auction Backend, written in Python using `pytest` + `requests`. Tests run against a live Spring Boot server via HTTP and exercise every endpoint through boundary, security, and lifecycle scenarios.

---

## Quick Start

```bash
# 1. Start the Spring Boot server (if not already running)
./gradlew bootRun

# 2. Run the API tests (auto-creates venv, installs deps, seeds admin)
ADMIN_PASSWORD=admin ./scripts/run_api_tests.sh

# 3. Run a subset
ADMIN_PASSWORD=admin ./scripts/run_api_tests.sh -k "test_admin"

# 4. Run without starting server (server already on localhost:8080)
EXTERNAL_SERVER=1 ADMIN_PASSWORD=admin ./scripts/run_api_tests.sh
```

---

## Architecture

```
tests_api/
├── __init__.py
├── requirements.txt        # pytest, requests
├── constants.py            # BASE_URL, ADMIN creds, timeouts
├── models.py               # Typed dataclasses: BaseResponse, ApiError, ItemEntity, ...
├── client.py               # HTTP layer: AuctionClient, AdminSession, UserSession
├── conftest.py             # Fixtures: client, admin, fresh_user, second_user
│
├── test_smoke.py           #  3 tests — server connectivity
├── test_auth.py            # 14 tests — register, login, refresh, logout
├── test_admin.py           # 37 tests — ★ ban, unban, cancelAuction boundary
├── test_items.py           #  8 tests — publish, get, self-cancel
└── test_security.py        #  5 tests — role escalation, auth bypass
```

### Design

| Layer | Role |
|-------|------|
| `AuctionClient` | Wraps `requests.Session` with connection pooling. Injects `Authorization: Bearer` headers. Parses JSON into typed models. |
| `AuthSession` | Holds access/refresh tokens for one logged-in user. Provides `.refresh()` and `.logout()`. |
| `UserSession(AuthSession)` | Regular user: `.publish_item()`, `.bid()`, `.buy_now()`, `.cancel_item()`, `.deposit()`. |
| `AdminSession(AuthSession)` | Admin user: `.ban_user()`, `.unban_user()`, `.cancel_auction()`. |
| `models.ApiError` | Raised on any non-2xx response (unless `raw=True`). Carries `status_code`, `message`, `body`. |

### Fixture Lifecycle

```
conftest.py
├── client           [session]    → single AuctionClient, reused across all tests
├── admin            [session]    → registers admin/admin if not exists, logs in once
├── fresh_user       [function]   → new unique user per test, auto-banned on teardown
└── second_user      [function]   → second unique user per test (for bidder vs seller)
```

---

## Test Catalog (67 tests)

### 1. Smoke Tests — `test_smoke.py`

Verify the server is running and public endpoints respond.

| # | Test | What It Checks |
|---|------|---------------|
| 1 | `test_server_responds` | TCP connection to `API_BASE_URL` succeeds |
| 2 | `test_public_items_endpoint` | `GET /items/all` returns 200 or 404 |
| 3 | `test_public_status_endpoint_requires_valid_id` | `GET /item/status/1` does not crash |

### 2. Auth Tests — `test_auth.py`

#### Registration (`POST /register`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 4 | `test_register_new_user` | Unique username, displayName, password | 200, `status:true` |
| 5 | `test_register_duplicate_username` | Same username twice | 400 |
| 6 | `test_register_empty_username` | `username:""` | 400 |
| 7 | `test_register_empty_password` | `password:""` | 400 |
| 8 | `test_register_with_spaces_in_username` | `username:"user name"` | 400 |

#### Login (`POST /login`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 9 | `test_login_valid_user` | Correct credentials | 200, valid JWT returned |
| 10 | `test_login_wrong_password` | Valid username, wrong password | 400 |
| 11 | `test_login_nonexistent_user` | Username not in DB | 400 |
| 12 | `test_login_empty_username` | `username:""` | 400 |
| 13 | `test_login_empty_password` | `password:""` | 400 |

#### Token Refresh (`POST /refresh`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 14 | `test_refresh_token` | Valid refresh token | 200, new access + refresh tokens |
| 15 | `test_refresh_with_invalid_token` | Garbage string | 400 or 498 |

#### Logout (`POST /logout`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 16 | `test_logout_invalidates_refresh` | Valid user logs out, then refreshes | Logout succeeds; notes whether refresh token is invalidated† |

† Spring Security's default `LogoutFilter` intercepts `POST /logout` before `AuthController`, so the application's `logoutUser()` may not run. This is a known limitation — see the comment in the test assertion.

### 3. Admin Tests — `test_admin.py`

#### 3a. POST `/admin/ban` — Authentication

| # | Test | Input | Expected |
|---|------|-------|----------|
| 17 | `test_ban_without_auth_returns_403` | No `Authorization` header | 403 |
| 18 | `test_ban_with_invalid_token` | `Bearer invalid.jwt.token` | 401 or 498 |
| 19 | `test_ban_as_regular_user_returns_403` | Valid user JWT (no ROLE_ADMIN) | 403 |

#### 3b. POST `/admin/ban` — Validation

| # | Test | Input | Expected |
|---|------|-------|----------|
| 20 | `test_ban_empty_body` | `username:""` | 400 |
| 21 | `test_ban_missing_username_field` | `{}` | 400 |
| 22 | `test_ban_whitespace_only_username` | `username:"   "` | 400 |
| 23 | `test_ban_non_string_username` | `username:12345` (integer) | 400 or 500 |

#### 3c. POST `/admin/ban` — Boundary & Logic

| # | Test | Input | Expected |
|---|------|-------|----------|
| 24 | `test_ban_self_admin_is_rejected` | `username:"admin"` | 400, `"can't ban admin"` |
| 25 | `test_ban_mixed_case_admin_passes_guard` | `username:"Admin"` | 400 `"User not found"` (passes string check, fails lookup) |
| 26 | `test_ban_nonexistent_user` | Random username | 400, `"User not found"` |
| 27 | `test_ban_valid_user_succeeds` | Valid user | 200, `"successfully banned user"` |
| 28 | `test_ban_already_banned_user_DOUBLE_TAP` | Same user banned twice | Reports actual server behavior (200, 400, or 500) |
| 29 | `test_banned_user_cannot_login` | Try login as banned user | 400, `"User was banned"` |
| 30 | `test_banned_user_stale_jwt_is_rejected` | Use pre-ban JWT after ban | 401, 498, or 403 |
| 31 | `test_ban_very_short_username` | `username:"x"` | 400 |
| 32 | `test_ban_very_long_username` | 200-char username | 400 |

#### 3d. POST `/admin/unban` — Authentication

| # | Test | Input | Expected |
|---|------|-------|----------|
| 33 | `test_unban_without_auth_returns_401` | No token | 401 or 403 |
| 34 | `test_unban_as_regular_user_returns_403` | Valid user JWT | 403 |

#### 3e. POST `/admin/unban` — Validation

| # | Test | Input | Expected |
|---|------|-------|----------|
| 35 | `test_unban_empty_body` | `{}` | 400 |
| 36 | `test_unban_missing_password` | Only username | 400 |
| 37 | `test_unban_missing_username` | Only password | 400 |
| 38 | `test_unban_empty_password` | `password:""` | 400 |
| 39 | `test_unban_empty_username` | `username:""` | 400 |

#### 3f. POST `/admin/unban` — Boundary & Logic

| # | Test | Input | Expected |
|---|------|-------|----------|
| 40 | `test_unban_nonexistent_user` | Random username | 400, `"User not found"` |
| 41 | `test_unban_never_banned_user_is_idempotent` | Unban user who wasn't banned | 200 (deleteById is no-op) |
| 42 | `test_ban_then_unban_then_login_full_cycle` | Ban → unban → login with new password | Full lifecycle succeeds |
| 43 | `test_unban_idempotent_twice` | Unban same user twice | Both return 200 |
| 44 | `test_stale_token_after_unban` | Use pre-ban JWT after unban | 200, 401, 498, or 403† |
| 45 | `test_unban_long_password` | 500-char password | Reports actual behavior (200, 400, 403, or 413) |

† Known issue: `unbanUser` deletes the `RevokedToken` DB record, so old valid JWTs survive unban.

#### 3g. POST `/admin/cancel/{itemId}` — Authentication

| # | Test | Input | Expected |
|---|------|-------|----------|
| 46 | `test_cancel_without_auth_returns_401` | No token | 401 or 403 |
| 47 | `test_cancel_as_regular_user_returns_403` | Valid user JWT | 403 |

#### 3h. POST `/admin/cancel/{itemId}` — Validation

| # | Test | Input | Expected |
|---|------|-------|----------|
| 48 | `test_cancel_nonexistent_item` | `itemId=99999` | 400, `"no such Item"` |
| 49 | `test_cancel_invalid_item_ids[0]` | `itemId=0` | 400 |
| 50 | `test_cancel_invalid_item_ids[-1]` | `itemId=-1` | 400 |
| 51 | `test_cancel_invalid_item_ids[9223372036854775807]` | Max Long value | 400 |

#### 3i. POST `/admin/cancel/{itemId}` — Boundary & Logic

| # | Test | Input | Expected |
|---|------|-------|----------|
| 52 | `test_cancel_active_item_succeeds` | Publish item, admin cancels | 200, status → `CANCELED` |
| 53 | `test_cancel_already_canceled_item` | Cancel same item twice | 400, `"Only ACTIVE"` |
| 54 | `test_cancel_then_bid_is_rejected` | Cancel item, then bid | 400 |
| 55 | `test_cancel_with_bidder_refund` | User bids, admin cancels | Bidder's balance restored |

### 4. Item Tests — `test_items.py`

#### Publish (`POST /items`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 56 | `test_publish_valid_item` | Valid item data | 200, item with `itemId` |
| 57 | `test_publish_empty_title` | `title:""` | 400 |
| 58 | `test_publish_negative_price` | `startingPrice:-10` | 400 |
| 59 | `test_publish_without_auth` | No token | 401 or 403 |

#### Get (`GET /items/{id}`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 60 | `test_get_existing_item` | Published item ID | 200, correct title |
| 61 | `test_get_nonexistent_item` | `itemId=99999` | 400 |

#### Cancel by Owner (`POST /items/cancel/{itemId}`)

| # | Test | Input | Expected |
|---|------|-------|----------|
| 62 | `test_cancel_own_item` | Owner cancels own item | 200 |
| 63 | `test_cancel_already_canceled_own_item` | Cancel twice | 400 |

### 5. Security Tests — `test_security.py`

| # | Test | What It Checks |
|---|------|---------------|
| 64 | `test_regular_user_cannot_access_admin_endpoints` | User JWT on `/admin/ban`, `/admin/unban`, `/admin/cancel/1` → all 403 |
| 65 | `test_user_cannot_cancel_others_item` | User A tries to cancel User B's item → 400 `"not the owner"` |
| 66 | `test_admin_can_access_admin_endpoints` | Admin JWT on `/admin/ban` → proceeds (400 on nonexistent user) |
| 67 | `test_admin_can_access_user_endpoints` | Admin JWT on `/users/me/balance` → 200 or 400 |

---

## Configuration

All via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `API_BASE_URL` | `http://localhost:8080` | Server URL |
| `ADMIN_USERNAME` | `admin` | Admin login username |
| `ADMIN_PASSWORD` | `admin` | Admin login password |
| `START_SERVER` | `1` | Shell script starts the server (`0` to skip) |
| `EXTERNAL_SERVER` | `0` | Use existing server; don't start one (`1` to enable) |

---

## Extending

### Adding a new endpoint test

1. Add typed models to `models.py` if the endpoint returns a new shape.
2. Add a method to `UserSession` or `AdminSession` in `client.py`.
3. Write test functions in the appropriate `test_*.py` file.

### Adding a new fixture

Add to `conftest.py`. The existing pattern:
- Use `uuid.uuid4().hex[:12]` for unique usernames.
- Clean up in teardown (ban the test user via `admin.ban_user()`).
- Choose the right scope: `session` for shared state, `function` for per-test isolation.
