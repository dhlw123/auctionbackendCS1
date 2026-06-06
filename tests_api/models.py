from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Any, Generic, TypeVar

import requests

T = TypeVar("T")


class ApiError(Exception):
    def __init__(self, status_code: int, message: str, body: dict | None = None):
        self.status_code = status_code
        self.message = message
        self.body = body or {}
        super().__init__(f"HTTP {status_code}: {message}")

    @classmethod
    def from_response(cls, resp: requests.Response) -> ApiError:
        try:
            body = resp.json()
        except (json.JSONDecodeError, ValueError):
            body = {}
        msg = (
            body.get("message", "")
            if isinstance(body, dict)
            else str(resp.text)[:200]
        )
        if not msg and isinstance(body, dict) and len(body) > 0:
            msg = "; ".join(f"{k}: {v}" for k, v in body.items())
        return cls(resp.status_code, msg, body)


@dataclass
class BaseResponse:
    status: bool
    message: str
    _raw: dict = field(default_factory=dict, repr=False)

    @classmethod
    def from_response(cls, resp: requests.Response) -> BaseResponse:
        data = resp.json()
        return cls(
            status=data.get("status", False),
            message=data.get("message", ""),
            _raw=data,
        )


@dataclass
class BaseObjectResponse(BaseResponse):
    entity: Any = None

    @classmethod
    def from_response(cls, resp: requests.Response) -> BaseObjectResponse:
        data = resp.json()
        return cls(
            status=data.get("status", False),
            message=data.get("message", ""),
            entity=data.get("entity"),
            _raw=data,
        )


@dataclass
class AuthResponse(BaseResponse):
    access_token: str = ""
    refresh_token: str = ""

    @classmethod
    def from_response(cls, resp: requests.Response) -> AuthResponse:
        data = resp.json()
        return cls(
            status=data.get("status", False),
            message=data.get("message", ""),
            access_token=data.get("accessToken", ""),
            refresh_token=data.get("refreshToken", ""),
            _raw=data,
        )


@dataclass
class ItemEntity:
    item_id: int | None = None
    title: str = ""
    description: str = ""
    seller_username: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> ItemEntity:
        return cls(
            item_id=data.get("itemId"),
            title=data.get("title", ""),
            description=data.get("description", ""),
            seller_username=data.get("seller_username", ""),
        )


@dataclass
class ItemStatusEntity:
    id: int | None = None
    current_price: float = 0.0
    highest_bid_user: str = ""
    start_time: int = 0
    end_time: int = 0
    max_end_time: int = 0
    starting_price: float = 0.0
    buy_it_now_price: float = 0.0
    bid_increment: float = 0.0
    item_status: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> ItemStatusEntity:
        return cls(
            id=data.get("id"),
            current_price=data.get("currentPrice", 0.0),
            highest_bid_user=data.get("highestBidUser", ""),
            start_time=data.get("startTime", 0),
            end_time=data.get("endTime", 0),
            max_end_time=data.get("maxEndTime", 0),
            starting_price=data.get("startingPrice", 0.0),
            buy_it_now_price=data.get("buyItNowPrice", 0.0),
            bid_increment=data.get("bidIncrement", 0.0),
            item_status=data.get("itemStatus", ""),
        )


@dataclass
class UserEntity:
    username: str = ""
    display_name: str = ""
    balance: float = 0.0

    @classmethod
    def from_dict(cls, data: dict) -> UserEntity:
        return cls(
            username=data.get("username", ""),
            display_name=data.get("displayName", ""),
            balance=data.get("balance", 0.0),
        )
