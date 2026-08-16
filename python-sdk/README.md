# AllNewsAPI Python SDK

[![PyPI](https://img.shields.io/pypi/v/allnewsapi)](https://pypi.org/project/allnewsapi/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight Python SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies.

## Installation

```bash
pip install allnewsapi
```

## Quick Start

```python
from allnewsapi import NewsAPI

client = NewsAPI("your-api-key")

results = client.search(q="artificial intelligence", lang="en", max=10)

for article in results.articles:
    print(f"{article.title} — {article.source.name}")
```

## Methods

### `search(**kwargs) -> SearchResponse`

Search for news articles matching any combination of filters.

```python
results = client.search(
    q="climate change",
    start_date="2024-01-01",
    end_date="2024-06-01",
    lang=["en", "fr"],
    category="science",
    max=20,
)
```

### `headlines(**kwargs) -> SearchResponse`

Fetch top headlines with the same filtering options as search.

```python
headlines = client.headlines(country="us", category="business", max=5)
```

### `usage() -> UsageResponse`

Check your API quota and consumption.

```python
usage = client.usage()
print(f"Remaining: {usage.requests_remaining_24_hours}/{usage.requests_limit_24_hours}")
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| q | `str` | Keywords to search for |
| start_date | `str \| datetime` | Start date (ISO 8601) |
| end_date | `str \| datetime` | End date (ISO 8601) |
| content | `bool` | Include full article content |
| lang | `str \| list[str]` | Language(s) to filter by |
| country | `str \| list[str]` | Country/countries to filter by |
| region | `str \| list[str]` | Region(s) to filter by |
| category | `str \| list[str]` | Category/categories to filter by |
| max | `int` | Maximum results (1-100) |
| attributes | `str \| list[str]` | Search in title/description/content |
| page | `int` | Page number |
| sortby | `str` | Sort by `publishedAt` or `relevance` |
| publisher | `str \| list[str]` | Publisher(s) to filter |
| format | `str` | Response format: `json`, `csv`, or `xlsx` |
| ai_sentiment | `str` | AI sentiment filter |
| ai_entity_name | `str` | AI entity name filter |
| ai_entity_type | `str` | AI entity type filter |

## Error Handling

```python
from allnewsapi import NewsAPI, NewsAPIError

client = NewsAPI("your-api-key")

try:
    results = client.search(q="python")
except NewsAPIError as e:
    print(f"Error {e.status_code}: {e.message}")
```

## License

MIT

## Support

Found a bug or have a feature request? Please [open an issue](https://github.com/AllNewsAPI/sdks/issues) on GitHub.
