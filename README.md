# AllNewsAPI SDKs

![CI](https://github.com/AllNewsAPI/allnewsapi-sdks/actions/workflows/ci.yml/badge.svg)

Official SDKs for the [AllNewsAPI](https://allnewsapi.com) — a news aggregation REST API providing search, headlines, and usage endpoints across 50,000+ sources worldwide.

## Architecture

This monorepo contains **6 language SDKs**, each providing a consistent interface to the AllNewsAPI:

| SDK | Directory | Package |
|-----|-----------|---------|
| Python | [`python-sdk/`](./python-sdk) | `pip install allnewsapi` |
| TypeScript | [`javascript-sdk/`](./javascript-sdk) | `npm install allnewsapi` |
| Go | [`go-sdk/`](./go-sdk) | `go get github.com/AllNewsAPI/go-sdk` |
| Java | [`java-sdk/`](./java-sdk) | Maven Central |
| PHP | [`php-sdk/`](./php-sdk) | `composer require allnewsapi/allnewsapi` |
| Ruby | [`ruby-sdk/`](./ruby-sdk) | `gem install allnewsapi` |

All SDKs share the same design principles:

- Zero external dependencies (stdlib HTTP only)
- Three methods: `search`, `headlines`, `usage`
- Typed request/response interfaces
- Consistent error handling with status codes and messages

## Quick Start

### Python

```bash
pip install allnewsapi
```

```python
from allnewsapi import NewsAPI

client = NewsAPI("your-api-key")
response = client.search(q="artificial intelligence", lang=["en"], max=10)

for article in response.articles:
    print(article.title)
```

### TypeScript

```bash
npm install allnewsapi
```

```typescript
import { NewsAPI } from 'allnewsapi';

const client = new NewsAPI('your-api-key');
const response = await client.search({ q: 'artificial intelligence', lang: ['en'], max: 10 });

for (const article of response.articles) {
  console.log(article.title);
}
```

### Go

```bash
go get github.com/AllNewsAPI/go-sdk
```

```go
package main

import (
    "fmt"
    newsapi "github.com/AllNewsAPI/go-sdk"
)

func main() {
    client := newsapi.NewClient("your-api-key")
    response, err := client.Search(newsapi.SearchOptions{
        Q:   "artificial intelligence",
        Lang: []string{"en"},
        Max:  10,
    })
    if err != nil {
        panic(err)
    }
    for _, article := range response.Articles {
        fmt.Println(article.Title)
    }
}
```

### Java

```xml
<dependency>
    <groupId>com.allnewsapi</groupId>
    <artifactId>allnewsapi</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
import com.allnewsapi.NewsAPI;
import com.allnewsapi.SearchOptions;
import com.allnewsapi.SearchResponse;

NewsAPI client = new NewsAPI("your-api-key");
SearchResponse response = client.search(
    new SearchOptions.Builder()
        .q("artificial intelligence")
        .lang("en")
        .max(10)
        .build()
);

for (var article : response.getArticles()) {
    System.out.println(article.getTitle());
}
```

### PHP

```bash
composer require allnewsapi/allnewsapi
```

```php
use AllNewsAPI\NewsAPI;

$client = new NewsAPI('your-api-key');
$response = $client->search([
    'q' => 'artificial intelligence',
    'lang' => ['en'],
    'max' => 10,
]);

foreach ($response['articles'] as $article) {
    echo $article['title'] . "\n";
}
```

### Ruby

```bash
gem install allnewsapi
```

```ruby
require 'allnewsapi'

client = AllNewsAPI::Client.new('your-api-key')
response = client.search(q: 'artificial intelligence', lang: ['en'], max: 10)

response['articles'].each do |article|
  puts article['title']
end
```

## Features

- **Full API coverage** — search, headlines, and usage endpoints with all query parameters
- **Zero external dependencies** — each SDK uses only stdlib HTTP (urllib, fetch, net/http, HttpURLConnection, curl, net/http)
- **Typed interfaces** — dataclasses, TypeScript interfaces, Go structs, Java classes for IDE autocompletion
- **Automated releases** — Release-Please manages versioning and changelogs per SDK

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development setup, commit conventions, and PR guidelines.

## License

MIT
