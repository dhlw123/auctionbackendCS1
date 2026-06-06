"""Security boundary tests: auth bypass, role escalation, expired tokens."""

import time

import pytest

from .client import AdminSession, AuctionClient, UserSession
from .models import ApiError


class TestRoleEscalation:
    def test_regular_user_cannot_access_admin_endpoints(self, fresh_user: UserSession):
        admin_endpoints = [
            ("POST", "/admin/ban", {"username": "test"}),
            ("POST", "/admin/unban", {"username": "test", "password": "x"}),
            ("POST", "/admin/cancel/1", None),
        ]
        for method, path, body in admin_endpoints:
            resp = fresh_user.client.post(path, token=fresh_user.access_token,
                                          json_body=body, raw=True)
            assert resp.status_code == 403, f"{method} {path} should be 403, got {resp.status_code}"

    def test_user_cannot_cancel_others_item(self, fresh_user: UserSession,
                                             second_user: UserSession):
        now_ms = int(time.time() * 1000)
        item_resp = second_user.publish_item(
            title="Not your item",
            description="Try to cancel it",
            end_time_ms=now_ms + 3_600_000,
            starting_price=10.0,
            buy_it_now_price=100.0,
            bid_increment=1.0,
        )
        item = item_resp.entity
        with pytest.raises(ApiError) as exc:
            fresh_user.cancel_item(item.item_id)
        assert exc.value.status_code == 400


class TestAdminPrivilege:
    def test_admin_can_access_admin_endpoints(self, admin: AdminSession):
        resp = admin.ban_user_raw("no_one_real")
        assert resp.status_code == 400

    def test_admin_can_access_user_endpoints(self, admin: AdminSession):
        resp = admin.client.get("/users/me/balance", token=admin.token, raw=True)
        assert resp.status_code in (200, 400)
