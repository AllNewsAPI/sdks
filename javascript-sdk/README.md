# AllNewsAPI TypeScript SDK

[![npm](https://img.shields.io/npm/v/allnewsapi)](https://www.npmjs.com/package/allnewsapi)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight TypeScript SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies. Uses native `fetch` (Node.js 18+).

## Installation

```bash
npm install allnewsapi
```

## Quick Start

```typescript
import { NewsAPI } from 'allnewsapi';

const client = new NewsAPI('your-api-key');

const results = await client.search({ q: 'artificial intelligence', lang: 'en', max: 10 });

for (const article of results.articles) {
  console.log(`${article.title} — ${article.source.name}`);
}
```

## Methods

### `search(options?: SearchOptions): Promise<SearchResponse>`

Search for news articles matching any combination of filters.

```typescript
const results = await client.search({
  q: 'climate change',
  startDate: '2024-01-01',
  endDate: '2024-06-01',
  lang: ['en', 'fr'],
  category: 'science',
  max: 20,
});
```

### `headlines(options?: SearchOptions): Promise<SearchResponse>`

Fetch top headlines with the same filtering options as search.

```typescript
const headlines = await client.headlines({ country: 'us', category: 'business', max: 5 });
```

### `usage(): Promise<UsageResponse>`

Check your API quota and consumption.

```typescript
const usage = await client.usage();
console.log(`Remaining: ${usage.requestsRemaining24Hours}/${usage.requestsLimit24Hours}`);
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| q | `string` | Keywords to search for |
| startDate | `string \| Date` | Start date (ISO 8601) |
| endDate | `string \| Date` | End date (ISO 8601) |
| content | `boolean` | Include full article content |
| lang | `string \| string[]` | Language(s) to filter by |
| country | `string \| string[]` | Country/countries to filter by |
| region | `string \| string[]` | Region(s) to filter by |
| category | `string \| string[]` | Category/categories to filter by |
| max | `number` | Maximum results (1-100) |
| attributes | `string \| string[]` | Search in title/description/content |
| page | `number` | Page number |
| sortby | `'publishedAt' \| 'relevance'` | Sort order |
| publisher | `string \| string[]` | Publisher(s) to filter |
| format | `'json' \| 'csv' \| 'xlsx'` | Response format |
| ai_sentiment | `string` | AI sentiment filter |
| ai_entity_name | `string` | AI entity name filter |
| ai_entity_type | `string` | AI entity type filter |

## Error Handling

```typescript
import { NewsAPI, NewsAPIError } from 'allnewsapi';

const client = new NewsAPI('your-api-key');

try {
  const results = await client.search({ q: 'typescript' });
} catch (error) {
  if (error instanceof NewsAPIError) {
    console.error(`Error ${error.statusCode}: ${error.message}`);
  }
  throw error;
}
```

## License

MIT
