# AllNewsAPI Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.allnewsapi/allnewsapi-java-sdk)](https://central.sonatype.com/artifact/com.allnewsapi/allnewsapi-java-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight Java SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies. Uses `java.net.HttpURLConnection` only.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.allnewsapi</groupId>
    <artifactId>allnewsapi-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.allnewsapi:allnewsapi-java-sdk:1.0.0'
```

## Quick Start

```java
import com.allnewsapi.NewsAPI;
import com.allnewsapi.SearchOptions;
import com.allnewsapi.SearchResponse;

NewsAPI client = new NewsAPI("your-api-key");

SearchResponse results = client.search(new SearchOptions.Builder()
    .q("artificial intelligence")
    .lang("en")
    .max(10)
    .build());

for (var article : results.getArticles()) {
    System.out.printf("%s — %s%n", article.getTitle(), article.getSource().getName());
}
```

## Methods

### `search(SearchOptions options) throws NewsAPIException`

Search for news articles matching any combination of filters.

```java
SearchResponse results = client.search(new SearchOptions.Builder()
    .q("climate change")
    .startDate("2024-01-01")
    .endDate("2024-06-01")
    .lang("en", "fr")
    .category("science")
    .max(20)
    .build());
```

### `headlines(SearchOptions options) throws NewsAPIException`

Fetch top headlines with the same filtering options as search.

```java
SearchResponse headlines = client.headlines(new SearchOptions.Builder()
    .country("us")
    .category("business")
    .max(5)
    .build());
```

### `usage() throws NewsAPIException`

Check your API quota and consumption.

```java
UsageResponse usage = client.usage();
System.out.printf("Remaining: %d/%d%n",
    usage.getRequestsRemaining24Hours(),
    usage.getRequestsLimit24Hours());
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| q | `String` | Keywords to search for |
| startDate | `String` | Start date (ISO 8601) |
| endDate | `String` | End date (ISO 8601) |
| content | `boolean` | Include full article content |
| lang | `String...` | Language(s) to filter by |
| country | `String...` | Country/countries to filter by |
| region | `String...` | Region(s) to filter by |
| category | `String...` | Category/categories to filter by |
| max | `int` | Maximum results (1-100) |
| attributes | `String...` | Search in title/description/content |
| page | `int` | Page number |
| sortby | `String` | Sort by `publishedAt` or `relevance` |
| publisher | `String...` | Publisher(s) to filter |
| format | `String` | Response format: `json`, `csv`, or `xlsx` |
| aiSentiment | `String` | AI sentiment filter |
| aiEntityName | `String` | AI entity name filter |
| aiEntityType | `String` | AI entity type filter |

## Error Handling

```java
import com.allnewsapi.NewsAPI;
import com.allnewsapi.NewsAPIException;

NewsAPI client = new NewsAPI("your-api-key");

try {
    SearchResponse results = client.search(new SearchOptions.Builder().q("java").build());
} catch (NewsAPIException e) {
    System.err.printf("Error %d: %s%n", e.getStatusCode(), e.getMessage());
}
```

## License

MIT

## Support

Found a bug or have a feature request? Please [open an issue](https://github.com/AllNewsAPI/sdks/issues) on GitHub.
