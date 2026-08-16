"""Unit tests for the AllNewsAPI Python SDK client."""

import io
import json
import urllib.error
from unittest.mock import MagicMock, patch

import pytest

from allnewsapi.client import NewsAPI
from allnewsapi.exceptions import NewsAPIError
from allnewsapi.models import Article, SearchResponse, Source, UsageResponse


def _mock_response(data: bytes) -> MagicMock:
    """Create a mock response object that works as a context manager."""
    mock_resp = MagicMock()
    mock_resp.read.return_value = data
    mock_resp.__enter__ = lambda s: s
    mock_resp.__exit__ = MagicMock(return_value=False)
    return mock_resp


_SEARCH_JSON = json.dumps(
    {
        "totalArticles": 42,
        "currentPage": 1,
        "nextPage": 2,
        "articles": [
            {
                "title": "Test Article",
                "description": "A test description",
                "category": "technology",
                "content": "Full article content",
                "country": "us",
                "region": "north_america",
                "lang": "en",
                "authors": ["Jane Doe"],
                "ai_sentiment": "positive",
                "ai_sentiment_scores": {
                    "positive": 0.9,
                    "negative": 0.05,
                    "neutral": 0.05,
                },
                "ai_entities": [
                    {"name": "OpenAI", "type": "organization"},
                ],
                "ai_summary": "A summary of the article",
                "url": "https://example.com/article",
                "image": "https://example.com/image.jpg",
                "publishedAt": "2024-06-01T12:00:00Z",
                "source": {
                    "name": "Example News",
                    "url": "https://example.com",
                },
            }
        ],
    }
).encode()

_USAGE_JSON = json.dumps(
    {
        "plan": "pro",
        "requestsUsed24Hours": 150,
        "requestsLimit24Hours": 1000,
        "requestsRemaining24Hours": 850,
        "requestsUsed30Days": 4500,
    }
).encode()


class TestSearchJsonResponse:
    """test_search_json_response - Verify search returns a properly populated SearchResponse."""

    @patch("urllib.request.urlopen")
    def test_search_json_response(self, mock_urlopen: MagicMock) -> None:
        mock_urlopen.return_value = _mock_response(_SEARCH_JSON)

        client = NewsAPI(api_key="test-key")
        result = client.search(q="technology")

        assert isinstance(result, SearchResponse)
        assert result.total_articles == 42
        assert result.current_page == 1
        assert result.next_page == 2
        assert len(result.articles) == 1

        article = result.articles[0]
        assert isinstance(article, Article)
        assert article.title == "Test Article"
        assert article.description == "A test description"
        assert article.category == "technology"
        assert article.country == "us"
        assert article.lang == "en"
        assert article.authors == ["Jane Doe"]
        assert article.ai_sentiment == "positive"
        assert article.ai_sentiment_scores["positive"] == 0.9
        assert len(article.ai_entities) == 1
        assert article.ai_entities[0].name == "OpenAI"
        assert article.ai_entities[0].type == "organization"
        assert article.ai_summary == "A summary of the article"
        assert article.url == "https://example.com/article"
        assert article.published_at == "2024-06-01T12:00:00Z"
        assert isinstance(article.source, Source)
        assert article.source.name == "Example News"
        assert article.source.url == "https://example.com"


class TestHeadlinesJsonResponse:
    """test_headlines_json_response - Verify headlines returns same structure as search."""

    @patch("urllib.request.urlopen")
    def test_headlines_json_response(self, mock_urlopen: MagicMock) -> None:
        mock_urlopen.return_value = _mock_response(_SEARCH_JSON)

        client = NewsAPI(api_key="test-key")
        result = client.headlines(category="technology")

        assert isinstance(result, SearchResponse)
        assert result.total_articles == 42
        assert result.current_page == 1
        assert result.next_page == 2
        assert len(result.articles) == 1
        assert result.articles[0].title == "Test Article"


class TestUsageResponse:
    """test_usage_response - Verify usage returns a properly populated UsageResponse."""

    @patch("urllib.request.urlopen")
    def test_usage_response(self, mock_urlopen: MagicMock) -> None:
        mock_urlopen.return_value = _mock_response(_USAGE_JSON)

        client = NewsAPI(api_key="test-key")
        result = client.usage()

        assert isinstance(result, UsageResponse)
        assert result.plan == "pro"
        assert result.requests_used_24_hours == 150
        assert result.requests_limit_24_hours == 1000
        assert result.requests_remaining_24_hours == 850
        assert result.requests_used_30_days == 4500


class TestSearchCsvReturnsBytes:
    """test_search_csv_returns_bytes - Verify CSV format returns raw bytes."""

    @patch("urllib.request.urlopen")
    def test_search_csv_returns_bytes(self, mock_urlopen: MagicMock) -> None:
        csv_data = b"title,description\nTest,Desc\n"
        mock_urlopen.return_value = _mock_response(csv_data)

        client = NewsAPI(api_key="test-key")
        result = client.search(q="test", format="csv")

        assert isinstance(result, bytes)
        assert result == csv_data


class TestSearchXlsxReturnsBytes:
    """test_search_xlsx_returns_bytes - Verify XLSX format returns raw bytes."""

    @patch("urllib.request.urlopen")
    def test_search_xlsx_returns_bytes(self, mock_urlopen: MagicMock) -> None:
        xlsx_data = b"\x50\x4b\x03\x04fake_xlsx_content"
        mock_urlopen.return_value = _mock_response(xlsx_data)

        client = NewsAPI(api_key="test-key")
        result = client.search(q="test", format="xlsx")

        assert isinstance(result, bytes)
        assert result == xlsx_data


class TestError400WithJsonBody:
    """test_error_400_with_json_body - Verify error uses detail.message from JSON body."""

    @patch("urllib.request.urlopen")
    def test_error_400_with_json_body(self, mock_urlopen: MagicMock) -> None:
        error_body = json.dumps(
            {"detail": {"message": "Missing required parameter: q"}}
        ).encode()
        http_error = urllib.error.HTTPError(
            url="https://api.allnewsapi.com/v1/search",
            code=400,
            msg="Bad Request",
            hdrs={},  # type: ignore[arg-type]
            fp=io.BytesIO(error_body),
        )
        mock_urlopen.side_effect = http_error

        client = NewsAPI(api_key="test-key")
        with pytest.raises(NewsAPIError) as exc_info:
            client.search(q="test")

        assert exc_info.value.status_code == 400
        assert exc_info.value.message == "Missing required parameter: q"


class TestError401DefaultMessage:
    """test_error_401_default_message - Verify default message when body is not JSON."""

    @patch("urllib.request.urlopen")
    def test_error_401_default_message(self, mock_urlopen: MagicMock) -> None:
        http_error = urllib.error.HTTPError(
            url="https://api.allnewsapi.com/v1/search",
            code=401,
            msg="Unauthorized",
            hdrs={},  # type: ignore[arg-type]
            fp=io.BytesIO(b"not json at all"),
        )
        mock_urlopen.side_effect = http_error

        client = NewsAPI(api_key="test-key")
        with pytest.raises(NewsAPIError) as exc_info:
            client.search(q="test")

        assert exc_info.value.status_code == 401
        assert "Unauthorized" in exc_info.value.message


class TestError429:
    """test_error_429 - Verify 429 response includes status_code for rate limit detection."""

    @patch("urllib.request.urlopen")
    def test_error_429(self, mock_urlopen: MagicMock) -> None:
        error_body = json.dumps(
            {"detail": {"message": "Rate limit exceeded"}}
        ).encode()
        http_error = urllib.error.HTTPError(
            url="https://api.allnewsapi.com/v1/search",
            code=429,
            msg="Too Many Requests",
            hdrs={},  # type: ignore[arg-type]
            fp=io.BytesIO(error_body),
        )
        mock_urlopen.side_effect = http_error

        client = NewsAPI(api_key="test-key")
        with pytest.raises(NewsAPIError) as exc_info:
            client.search(q="test")

        assert exc_info.value.status_code == 429
        assert exc_info.value.message == "Rate limit exceeded"


class TestNetworkError:
    """test_network_error - Verify URLError is wrapped into NewsAPIError with status 500."""

    @patch("urllib.request.urlopen")
    def test_network_error(self, mock_urlopen: MagicMock) -> None:
        mock_urlopen.side_effect = urllib.error.URLError("Name or service not known")

        client = NewsAPI(api_key="test-key")
        with pytest.raises(NewsAPIError) as exc_info:
            client.search(q="test")

        assert exc_info.value.status_code == 500
        assert "Request failed" in exc_info.value.message
        assert "Name or service not known" in exc_info.value.message


class TestApiKeyRequired:
    """test_api_key_required - Verify empty/None API key raises NewsAPIError at init."""

    def test_empty_string_raises(self) -> None:
        with pytest.raises(NewsAPIError) as exc_info:
            NewsAPI(api_key="")
        assert exc_info.value.status_code == 401

    def test_none_raises(self) -> None:
        with pytest.raises(NewsAPIError):
            NewsAPI(api_key=None)  # type: ignore[arg-type]


class TestCustomBaseUrl:
    """test_custom_base_url - Verify requests use the configured base URL."""

    @patch("urllib.request.urlopen")
    def test_custom_base_url(self, mock_urlopen: MagicMock) -> None:
        mock_urlopen.return_value = _mock_response(_SEARCH_JSON)

        custom_url = "https://custom.api.example.com"
        client = NewsAPI(api_key="test-key", base_url=custom_url)
        client.search(q="test")

        # Inspect the URL that was passed to urlopen
        call_args = mock_urlopen.call_args
        request_obj = call_args[0][0]
        actual_url = request_obj.full_url
        assert actual_url.startswith(custom_url)
        assert "/v1/search" in actual_url
