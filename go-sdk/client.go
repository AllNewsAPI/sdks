// Package allnewsapi provides a client for the AllNewsAPI.
package allnewsapi

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

// Client is an AllNewsAPI client.
type Client struct {
	apiKey     string
	baseURL    string
	httpClient *http.Client
}

// ClientOption is a function that configures a Client.
type ClientOption func(*Client)

// WithBaseURL sets a custom base URL for the API.
func WithBaseURL(baseURL string) ClientOption {
	return func(c *Client) {
		c.baseURL = baseURL
	}
}

// WithTimeout sets a custom timeout for HTTP requests.
func WithTimeout(timeout time.Duration) ClientOption {
	return func(c *Client) {
		c.httpClient.Timeout = timeout
	}
}

// NewClient creates a new AllNewsAPI client.
func NewClient(apiKey string, options ...ClientOption) (*Client, error) {
	if apiKey == "" {
		return nil, &APIError{StatusCode: 401, Message: "API key is required"}
	}

	client := &Client{
		apiKey:  apiKey,
		baseURL: "https://api.allnewsapi.com",
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}

	for _, option := range options {
		option(client)
	}

	return client, nil
}

// SearchOptions contains all possible parameters for the search and headlines endpoints.
type SearchOptions struct {
	Query        string      // Search query
	StartDate    interface{} // string or time.Time
	EndDate      interface{} // string or time.Time
	Content      *bool       // Whether to include full content
	Lang         []string    // Languages to filter by
	Country      []string    // Countries to filter by
	Region       []string    // Regions to filter by
	Category     []string    // Categories to filter by
	Max          int         // Maximum number of results (1-100)
	Attributes   []string    // Attributes to search in (title, description, content)
	Page         int         // Page number for pagination
	SortBy       string      // Sort by 'publishedAt' or 'relevance'
	Publisher    []string    // Publishers to filter by
	Format       string      // Response format (json, csv, xlsx)
	AISentiment  string      // AI sentiment filter
	AIEntityName string      // AI entity name filter
	AIEntityType string      // AI entity type filter
}

// buildParams constructs URL query parameters from SearchOptions.
func (c *Client) buildParams(options *SearchOptions) (url.Values, error) {
	params := url.Values{}
	params.Add("apikey", c.apiKey)

	if options == nil {
		return params, nil
	}

	if options.Query != "" {
		params.Add("q", options.Query)
	}

	// Handle start date
	if options.StartDate != nil {
		startDate, err := formatDate(options.StartDate)
		if err != nil {
			return nil, err
		}
		params.Add("startDate", startDate)
	}

	// Handle end date
	if options.EndDate != nil {
		endDate, err := formatDate(options.EndDate)
		if err != nil {
			return nil, err
		}
		params.Add("endDate", endDate)
	}

	// Handle boolean content parameter
	if options.Content != nil {
		if *options.Content {
			params.Add("content", "true")
		} else {
			params.Add("content", "false")
		}
	}

	// Handle array parameters
	if len(options.Lang) > 0 {
		params.Add("lang", strings.Join(options.Lang, ","))
	}
	if len(options.Country) > 0 {
		params.Add("country", strings.Join(options.Country, ","))
	}
	if len(options.Region) > 0 {
		params.Add("region", strings.Join(options.Region, ","))
	}
	if len(options.Category) > 0 {
		params.Add("category", strings.Join(options.Category, ","))
	}
	if len(options.Attributes) > 0 {
		params.Add("attributes", strings.Join(options.Attributes, ","))
	}
	if len(options.Publisher) > 0 {
		params.Add("publisher", strings.Join(options.Publisher, ","))
	}

	// Handle integer parameters
	if options.Max > 0 {
		params.Add("max", fmt.Sprintf("%d", options.Max))
	}
	if options.Page > 0 {
		params.Add("page", fmt.Sprintf("%d", options.Page))
	}

	// Handle string parameters
	if options.SortBy != "" {
		params.Add("sortby", options.SortBy)
	}
	if options.Format != "" {
		params.Add("format", options.Format)
	}
	if options.AISentiment != "" {
		params.Add("ai_sentiment", options.AISentiment)
	}
	if options.AIEntityName != "" {
		params.Add("ai_entity_name", options.AIEntityName)
	}
	if options.AIEntityType != "" {
		params.Add("ai_entity_type", options.AIEntityType)
	}

	return params, nil
}

// doRequest performs an HTTP GET request and returns the response body bytes.
func (c *Client) doRequest(endpoint string, params url.Values) ([]byte, error) {
	requestURL := fmt.Sprintf("%s%s?%s", c.baseURL, endpoint, params.Encode())

	req, err := http.NewRequest("GET", requestURL, nil)
	if err != nil {
		return nil, &APIError{StatusCode: 500, Message: fmt.Sprintf("Request failed: %s", err.Error())}
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, &APIError{StatusCode: 500, Message: fmt.Sprintf("Request failed: %s", err.Error())}
	}
	defer func() { _ = resp.Body.Close() }()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, &APIError{StatusCode: 500, Message: fmt.Sprintf("Request failed: %s", err.Error())}
	}

	if resp.StatusCode != http.StatusOK {
		message := extractErrorMessage(body, resp.StatusCode)
		return nil, &APIError{StatusCode: resp.StatusCode, Message: message}
	}

	return body, nil
}

// extractErrorMessage attempts to parse the error message from the API response body.
// Falls back to a default message if parsing fails.
func extractErrorMessage(body []byte, statusCode int) string {
	var errorResp struct {
		Detail struct {
			Message string `json:"message"`
		} `json:"detail"`
	}
	if err := json.Unmarshal(body, &errorResp); err == nil && errorResp.Detail.Message != "" {
		return errorResp.Detail.Message
	}
	return defaultErrorMessage(statusCode)
}

// defaultErrorMessage returns a human-readable default error message for the given status code.
func defaultErrorMessage(statusCode int) string {
	defaults := map[int]string{
		400: "Bad Request - Your request is invalid",
		401: "Unauthorized - Invalid API Key or Account status is inactive",
		403: "Forbidden - Your account is not authorized to make that request",
		429: "Too Many Requests - You have reached your daily request limit",
		500: "Internal Server Error - We had a problem with our server",
		503: "Service Unavailable - We're temporarily offline for maintenance",
	}
	if msg, ok := defaults[statusCode]; ok {
		return msg
	}
	return fmt.Sprintf("HTTP Error %d", statusCode)
}

// Search searches for news articles.
func (c *Client) Search(options *SearchOptions) (*SearchResponse, error) {
	params, err := c.buildParams(options)
	if err != nil {
		return nil, err
	}

	body, err := c.doRequest("/search", params)
	if err != nil {
		return nil, err
	}

	var response SearchResponse
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, fmt.Errorf("error parsing response: %w", err)
	}

	return &response, nil
}

// SearchRaw searches for news articles and returns the raw response bytes.
// Use this for CSV/XLSX format responses.
func (c *Client) SearchRaw(options *SearchOptions) ([]byte, error) {
	params, err := c.buildParams(options)
	if err != nil {
		return nil, err
	}
	return c.doRequest("/search", params)
}

// Headlines fetches news headlines.
func (c *Client) Headlines(options *SearchOptions) (*SearchResponse, error) {
	params, err := c.buildParams(options)
	if err != nil {
		return nil, err
	}

	body, err := c.doRequest("/headlines", params)
	if err != nil {
		return nil, err
	}

	var response SearchResponse
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, fmt.Errorf("error parsing response: %w", err)
	}

	return &response, nil
}

// HeadlinesRaw fetches news headlines and returns the raw response bytes.
// Use this for CSV/XLSX format responses.
func (c *Client) HeadlinesRaw(options *SearchOptions) ([]byte, error) {
	params, err := c.buildParams(options)
	if err != nil {
		return nil, err
	}
	return c.doRequest("/headlines", params)
}

// Usage retrieves the current API usage statistics.
func (c *Client) Usage() (*UsageResponse, error) {
	params := url.Values{}
	params.Add("apikey", c.apiKey)

	body, err := c.doRequest("/usage", params)
	if err != nil {
		return nil, err
	}

	var response UsageResponse
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, fmt.Errorf("error parsing response: %w", err)
	}

	return &response, nil
}

// formatDate converts a date value (string or time.Time) to a string.
func formatDate(value interface{}) (string, error) {
	switch v := value.(type) {
	case string:
		return v, nil
	case time.Time:
		return v.Format(time.RFC3339), nil
	default:
		return "", &APIError{StatusCode: 400, Message: "date must be string or time.Time"}
	}
}
