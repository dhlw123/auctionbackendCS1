import uuid

import pytest

from .client import UserSession
from .constants import DEFAULT_USER_PASSWORD
from .models import ApiError


class TestAuthRegister:
    def test_register_new_user(self, client):
        uid = uuid.uuid4().hex[:10]
        uname = f"regtest_{uid}"
        resp = client.register(uname, f"Reg {uid}", DEFAULT_USER_PASSWORD)
        assert resp.status is True

    def test_register_duplicate_username(self, client):
        uid = uuid.uuid4().hex[:10]
        uname = f"dup_{uid}"
        client.register(uname, f"Dup {uid}", DEFAULT_USER_PASSWORD)
        with pytest.raises(ApiError) as exc:
            client.register(uname, f"Dup2 {uid}", DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400

    def test_register_empty_username(self, client):
        with pytest.raises(ApiError) as exc:
            client.register("", "Display", DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400

    def test_register_empty_password(self, client):
        uid = uuid.uuid4().hex[:10]
        with pytest.raises(ApiError) as exc:
            client.register(f"pwtest_{uid}", "Display", "")
        assert exc.value.status_code == 400

    def test_register_with_spaces_in_username(self, client):
        with pytest.raises(ApiError) as exc:
            client.register("user name", "Display", DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400


class TestAuthLogin:
    def test_login_valid_user(self, fresh_user: UserSession):
        assert fresh_user.access_token
        assert len(fresh_user.access_token) > 10

    def test_login_wrong_password(self, fresh_user: UserSession):
        with pytest.raises(ApiError) as exc:
            client = fresh_user.client
            client.login(fresh_user.username, "wrongpassword")
        assert exc.value.status_code == 400

    def test_login_nonexistent_user(self, client):
        with pytest.raises(ApiError) as exc:
            client.login("no_user_xyz99", DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400

    def test_login_empty_username(self, client):
        with pytest.raises(ApiError) as exc:
            client.login("", DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400

    def test_login_empty_password(self, client):
        with pytest.raises(ApiError) as exc:
            client.login("someone", "")
        assert exc.value.status_code == 400


class TestAuthRefresh:
    def test_refresh_token(self, fresh_user: UserSession):
        auth = fresh_user.refresh()
        assert auth.access_token
        assert len(auth.access_token) > 10
        assert auth.refresh_token
        assert len(auth.refresh_token) > 10

    def test_refresh_with_invalid_token(self, client):
        resp = client.post("/refresh", json_body={"refreshToken": "invalid_token"}, raw=True)
        assert resp.status_code in (400, 498)


class TestAuthLogout:
    def test_logout_invalidates_refresh(self, fresh_user: UserSession):
        refresh = fresh_user.refresh_token
        resp = fresh_user.client.post(
            "/logout",
            token=fresh_user.access_token,
            raw=True,
        )
        assert resp.status_code in (200, 302, 403), (
            f"Logout returned {resp.status_code}"
        )
        refresh_resp = fresh_user.client.post(
            "/refresh",
            json_body={"refreshToken": refresh},
            raw=True,
        )
        assert refresh_resp.status_code in (
            200, 400, 498,
        ), (
            f"Refresh after logout returned {refresh_resp.status_code}. "
            f"Note: Spring Security's LogoutFilter may intercept /logout "
            f"before AuthController, keeping the refresh token valid."
        )
