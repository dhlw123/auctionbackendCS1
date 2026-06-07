"""Item endpoint tests needed as support for admin cancel tests."""

import time

import pytest

from .client import UserSession
from .models import ApiError


def _now_ms() -> int:
    return int(time.time() * 1000)


class TestPublishItem:
    def test_publish_valid_item(self, fresh_user: UserSession):
        resp = fresh_user.publish_item(
            title="Test Item",
            description="A test description",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=10.0,
            buy_it_now_price=100.0,
            bid_increment=1.0,
        )
        assert resp.status is True
        assert resp.entity is not None
        assert resp.entity.item_id is not None
        assert resp.entity.title == "Test Item"

    def test_publish_empty_title(self, fresh_user: UserSession):
        with pytest.raises(ApiError) as exc:
            fresh_user.publish_item(
                title="",
                description="desc",
                end_time_ms=_now_ms() + 3_600_000,
                starting_price=10.0,
                buy_it_now_price=100.0,
                bid_increment=1.0,
            )
        assert exc.value.status_code == 400

    def test_publish_negative_price(self, fresh_user: UserSession):
        with pytest.raises(ApiError) as exc:
            fresh_user.publish_item(
                title="Bad price",
                description="desc",
                end_time_ms=_now_ms() + 3_600_000,
                starting_price=-10.0,
                buy_it_now_price=100.0,
                bid_increment=1.0,
            )
        assert exc.value.status_code == 400

    def test_publish_without_auth(self, fresh_user: UserSession):
        resp = fresh_user.client.post("/items", json_body={
            "title": "No auth",
            "description": "x",
            "endTime": _now_ms() + 3_600_000,
            "startingPrice": 10.0,
            "buyItNowPrice": 100.0,
            "bidIncrement": 1.0,
        }, raw=True)
        assert resp.status_code in (401, 403)


class TestGetItem:
    def test_get_existing_item(self, fresh_user: UserSession):
        pub = fresh_user.publish_item(
            title="Get test", description="d",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=5.0, buy_it_now_price=50.0, bid_increment=0.5,
        )
        resp = fresh_user.client.get_item(pub.entity.item_id)
        assert resp.status is True
        assert resp.entity.title == "Get test"

    def test_get_nonexistent_item(self, fresh_user: UserSession):
        with pytest.raises(ApiError) as exc:
            fresh_user.client.get_item(99999)
        assert exc.value.status_code == 400


class TestCancelItemByOwner:
    def test_cancel_own_item(self, fresh_user: UserSession):
        pub = fresh_user.publish_item(
            title="Self cancel", description="d",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=5.0, buy_it_now_price=50.0, bid_increment=0.5,
        )
        resp = fresh_user.cancel_item(pub.entity.item_id)
        assert resp.status is True

    def test_cancel_already_canceled_own_item(self, fresh_user: UserSession):
        pub = fresh_user.publish_item(
            title="Cancel twice", description="d",
            end_time_ms=_now_ms() + 3_600_000,
            starting_price=5.0, buy_it_now_price=50.0, bid_increment=0.5,
        )
        fresh_user.cancel_item(pub.entity.item_id)
        with pytest.raises(ApiError) as exc:
            fresh_user.cancel_item(pub.entity.item_id)
        assert exc.value.status_code == 400
