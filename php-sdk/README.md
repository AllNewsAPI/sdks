# AllNewsAPI PHP SDK

[![Packagist](https://img.shields.io/packagist/v/allnewsapi/allnewsapi)](https://packagist.org/packages/allnewsapi/allnewsapi)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight PHP SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies. Uses the `curl` extension only.

## Installation

```bash
composer require allnewsapi/allnewsapi
```

## Quick Start

```php
<?php

require_once 'vendor/autoload.php';

use AllNewsAPI\NewsAPI;

$client = new NewsAPI('your-api-key');

$results = $client->search(['q' => 'artificial intelligence', 'lang' => 'en', 'max' => 10]);

foreach ($results['articles'] as $article) {
    echo $article['title'] . ' — ' . $article['source']['name'] . "\n";
}
```

## Methods

### `search(array $params = []): array`

Search for news articles matching any combination of filters.

```php
$results = $client->search([
    'q' => 'climate change',
    'startDate' => '2024-01-01',
    'endDate' => '2024-06-01',
    'lang' => ['en', 'fr'],
    'category' => 'science',
    'max' => 20,
]);
```

### `headlines(array $params = []): array`

Fetch top headlines with the same filtering options as search.

```php
$headlines = $client->headlines(['country' => 'us', 'category' => 'business', 'max' => 5]);
```

### `usage(): array`

Check your API quota and consumption.

```php
$usage = $client->usage();
echo "Remaining: {$usage['requestsRemaining24Hours']}/{$usage['requestsLimit24Hours']}\n";
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| q | `string` | Keywords to search for |
| startDate | `string` | Start date (ISO 8601) |
| endDate | `string` | End date (ISO 8601) |
| content | `bool` | Include full article content |
| lang | `string \| array` | Language(s) to filter by |
| country | `string \| array` | Country/countries to filter by |
| region | `string \| array` | Region(s) to filter by |
| category | `string \| array` | Category/categories to filter by |
| max | `int` | Maximum results (1-100) |
| attributes | `string \| array` | Search in title/description/content |
| page | `int` | Page number |
| sortby | `string` | Sort by `publishedAt` or `relevance` |
| publisher | `string \| array` | Publisher(s) to filter |
| format | `string` | Response format: `json`, `csv`, or `xlsx` |
| ai_sentiment | `string` | AI sentiment filter |
| ai_entity_name | `string` | AI entity name filter |
| ai_entity_type | `string` | AI entity type filter |

## Error Handling

```php
use AllNewsAPI\NewsAPI;
use AllNewsAPI\NewsAPIException;

$client = new NewsAPI('your-api-key');

try {
    $results = $client->search(['q' => 'php']);
} catch (NewsAPIException $e) {
    echo "Error {$e->getStatusCode()}: {$e->getMessage()}\n";
}
```

## License

MIT
