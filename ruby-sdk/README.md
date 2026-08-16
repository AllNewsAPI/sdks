# AllNewsAPI Ruby SDK

[![Gem](https://img.shields.io/gem/v/allnewsapi)](https://rubygems.org/gems/allnewsapi)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight Ruby SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies. Uses `net/http` only.

## Installation

```bash
gem install allnewsapi
```

Or add to your Gemfile:

```ruby
gem 'allnewsapi'
```

## Quick Start

```ruby
require 'allnewsapi'

client = AllNewsAPI::Client.new('your-api-key')

results = client.search(q: 'artificial intelligence', lang: 'en', max: 10)

results['articles'].each do |article|
  puts "#{article['title']} — #{article['source']['name']}"
end
```

## Methods

### `search(**params) -> Hash`

Search for news articles matching any combination of filters.

```ruby
results = client.search(
  q: 'climate change',
  start_date: '2024-01-01',
  end_date: '2024-06-01',
  lang: ['en', 'fr'],
  category: 'science',
  max: 20
)
```

### `headlines(**params) -> Hash`

Fetch top headlines with the same filtering options as search.

```ruby
headlines = client.headlines(country: 'us', category: 'business', max: 5)
```

### `usage -> Hash`

Check your API quota and consumption.

```ruby
usage = client.usage
puts "Remaining: #{usage['requestsRemaining24Hours']}/#{usage['requestsLimit24Hours']}"
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| q | `String` | Keywords to search for |
| start_date | `String \| Date` | Start date (ISO 8601) |
| end_date | `String \| Date` | End date (ISO 8601) |
| content | `Boolean` | Include full article content |
| lang | `String \| Array` | Language(s) to filter by |
| country | `String \| Array` | Country/countries to filter by |
| region | `String \| Array` | Region(s) to filter by |
| category | `String \| Array` | Category/categories to filter by |
| max | `Integer` | Maximum results (1-100) |
| attributes | `String \| Array` | Search in title/description/content |
| page | `Integer` | Page number |
| sortby | `String` | Sort by `publishedAt` or `relevance` |
| publisher | `String \| Array` | Publisher(s) to filter |
| format | `String` | Response format: `json`, `csv`, or `xlsx` |
| ai_sentiment | `String` | AI sentiment filter |
| ai_entity_name | `String` | AI entity name filter |
| ai_entity_type | `String` | AI entity type filter |

## Error Handling

```ruby
require 'allnewsapi'

client = AllNewsAPI::Client.new('your-api-key')

begin
  results = client.search(q: 'ruby')
rescue AllNewsAPI::NewsAPIError => e
  puts "Error #{e.status_code}: #{e.message}"
end
```

## License

MIT

## Support

Found a bug or have a feature request? Please [open an issue](https://github.com/AllNewsAPI/sdks/issues) on GitHub.
