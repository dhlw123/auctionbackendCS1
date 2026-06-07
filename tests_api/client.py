from __future__ import annotations

import time
from typing import Any

import requests

from .constants import API_BASE_URL, REQUEST_TIMEOUT
from .models import (
    ApiError,
    AuthResponse,
    BaseObjectResponse,
    BaseResponse,
    ItemEntity,
    ItemStatusEntity,
    UserEntity,
)


class AuctionClient:
    """Low-level HTTP client wrapping requests.Session with auth and JSON handling."""

    def __init__(self, base_url: str = API_BASE_URL):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def close(self):
        self.session.close()

    # ── raw request ──────────────────────────────────────────────

    def _request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        json_body: dict | None = None,
        raw_response: bool = False,
        **kwargs,
    ) -> requests.Response:
        url = f"{self.base_url}{path}"
        headers = kwargs.pop("headers", {})
        if token:
            headers["Authorization"] = f"Bearer {token}"
        kwargs.setdefault("timeout", REQUEST_TIMEOUT)
        kwargs["stream"] = True
        try:
            resp = self.session.request(
                method=method, url=url, json=json_body, headers=headers, **kwargs
            )
        except requests.exceptions.ChunkedEncodingError:
            raise ApiError(0, "Connection reset by server (chunked encoding error)", {})
        if not resp.ok and not raw_response:
            raise ApiError.from_response(resp)
        return resp

    def get(self, path: str, *, token: str | None = None, raw: bool = False, **kw) -> requests.Response:
        return self._request("GET", path, token=token, raw_response=raw, **kw)

    def post(self, path: str, *, token: str | None = None, json_body: dict | None = None, raw: bool = False, **kw) -> requests.Response:
        return self._request("POST", path, token=token, json_body=json_body, raw_response=raw, **kw)

    # ── auth endpoints (no token needed) ──────────────────────────

    def register(self, username: str, display_name: str, password: str) -> BaseResponse:
        r = self.post("/register", json_body={
            "username": username,
            "displayName": display_name,
            "password": password,
        })
        return BaseResponse.from_response(r)

    def login(self, username: str, password: str) -> AuthResponse:
        r = self.post("/login", json_body={
            "username": username,
            "password": password,
        })
        return AuthResponse.from_response(r)

    # ── item / status lookups (public) ────────────────────────────

    def get_item(self, item_id: int) -> BaseObjectResponse:
        r = self.get(f"/items/{item_id}")
        resp = BaseObjectResponse.from_response(r)
        if resp.entity:
            resp.entity = ItemEntity.from_dict(resp.entity)
        return resp

    def get_item_status(self, item_id: int) -> BaseObjectResponse:
        r = self.get(f"/item/status/{item_id}")
        resp = BaseObjectResponse.from_response(r)
        if resp.entity:
            resp.entity = ItemStatusEntity.from_dict(resp.entity)
        return resp


class AuthSession:
    """Holds a logged-in user's tokens and provides a reference to the client."""

    def __init__(self, client: AuctionClient, auth: AuthResponse, username: str):
        self.client = client
        self.access_token = auth.access_token
        self.refresh_token = auth.refresh_token
        self.username = username
        self.is_admin = username == "admin"

    @property
    def token(self) -> str:
        return self.access_token

    def refresh(self) -> AuthResponse:
        r = self.client.post("/refresh", json_body={"refreshToken": self.refresh_token})
        auth = AuthResponse.from_response(r)
        self.access_token = auth.access_token
        self.refresh_token = auth.refresh_token
        return auth

    def logout(self) -> BaseResponse:
        r = self.client.post("/logout", token=self.access_token)
        return BaseResponse.from_response(r)

    # ── user endpoints ──────────────────────────────────────────

    def get_balance(self) -> BaseObjectResponse:
        r = self.client.get("/users/me/balance", token=self.access_token)
        return BaseObjectResponse.from_response(r)

    def deposit(self, amount: float) -> BaseObjectResponse:
        r = self.client.post("/users/me/deposit", token=self.access_token,
                             json_body={"amount": amount})
        return BaseObjectResponse.from_response(r)


class UserSession(AuthSession):
    """Logged-in regular user with item/bid actions."""

    def publish_item(
        self,
        title: str,
        description: str,
        end_time_ms: int,
        starting_price: float,
        buy_it_now_price: float,
        bid_increment: float,
    ) -> BaseObjectResponse:
        r = self.client.post("/items", token=self.access_token, json_body={
            "title": title,
            "description": description,
            "endTime": end_time_ms,
            "startingPrice": starting_price,
            "buyItNowPrice": buy_it_now_price,
            "bidIncrement": bid_increment,
        })
        resp = BaseObjectResponse.from_response(r)
        if resp.entity:
            resp.entity = ItemEntity.from_dict(resp.entity)
        return resp

    def cancel_item(self, item_id: int) -> BaseResponse:
        r = self.client.post(f"/items/cancel/{item_id}", token=self.access_token)
        return BaseResponse.from_response(r)

    def bid(self, item_id: int, bid_amount: float) -> BaseObjectResponse:
        r = self.client.post("/bid", token=self.access_token, json_body={
            "itemId": item_id,
            "bidAmount": bid_amount,
        })
        return BaseObjectResponse.from_response(r)

    def bid_raw(self, item_id: int | None = None, bid_amount: float | None = None, **extra) -> requests.Response:
        body = dict(extra)
        if item_id is not None:
            body["itemId"] = item_id
        if bid_amount is not None:
            body["bidAmount"] = bid_amount
        return self.client.post("/bid", token=self.access_token, json_body=body or None, raw=True)

    def buy_now(self, item_id: int) -> BaseResponse:
        r = self.client.post(f"/buy-now/{item_id}", token=self.access_token)
        return BaseResponse.from_response(r)

    def buy_now_raw(self, item_id: int) -> requests.Response:
        return self.client.post(f"/buy-now/{item_id}", token=self.access_token, raw=True)

    def auto_bid(self, item_id: int, max_bid_limit: float) -> BaseResponse:
        r = self.client.post("/auto-bid", token=self.access_token, json_body={
            "itemId": item_id,
            "maxBidLimit": max_bid_limit,
        })
        return BaseResponse.from_response(r)

    def auto_bid_raw(self, item_id: int | None = None, max_bid_limit: float | None = None, **extra) -> requests.Response:
        body = dict(extra)
        if item_id is not None:
            body["itemId"] = item_id
        if max_bid_limit is not None:
            body["maxBidLimit"] = max_bid_limit
        return self.client.post("/auto-bid", token=self.access_token, json_body=body or None, raw=True)

    def get_my_bids(self, page: int = 0, size: int = 10) -> BaseObjectResponse:
        r = self.client.get(f"/me/bids?page={page}&size={size}", token=self.access_token)
        return BaseObjectResponse.from_response(r)

    def get_my_wins(self) -> BaseObjectResponse:
        r = self.client.get("/me/wins", token=self.access_token)
        return BaseObjectResponse.from_response(r)


class AdminSession(AuthSession):
    """Logged-in admin user with ban/unban/cancelAuction actions."""

    def ban_user(self, username: str) -> BaseResponse:
        r = self.client.post("/admin/ban", token=self.access_token,
                             json_body={"username": username})
        return BaseResponse.from_response(r)

    def ban_user_raw(self, username: str) -> requests.Response:
        return self.client.post("/admin/ban", token=self.access_token,
                                json_body={"username": username}, raw=True)

    def unban_user(self, username: str, password: str) -> BaseResponse:
        r = self.client.post("/admin/unban", token=self.access_token,
                             json_body={"username": username, "password": password})
        return BaseResponse.from_response(r)

    def unban_user_raw(self, username: str, password: str) -> requests.Response:
        return self.client.post("/admin/unban", token=self.access_token,
                                json_body={"username": username, "password": password}, raw=True)

    def cancel_auction(self, item_id: int) -> BaseResponse:
        r = self.client.post(f"/admin/cancel/{item_id}", token=self.access_token)
        return BaseResponse.from_response(r)

    def cancel_auction_raw(self, item_id: int) -> requests.Response:
        return self.client.post(f"/admin/cancel/{item_id}", token=self.access_token, raw=True)

    def finalize_expired_auctions(self) -> BaseResponse:
        r = self.client.post("/admin/finalize", token=self.access_token)
        return BaseResponse.from_response(r)
