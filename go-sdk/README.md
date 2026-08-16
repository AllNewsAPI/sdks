# AllNewsAPI Go SDK

[![Go Reference](https://pkg.go.dev/badge/github.com/AllNewsAPI/go-sdk.svg)](https://pkg.go.dev/github.com/AllNewsAPI/go-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A lightweight Go SDK for the [AllNewsAPI](https://allnewsapi.com) with zero external dependencies.

## Installation

```bash
go get github.com/AllNewsAPI/go-sdk
```

## Quick Start

```go
package main

import (
    "fmt"
    "log"

    newsapi "github.com/AllNewsAPI/go-sdk"
)

func main() {
    client, err := newsapi.NewClient("your-api-key")
    if err != nil {
        log.Fatal(err)
    }

    results, err := client.Search(newsapi.SearchOptions{
        Q:   "artificial intelligence",
        Lang: []string{"en"},
        Max:  10,
    })
    if err != nil {
        log.Fatal(err)
    }

    for _, article := range results.Articles {
        fmt.Printf("%s — %s\n", article.Title, article.Source.Name)
    }
}
```

## Methods

### `Search(opts SearchOptions) (*SearchResponse, error)`

Search for news articles matching any combination of filters.

```go
results, err := client.Search(newsapi.SearchOptions{
    Q:         "climate change",
    StartDate: "2024-01-01",
    EndDate:   "2024-06-01",
    Lang:      []string{"en", "fr"},
    Category:  []string{"science"},
    Max:       20,
})
```

### `Headlines(opts SearchOptions) (*SearchResponse, error)`

Fetch top headlines with the same filtering options as search.

```go
headlines, err := client.Headlines(newsapi.SearchOptions{
    Country:  []string{"us"},
    Category: []string{"business"},
    Max:      5,
})
```

### `Usage() (*UsageResponse, error)`

Check your API quota and consumption.

```go
usage, err := client.Usage()
if err != nil {
    log.Fatal(err)
}
fmt.Printf("Remaining: %d/%d\n", usage.RequestsRemaining24Hours, usage.RequestsLimit24Hours)
```

## Parameter Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| Q | `string` | Keywords to search for |
| StartDate | `string` | Start date (ISO 8601) |
| EndDate | `string` | End date (ISO 8601) |
| Content | `bool` | Include full article content |
| Lang | `[]string` | Language(s) to filter by |
| Country | `[]string` | Country/countries to filter by |
| Region | `[]string` | Region(s) to filter by |
| Category | `[]string` | Category/categories to filter by |
| Max | `int` | Maximum results (1-100) |
| Attributes | `[]string` | Search in title/description/content |
| Page | `int` | Page number |
| SortBy | `string` | Sort by `publishedAt` or `relevance` |
| Publisher | `[]string` | Publisher(s) to filter |
| Format | `string` | Response format: `json`, `csv`, or `xlsx` |
| AISentiment | `string` | AI sentiment filter |
| AIEntityName | `string` | AI entity name filter |
| AIEntityType | `string` | AI entity type filter |

## Error Handling

```go
results, err := client.Search(newsapi.SearchOptions{Q: "golang"})
if err != nil {
    var apiErr *newsapi.APIError
    if errors.As(err, &apiErr) {
        fmt.Printf("Error %d: %s\n", apiErr.StatusCode, apiErr.Message)
    }
}
```

## License

MIT
