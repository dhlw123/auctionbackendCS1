from __future__ import annotations

import uuid

import pytest

from .client import AdminSession, AuctionClient, UserSession
from .constants import (
    ADMIN_PASSWORD,
    ADMIN_USERNAME,
    API_BASE_URL,
    DEFAULT_USER_PASSWORD,
)
from .models import ApiError


@pytest.fixture(scope="session")
def client() -> AuctionClient:
    c = AuctionClient(API_BASE_URL)
    yield c
    c.close()


@pytest.fixture(scope="session")
def admin(client: AuctionClient) -> AdminSession:
    try:
        auth = client.login(ADMIN_USERNAME, ADMIN_PASSWORD)
    except ApiError:
        client.register(ADMIN_USERNAME, "Administrator", ADMIN_PASSWORD)
        auth = client.login(ADMIN_USERNAME, ADMIN_PASSWORD)
    return AdminSession(client, auth, ADMIN_USERNAME)


@pytest.fixture
def fresh_user(client: AuctionClient, admin: AdminSession) -> UserSession:
    uid = uuid.uuid4().hex[:12]
    uname = f"tester_{uid}"
    client.register(uname, f"Test {uid}", DEFAULT_USER_PASSWORD)
    auth = client.login(uname, DEFAULT_USER_PASSWORD)
    session = UserSession(client, auth, uname)
    yield session
    try:
        admin.ban_user(uname)
    except ApiError:
        pass


@pytest.fixture
def second_user(client: AuctionClient, admin: AdminSession) -> UserSession:
    uid = uuid.uuid4().hex[:12]
    uname = f"second_{uid}"
    client.register(uname, f"Second {uid}", DEFAULT_USER_PASSWORD)
    auth = client.login(uname, DEFAULT_USER_PASSWORD)
    session = UserSession(client, auth, uname)
    yield session
    try:
        admin.ban_user(uname)
    except ApiError:
        pass
