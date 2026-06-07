import os

API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8080")
ADMIN_USERNAME = os.environ.get("ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "admin")
DEFAULT_USER_PASSWORD = "password123"
BAN_HASH = "1"
REQUEST_TIMEOUT = 10  # seconds
JWT_EXPIRATION_MS = 360_000  # 6 minutes
