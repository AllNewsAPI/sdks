package com.allnewsapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AllNewsAPI Java SDK client.
 * Uses only java.net.HttpURLConnection — no external dependencies.
 */
public class NewsAPI {
    private static final String DEFAULT_BASE_URL = "https://api.allnewsapi.com";
    private static final int DEFAULT_TIMEOUT = 30000;

    private final String apiKey;
    private final String baseUrl;
    private final int timeout;

    /**
     * Create a new NewsAPI client with default configuration.
     *
     * @param apiKey your AllNewsAPI key
     * @throws NewsAPIException if apiKey is null or empty
     */
    public NewsAPI(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
    }

    /**
     * Create a new NewsAPI client with custom configuration.
     *
     * @param apiKey  your AllNewsAPI key
     * @param baseUrl the base URL for the API
     * @param timeout connection and read timeout in milliseconds
     * @throws NewsAPIException if apiKey is null or empty
     */
    public NewsAPI(String apiKey, String baseUrl, int timeout) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new NewsAPIException(401, "API key is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
    }

    /**
     * Search for news articles.
     *
     * @param options search options
     * @return parsed SearchResponse
     * @throws NewsAPIException on API or network error
     */
    public SearchResponse search(SearchOptions options) {
        String queryString = buildQueryString(options);
        byte[] responseBytes = doRequest("/search", queryString);
        String json = new String(responseBytes, StandardCharsets.UTF_8);
        return parseSearchResponse(json);
    }

    /**
     * Get news headlines.
     *
     * @param options search options
     * @return parsed SearchResponse
     * @throws NewsAPIException on API or network error
     */
    public SearchResponse headlines(SearchOptions options) {
        String queryString = buildQueryString(options);
        byte[] responseBytes = doRequest("/headlines", queryString);
        String json = new String(responseBytes, StandardCharsets.UTF_8);
        return parseSearchResponse(json);
    }

    /**
     * Search for news articles and return raw bytes (for CSV/XLSX format).
     *
     * @param options search options
     * @return raw response bytes
     * @throws NewsAPIException on API or network error
     */
    public byte[] searchRaw(SearchOptions options) {
        String queryString = buildQueryString(options);
        return doRequest("/search", queryString);
    }

    /**
     * Get news headlines and return raw bytes (for CSV/XLSX format).
     *
     * @param options search options
     * @return raw response bytes
     * @throws NewsAPIException on API or network error
     */
    public byte[] headlinesRaw(SearchOptions options) {
        String queryString = buildQueryString(options);
        return doRequest("/headlines", queryString);
    }

    /**
     * Get API usage statistics.
     *
     * @return parsed UsageResponse
     * @throws NewsAPIException on API or network error
     */
    public UsageResponse usage() {
        String queryString = "apikey=" + urlEncode(apiKey);
        byte[] responseBytes = doRequest("/usage", queryString);
        String json = new String(responseBytes, StandardCharsets.UTF_8);
        return parseUsageResponse(json);
    }

    // ───────────────────────── Private Helpers ─────────────────────────

    /**
     * Build the query string from SearchOptions.
     */
    private String buildQueryString(SearchOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("apikey=").append(urlEncode(apiKey));

        if (options == null) {
            return sb.toString();
        }

        appendParam(sb, "q", options.getQ());
        appendParam(sb, "startDate", options.getStartDate());
        appendParam(sb, "endDate", options.getEndDate());
        if (options.getContent() != null) {
            appendParam(sb, "content", options.getContent().toString());
        }
        appendListParam(sb, "lang", options.getLang());
        appendListParam(sb, "country", options.getCountry());
        appendListParam(sb, "region", options.getRegion());
        appendListParam(sb, "category", options.getCategory());
        if (options.getMax() != null) {
            appendParam(sb, "max", options.getMax().toString());
        }
        appendListParam(sb, "attributes", options.getAttributes());
        if (options.getPage() != null) {
            appendParam(sb, "page", options.getPage().toString());
        }
        appendParam(sb, "sortby", options.getSortby());
        appendListParam(sb, "publisher", options.getPublisher());
        appendParam(sb, "format", options.getFormat());
        appendParam(sb, "ai_sentiment", options.getAiSentiment());
        appendParam(sb, "ai_entity_name", options.getAiEntityName());
        appendParam(sb, "ai_entity_type", options.getAiEntityType());

        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("&").append(key).append("=").append(urlEncode(value));
        }
    }

    private void appendListParam(StringBuilder sb, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    joined.append(",");
                }
                joined.append(values.get(i));
            }
            sb.append("&").append(key).append("=").append(urlEncode(joined.toString()));
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Perform an HTTP GET request.
     */
    private byte[] doRequest(String endpoint, String queryString) {
        HttpURLConnection connection = null;
        try {
            String urlStr = baseUrl + endpoint + "?" + queryString;
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("Accept", "application/json");

            int statusCode = connection.getResponseCode();

            if (statusCode >= 200 && statusCode < 300) {
                return readAllBytes(connection.getInputStream());
            } else {
                byte[] errorBytes = readAllBytes(connection.getErrorStream());
                String errorBody = errorBytes != null ? new String(errorBytes, StandardCharsets.UTF_8) : "";
                String message = extractErrorMessage(errorBody, statusCode);
                throw new NewsAPIException(statusCode, message);
            }
        } catch (NewsAPIException e) {
            throw e;
        } catch (Exception e) {
            throw new NewsAPIException(500, "Request failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Read all bytes from an input stream.
     */
    private byte[] readAllBytes(InputStream is) {
        if (is == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Extract error message from JSON response body or fall back to defaults.
     */
    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.isEmpty()) {
            String extracted = extractJsonString(responseBody, "message");
            if (extracted != null && !extracted.isEmpty()) {
                return extracted;
            }
        }
        return getDefaultErrorMessage(statusCode);
    }

    /**
     * Get default error message for a given HTTP status code.
     */
    private String getDefaultErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized - Invalid API Key or Account status is inactive";
            case 403:
                return "Forbidden";
            case 429:
                return "Too Many Requests - You have reached your daily request limit";
            case 500:
                return "Internal Server Error";
            case 503:
                return "Service Unavailable";
            default:
                return "HTTP Error " + statusCode;
        }
    }

    // ───────────────────────── JSON Parsing ─────────────────────────

    /**
     * Parse a SearchResponse from JSON string.
     */
    private SearchResponse parseSearchResponse(String json) {
        SearchResponse response = new SearchResponse();
        response.setTotalArticles(extractJsonInt(json, "totalArticles", 0));
        response.setCurrentPage(extractJsonInt(json, "currentPage", 0));

        Integer nextPage = extractJsonIntOrNull(json, "nextPage");
        response.setNextPage(nextPage);

        List<Article> articles = parseArticlesArray(json);
        response.setArticles(articles);

        return response;
    }

    /**
     * Parse a UsageResponse from JSON string.
     */
    private UsageResponse parseUsageResponse(String json) {
        UsageResponse response = new UsageResponse();
        response.setPlan(extractJsonString(json, "plan"));
        response.setRequestsUsed24Hours(extractJsonInt(json, "requestsUsed24Hours", 0));
        response.setRequestsLimit24Hours(extractJsonInt(json, "requestsLimit24Hours", 0));
        response.setRequestsRemaining24Hours(extractJsonInt(json, "requestsRemaining24Hours", 0));
        response.setRequestsUsed30Days(extractJsonInt(json, "requestsUsed30Days", 0));
        return response;
    }

    /**
     * Parse the articles array from the response JSON.
     */
    private List<Article> parseArticlesArray(String json) {
        List<Article> articles = new ArrayList<>();
        int articlesStart = json.indexOf("\"articles\"");
        if (articlesStart == -1) {
            return articles;
        }

        int arrayStart = json.indexOf('[', articlesStart);
        if (arrayStart == -1) {
            return articles;
        }

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd == -1) {
            return articles;
        }

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        List<String> objectStrings = splitJsonArray(arrayContent);

        for (String objStr : objectStrings) {
            articles.add(parseArticle(objStr));
        }

        return articles;
    }

    /**
     * Parse a single Article from a JSON object string.
     */
    private Article parseArticle(String json) {
        Article article = new Article();
        article.setTitle(extractJsonString(json, "title"));
        article.setDescription(extractJsonString(json, "description"));
        article.setCategory(extractJsonString(json, "category"));
        article.setContent(extractJsonString(json, "content"));
        article.setCountry(extractJsonString(json, "country"));
        article.setRegion(extractJsonString(json, "region"));
        article.setLang(extractJsonString(json, "lang"));
        article.setAuthors(extractJsonStringArray(json, "authors"));
        article.setAiSentiment(extractJsonString(json, "ai_sentiment"));
        article.setAiSentimentScores(extractJsonNumberMap(json, "ai_sentiment_scores"));
        article.setAiEntities(parseEntitiesArray(json));
        article.setAiSummary(extractJsonString(json, "ai_summary"));
        article.setUrl(extractJsonString(json, "url"));
        article.setImage(extractJsonString(json, "image"));
        article.setPublishedAt(extractJsonString(json, "publishedAt"));

        // Parse source object
        int sourceStart = json.indexOf("\"source\"");
        if (sourceStart != -1) {
            int objStart = json.indexOf('{', sourceStart);
            if (objStart != -1) {
                int objEnd = findMatchingBracket(json, objStart, '{', '}');
                if (objEnd != -1) {
                    String sourceJson = json.substring(objStart, objEnd + 1);
                    Source source = new Source();
                    source.setName(extractJsonString(sourceJson, "name"));
                    source.setUrl(extractJsonString(sourceJson, "url"));
                    article.setSource(source);
                }
            }
        }

        return article;
    }

    /**
     * Parse ai_entities array from article JSON.
     */
    private List<Entity> parseEntitiesArray(String json) {
        List<Entity> entities = new ArrayList<>();
        int entitiesStart = json.indexOf("\"ai_entities\"");
        if (entitiesStart == -1) {
            return entities;
        }

        int arrayStart = json.indexOf('[', entitiesStart);
        if (arrayStart == -1) {
            return entities;
        }

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd == -1) {
            return entities;
        }

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        List<String> objectStrings = splitJsonArray(arrayContent);

        for (String objStr : objectStrings) {
            Entity entity = new Entity();
            entity.setName(extractJsonString(objStr, "name"));
            entity.setType(extractJsonString(objStr, "type"));
            entities.add(entity);
        }

        return entities;
    }

    // ───────────────────────── JSON Utility Methods ─────────────────────────

    /**
     * Extract a string value for a given key from JSON.
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = findKeyIndex(json, key);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(valueStart);
        if (firstChar == 'n' && json.startsWith("null", valueStart)) {
            return null;
        }

        if (firstChar != '"') {
            return null;
        }

        // Parse the string value handling escape sequences
        StringBuilder sb = new StringBuilder();
        int i = valueStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(next);
                        break;
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Find the index of a JSON key, ensuring it is a top-level key match.
     */
    private int findKeyIndex(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int index = 0;
        while (index < json.length()) {
            int found = json.indexOf(searchKey, index);
            if (found == -1) {
                return -1;
            }
            // Verify there's a colon after the key (possibly with whitespace)
            int afterKey = found + searchKey.length();
            int check = afterKey;
            while (check < json.length() && Character.isWhitespace(json.charAt(check))) {
                check++;
            }
            if (check < json.length() && json.charAt(check) == ':') {
                return found;
            }
            index = found + 1;
        }
        return -1;
    }

    /**
     * Extract an integer value for a given key from JSON.
     */
    private int extractJsonInt(String json, String key, int defaultValue) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = findKeyIndex(json, key);
        if (keyIndex == -1) {
            return defaultValue;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return defaultValue;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return defaultValue;
        }

        StringBuilder numStr = new StringBuilder();
        int i = valueStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) {
                numStr.append(c);
                i++;
            } else {
                break;
            }
        }

        if (numStr.length() == 0) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(numStr.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Extract an Integer value that may be null for a given key from JSON.
     */
    private Integer extractJsonIntOrNull(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = findKeyIndex(json, key);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        if (json.charAt(valueStart) == 'n' && json.startsWith("null", valueStart)) {
            return null;
        }

        StringBuilder numStr = new StringBuilder();
        int i = valueStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) {
                numStr.append(c);
                i++;
            } else {
                break;
            }
        }

        if (numStr.length() == 0) {
            return null;
        }

        try {
            return Integer.parseInt(numStr.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract a string array for a given key from JSON.
     */
    private List<String> extractJsonStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int keyIndex = findKeyIndex(json, key);
        if (keyIndex == -1) {
            return result;
        }

        String searchKey = "\"" + key + "\"";
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return result;
        }

        int arrayStart = json.indexOf('[', colonIndex);
        if (arrayStart == -1) {
            return result;
        }

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd == -1) {
            return result;
        }

        String arrayContent = json.substring(arrayStart + 1, arrayEnd).trim();
        if (arrayContent.isEmpty()) {
            return result;
        }

        // Parse strings from the array
        int i = 0;
        while (i < arrayContent.length()) {
            int quoteStart = arrayContent.indexOf('"', i);
            if (quoteStart == -1) {
                break;
            }
            int quoteEnd = findClosingQuote(arrayContent, quoteStart + 1);
            if (quoteEnd == -1) {
                break;
            }
            result.add(unescapeJsonString(arrayContent.substring(quoteStart + 1, quoteEnd)));
            i = quoteEnd + 1;
        }

        return result;
    }

    /**
     * Extract a map of string to double for a given key from JSON.
     */
    private Map<String, Double> extractJsonNumberMap(String json, String key) {
        Map<String, Double> result = new HashMap<>();
        int keyIndex = findKeyIndex(json, key);
        if (keyIndex == -1) {
            return result;
        }

        String searchKey = "\"" + key + "\"";
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return result;
        }

        int objStart = json.indexOf('{', colonIndex);
        if (objStart == -1) {
            return result;
        }

        int objEnd = findMatchingBracket(json, objStart, '{', '}');
        if (objEnd == -1) {
            return result;
        }

        String objContent = json.substring(objStart + 1, objEnd).trim();
        if (objContent.isEmpty()) {
            return result;
        }

        // Parse key-value pairs
        int i = 0;
        while (i < objContent.length()) {
            int quoteStart = objContent.indexOf('"', i);
            if (quoteStart == -1) {
                break;
            }
            int quoteEnd = findClosingQuote(objContent, quoteStart + 1);
            if (quoteEnd == -1) {
                break;
            }
            String mapKey = objContent.substring(quoteStart + 1, quoteEnd);

            int mapColonIndex = objContent.indexOf(':', quoteEnd);
            if (mapColonIndex == -1) {
                break;
            }

            int valueStart = mapColonIndex + 1;
            while (valueStart < objContent.length() && Character.isWhitespace(objContent.charAt(valueStart))) {
                valueStart++;
            }

            StringBuilder numStr = new StringBuilder();
            int j = valueStart;
            while (j < objContent.length()) {
                char c = objContent.charAt(j);
                if (c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 'e' || c == 'E' || c == '+') {
                    numStr.append(c);
                    j++;
                } else {
                    break;
                }
            }

            if (numStr.length() > 0) {
                try {
                    result.put(mapKey, Double.parseDouble(numStr.toString()));
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }

            i = j;
        }

        return result;
    }

    /**
     * Find the matching closing bracket, respecting nesting and string literals.
     */
    private int findMatchingBracket(String json, int openIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++; // skip escaped char
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Split a JSON array content into individual object/value strings.
     */
    private List<String> splitJsonArray(String arrayContent) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int itemStart = -1;

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);

            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    if (depth == 0 && itemStart == -1) {
                        itemStart = i;
                    }
                } else if (c == '{' || c == '[') {
                    if (depth == 0 && itemStart == -1) {
                        itemStart = i;
                    }
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0 && itemStart != -1) {
                        items.add(arrayContent.substring(itemStart, i + 1));
                        itemStart = -1;
                    }
                } else if (c == ',' && depth == 0) {
                    itemStart = -1;
                }
            }
        }

        return items;
    }

    /**
     * Find closing quote index, handling escape sequences.
     */
    private int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Unescape a JSON string value.
     */
    private String unescapeJsonString(String s) {
        if (s == null || !s.contains("\\")) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(next);
                        break;
                }
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
