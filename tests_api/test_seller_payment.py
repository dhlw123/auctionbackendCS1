"""Verify seller receives payment after an auction sale completes.

Covers all code paths that should result in the seller being paid:
  1. Natural auction end (no bids) — seller gets 0
  2. Buy-now — seller paid via AuctionFinalizer
  3. Buy-now with prior bids — bidder refunded AND seller paid
  4. Admin cancel on ended auction — rejected, seller paid via finalize
  5. Payment idempotency — paid exactly once
  6. Competing auto-bids — seller gets buyItNowPrice via finalize
  7. Full lifecycle: publish → bid → buy-now → finalize → seller paid

AuctionFinalizer (scheduled + admin endpoint) handles seller payment
in its own independent transaction, eliminating the rollback bug.
"""

import time
import uuid

import pytest

from .client import AdminSession, AuctionClient, UserSession
from .constants import DEFAULT_USER_PASSWORD
from .models import ApiError

from .test_auction import _deposit, _now_ms, _publish


def _register_and_login(client: AuctionClient, prefix: str) -> UserSession:
    uid = uuid.uuid4().hex[:12]
    uname = f"{prefix}_{uid}"
    client.register(uname, f"User {uid}", DEFAULT_USER_PASSWORD)
    auth = client.login(uname, DEFAULT_USER_PASSWORD)
    return UserSession(client, auth, uname)


def _end_via_buy_now(item_id: int, buyer: UserSession, buy_price: float):
    _deposit(buyer, buy_price * 2 + 100)
    resp = buyer.buy_now(item_id)
    assert resp.status is True


def _finalize(admin: AdminSession):
    resp = admin.finalize_expired_auctions()
    assert resp.status is True


# ═══════════════════════════════════════════════════════════════════
# 1. Natural auction end — no bids, seller gets 0
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentNaturalEnd:

    def test_natural_end_no_bids_seller_gets_zero(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        end_ms = _now_ms() + 3_000
        item_id = _publish(fresh_user, end_ms=end_ms, start_price=10, inc=1)
        time.sleep(4)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before, (
            f"Expected no balance change (currentPrice=0), "
            f"but went from {seller_bal_before} to {seller_bal_after}"
        )

        status = fresh_user.client.get_item_status(item_id)
        assert status.entity is not None
        assert status.entity.item_status == "ENDED"

    def test_natural_end_then_cancel_rejected_status_ended(
        self, admin: AdminSession, fresh_user: UserSession
    ):
        end_ms = _now_ms() + 3_000
        item_id = _publish(fresh_user, end_ms=end_ms, start_price=10, inc=1)
        time.sleep(4)

        _finalize(admin)

        with pytest.raises(ApiError) as exc:
            fresh_user.cancel_item(item_id)
        assert exc.value.status_code == 400
        assert "only active" in exc.value.message.lower()

        status = fresh_user.client.get_item_status(item_id)
        assert status.entity is not None
        assert status.entity.item_status == "ENDED"


# ═══════════════════════════════════════════════════════════════════
# 2. Buy-now — seller paid via finalize
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentBuyNow:

    def test_buy_now_then_finalize_pays_seller(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=200)
        _end_via_buy_now(item_id, second_user, 200)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before + 200.0, (
            f"Expected seller balance {seller_bal_before} + 200 = "
            f"{seller_bal_before + 200}, got {seller_bal_after}"
        )

    def test_buy_now_multiple_bidders_pays_seller(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession, client: AuctionClient
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1, buy_now=300)
        _deposit(second_user, 500)
        second_user.bid(item_id, 50)
        second_user.bid(item_id, 80)

        buyer3 = _register_and_login(client, "buy3")
        _end_via_buy_now(item_id, buyer3, 300)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before + 300.0, (
            f"Expected seller balance {seller_bal_before} + 300 = "
            f"{seller_bal_before + 300}, got {seller_bal_after}"
        )

    def test_buy_now_then_auto_bid_rejected_already_ended(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=200)
        _end_via_buy_now(item_id, second_user, 200)
        _finalize(admin)

        _deposit(second_user, 500)
        with pytest.raises(ApiError) as exc:
            second_user.auto_bid(item_id, 300)
        assert exc.value.status_code == 400
        assert "already ended" in exc.value.message.lower()

        seller_bal = fresh_user.get_balance().entity
        assert seller_bal == 200.0, (
            f"Expected seller balance 200.0, got {seller_bal}"
        )


# ═══════════════════════════════════════════════════════════════════
# 3. Buy-now with prior bids — bidder refunded AND seller paid
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentPriorBids:

    def test_buy_now_refunds_bidder_and_pays_seller(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession, client: AuctionClient
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1, buy_now=200)

        _deposit(second_user, 500)
        second_user.bid(item_id, 50)
        bidder_bal_before = second_user.get_balance().entity

        buyer = _register_and_login(client, "buynow")
        _end_via_buy_now(item_id, buyer, 200)

        bidder_bal_after = second_user.get_balance().entity
        assert bidder_bal_after > bidder_bal_before, (
            f"Prior bidder should be refunded. "
            f"Before: {bidder_bal_before}, After: {bidder_bal_after}"
        )

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before + 200.0, (
            f"Expected seller balance {seller_bal_before} + 200 = "
            f"{seller_bal_before + 200}, got {seller_bal_after}"
        )


# ═══════════════════════════════════════════════════════════════════
# 4. Admin cancel after auction ended — rejected, paid via finalize
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentCancelAfterEnd:

    def test_cancel_after_buy_now_pays_seller_then_rejects(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1, buy_now=200)
        _deposit(second_user, 500)
        second_user.bid(item_id, 30)
        _end_via_buy_now(item_id, second_user, 200)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before + 200.0, (
            f"Expected seller balance {seller_bal_before} + 200 = "
            f"{seller_bal_before + 200}, got {seller_bal_after}"
        )

        with pytest.raises(ApiError) as exc:
            admin.cancel_auction(item_id)
        assert exc.value.status_code == 400
        assert "only active" in exc.value.message.lower()

        status = fresh_user.client.get_item_status(item_id)
        assert status.entity is not None
        assert status.entity.item_status == "ENDED"

    def test_cancel_after_natural_end_no_bids(
        self, admin: AdminSession, fresh_user: UserSession
    ):
        end_ms = _now_ms() + 3_000
        item_id = _publish(fresh_user, end_ms=end_ms, start_price=10, inc=1)
        time.sleep(4)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)

        with pytest.raises(ApiError) as exc:
            admin.cancel_auction(item_id)
        assert exc.value.status_code == 400
        assert "only active" in exc.value.message.lower()

        seller_bal_after = fresh_user.get_balance().entity
        assert seller_bal_after == seller_bal_before, (
            f"Expected no change (currentPrice=0). "
            f"Before: {seller_bal_before}, After: {seller_bal_after}"
        )


# ═══════════════════════════════════════════════════════════════════
# 5. Payment idempotency — paid exactly once
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentIdempotency:

    def test_seller_paid_only_once(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=200)
        _end_via_buy_now(item_id, second_user, 200)

        _finalize(admin)
        seller_bal_after_first = fresh_user.get_balance().entity
        assert seller_bal_after_first == 200.0

        _finalize(admin)
        seller_bal_after_second = fresh_user.get_balance().entity

        assert seller_bal_after_second == seller_bal_after_first, (
            f"Second finalize should NOT change balance. "
            f"Expected {seller_bal_after_first}, got {seller_bal_after_second}"
        )

    def test_seller_not_paid_twice_across_items(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item1 = _publish(fresh_user, buy_now=100)
        _end_via_buy_now(item1, second_user, 100)

        item2 = _publish(fresh_user, buy_now=150)
        _end_via_buy_now(item2, second_user, 150)

        _finalize(admin)
        seller_bal = fresh_user.get_balance().entity
        assert seller_bal == 250.0, (
            f"Expected seller balance 100+150=250, got {seller_bal}"
        )


# ═══════════════════════════════════════════════════════════════════
# 6. Competing auto-bids → buy-now ends → seller paid
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentAutoBidCompetition:

    def test_auto_bid_competition_seller_paid(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession, client: AuctionClient
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1, buy_now=500)

        _deposit(second_user, 500)
        second_user.auto_bid(item_id, 150)

        bidder3 = _register_and_login(client, "auto3")
        _deposit(bidder3, 500)
        bidder3.auto_bid(item_id, 250)

        bidder4 = _register_and_login(client, "auto4")
        _deposit(bidder4, 500)
        bidder4.auto_bid(item_id, 350)

        buyer = _register_and_login(client, "ender")
        _end_via_buy_now(item_id, buyer, 500)

        seller_bal_before = fresh_user.get_balance().entity
        _finalize(admin)
        seller_bal_after = fresh_user.get_balance().entity

        assert seller_bal_after == seller_bal_before + 500.0, (
            f"Expected seller balance {seller_bal_before} + 500 = "
            f"{seller_bal_before + 500}, got {seller_bal_after}"
        )


# ═══════════════════════════════════════════════════════════════════
# 7. End-to-end: publish → bid → buy-now → finalize → seller paid
# ═══════════════════════════════════════════════════════════════════

class TestSellerPaymentEndToEnd:

    def test_full_cycle_seller_gets_paid(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession, client: AuctionClient
    ):
        item_id = _publish(fresh_user, start_price=10, inc=1, buy_now=300)

        _deposit(second_user, 500)
        second_user.bid(item_id, 40)
        second_user.bid(item_id, 60)

        buyer = _register_and_login(client, "final")
        _end_via_buy_now(item_id, buyer, 300)

        seller_initial = fresh_user.get_balance().entity
        _finalize(admin)
        seller_final = fresh_user.get_balance().entity

        assert seller_final == seller_initial + 300.0, (
            f"Expected {seller_initial} + 300 = {seller_initial + 300}, "
            f"got {seller_final}"
        )

        status = fresh_user.client.get_item_status(item_id)
        assert status.entity is not None
        assert status.entity.item_status == "ENDED"

    def test_no_bids_full_cycle(
        self, admin: AdminSession, fresh_user: UserSession,
        second_user: UserSession
    ):
        item_id = _publish(fresh_user, buy_now=150)
        _end_via_buy_now(item_id, second_user, 150)

        seller_initial = fresh_user.get_balance().entity
        _finalize(admin)
        seller_final = fresh_user.get_balance().entity

        assert seller_final == seller_initial + 150.0, (
            f"Expected {seller_initial} + 150 = {seller_initial + 150}, "
            f"got {seller_final}"
        )
