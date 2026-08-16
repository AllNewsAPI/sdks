"""AllNewsAPI response models."""

from dataclasses import dataclass


@dataclass
class Source:
    """A news source."""

    name: str
    url: str


@dataclass
class Entity:
    """An AI-extracted entity from an article."""

    name: str
    type: str


@dataclass
class Article:
    """A news article returned by the API."""

    title: str
    description: str
    category: str
    content: str
    country: str
    region: str
    lang: str
    authors: list[str]
    ai_sentiment: str
    ai_sentiment_scores: dict[str, float]
    ai_entities: list[Entity]
    ai_summary: str
    url: str
    image: str
    published_at: str
    source: Source


@dataclass
class SearchResponse:
    """Response from the search or headlines endpoint."""

    total_articles: int
    current_page: int
    next_page: int | None
    articles: list[Article]


@dataclass
class UsageResponse:
    """Response from the usage endpoint."""

    plan: str
    requests_used_24_hours: int
    requests_limit_24_hours: int
    requests_remaining_24_hours: int
    requests_used_30_days: int
