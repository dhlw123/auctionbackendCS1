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
    """Session-scoped HTTP client shared across all tests.

    Lives for the entire test run. Connections are pooled via
    requests.Session for performance.
    """
    c = AuctionClient(API_BASE_URL)
    yield c
    c.close()


@pytest.fixture(scope="session")
def admin(client: AuctionClient) -> AdminSession:
    """Session-scoped admin session, auto-registered if needed.

    Attempts to log in as admin. If the account doesn't exist
    (fresh database), registers it first. Returns an AdminSession
    with an active admin JWT valid for the entire test run.
    """
    try:
        auth = client.login(ADMIN_USERNAME, ADMIN_PASSWORD)
    except ApiError:
        client.register(ADMIN_USERNAME, "Administrator", ADMIN_PASSWORD)
        auth = client.login(ADMIN_USERNAME, ADMIN_PASSWORD)
    return AdminSession(client, auth, ADMIN_USERNAME)


@pytest.fixture
def fresh_user(client: AuctionClient, admin: AdminSession) -> UserSession:
    """Per-test unique regular user, auto-banned on teardown.

    Registers a new user with a UUID-based username, logs in,
    and returns a UserSession. On teardown the user is banned
    via admin to clean up tokens and prevent accidental reuse.

    Use this fixture whenever a test needs a seller or a
    single bidder.
    """
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
    """Per-test second unique user for bidder-vs-seller scenarios.

    Same lifecycle as fresh_user: registers, logs in, returns
    UserSession, banned on teardown. Use this when a test needs
    two distinct non-admin users (e.g., seller publishes an item
    and a different user places a bid on it).
    """
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


@pytest.fixture
def third_user(client: AuctionClient, admin: AdminSession) -> UserSession:
    """Per-test second unique user for bidder-vs-seller scenarios.

    Same lifecycle as fresh_user: registers, logs in, returns
    UserSession, banned on teardown. Use this when a test needs
    two distinct non-admin users (e.g., seller publishes an item
    and a different user places a bid on it).
    """
    uid = uuid.uuid4().hex[:12]
    uname = f"third_{uid}"
    client.register(uname, f"Third {uid}", DEFAULT_USER_PASSWORD)
    auth = client.login(uname, DEFAULT_USER_PASSWORD)
    session = UserSession(client, auth, uname)
    yield session
    try:
        admin.ban_user(uname)
    except ApiError:
        pass
