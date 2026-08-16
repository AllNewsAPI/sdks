package com.allnewsapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for the Java SDK using com.sun.net.httpserver for HTTP mocking.
 * No JUnit dependency — uses a simple main-method test runner.
 */
public class NewsAPITest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testApiKeyRequiredNull();
        testApiKeyRequiredEmpty();
        testSearchJsonResponse();
        testHeadlinesJsonResponse();
        testUsageResponse();
        testSearchRaw();
        testError400WithJsonBody();
        testError401Default();
        testError429();
        testNetworkError();
        testCustomBaseUrl();

        System.out.println("\n=== Test Results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));

        if (failed > 0) {
            System.exit(1);
        }
    }

    // ───────────────────────── Test Cases ─────────────────────────

    /**
     * Test 1a: Null API key throws NewsAPIException with status 401.
     */
    private static void testApiKeyRequiredNull() {
        try {
            new NewsAPI(null);
            fail("testApiKeyRequiredNull", "Expected NewsAPIException but no exception was thrown");
        } catch (NewsAPIException e) {
            assertEqual("testApiKeyRequiredNull - statusCode", 401, e.getStatusCode());
            pass("testApiKeyRequiredNull");
        } catch (Exception e) {
            fail("testApiKeyRequiredNull", "Unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    /**
     * Test 1b: Empty API key throws NewsAPIException with status 401.
     */
    private static void testApiKeyRequiredEmpty() {
        try {
            new NewsAPI("   ");
            fail("testApiKeyRequiredEmpty", "Expected NewsAPIException but no exception was thrown");
        } catch (NewsAPIException e) {
            assertEqual("testApiKeyRequiredEmpty - statusCode", 401, e.getStatusCode());
            pass("testApiKeyRequiredEmpty");
        } catch (Exception e) {
            fail("testApiKeyRequiredEmpty", "Unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    /**
     * Test 2: Mock server returns valid search JSON, verify parsed SearchResponse.
     */
    private static void testSearchJsonResponse() throws Exception {
        String responseJson = "{"
                + "\"totalArticles\": 42,"
                + "\"currentPage\": 1,"
                + "\"nextPage\": 2,"
                + "\"articles\": ["
                + "  {"
                + "    \"title\": \"Test Article\","
                + "    \"description\": \"A test description\","
                + "    \"category\": \"technology\","
                + "    \"content\": \"Full article content\","
                + "    \"country\": \"us\","
                + "    \"region\": \"north_america\","
                + "    \"lang\": \"en\","
                + "    \"authors\": [\"John Doe\"],"
                + "    \"ai_sentiment\": \"positive\","
                + "    \"ai_sentiment_scores\": {\"positive\": 0.85, \"negative\": 0.05, \"neutral\": 0.10},"
                + "    \"ai_entities\": [{\"name\": \"OpenAI\", \"type\": \"organization\"}],"
                + "    \"ai_summary\": \"A summary of the article\","
                + "    \"url\": \"https://example.com/article\","
                + "    \"image\": \"https://example.com/image.jpg\","
                + "    \"publishedAt\": \"2024-01-15T10:30:00Z\","
                + "    \"source\": {\"name\": \"Example News\", \"url\": \"https://example.com\"}"
                + "  }"
                + "]"
                + "}";

        HttpServer server = createMockServer("/search", 200, responseJson);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().q("technology").build();
            SearchResponse response = client.search(options);

            assertEqual("testSearchJsonResponse - totalArticles", 42, response.getTotalArticles());
            assertEqual("testSearchJsonResponse - currentPage", 1, response.getCurrentPage());
            assertEqual("testSearchJsonResponse - nextPage", Integer.valueOf(2), response.getNextPage());
            assertEqual("testSearchJsonResponse - articles.size", 1, response.getArticles().size());

            Article article = response.getArticles().get(0);
            assertEqual("testSearchJsonResponse - title", "Test Article", article.getTitle());
            assertEqual("testSearchJsonResponse - description", "A test description", article.getDescription());
            assertEqual("testSearchJsonResponse - category", "technology", article.getCategory());
            assertEqual("testSearchJsonResponse - country", "us", article.getCountry());
            assertEqual("testSearchJsonResponse - lang", "en", article.getLang());
            assertEqual("testSearchJsonResponse - aiSentiment", "positive", article.getAiSentiment());
            assertEqual("testSearchJsonResponse - url", "https://example.com/article", article.getUrl());
            assertEqual("testSearchJsonResponse - publishedAt", "2024-01-15T10:30:00Z", article.getPublishedAt());
            assertEqual("testSearchJsonResponse - source.name", "Example News", article.getSource().getName());
            assertEqual("testSearchJsonResponse - source.url", "https://example.com", article.getSource().getUrl());
            assertEqual("testSearchJsonResponse - authors", Arrays.asList("John Doe"), article.getAuthors());
            assertEqual("testSearchJsonResponse - aiSummary", "A summary of the article", article.getAiSummary());

            // Verify entities
            assertEqual("testSearchJsonResponse - entities.size", 1, article.getAiEntities().size());
            assertEqual("testSearchJsonResponse - entity.name", "OpenAI", article.getAiEntities().get(0).getName());
            assertEqual("testSearchJsonResponse - entity.type", "organization", article.getAiEntities().get(0).getType());

            pass("testSearchJsonResponse");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 3: Mock server returns valid headlines JSON, verify parsed SearchResponse.
     */
    private static void testHeadlinesJsonResponse() throws Exception {
        String responseJson = "{"
                + "\"totalArticles\": 10,"
                + "\"currentPage\": 1,"
                + "\"nextPage\": null,"
                + "\"articles\": ["
                + "  {"
                + "    \"title\": \"Breaking News\","
                + "    \"description\": \"Important headline\","
                + "    \"category\": \"general\","
                + "    \"content\": \"Headline content\","
                + "    \"country\": \"gb\","
                + "    \"region\": \"europe\","
                + "    \"lang\": \"en\","
                + "    \"authors\": [],"
                + "    \"ai_sentiment\": \"neutral\","
                + "    \"ai_sentiment_scores\": {},"
                + "    \"ai_entities\": [],"
                + "    \"ai_summary\": \"\","
                + "    \"url\": \"https://example.com/headline\","
                + "    \"image\": \"\","
                + "    \"publishedAt\": \"2024-02-01T08:00:00Z\","
                + "    \"source\": {\"name\": \"BBC\", \"url\": \"https://bbc.co.uk\"}"
                + "  }"
                + "]"
                + "}";

        HttpServer server = createMockServer("/headlines", 200, responseJson);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().category(Arrays.asList("general")).build();
            SearchResponse response = client.headlines(options);

            assertEqual("testHeadlinesJsonResponse - totalArticles", 10, response.getTotalArticles());
            assertEqual("testHeadlinesJsonResponse - currentPage", 1, response.getCurrentPage());
            assertEqual("testHeadlinesJsonResponse - nextPage", null, response.getNextPage());
            assertEqual("testHeadlinesJsonResponse - articles.size", 1, response.getArticles().size());

            Article article = response.getArticles().get(0);
            assertEqual("testHeadlinesJsonResponse - title", "Breaking News", article.getTitle());
            assertEqual("testHeadlinesJsonResponse - source.name", "BBC", article.getSource().getName());

            pass("testHeadlinesJsonResponse");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 4: Mock server returns usage JSON, verify UsageResponse fields.
     */
    private static void testUsageResponse() throws Exception {
        String responseJson = "{"
                + "\"plan\": \"pro\","
                + "\"requestsUsed24Hours\": 150,"
                + "\"requestsLimit24Hours\": 1000,"
                + "\"requestsRemaining24Hours\": 850,"
                + "\"requestsUsed30Days\": 4500"
                + "}";

        HttpServer server = createMockServer("/usage", 200, responseJson);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            UsageResponse response = client.usage();

            assertEqual("testUsageResponse - plan", "pro", response.getPlan());
            assertEqual("testUsageResponse - requestsUsed24Hours", 150, response.getRequestsUsed24Hours());
            assertEqual("testUsageResponse - requestsLimit24Hours", 1000, response.getRequestsLimit24Hours());
            assertEqual("testUsageResponse - requestsRemaining24Hours", 850, response.getRequestsRemaining24Hours());
            assertEqual("testUsageResponse - requestsUsed30Days", 4500, response.getRequestsUsed30Days());

            pass("testUsageResponse");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 5: Verify raw bytes are returned from searchRaw.
     */
    private static void testSearchRaw() throws Exception {
        String csvContent = "title,description,url\nTest,Desc,https://example.com";

        HttpServer server = createMockServer("/search", 200, csvContent);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().q("test").format("csv").build();
            byte[] rawBytes = client.searchRaw(options);

            String result = new String(rawBytes, StandardCharsets.UTF_8);
            assertEqual("testSearchRaw - content", csvContent, result);

            pass("testSearchRaw");
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 6: Mock 400 with detail.message in JSON body.
     */
    private static void testError400WithJsonBody() throws Exception {
        String errorJson = "{\"detail\": {\"message\": \"Missing required parameter: q\"}}";

        HttpServer server = createMockServer("/search", 400, errorJson);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().build();
            try {
                client.search(options);
                fail("testError400WithJsonBody", "Expected NewsAPIException but no exception was thrown");
            } catch (NewsAPIException e) {
                assertEqual("testError400WithJsonBody - statusCode", 400, e.getStatusCode());
                // The SDK should extract "message" from the error body
                // The current implementation looks for top-level "message" key
                assertContains("testError400WithJsonBody - message", e.getMessage(), "Missing required parameter");
                pass("testError400WithJsonBody");
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 7: Mock 401 with non-JSON body, verify default error message.
     */
    private static void testError401Default() throws Exception {
        String nonJsonBody = "Unauthorized access";

        HttpServer server = createMockServer("/search", 401, nonJsonBody);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().q("test").build();
            try {
                client.search(options);
                fail("testError401Default", "Expected NewsAPIException but no exception was thrown");
            } catch (NewsAPIException e) {
                assertEqual("testError401Default - statusCode", 401, e.getStatusCode());
                assertContains("testError401Default - message", e.getMessage(), "Unauthorized");
                pass("testError401Default");
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 8: Mock 429, verify statusCode=429 for rate limit detection.
     */
    private static void testError429() throws Exception {
        String errorJson = "{\"detail\": {\"message\": \"Rate limit exceeded\"}}";

        HttpServer server = createMockServer("/search", 429, errorJson);
        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().q("test").build();
            try {
                client.search(options);
                fail("testError429", "Expected NewsAPIException but no exception was thrown");
            } catch (NewsAPIException e) {
                assertEqual("testError429 - statusCode", 429, e.getStatusCode());
                pass("testError429");
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 9: Use unreachable port, verify wrapped exception.
     */
    private static void testNetworkError() {
        // Use a port that's almost certainly not listening
        String baseUrl = "http://localhost:1";
        NewsAPI client = new NewsAPI("test-api-key", baseUrl, 1000);

        SearchOptions options = SearchOptions.builder().q("test").build();
        try {
            client.search(options);
            fail("testNetworkError", "Expected NewsAPIException but no exception was thrown");
        } catch (NewsAPIException e) {
            assertEqual("testNetworkError - statusCode", 500, e.getStatusCode());
            assertContains("testNetworkError - message", e.getMessage(), "Request failed");
            pass("testNetworkError");
        } catch (Exception e) {
            fail("testNetworkError", "Unexpected exception type: " + e.getClass().getName());
        }
    }

    /**
     * Test 10: Verify requests hit the custom base URL (mock server).
     */
    private static void testCustomBaseUrl() throws Exception {
        final boolean[] requestReceived = {false};

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/search", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestReceived[0] = true;
                String response = "{\"totalArticles\": 0, \"currentPage\": 1, \"nextPage\": null, \"articles\": []}";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        });
        server.setExecutor(null);
        server.start();

        try {
            int port = server.getAddress().getPort();
            String baseUrl = "http://localhost:" + port;
            NewsAPI client = new NewsAPI("test-api-key", baseUrl, 5000);

            SearchOptions options = SearchOptions.builder().q("test").build();
            client.search(options);

            assertTrue("testCustomBaseUrl - request received", requestReceived[0]);
            pass("testCustomBaseUrl");
        } finally {
            server.stop(0);
        }
    }

    // ───────────────────────── Mock Server Helper ─────────────────────────

    private static HttpServer createMockServer(String path, int statusCode, String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        });
        server.setExecutor(null);
        server.start();
        return server;
    }

    // ───────────────────────── Assertion Helpers ─────────────────────────

    private static void assertEqual(String label, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        fail(label, "Expected: " + expected + ", Actual: " + actual);
    }

    private static void assertEqual(String label, int expected, int actual) {
        if (expected != actual) {
            fail(label, "Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertContains(String label, String actual, String expectedSubstring) {
        if (actual == null || !actual.contains(expectedSubstring)) {
            fail(label, "Expected to contain: \"" + expectedSubstring + "\", Actual: \"" + actual + "\"");
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            fail(label, "Expected true but was false");
        }
    }

    private static void pass(String testName) {
        passed++;
        System.out.println("  PASS: " + testName);
    }

    private static void fail(String testName, String reason) {
        failed++;
        System.out.println("  FAIL: " + testName + " - " + reason);
    }
}
