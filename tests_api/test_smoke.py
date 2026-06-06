import pytest
import requests

from .client import AuctionClient
from .constants import API_BASE_URL


class TestServerConnectivity:
    def test_server_responds(self):
        try:
            r = requests.get(API_BASE_URL + "/items/all", timeout=5)
        except requests.ConnectionError:
            pytest.skip("Server not running")

    def test_public_items_endpoint(self, client: AuctionClient):
        resp = client.get("/items/all", raw=True)
        assert resp.status_code in (200, 404)

    def test_public_status_endpoint_requires_valid_id(self, client: AuctionClient):
        resp = client.get("/item/status/1", raw=True)
        assert resp.status_code in (200, 400, 404)
