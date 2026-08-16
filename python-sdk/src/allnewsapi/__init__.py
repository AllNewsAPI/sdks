"""AllNewsAPI - Python SDK for AllNewsAPI."""

from allnewsapi.client import NewsAPI
from allnewsapi.exceptions import NewsAPIError
from allnewsapi.models import (
    Article,
    Entity,
    SearchResponse,
    Source,
    UsageResponse,
)

__all__ = [
    "Article",
    "Entity",
    "NewsAPI",
    "NewsAPIError",
    "SearchResponse",
    "Source",
    "UsageResponse",
]
__version__ = "0.1.1"
