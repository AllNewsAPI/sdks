import com.allnewsapi.NewsAPI;
import com.allnewsapi.NewsAPIException;
import com.allnewsapi.SearchOptions;
import com.allnewsapi.SearchResponse;
import com.allnewsapi.UsageResponse;

public class FreeNewsAPIExample {
    public static void main(String[] args) {
        try {
            NewsAPI client = new NewsAPI("bcsYSbIeGBgCQUW7KmWZQA");

            // Search
            System.out.println("--- Search for 'bitcoin' ---");
            SearchResponse results = client.search(SearchOptions.builder()
                .q("bitcoin")
                .max(3)
                .build());

            System.out.println("Total articles: " + results.getTotalArticles());
            for (var article : results.getArticles()) {
                System.out.println("  " + article.getTitle());
                System.out.println("  Source: " + article.getSource().getName());
                System.out.println("  URL: " + article.getUrl());
                System.out.println();
            }

            // Headlines
            System.out.println("--- Top Headlines ---");
            SearchResponse headlines = client.headlines(SearchOptions.builder()
                .max(3)
                .build());

            System.out.println("Total articles: " + headlines.getTotalArticles());
            for (var article : headlines.getArticles()) {
                System.out.println("  " + article.getTitle());
            }
            System.out.println();

            // Usage
            System.out.println("--- API Usage ---");
            UsageResponse usage = client.usage();
            System.out.println("Plan: " + usage.getPlan());
            System.out.println("Requests today: " + usage.getRequestsUsed24Hours() + "/" + usage.getRequestsLimit24Hours());

        } catch (NewsAPIException e) {
            System.err.println("Error " + e.getStatusCode() + ": " + e.getMessage());
        }
    }
}
