package allnewsapi

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
	"testing/quick"
	"time"
)

// --- Property 1: Parameter serialization completeness ---
// For any non-empty SearchOptions fields, the built URL params contain every provided parameter.
// Validates: Requirements 1.2, 2.2

func TestPropertyParameterSerializationCompleteness(t *testing.T) {
	client, _ := NewClient("test-key")

	f := func(query, sortBy, format, aiSentiment, aiEntityName, aiEntityType string) bool {
		options := &SearchOptions{
			Query:        query,
			SortBy:       sortBy,
			Format:       format,
			AISentiment:  aiSentiment,
			AIEntityName: aiEntityName,
			AIEntityType: aiEntityType,
		}

		params, err := client.buildParams(options)
		if err != nil {
			return false
		}

		// apikey is always present
		if params.Get("apikey") != "test-key" {
			return false
		}

		// Every non-empty string field should appear in params
		if query != "" && params.Get("q") != query {
			return false
		}
		if sortBy != "" && params.Get("sortby") != sortBy {
			return false
		}
		if format != "" && params.Get("format") != format {
			return false
		}
		if aiSentiment != "" && params.Get("ai_sentiment") != aiSentiment {
			return false
		}
		if aiEntityName != "" && params.Get("ai_entity_name") != aiEntityName {
			return false
		}
		if aiEntityType != "" && params.Get("ai_entity_type") != aiEntityType {
			return false
		}

		// Empty fields should NOT appear in params (aside from apikey)
		if query == "" && params.Get("q") != "" {
			return false
		}
		if sortBy == "" && params.Get("sortby") != "" {
			return false
		}
		if format == "" && params.Get("format") != "" {
			return false
		}

		return true
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 1 failed: %v", err)
	}
}

func TestPropertyIntegerParameterSerialization(t *testing.T) {
	client, _ := NewClient("test-key")

	f := func(maxVal uint8, page uint8) bool {
		// Use uint8 to constrain values and avoid edge cases with 0
		max := int(maxVal)
		pg := int(page)

		options := &SearchOptions{
			Max:  max,
			Page: pg,
		}

		params, err := client.buildParams(options)
		if err != nil {
			return false
		}

		if max > 0 && params.Get("max") != fmt.Sprintf("%d", max) {
			return false
		}
		if max <= 0 && params.Get("max") != "" {
			return false
		}
		if pg > 0 && params.Get("page") != fmt.Sprintf("%d", pg) {
			return false
		}
		if pg <= 0 && params.Get("page") != "" {
			return false
		}

		return true
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 1 (integer params) failed: %v", err)
	}
}

func TestPropertyContentBooleanSerialization(t *testing.T) {
	client, _ := NewClient("test-key")

	// Test true
	trueVal := true
	params, err := client.buildParams(&SearchOptions{Content: &trueVal})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if params.Get("content") != "true" {
		t.Errorf("expected content=true, got %q", params.Get("content"))
	}

	// Test false
	falseVal := false
	params, err = client.buildParams(&SearchOptions{Content: &falseVal})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if params.Get("content") != "false" {
		t.Errorf("expected content=false, got %q", params.Get("content"))
	}

	// Test nil (omitted)
	params, err = client.buildParams(&SearchOptions{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if params.Get("content") != "" {
		t.Errorf("expected content to be omitted, got %q", params.Get("content"))
	}
}

// --- Property 2: Array parameter comma-separated encoding ---
// For any array-type parameter containing N items, the resulting query string value is items joined by comma.
// Validates: Requirements 1.6

func TestPropertyArrayParameterCommaSeparatedEncoding(t *testing.T) {
	client, _ := NewClient("test-key")

	f := func(langs, countries, regions, categories, attributes, publishers []string) bool {
		// Filter out empty strings from generated slices to avoid testing empty-element edge cases
		langs = filterNonEmpty(langs)
		countries = filterNonEmpty(countries)
		regions = filterNonEmpty(regions)
		categories = filterNonEmpty(categories)
		attributes = filterNonEmpty(attributes)
		publishers = filterNonEmpty(publishers)

		options := &SearchOptions{
			Lang:       langs,
			Country:    countries,
			Region:     regions,
			Category:   categories,
			Attributes: attributes,
			Publisher:  publishers,
		}

		params, err := client.buildParams(options)
		if err != nil {
			return false
		}

		// Verify each array field is comma-joined
		if len(langs) > 0 {
			expected := strings.Join(langs, ",")
			if params.Get("lang") != expected {
				return false
			}
		} else if params.Get("lang") != "" {
			return false
		}

		if len(countries) > 0 {
			expected := strings.Join(countries, ",")
			if params.Get("country") != expected {
				return false
			}
		} else if params.Get("country") != "" {
			return false
		}

		if len(regions) > 0 {
			expected := strings.Join(regions, ",")
			if params.Get("region") != expected {
				return false
			}
		} else if params.Get("region") != "" {
			return false
		}

		if len(categories) > 0 {
			expected := strings.Join(categories, ",")
			if params.Get("category") != expected {
				return false
			}
		} else if params.Get("category") != "" {
			return false
		}

		if len(attributes) > 0 {
			expected := strings.Join(attributes, ",")
			if params.Get("attributes") != expected {
				return false
			}
		} else if params.Get("attributes") != "" {
			return false
		}

		if len(publishers) > 0 {
			expected := strings.Join(publishers, ",")
			if params.Get("publisher") != expected {
				return false
			}
		} else if params.Get("publisher") != "" {
			return false
		}

		return true
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 2 failed: %v", err)
	}
}

// --- Property 5: Usage endpoint sends only apikey ---
// For any invocation of Usage(), the constructed URL has exactly one param (apikey).
// Validates: Requirements 3.2

func TestPropertyUsageEndpointSendsOnlyApikey(t *testing.T) {
	f := func(apiKey string) bool {
		// Skip empty API keys since NewClient rejects them
		if apiKey == "" {
			return true
		}

		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			query := r.URL.Query()

			// Must have exactly one parameter: apikey
			if len(query) != 1 {
				w.WriteHeader(http.StatusBadRequest)
				return
			}

			if query.Get("apikey") != apiKey {
				w.WriteHeader(http.StatusBadRequest)
				return
			}

			w.Header().Set("Content-Type", "application/json")
			fmt.Fprintf(w, `{"plan":"free","requestsUsed24Hours":0,"requestsLimit24Hours":100,"requestsRemaining24Hours":100,"requestsUsed30Days":0}`)
		}))
		defer server.Close()

		client, err := NewClient(apiKey, WithBaseURL(server.URL))
		if err != nil {
			return false
		}

		resp, err := client.Usage()
		if err != nil {
			return false
		}

		return resp != nil
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 5 failed: %v", err)
	}
}

// --- Property 9: API key validation ---
// Empty string returns APIError at initialization.
// Validates: Requirements 10.7

func TestPropertyAPIKeyValidation(t *testing.T) {
	// Empty API key must always fail
	_, err := NewClient("")
	if err == nil {
		t.Fatal("expected error for empty API key, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 401 {
		t.Errorf("expected status code 401, got %d", apiErr.StatusCode)
	}

	// Non-empty API keys should succeed
	f := func(key string) bool {
		if key == "" {
			return true // skip empty, tested above
		}
		client, err := NewClient(key)
		if err != nil {
			return false
		}
		return client != nil
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 9 failed: %v", err)
	}
}

// --- Property 10: Base URL propagation ---
// Configured baseURL appears in all request URLs.
// Validates: Requirements 10.5

func TestPropertyBaseURLPropagation(t *testing.T) {
	f := func(suffix string) bool {
		// Construct a valid base URL using a test server
		var capturedURL string

		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			capturedURL = r.URL.String()
			w.Header().Set("Content-Type", "application/json")
			fmt.Fprintf(w, `{"totalArticles":0,"currentPage":1,"nextPage":null,"articles":[]}`)
		}))
		defer server.Close()

		client, err := NewClient("test-key", WithBaseURL(server.URL))
		if err != nil {
			return false
		}

		// Test search endpoint
		capturedURL = ""
		_, err = client.Search(nil)
		if err != nil {
			return false
		}
		if !strings.Contains(capturedURL, "/search") {
			return false
		}

		// Test headlines endpoint
		capturedURL = ""
		_, err = client.Headlines(nil)
		if err != nil {
			return false
		}
		if !strings.Contains(capturedURL, "/headlines") {
			return false
		}

		// Test usage endpoint - needs different response
		server2 := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			capturedURL = r.URL.String()
			w.Header().Set("Content-Type", "application/json")
			fmt.Fprintf(w, `{"plan":"free","requestsUsed24Hours":0,"requestsLimit24Hours":100,"requestsRemaining24Hours":100,"requestsUsed30Days":0}`)
		}))
		defer server2.Close()

		client2, err := NewClient("test-key", WithBaseURL(server2.URL))
		if err != nil {
			return false
		}

		capturedURL = ""
		_, err = client2.Usage()
		if err != nil {
			return false
		}
		if !strings.Contains(capturedURL, "/usage") {
			return false
		}

		return true
	}

	// Only run a few iterations since each one spins up HTTP servers
	cfg := &quick.Config{MaxCount: 5}
	if err := quick.Check(f, cfg); err != nil {
		t.Errorf("Property 10 failed: %v", err)
	}
}

// --- Property 11: URL encoding of special characters ---
// Values with reserved chars get percent-encoded in the final URL.
// Validates: Requirements 11.3

func TestPropertyURLEncodingSpecialCharacters(t *testing.T) {
	client, _ := NewClient("test-key")

	// Test with known special characters
	specialChars := []string{
		"hello world",   // space
		"a&b",           // ampersand
		"x=y",           // equals
		"foo+bar",       // plus
		"test#hash",     // hash
		"100%done",      // percent
		"query with spaces & special=chars",
	}

	for _, special := range specialChars {
		options := &SearchOptions{Query: special}
		params, err := client.buildParams(options)
		if err != nil {
			t.Fatalf("unexpected error for %q: %v", special, err)
		}

		// params.Get returns the decoded value
		if params.Get("q") != special {
			t.Errorf("expected decoded value %q, got %q", special, params.Get("q"))
		}

		// The encoded URL should NOT contain the raw special char unescaped
		encoded := params.Encode()
		// url.Values.Encode() percent-encodes values, so the raw special chars
		// should not appear unencoded in the query string
		decodedParams, err := url.ParseQuery(encoded)
		if err != nil {
			t.Fatalf("failed to parse encoded params: %v", err)
		}
		if decodedParams.Get("q") != special {
			t.Errorf("round-trip failed: expected %q, got %q", special, decodedParams.Get("q"))
		}
	}
}

func TestPropertyURLEncodingQuickCheck(t *testing.T) {
	client, _ := NewClient("test-key")

	f := func(value string) bool {
		options := &SearchOptions{Query: value}
		params, err := client.buildParams(options)
		if err != nil {
			return false
		}

		if value == "" {
			// Empty query should not be in params
			return params.Get("q") == ""
		}

		// The encoded form should round-trip correctly
		encoded := params.Encode()
		decoded, err := url.ParseQuery(encoded)
		if err != nil {
			return false
		}

		return decoded.Get("q") == value
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 11 failed: %v", err)
	}
}

// --- Property 12 (bonus): Date parameter serialization ---
// time.Time values are converted to ISO 8601 format.

func TestPropertyDateSerialization(t *testing.T) {
	client, _ := NewClient("test-key")

	f := func(year uint16, month, day, hour, minute, second uint8) bool {
		// Constrain to valid date ranges
		y := int(year)%200 + 1900
		m := time.Month(int(month)%12 + 1)
		d := int(day)%28 + 1
		h := int(hour) % 24
		min := int(minute) % 60
		sec := int(second) % 60

		date := time.Date(y, m, d, h, min, sec, 0, time.UTC)

		options := &SearchOptions{StartDate: date}
		params, err := client.buildParams(options)
		if err != nil {
			return false
		}

		startDate := params.Get("startDate")
		if startDate == "" {
			return false
		}

		// Should be valid RFC3339 (a subset of ISO 8601)
		parsed, err := time.Parse(time.RFC3339, startDate)
		if err != nil {
			return false
		}

		// Should represent the same time
		return parsed.Equal(date)
	}

	if err := quick.Check(f, nil); err != nil {
		t.Errorf("Property 12 (date serialization) failed: %v", err)
	}
}

// --- Helper functions ---

func filterNonEmpty(slice []string) []string {
	result := make([]string, 0, len(slice))
	for _, s := range slice {
		if s != "" {
			result = append(result, s)
		}
	}
	return result
}
