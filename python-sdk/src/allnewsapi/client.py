"""AllNewsAPI client implementation."""

import json
import urllib.parse
import urllib.request
from datetime import date, datetime
from typing import Any

from allnewsapi.exceptions import NewsAPIError
from allnewsapi.models import (
    Article,
    Entity,
    SearchResponse,
    Source,
    UsageResponse,
)

_DEFAULT_ERROR_MESSAGES: dict[int, str] = {
    400: "Bad Request - Your request is invalid",
    401: "Unauthorized - Invalid API Key or Account status is inactive",
    403: "Forbidden - Your account is not authorized to make that request",
    429: "Too Many Requests - You have reached your daily request limit",
    500: "Internal Server Error - We had a problem with our server",
    503: "Service Unavailable - We're temporarily offline for maintenance",
}


class NewsAPI:
    """Client for the AllNewsAPI.

    Args:
        api_key: Your AllNewsAPI key (required).
        base_url: API base URL. Defaults to https://api.allnewsapi.com.
        timeout: Request timeout in seconds. Defaults to 30.
    """

    def __init__(
        self,
        api_key: str,
        base_url: str = "https://api.allnewsapi.com",
        timeout: int = 30,
    ) -> None:
        if not api_key:
            raise NewsAPIError(401, "API key is required")
        self._api_key = api_key
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    def search(self, **kwargs: Any) -> SearchResponse | bytes:
        """Search for articles.

        Returns:
            SearchResponse for JSON format, raw bytes for CSV/XLSX.
        """
        return self._request("/search", kwargs)

    def headlines(self, **kwargs: Any) -> SearchResponse | bytes:
        """Get top headlines.

        Returns:
            SearchResponse for JSON format, raw bytes for CSV/XLSX.
        """
        return self._request("/headlines", kwargs)

    def usage(self) -> UsageResponse:
        """Get account usage statistics.

        Returns:
            UsageResponse with plan and request count details.
        """
        params = {"apikey": self._api_key}
        url = f"{self._base_url}/usage?{urllib.parse.urlencode(params)}"
        data = self._make_request(url)
        body = json.loads(data)
        return self._parse_usage_response(body)

    def _request(
        self, endpoint: str, params: dict[str, Any]
    ) -> SearchResponse | bytes:
        """Build URL, make request, and parse response."""
        fmt = params.get("format", "json")
        query_params = self._serialize_params(params)
        url = f"{self._base_url}{endpoint}?{query_params}"
        data = self._make_request(url)

        if fmt in ("csv", "xlsx"):
            return data

        body = json.loads(data)
        return self._parse_search_response(body)

    def _serialize_params(self, params: dict[str, Any]) -> str:
        """Serialize parameters into a URL-encoded query string."""
        serialized: dict[str, str] = {"apikey": self._api_key}

        for key, value in params.items():
            if value is None:
                continue
            if isinstance(value, (list, tuple)):
                serialized[key] = ",".join(str(v) for v in value)
            elif isinstance(value, datetime):
                serialized[key] = value.isoformat()
            elif isinstance(value, date):
                serialized[key] = value.isoformat()
            elif isinstance(value, bool):
                serialized[key] = "true" if value else "false"
            else:
                serialized[key] = str(value)

        return urllib.parse.urlencode(serialized)

    def _make_request(self, url: str) -> bytes:
        """Execute HTTP GET and return response body bytes."""
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "AllNewsAPI-Python/1.0.0")
            with urllib.request.urlopen(req, timeout=self._timeout) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            status_code = e.code
            message = self._extract_error_message(e, status_code)
            raise NewsAPIError(status_code, message) from e
        except Exception as e:
            raise NewsAPIError(500, f"Request failed: {e}") from e

    def _extract_error_message(
        self, error: urllib.error.HTTPError, status_code: int
    ) -> str:
        """Extract error message from HTTP error response."""
        try:
            body = error.read()
            data = json.loads(body)
            message = data.get("detail", {}).get("message")
            if message:
                return message
        except (json.JSONDecodeError, ValueError, AttributeError):
            pass

        return _DEFAULT_ERROR_MESSAGES.get(
            status_code,
            f"HTTP Error {status_code}",
        )

    @staticmethod
    def _parse_search_response(body: dict[str, Any]) -> SearchResponse:
        """Parse a JSON response body into a SearchResponse."""
        articles = [
            NewsAPI._parse_article(a) for a in body.get("articles", [])
        ]
        return SearchResponse(
            total_articles=body.get("totalArticles", 0),
            current_page=body.get("currentPage", 1),
            next_page=body.get("nextPage"),
            articles=articles,
        )

    @staticmethod
    def _parse_article(data: dict[str, Any]) -> Article:
        """Parse a JSON dict into an Article dataclass."""
        source_data = data.get("source", {})
        source = Source(
            name=source_data.get("name", ""),
            url=source_data.get("url", ""),
        )
        entities = [
            Entity(name=e.get("name", ""), type=e.get("type", ""))
            for e in data.get("ai_entities", [])
        ]
        return Article(
            title=data.get("title", ""),
            description=data.get("description", ""),
            category=data.get("category", ""),
            content=data.get("content", ""),
            country=data.get("country", ""),
            region=data.get("region", ""),
            lang=data.get("lang", ""),
            authors=data.get("authors", []),
            ai_sentiment=data.get("ai_sentiment", ""),
            ai_sentiment_scores=data.get("ai_sentiment_scores", {}),
            ai_entities=entities,
            ai_summary=data.get("ai_summary", ""),
            url=data.get("url", ""),
            image=data.get("image", ""),
            published_at=data.get("publishedAt", ""),
            source=source,
        )

    @staticmethod
    def _parse_usage_response(body: dict[str, Any]) -> UsageResponse:
        """Parse a JSON response body into a UsageResponse."""
        return UsageResponse(
            plan=body.get("plan", ""),
            requests_used_24_hours=body.get("requestsUsed24Hours", 0),
            requests_limit_24_hours=body.get("requestsLimit24Hours", 0),
            requests_remaining_24_hours=body.get(
                "requestsRemaining24Hours", 0
            ),
            requests_used_30_days=body.get("requestsUsed30Days", 0),
        )
