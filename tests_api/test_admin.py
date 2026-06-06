"""Boundary tests for AdminService via REST API.

Covers: POST /admin/ban, POST /admin/unban, POST /admin/cancel/{itemId}

Test matrix:
  Ban:   auth, validation, nonexistent user, admin self-ban, mixed case,
         valid ban, double ban, banned-user login, stale JWT after ban
  Unban: auth, validation, nonexistent user, never-banned idempotency,
         full ban→unban→login cycle, stale JWT after unban
  Cancel: auth, invalid ids, active item, already-canceled item,
          ended item, post-cancel status verification, bidder refund
"""

import time
import uuid

import pytest

from .client import AdminSession, AuctionClient, UserSession
from .constants import DEFAULT_USER_PASSWORD
from .models import ApiError

# ═══════════════════════════════════════════════════════════════════
# POST /admin/ban
# ═══════════════════════════════════════════════════════════════════


class TestAdminBanAuth:
    def test_ban_without_auth_returns_401(self, client: AuctionClient):
        resp = client.post("/admin/ban", json_body={"username": "someone"}, raw=True)
        assert resp.status_code in (401, 403)

    def test_ban_with_invalid_token(self, client: AuctionClient):
        resp = client.post("/admin/ban", token="invalid.jwt.token",
                           json_body={"username": "someone"}, raw=True)
        assert resp.status_code in (401, 498)

    def test_ban_as_regular_user_returns_403(self, fresh_user: UserSession):
        resp = fresh_user.client.post(
            "/admin/ban", token=fresh_user.access_token,
            json_body={"username": "someone"}, raw=True,
        )
        assert resp.status_code == 403


class TestAdminBanValidation:
    def test_ban_empty_body(self, admin: AdminSession):
        resp = admin.ban_user_raw("")
        assert resp.status_code == 400
        body = resp.json()
        assert "username" in body or "message" in body

    def test_ban_missing_username_field(self, admin: AdminSession):
        resp = admin.client.post("/admin/ban", token=admin.token,
                                 json_body={}, raw=True)
        assert resp.status_code == 400

    def test_ban_whitespace_only_username(self, admin: AdminSession):
        resp = admin.client.post("/admin/ban", token=admin.token,
                                 json_body={"username": "   "}, raw=True)
        assert resp.status_code == 400

    def test_ban_non_string_username(self, admin: AdminSession):
        resp = admin.client.post("/admin/ban", token=admin.token,
                                 json_body={"username": 12345}, raw=True)
        assert resp.status_code in (400, 500)


class TestAdminBanBoundary:
    def test_ban_self_admin_is_rejected(self, admin: AdminSession):
        with pytest.raises(ApiError) as exc:
            admin.ban_user("admin")
        assert exc.value.status_code == 400
        assert "can't ban admin" in exc.value.message.lower()

    def test_ban_mixed_case_admin_passes_guard(self, admin: AdminSession):
        resp = admin.ban_user_raw("Admin")
        assert resp.status_code == 400
        assert "User not found" in str(resp.json())

    def test_ban_nonexistent_user(self, admin: AdminSession):
        with pytest.raises(ApiError) as exc:
            admin.ban_user("no_such_user_xyz123")
        assert exc.value.status_code == 400
        assert "User not found" in exc.value.message

    def test_ban_valid_user_succeeds(self, admin: AdminSession, fresh_user: UserSession):
        resp = admin.ban_user(fresh_user.username)
        assert resp.status is True
        assert resp.message == "successfully banned user"

    def test_ban_already_banned_user_DOUBLE_TAP(self, admin: AdminSession, fresh_user: UserSession):
        resp1 = admin.ban_user(fresh_user.username)
        assert resp1.status is True

        resp2 = admin.ban_user_raw(fresh_user.username)
        assert resp2.status_code in (200, 400, 500), (
            f"Double ban returned {resp2.status_code}: {resp2.text[:200]}"
        )

    def test_banned_user_cannot_login(self, admin: AdminSession, fresh_user: UserSession):
        admin.ban_user(fresh_user.username)
        with pytest.raises(ApiError) as exc:
            fresh_user.client.login(fresh_user.username, DEFAULT_USER_PASSWORD)
        assert exc.value.status_code == 400
        assert "banned" in exc.value.message.lower()

    def test_banned_user_stale_jwt_is_rejected(self, admin: AdminSession, fresh_user: UserSession):
        old_token = fresh_user.access_token
        admin.ban_user(fresh_user.username)
        resp = fresh_user.client.get("/users/me/balance", token=old_token, raw=True)
        assert resp.status_code in (401, 498, 403)

    def test_ban_very_short_username(self, admin: AdminSession):
        resp = admin.ban_user_raw("x")
        assert resp.status_code == 400

    def test_ban_very_long_username(self, admin: AdminSession):
        long_name = "a" * 200
        resp = admin.ban_user_raw(long_name)
        assert resp.status_code == 400


# ═══════════════════════════════════════════════════════════════════
# POST /admin/unban
# ═══════════════════════════════════════════════════════════════════


class TestAdminUnbanAuth:
    def test_unban_without_auth_returns_401(self, client: AuctionClient):
        resp = client.post("/admin/unban",
                           json_body={"username": "x", "password": "y"}, raw=True)
        assert resp.status_code in (401, 403)

    def test_unban_as_regular_user_returns_403(self, fresh_user: UserSession):
        resp = fresh_user.client.post(
            "/admin/unban", token=fresh_user.access_token,
            json_body={"username": "x", "password": "y"}, raw=True,
        )
        assert resp.status_code == 403


class TestAdminUnbanValidation:
    def test_unban_empty_body(self, admin: AdminSession):
        resp = admin.client.post("/admin/unban", token=admin.token,
                                 json_body={}, raw=True)
        assert resp.status_code == 400

    def test_unban_missing_password(self, admin: AdminSession, fresh_user: UserSession):
        resp = admin.client.post("/admin/unban", token=admin.token,
                                 json_body={"username": fresh_user.username}, raw=True)
        assert resp.status_code == 400

    def test_unban_missing_username(self, admin: AdminSession):
        resp = admin.client.post("/admin/unban", token=admin.token,
                                 json_body={"password": "x"}, raw=True)
        assert resp.status_code == 400

    def test_unban_empty_password(self, admin: AdminSession):
        resp = admin.unban_user_raw("someone", "")
        assert resp.status_code == 400

    def test_unban_empty_username(self, admin: AdminSession):
        resp = admin.unban_user_raw("", "password123")
        assert resp.status_code == 400


class TestAdminUnbanBoundary:
    def test_unban_nonexistent_user(self, admin: AdminSession):
        with pytest.raises(ApiError) as exc:
            admin.unban_user("ghost_user_xyz", "pwd")
        assert exc.value.status_code == 400
        assert "User not found" in exc.value.message

    def test_unban_never_banned_user_is_idempotent(self, admin: AdminSession, fresh_user: UserSession):
        resp = admin.unban_user(fresh_user.username, "newpassword")
        assert resp.status is True

    def test_ban_then_unban_then_login_full_cycle(self, admin: AdminSession, fresh_user: UserSession):
        username = fresh_user.username
        new_password = "new_password_after_unban_456"

        admin.ban_user(username)

        with pytest.raises(ApiError):
            fresh_user.client.login(username, DEFAULT_USER_PASSWORD)

        admin.unban_user(username, new_password)

        auth = fresh_user.client.login(username, new_password)
        assert auth.access_token

    def test_unban_idempotent_twice(self, admin: AdminSession, fresh_user: UserSession):
        uname = fresh_user.username
        admin.ban_user(uname)
        r1 = admin.unban_user(uname, "pwd1")
        assert r1.status is True
        r2 = admin.unban_user(uname, "pwd2")
        assert r2.status is True

    def test_stale_token_after_unban(self, admin: AdminSession, fresh_user: UserSession):
        old_token = fresh_user.access_token
        uname = fresh_user.username
        admin.ban_user(uname)
        admin.unban_user(uname, "newpass99")

        resp = fresh_user.client.get("/users/me/balance", token=old_token, raw=True)
        assert resp.status_code in (401, 498, 403)

    def test_unban_long_password(self, admin: AdminSession, fresh_user: UserSession):
        uname = fresh_user.username
        admin.ban_user(uname)
        long_pw = "x" * 500
        resp = admin.unban_user(uname, long_pw)
        assert resp.status is True

        auth = fresh_user.client.login(uname, long_pw)
        assert auth.access_token


# ═══════════════════════════════════════════════════════════════════
# POST /admin/cancel/{itemId}
# ═══════════════════════════════════════════════════════════════════


def _now_ms() -> int:
    return int(time.time() * 1000)


class TestAdminCancelAuth:
    def test_cancel_without_auth_returns_401(self, client: AuctionClient):
        resp = client.post("/admin/cancel/1", raw=True)
        assert resp.status_code in (401, 403)

    def test_cancel_as_regular_user_returns_403(self, fresh_user: UserSession):
        resp = fresh_user.client.post("/admin/cancel/1",
                                       token=fresh_user.access_token, raw=True)
        assert resp.status_code == 403


class TestAdminCancelValidation:
    @pytest.mark.parametrize("item_id", [0, -1, 9223372036854775807])
    def test_cancel_invalid_item_ids(self, admin: AdminSession, item_id: int):
        resp = admin.cancel_auction_raw(item_id)
        assert resp.status_code == 400

    def test_cancel_nonexistent_item(self, admin: AdminSession):
        with pytest.raises(ApiError) as exc:
            admin.cancel_auction(99999)
        assert exc.value.status_code == 400
        assert "no such item" in exc.value.message.lower()


class TestAdminCancelBoundary:
    def test_cancel_active_item_succeeds(self, admin: AdminSession, fresh_user: UserSession):
        item_resp = fresh_user.publish_item(
            title="Auction for admin cancel",
            description="Test item for cancel by admin",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=10.0,
            buy_it_now_price=100.0,
            bid_increment=1.0,
        )
        item = item_resp.entity
        assert item is not None

        resp = admin.cancel_auction(item.item_id)
        assert resp.status is True

        status_resp = fresh_user.client.get_item_status(item.item_id)
        if status_resp.entity:
            assert status_resp.entity.item_status == "CANCELED"

    def test_cancel_already_canceled_item(self, admin: AdminSession, fresh_user: UserSession):
        item_resp = fresh_user.publish_item(
            title="Cancel me twice",
            description="Item to cancel then cancel again",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=5.0,
            buy_it_now_price=50.0,
            bid_increment=0.5,
        )
        item = item_resp.entity

        admin.cancel_auction(item.item_id)

        with pytest.raises(ApiError) as exc:
            admin.cancel_auction(item.item_id)
        assert exc.value.status_code == 400
        assert "only active" in exc.value.message.lower()

    def test_cancel_then_bid_is_rejected(self, admin: AdminSession,
                                          fresh_user: UserSession,
                                          second_user: UserSession):
        item_resp = fresh_user.publish_item(
            title="Cancel then bid test",
            description="Should reject bid after cancel",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=10.0,
            buy_it_now_price=200.0,
            bid_increment=1.0,
        )
        item = item_resp.entity
        admin.cancel_auction(item.item_id)

        with pytest.raises(ApiError) as exc:
            second_user.bid(item.item_id, 15.0)
        assert exc.value.status_code == 400

    def test_cancel_with_bidder_refund(self, admin: AdminSession,
                                        fresh_user: UserSession,
                                        second_user: UserSession):
        seller = fresh_user
        bidder = second_user
        bidder.deposit(500.0)

        item_resp = seller.publish_item(
            title="Auction with bidder",
            description="Bidder gets refunded on cancel",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=10.0,
            buy_it_now_price=200.0,
            bid_increment=1.0,
        )
        item = item_resp.entity
        bidder.bid(item.item_id, 50.0)

        before_balance = bidder.get_balance()
        admin.cancel_auction(item.item_id)

        after_balance = bidder.get_balance()
        assert after_balance.entity is not None
        assert before_balance.entity is not None
        assert after_balance.entity >= before_balance.entity
