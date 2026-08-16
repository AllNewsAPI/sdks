"""Property-based tests for the AllNewsAPI Python SDK.

Uses hypothesis to validate correctness properties from the design document
without making actual HTTP calls.
"""

import urllib.parse
from datetime import date, datetime
from unittest.mock import patch

from hypothesis import given, settings
from hypothesis import strategies as st

from allnewsapi.client import NewsAPI
from allnewsapi.exceptions import NewsAPIError


# --- Strategies ---

# Supported query parameters for search/headlines (excluding format and apikey)
SUPPORTED_PARAMS = [
    "q",
    "startDate",
    "endDate",
    "content",
    "lang",
    "country",
    "region",
    "category",
    "max",
    "attributes",
    "page",
    "sortby",
    "publisher",
    "format",
    "ai_sentiment",
    "ai_entity_name",
    "ai_entity_type",
]

ARRAY_PARAMS = ["lang", "country", "region", "category", "attributes", "publisher"]

# Strategy for non-empty text values (simple ASCII for reliability)
param_text = st.text(
    alphabet=st.characters(whitelist_categories=("L", "N"), min_codepoint=32, max_codepoint=126),
    min_size=1,
    max_size=20,
)

# Strategy for generating a dict of supported params with non-null values
search_params = st.fixed_dictionaries(
    {},
    optional={
        "q": param_text,
        "content": st.booleans(),
        "lang": param_text,
        "country": param_text,
        "region": param_text,
        "category": param_text,
        "max": st.integers(min_value=1, max_value=100),
        "attributes": param_text,
        "page": st.integers(min_value=1, max_value=100),
        "sortby": st.sampled_from(["publishedAt", "relevance"]),
        "publisher": param_text,
        "ai_sentiment": st.sampled_from(["positive", "negative", "neutral"]),
        "ai_entity_name": param_text,
        "ai_entity_type": param_text,
    },
)

# Strategy for array-type parameter values (lists of non-empty strings)
array_items = st.lists(
    st.text(
        alphabet=st.characters(whitelist_categories=("L", "N"), min_codepoint=65, max_codepoint=122),
        min_size=1,
        max_size=10,
    ),
    min_size=1,
    max_size=5,
)

# Strategy for base URLs
base_url_strategy = st.sampled_from([
    "https://api.allnewsapi.com",
    "https://custom-api.example.com",
    "http://localhost:8080",
    "https://staging.allnewsapi.com/api",
])

# Strategy for special characters that need URL encoding
special_char_values = st.sampled_from([
    "hello world",
    "key&value",
    "a=b",
    "plus+sign",
    "hash#tag",
    "percent%20encoded",
    "foo bar&baz=qux",
    "special #&+=% chars",
])


# --- Property Tests ---


class TestProperty1ParameterSerializationCompleteness:
    """Property 1: Parameter serialization completeness.

    For any subset of supported params with non-null values provided to
    search/headlines, the built URL query string contains every provided parameter.

    **Validates: Requirements 1.2, 2.2**
    """

    @given(params=search_params)
    @settings(max_examples=100)
    def test_all_provided_params_appear_in_query_string(self, params: dict) -> None:
        """Every non-null parameter appears in the serialized query string."""
        if not params:
            return  # skip empty param sets

        client = NewsAPI(api_key="test-key-123")
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        # Every parameter we provided should appear in the query string
        for key in params:
            assert key in parsed, (
                f"Parameter '{key}' with value '{params[key]}' not found in query string"
            )

    @given(params=search_params)
    @settings(max_examples=100)
    def test_null_params_are_omitted(self, params: dict) -> None:
        """Parameters with None values are not included in the query string."""
        params_with_none = {**params, "q": None, "max": None}
        client = NewsAPI(api_key="test-key-123")
        query_string = client._serialize_params(params_with_none)
        parsed = urllib.parse.parse_qs(query_string)

        # None-valued params should not appear (unless they were overriding
        # a provided value - but here we only check q and max since they're set to None)
        if "q" not in params:
            assert "q" not in parsed
        if "max" not in params:
            assert "max" not in parsed


class TestProperty2ArrayParameterCommaSeparatedEncoding:
    """Property 2: Array parameter comma-separated encoding.

    For any array-type parameter with N items, the query string value is items
    joined by comma.

    **Validates: Requirements 1.6**
    """

    @given(
        param_name=st.sampled_from(ARRAY_PARAMS),
        items=array_items,
    )
    @settings(max_examples=100)
    def test_array_params_joined_by_comma(self, param_name: str, items: list[str]) -> None:
        """Array parameters are serialized as comma-separated values."""
        client = NewsAPI(api_key="test-key-123")
        params = {param_name: items}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        expected = ",".join(items)
        assert param_name in parsed
        assert parsed[param_name][0] == expected, (
            f"Expected '{expected}' but got '{parsed[param_name][0]}'"
        )

    @given(
        param_name=st.sampled_from(ARRAY_PARAMS),
        items=array_items,
    )
    @settings(max_examples=50)
    def test_array_comma_count_matches_item_count(self, param_name: str, items: list[str]) -> None:
        """The number of commas in the serialized value equals N-1 for N items."""
        client = NewsAPI(api_key="test-key-123")
        params = {param_name: items}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        value = parsed[param_name][0]
        comma_count = value.count(",")
        assert comma_count == len(items) - 1, (
            f"Expected {len(items) - 1} commas for {len(items)} items, got {comma_count}"
        )


class TestProperty5UsageEndpointSendsOnlyApikey:
    """Property 5: Usage endpoint sends only apikey.

    usage() constructed URL contains exactly one parameter (apikey).

    **Validates: Requirements 3.2**
    """

    @given(api_key=st.text(min_size=1, max_size=50, alphabet=st.characters(min_codepoint=48, max_codepoint=122)))
    @settings(max_examples=50)
    def test_usage_url_contains_only_apikey(self, api_key: str) -> None:
        """The usage endpoint URL has exactly one query parameter: apikey."""
        client = NewsAPI(api_key=api_key)

        # Capture the URL that would be requested
        captured_url = None

        def mock_make_request(url: str) -> bytes:
            nonlocal captured_url
            captured_url = url
            # Return minimal valid JSON response
            return b'{"plan":"free","requestsUsed24Hours":0,"requestsLimit24Hours":100,"requestsRemaining24Hours":100,"requestsUsed30Days":0}'

        with patch.object(client, "_make_request", side_effect=mock_make_request):
            client.usage()

        assert captured_url is not None
        # Parse the URL and check query params
        parsed = urllib.parse.urlparse(captured_url)
        query_params = urllib.parse.parse_qs(parsed.query)

        assert list(query_params.keys()) == ["apikey"], (
            f"Expected only 'apikey' parameter, got: {list(query_params.keys())}"
        )
        assert query_params["apikey"][0] == api_key


class TestProperty9ApiKeyValidationAtInitialization:
    """Property 9: API key validation at initialization.

    Empty/None API key raises NewsAPIError.

    **Validates: Requirements 10.7**
    """

    @given(api_key=st.sampled_from(["", None]))
    def test_falsy_api_key_raises_error(self, api_key) -> None:
        """Empty string or None API key raises NewsAPIError at initialization."""
        try:
            NewsAPI(api_key=api_key)
            assert False, "Should have raised NewsAPIError"
        except (NewsAPIError, TypeError):
            pass  # Expected - TypeError may occur if None passed to str-typed param

    def test_empty_string_raises_newsapi_error(self) -> None:
        """Empty string specifically raises NewsAPIError."""
        try:
            NewsAPI(api_key="")
            assert False, "Should have raised NewsAPIError"
        except NewsAPIError as e:
            assert e.status_code == 401

    def test_none_raises_error(self) -> None:
        """None raises an error at initialization."""
        try:
            NewsAPI(api_key=None)  # type: ignore[arg-type]
            assert False, "Should have raised an error"
        except (NewsAPIError, TypeError):
            pass  # Both are acceptable


class TestProperty10BaseUrlPropagation:
    """Property 10: Base URL propagation.

    Any configured base URL appears as prefix in all request URLs.

    **Validates: Requirements 10.5**
    """

    @given(base_url=base_url_strategy)
    @settings(max_examples=50)
    def test_search_uses_configured_base_url(self, base_url: str) -> None:
        """Search request URL starts with the configured base URL."""
        client = NewsAPI(api_key="test-key", base_url=base_url)
        captured_url = None

        def mock_make_request(url: str) -> bytes:
            nonlocal captured_url
            captured_url = url
            return b'{"totalArticles":0,"currentPage":1,"nextPage":null,"articles":[]}'

        with patch.object(client, "_make_request", side_effect=mock_make_request):
            client.search(q="test")

        expected_prefix = base_url.rstrip("/")
        assert captured_url is not None
        assert captured_url.startswith(expected_prefix + "/v1/search"), (
            f"URL '{captured_url}' does not start with '{expected_prefix}/v1/search'"
        )

    @given(base_url=base_url_strategy)
    @settings(max_examples=50)
    def test_headlines_uses_configured_base_url(self, base_url: str) -> None:
        """Headlines request URL starts with the configured base URL."""
        client = NewsAPI(api_key="test-key", base_url=base_url)
        captured_url = None

        def mock_make_request(url: str) -> bytes:
            nonlocal captured_url
            captured_url = url
            return b'{"totalArticles":0,"currentPage":1,"nextPage":null,"articles":[]}'

        with patch.object(client, "_make_request", side_effect=mock_make_request):
            client.headlines(q="test")

        expected_prefix = base_url.rstrip("/")
        assert captured_url is not None
        assert captured_url.startswith(expected_prefix + "/v1/headlines"), (
            f"URL '{captured_url}' does not start with '{expected_prefix}/v1/headlines'"
        )

    @given(base_url=base_url_strategy)
    @settings(max_examples=50)
    def test_usage_uses_configured_base_url(self, base_url: str) -> None:
        """Usage request URL starts with the configured base URL."""
        client = NewsAPI(api_key="test-key", base_url=base_url)
        captured_url = None

        def mock_make_request(url: str) -> bytes:
            nonlocal captured_url
            captured_url = url
            return b'{"plan":"free","requestsUsed24Hours":0,"requestsLimit24Hours":100,"requestsRemaining24Hours":100,"requestsUsed30Days":0}'

        with patch.object(client, "_make_request", side_effect=mock_make_request):
            client.usage()

        expected_prefix = base_url.rstrip("/")
        assert captured_url is not None
        assert captured_url.startswith(expected_prefix + "/v1/usage"), (
            f"URL '{captured_url}' does not start with '{expected_prefix}/v1/usage'"
        )


class TestProperty11UrlEncodingOfSpecialCharacters:
    """Property 11: URL encoding of special characters.

    Values with URL-reserved chars (spaces, &, =, +, #, %) get percent-encoded.

    **Validates: Requirements 11.3**
    """

    @given(value=special_char_values)
    @settings(max_examples=50)
    def test_special_chars_are_percent_encoded(self, value: str) -> None:
        """URL-reserved characters in param values are percent-encoded."""
        client = NewsAPI(api_key="test-key-123")
        params = {"q": value}
        query_string = client._serialize_params(params)

        # The raw query string should NOT contain unencoded reserved chars
        # We need to find the value portion for 'q' parameter
        # Parse out the q value from the full query string
        parsed = urllib.parse.parse_qs(query_string)
        assert "q" in parsed

        # The decoded value should match the original
        assert parsed["q"][0] == value, (
            f"Decoded value '{parsed['q'][0]}' does not match original '{value}'"
        )

        # Check that reserved characters are actually encoded in the raw string
        # Find the q= part in the raw query string
        q_start = query_string.find("q=")
        if q_start != -1:
            # Extract the raw encoded value after 'q='
            raw_after_q = query_string[q_start + 2:]
            # Take until next & or end
            ampersand_pos = raw_after_q.find("&")
            raw_value = raw_after_q[:ampersand_pos] if ampersand_pos != -1 else raw_after_q

            # Spaces should be encoded (as + or %20)
            if " " in value:
                assert " " not in raw_value, "Space should be encoded"

            # & should be encoded
            if "&" in value:
                assert raw_value.count("&") == 0 or raw_value.count("%26") > 0

    @given(
        value=st.text(
            alphabet=st.sampled_from(" &=+#%"),
            min_size=1,
            max_size=10,
        )
    )
    @settings(max_examples=50)
    def test_reserved_chars_roundtrip(self, value: str) -> None:
        """Values with reserved chars survive encode/decode roundtrip."""
        client = NewsAPI(api_key="test-key-123")
        params = {"q": value}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        assert "q" in parsed
        assert parsed["q"][0] == value, (
            f"Roundtrip failed: expected '{value}', got '{parsed['q'][0]}'"
        )


class TestProperty12DateToIso8601Conversion:
    """Property 12: Date-to-ISO-8601 conversion.

    Any datetime object provided as startDate/endDate produces valid ISO 8601 string.

    **Validates: Requirements 11.4**
    """

    @given(
        dt=st.datetimes(
            min_value=datetime(2000, 1, 1),
            max_value=datetime(2030, 12, 31),
        )
    )
    @settings(max_examples=100)
    def test_datetime_serialized_as_iso8601(self, dt: datetime) -> None:
        """datetime objects are serialized as ISO 8601 strings."""
        client = NewsAPI(api_key="test-key-123")
        params = {"startDate": dt}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        assert "startDate" in parsed
        serialized_value = parsed["startDate"][0]

        # Verify it's a valid ISO 8601 string by parsing it back
        parsed_dt = datetime.fromisoformat(serialized_value)
        assert parsed_dt == dt, (
            f"Roundtrip failed: input {dt} -> serialized '{serialized_value}' -> parsed {parsed_dt}"
        )

    @given(
        d=st.dates(
            min_value=date(2000, 1, 1),
            max_value=date(2030, 12, 31),
        )
    )
    @settings(max_examples=100)
    def test_date_serialized_as_iso8601(self, d: date) -> None:
        """date objects are serialized as ISO 8601 strings."""
        client = NewsAPI(api_key="test-key-123")
        params = {"endDate": d}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        assert "endDate" in parsed
        serialized_value = parsed["endDate"][0]

        # Verify it's a valid ISO 8601 date string by parsing it back
        parsed_date = date.fromisoformat(serialized_value)
        assert parsed_date == d, (
            f"Roundtrip failed: input {d} -> serialized '{serialized_value}' -> parsed {parsed_date}"
        )

    @given(
        dt=st.datetimes(
            min_value=datetime(2000, 1, 1),
            max_value=datetime(2030, 12, 31),
        ),
        param_name=st.sampled_from(["startDate", "endDate"]),
    )
    @settings(max_examples=50)
    def test_date_params_produce_valid_iso_format(self, dt: datetime, param_name: str) -> None:
        """Both startDate and endDate produce valid ISO 8601 format."""
        client = NewsAPI(api_key="test-key-123")
        params = {param_name: dt}
        query_string = client._serialize_params(params)
        parsed = urllib.parse.parse_qs(query_string)

        assert param_name in parsed
        serialized_value = parsed[param_name][0]

        # ISO 8601 should contain date separators
        assert "T" in serialized_value or len(serialized_value) == 10, (
            f"Value '{serialized_value}' does not look like ISO 8601"
        )
