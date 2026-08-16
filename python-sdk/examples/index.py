"""Example usage of the AllNewsAPI Python SDK."""

from allnewsapi import NewsAPI, NewsAPIError


def main():
    api = NewsAPI("bcsYSbIeGBgCQUW7KmWZQA")

    # Search
    try:
        print("--- Search for 'bitcoin' ---")
        results = api.search(q="bitcoin", max=3)
        print(f"Total articles: {results.total_articles}")
        for article in results.articles:
            print(f"  {article.title}")
            print(f"  Source: {article.source.name}")
            print(f"  URL: {article.url}")
            print()
    except NewsAPIError as e:
        print(f"Error {e.status_code}: {e.message}")

    # Headlines
    try:
        print("--- Top Headlines ---")
        headlines = api.headlines(max=3)
        print(f"Total articles: {headlines.total_articles}")
        for article in headlines.articles:
            print(f"  {article.title}")
            print()
    except NewsAPIError as e:
        print(f"Error {e.status_code}: {e.message}")

    # Usage
    try:
        print("--- API Usage ---")
        usage = api.usage()
        print(f"Plan: {usage.plan}")
        print(f"Requests today: {usage.requests_used_24_hours}/{usage.requests_limit_24_hours}")
    except NewsAPIError as e:
        print(f"Error {e.status_code}: {e.message}")


if __name__ == "__main__":
    main()
