"""Endpoint tests for AuctionService: bid, buy-now, auto-bid, me/bids, me/wins.

Covers: POST /bid, POST /buy-now/{itemId}, POST /auto-bid, GET /me/bids, GET /me/wins

Boundary matrix:
  Bid:     auth, self-bid, nonexistent item, below starting price,
           below increment, auction ended, insufficient balance,
           negative/zero amount, empty body, first bid success,
           existing bidder update, outbid refund, extension trigger.
  BuyNow:  auth, own item, nonexistent item, insufficient balance,
           success ends auction, already ended.
  AutoBid: auth, own item, insufficient balance, first auto-bid,
           competing auto-bidders.
  Queries: auth checks, paginated bids, empty winnings.
"""

import time

import pytest

from .client import AuctionClient, UserSession
from .models import ApiError


def _now_ms() -> int:
    """Return the current time as epoch milliseconds."""
    return int(time.time() * 1000)


# ═══════════════════════════════════════════════════════════════════
# Fixtures for publishing items needed by bid tests
# ═══════════════════════════════════════════════════════════════════


def _publish(
    seller: UserSession,
    title: str = "Bid Test Item",
    end_ms: int | None = None,
    start_price: float = 10.0,
    buy_now: float = 200.0,
    inc: float = 1.0,
) -> int:
    """Publish an auction item and return its item_id.

    Args:
        seller: The UserSession that owns the listing.
        title: Item title displayed to bidders.
        end_ms: End time as epoch milliseconds (default: now + 1 hour).
        start_price: Opening bid price.
        buy_now: Buy-It-Now instant purchase price.
        inc: Minimum bid increment between successive bids.

    Returns:
        The newly created item's item_id.

    Raises:
        AssertionError: If the publish API call fails.
    """
    if end_ms is None:
        end_ms = _now_ms() + 3_600_000
    resp = seller.publish_item(
        title=title,
        description="Test desc",
        end_time_ms=end_ms,
        starting_price=start_price,
        buy_it_now_price=buy_now,
        bid_increment=inc,
    )
    assert resp.status is True
    return resp.entity.item_id


def _deposit(user: UserSession, amount: float) -> None:
    """Deposit money into a user's account balance.

    Args:
        user: The UserSession receiving the deposit.
        amount: Amount to deposit (must be positive).

    Raises:
        AssertionError: If the deposit API call fails.
    """
    resp = user.deposit(amount)
    assert resp.status is True


# ═══════════════════════════════════════════════════════════════════
# POST /bid
# ═══════════════════════════════════════════════════════════════════


class TestBidAuth:
    def test_bid_without_auth(self, client: AuctionClient):
        resp = client.post("/bid", json_body={"itemId": 1, "bidAmount": 100}, raw=True)
        assert resp.status_code in (401, 403)


class TestBidValidation:
    def test_bid_empty_body(self, fresh_user: UserSession):
        resp = fresh_user.bid_raw()
        assert resp.status_code in (400, 403)

    def test_bid_non_existent_item(self, fresh_user: UserSession):
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.bid(99999, 50)
        assert exc.value.status_code in (400, 404)

    def test_bid_negative_amount(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(second_user, start_price=10)
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.bid(item_id, -50)
        assert exc.value.status_code == 400

    def test_bid_zero_amount(self, fresh_user: UserSession, second_user: UserSession):
        item_id = _publish(second_user, start_price=10)
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.bid(item_id, 0)
        assert exc.value.status_code == 400

    def test_bid_missing_item_id(self, fresh_user: UserSession):
        resp = fresh_user.bid_raw(bid_amount=50)
        assert resp.status_code == 400

    def test_bid_missing_bid_amount(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(second_user, start_price=10)
        resp = fresh_user.bid_raw(item_id=item_id)
        body = resp.json() if resp.text else {}
        assert resp.status_code == 400
        assert "bidAmount" in str(body), (
            f"Expected bidAmount validation error, got: {body}"
        )


class TestBidBoundary:
    def test_bid_self_bid_rejected(self, fresh_user: UserSession):
        item_id = _publish(fresh_user)
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.bid(item_id, 15)
        assert exc.value.status_code == 400
        assert "own item" in exc.value.message.lower()

    def test_bid_below_starting_price(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=100)
        _deposit(second_user, 500)
        with pytest.raises(ApiError) as exc:
            second_user.bid(item_id, 50)
        assert exc.value.status_code == 400
        assert "starting price" in exc.value.message.lower()

    def test_bid_below_increment(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10, inc=5)
        _deposit(second_user, 500)
        second_user.bid(item_id, 28)
        with pytest.raises(ApiError) as exc:
            second_user.bid(item_id, 30)
        assert exc.value.status_code == 400
        assert "current highest" in exc.value.message.lower()

    def test_bid_insufficient_balance(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 5)
        with pytest.raises(ApiError) as exc:
            second_user.bid(item_id, 15)
        assert exc.value.status_code == 400
        assert "enough money" in exc.value.message.lower()

    def test_bid_on_canceled_item(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        fresh_user.cancel_item(item_id)
        _deposit(second_user, 500)
        with pytest.raises(ApiError) as exc:
            second_user.bid(item_id, 15)
        assert exc.value.status_code == 400

    def test_first_bid_succeeds(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 500)
        resp = second_user.bid(item_id, 15)
        assert resp.status is True

    def test_same_user_increases_bid(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1)
        _deposit(second_user, 500)
        second_user.bid(item_id, 20)
        resp = second_user.bid(item_id, 40)
        assert resp.status is True

    def test_outbid_refunds_previous_bidder(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        import uuid

        item_id = _publish(fresh_user, start_price=10, inc=1)
        _deposit(second_user, 500)
        second_user.bid(item_id, 30)

        # Create a third user to outbid
        uid = uuid.uuid4().hex[:12]
        uname = f"bidder3_{uid}"
        client = fresh_user.client
        client.register(uname, f"Bidder {uid}", "password123")
        auth = client.login(uname, "password123")
        from .client import UserSession

        bidder3 = UserSession(client, auth, uname)
        _deposit(bidder3, 500)

        bal_before = second_user.get_balance()
        assert bal_before.entity is not None

        bidder3.bid(item_id, 50)

        bal_after = second_user.get_balance()
        assert bal_after.entity is not None
        assert bal_after.entity > bal_before.entity

    def test_bid_near_end_triggers_extension(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        end_ms = _now_ms() + 10_000
        item_id = _publish(fresh_user, end_ms=end_ms, start_price=10, inc=1)
        _deposit(second_user, 500)
        second_user.bid(item_id, 30)

        status_resp = fresh_user.client.get_item_status(item_id)
        assert status_resp.entity is not None, "Expected entity in item status response"
        new_end = status_resp.entity.end_time
        assert new_end > end_ms, (
            f"End time should be extended. Was {end_ms}, now {new_end}"
        )

    def test_bid_exact_increment_boundary(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10, inc=5)
        _deposit(second_user, 500)
        second_user.bid(item_id, 20)
        resp = second_user.bid(item_id, 25)
        assert resp.status is True


# ═══════════════════════════════════════════════════════════════════
# POST /buy-now/{itemId}
# ═══════════════════════════════════════════════════════════════════


class TestBuyNowAuth:
    def test_buy_now_without_auth(self, client: AuctionClient):
        resp = client.post("/buy-now/1", raw=True)
        assert resp.status_code in (401, 403)


class TestBuyNowBoundary:
    def test_buy_now_self_item_rejected(self, fresh_user: UserSession):
        item_id = _publish(fresh_user, buy_now=500)
        _deposit(fresh_user, 1000)
        with pytest.raises(ApiError) as exc:
            fresh_user.buy_now(item_id)
        assert exc.value.status_code == 400

    def test_buy_now_non_existent_item(self, fresh_user: UserSession):
        _deposit(fresh_user, 1000)
        with pytest.raises(ApiError) as exc:
            fresh_user.buy_now(99999)
        assert exc.value.status_code == 400

    def test_buy_now_insufficient_balance(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=500)
        _deposit(second_user, 10)
        with pytest.raises(ApiError) as exc:
            second_user.buy_now(item_id)
        assert exc.value.status_code == 400

    def test_buy_now_success_ends_auction(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=200)
        _deposit(second_user, 1000)
        resp = second_user.buy_now(item_id)
        assert resp.status is True

        status_resp = fresh_user.client.get_item_status(item_id)
        assert status_resp.entity is not None, "Expected entity in item status response"
        now = _now_ms()
        assert status_resp.entity.end_time <= now, (
            f"End time ({status_resp.entity.end_time}) should be <= now ({now})"
        )

    def test_buy_now_on_ended_item(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=200)
        _deposit(second_user, 1000)
        second_user.buy_now(item_id)
        with pytest.raises(ApiError) as exc:
            second_user.buy_now(item_id)
        assert exc.value.status_code == 400


# ═══════════════════════════════════════════════════════════════════
# POST /auto-bid
# ═══════════════════════════════════════════════════════════════════


class TestAutoBidAuth:
    def test_auto_bid_without_auth(self, client: AuctionClient):
        resp = client.post(
            "/auto-bid", json_body={"itemId": 1, "maxBidLimit": 100}, raw=True
        )
        assert resp.status_code in (401, 403)


class TestAutoBidValidation:
    def test_auto_bid_empty_body(self, fresh_user: UserSession):
        resp = fresh_user.auto_bid_raw()
        assert resp.status_code in (400, 403)

    def test_auto_bid_missing_item_id(self, fresh_user: UserSession):
        resp = fresh_user.auto_bid_raw(max_bid_limit=100)
        assert resp.status_code == 400

    def test_auto_bid_missing_limit(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(second_user, start_price=10)
        resp = fresh_user.auto_bid_raw(item_id=item_id)
        body = resp.json() if resp.text else {}
        assert resp.status_code == 400
        assert "maxBidLimit" in str(body), (
            f"Expected maxBidLimit validation error, got: {body}"
        )

    def test_auto_bid_negative_limit(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(second_user, start_price=10)
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.auto_bid(item_id, -100)
        assert exc.value.status_code == 400


class TestAutoBidBoundary:
    def test_auto_bid_self_item_rejected(self, fresh_user: UserSession):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(fresh_user, 500)
        with pytest.raises(ApiError) as exc:
            fresh_user.auto_bid(item_id, 100)
        assert exc.value.status_code == 400

    def test_auto_bid_insufficient_balance(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 5)
        with pytest.raises(ApiError) as exc:
            second_user.auto_bid(item_id, 100)
        assert exc.value.status_code == 400

    def test_auto_bid_first_no_bids_succeeds(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 500)
        resp = second_user.auto_bid(item_id, 100)
        assert resp.status is True

    def test_auto_bid_competing_higher_limit_wins(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        import uuid

        item_id = _publish(fresh_user, start_price=10, inc=1)
        _deposit(second_user, 500)
        second_user.auto_bid(item_id, 50)

        # Create a third user with higher limit
        uid = uuid.uuid4().hex[:12]
        uname = f"autobid_{uid}"
        client = fresh_user.client
        client.register(uname, f"Auto {uid}", "password123")
        auth = client.login(uname, "password123")
        from .client import UserSession

        bidder3 = UserSession(client, auth, uname)
        _deposit(bidder3, 500)

        resp = bidder3.auto_bid(item_id, 100)
        assert resp.status is True

        status_resp = fresh_user.client.get_item_status(item_id)
        assert status_resp.entity is not None, "Expected item status entity"
        assert status_resp.entity.highest_bid_user == uname, (
            f"Expected {uname} to be highest bidder, "
            f"got {status_resp.entity.highest_bid_user}"
        )

    def test_auto_bid_same_user_increases_limit(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 1000)
        second_user.auto_bid(item_id, 50)

        bal_before = second_user.get_balance()
        assert bal_before.entity is not None

        resp = second_user.auto_bid(item_id, 80)
        assert resp.status is True

        bal_after = second_user.get_balance()
        assert bal_after.entity is not None
        assert bal_before.entity - bal_after.entity == 30.0, (
            f"Increasing limit from 50 to 80 should deduct 30. "
            f"Balance went from {bal_before.entity} to {bal_after.entity}"
        )

    def test_auto_bid_same_user_decreases_limit(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10)
        _deposit(second_user, 1000)
        second_user.auto_bid(item_id, 100)

        bal_before = second_user.get_balance()
        assert bal_before.entity is not None

        resp = second_user.auto_bid(item_id, 60)
        assert resp.status is True

        bal_after = second_user.get_balance()
        assert bal_after.entity is not None
        assert bal_after.entity - bal_before.entity == 40.0, (
            f"Decreasing limit from 100 to 60 should add 40 back. "
            f"Balance went from {bal_before.entity} to {bal_after.entity}"
        )

        def test_auto_bid_lower_than_current_price1(
            self,
            fresh_user: UserSession,
            second_user: UserSession,
            third_user: UserSession,
        ):
            item_id = _publish(fresh_user, start_price=100, inc=10)
            _deposit(second_user, 1000)
            _deposit(third_user, 1000)

            res = second_user.bid(item_id, 189)
            assert res.status is True
            item_status = fresh_user.client.get_item_status(item_id)
            assert item_status.entity.current_price == 189

            res = second_user.auto_bid(item_id, 200)
            assert res.status is True
            item_status = fresh_user.client.get_item_status(item_id)
            assert item_status.entity.current_price == 189

    def test_auto_bid_lower_than_current_price2(
        self, fresh_user: UserSession, second_user: UserSession, third_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=100, inc=10)
        _deposit(second_user, 1000)
        _deposit(third_user, 1000)

        res = second_user.bid(item_id, 189)
        assert res.status is True
        item_status = fresh_user.client.get_item_status(item_id)
        assert item_status.entity.current_price == 189

        res = third_user.auto_bid_raw(item_id, 190)
        assert res.status_code == 400
        item_status = fresh_user.client.get_item_status(item_id)
        assert item_status.entity.current_price == 189

        res = second_user.auto_bid(item_id, 200)
        assert res.status is True
        item_status = fresh_user.client.get_item_status(item_id)
        assert item_status.entity.current_price == 189


# ═══════════════════════════════════════════════════════════════════
# GET /me/bids  &  GET /me/wins
# ═══════════════════════════════════════════════════════════════════


class TestBidQueries:
    def test_get_my_bids_without_auth(self, client: AuctionClient):
        resp = client.get("/me/bids", raw=True)
        assert resp.status_code in (401, 403)

    def test_get_my_bids_with_auth(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1)
        _deposit(second_user, 500)
        second_user.bid(item_id, 30)
        resp = second_user.get_my_bids()
        assert resp.status is True

    def test_get_my_wins_without_auth(self, client: AuctionClient):
        resp = client.get("/me/wins", raw=True)
        assert resp.status_code in (401, 403)

    def test_get_my_wins_empty(self, fresh_user: UserSession):
        resp = fresh_user.get_my_wins()
        assert resp.status is True

    def test_get_my_wins_with_winning_bid(
        self, fresh_user: UserSession, second_user: UserSession
    ):
        title = f"WinTest_{int(time.time())}"
        item_id = _publish(fresh_user, title=title, buy_now=100)

        _deposit(second_user, 500)
        buy_resp = second_user.buy_now(item_id)
        assert buy_resp.status is True, f"Buy-now failed: {buy_resp.message}"

        time.sleep(0.2)

        resp = second_user.get_my_wins()
        assert resp.status is True, f"get_my_wins failed: {resp.message}"
        assert resp.entity is not None, "Expected non-empty winnings list"
        assert isinstance(resp.entity, list), f"Expected list, got {type(resp.entity)}"
        assert len(resp.entity) > 0, (
            f"Expected at least 1 won item, got empty list. entity={resp.entity}"
        )

        won = resp.entity[0]
        assert isinstance(won, dict), f"Expected dict, got {type(won)}"
        item = won.get("item", {})
        assert item.get("title") == title, (
            f"Expected won item title '{title}', got '{item.get('title')}'. "
            f"Full response: {resp.entity}"
        )
