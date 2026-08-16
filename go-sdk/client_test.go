package allnewsapi

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestSearch(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/search" {
			t.Errorf("expected path /search, got %s", r.URL.Path)
		}
		if r.URL.Query().Get("apikey") != "test-key" {
			t.Errorf("expected apikey=test-key, got %s", r.URL.Query().Get("apikey"))
		}
		if r.URL.Query().Get("q") != "golang" {
			t.Errorf("expected q=golang, got %s", r.URL.Query().Get("q"))
		}

		resp := SearchResponse{
			TotalArticles: 42,
			CurrentPage:   1,
			Articles: []Article{
				{
					Title:       "Go 1.21 Released",
					Description: "New version of Go",
					Category:    "technology",
					Lang:        "en",
					Country:     "us",
					URL:         "https://example.com/article",
					Source:      Source{Name: "TechNews", URL: "https://technews.com"},
				},
			},
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	result, err := client.Search(&SearchOptions{Query: "golang"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if result.TotalArticles != 42 {
		t.Errorf("expected TotalArticles=42, got %d", result.TotalArticles)
	}
	if result.CurrentPage != 1 {
		t.Errorf("expected CurrentPage=1, got %d", result.CurrentPage)
	}
	if len(result.Articles) != 1 {
		t.Fatalf("expected 1 article, got %d", len(result.Articles))
	}
	if result.Articles[0].Title != "Go 1.21 Released" {
		t.Errorf("expected title 'Go 1.21 Released', got %s", result.Articles[0].Title)
	}
	if result.Articles[0].Source.Name != "TechNews" {
		t.Errorf("expected source name 'TechNews', got %s", result.Articles[0].Source.Name)
	}
}

func TestHeadlines(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/headlines" {
			t.Errorf("expected path /headlines, got %s", r.URL.Path)
		}
		if r.URL.Query().Get("lang") != "en,fr" {
			t.Errorf("expected lang=en,fr, got %s", r.URL.Query().Get("lang"))
		}

		nextPage := 2
		resp := SearchResponse{
			TotalArticles: 100,
			CurrentPage:   1,
			NextPage:      &nextPage,
			Articles: []Article{
				{
					Title:       "Breaking News",
					Description: "Something happened",
					Category:    "general",
					Lang:        "en",
					Country:     "gb",
					URL:         "https://example.com/headline",
					Source:      Source{Name: "BBC", URL: "https://bbc.co.uk"},
				},
			},
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	result, err := client.Headlines(&SearchOptions{
		Lang: []string{"en", "fr"},
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if result.TotalArticles != 100 {
		t.Errorf("expected TotalArticles=100, got %d", result.TotalArticles)
	}
	if result.NextPage == nil || *result.NextPage != 2 {
		t.Errorf("expected NextPage=2, got %v", result.NextPage)
	}
	if len(result.Articles) != 1 {
		t.Fatalf("expected 1 article, got %d", len(result.Articles))
	}
	if result.Articles[0].Title != "Breaking News" {
		t.Errorf("expected title 'Breaking News', got %s", result.Articles[0].Title)
	}
}

func TestUsage(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/usage" {
			t.Errorf("expected path /usage, got %s", r.URL.Path)
		}
		if r.URL.Query().Get("apikey") != "test-key" {
			t.Errorf("expected apikey=test-key, got %s", r.URL.Query().Get("apikey"))
		}

		resp := UsageResponse{
			Plan:                     "pro",
			RequestsUsed24Hours:      150,
			RequestsLimit24Hours:     1000,
			RequestsRemaining24Hours: 850,
			RequestsUsed30Days:       4500,
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	result, err := client.Usage()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if result.Plan != "pro" {
		t.Errorf("expected Plan='pro', got %s", result.Plan)
	}
	if result.RequestsUsed24Hours != 150 {
		t.Errorf("expected RequestsUsed24Hours=150, got %d", result.RequestsUsed24Hours)
	}
	if result.RequestsLimit24Hours != 1000 {
		t.Errorf("expected RequestsLimit24Hours=1000, got %d", result.RequestsLimit24Hours)
	}
	if result.RequestsRemaining24Hours != 850 {
		t.Errorf("expected RequestsRemaining24Hours=850, got %d", result.RequestsRemaining24Hours)
	}
	if result.RequestsUsed30Days != 4500 {
		t.Errorf("expected RequestsUsed30Days=4500, got %d", result.RequestsUsed30Days)
	}
}

func TestSearchRaw(t *testing.T) {
	csvData := "title,description\n\"Go Released\",\"New version\"\n"
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/search" {
			t.Errorf("expected path /search, got %s", r.URL.Path)
		}
		if r.URL.Query().Get("format") != "csv" {
			t.Errorf("expected format=csv, got %s", r.URL.Query().Get("format"))
		}
		w.Header().Set("Content-Type", "text/csv")
		w.Write([]byte(csvData))
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	result, err := client.SearchRaw(&SearchOptions{
		Query:  "golang",
		Format: "csv",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if string(result) != csvData {
		t.Errorf("expected raw CSV data, got %s", string(result))
	}
}

func TestError400(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(`{"detail":{"message":"Invalid query parameter: xyz"}}`))
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	_, err = client.Search(&SearchOptions{Query: "test"})
	if err == nil {
		t.Fatal("expected error, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 400 {
		t.Errorf("expected StatusCode=400, got %d", apiErr.StatusCode)
	}
	if apiErr.Message != "Invalid query parameter: xyz" {
		t.Errorf("expected message 'Invalid query parameter: xyz', got %s", apiErr.Message)
	}
}

func TestError401Default(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
		w.Write([]byte("not json"))
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	_, err = client.Search(&SearchOptions{Query: "test"})
	if err == nil {
		t.Fatal("expected error, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 401 {
		t.Errorf("expected StatusCode=401, got %d", apiErr.StatusCode)
	}
	if apiErr.Message != "Unauthorized - Invalid API Key or Account status is inactive" {
		t.Errorf("expected default 401 message, got %s", apiErr.Message)
	}
}

func TestError429(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusTooManyRequests)
		w.Write([]byte(`{"detail":{"message":"Rate limit exceeded"}}`))
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	_, err = client.Search(&SearchOptions{Query: "test"})
	if err == nil {
		t.Fatal("expected error, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 429 {
		t.Errorf("expected StatusCode=429, got %d", apiErr.StatusCode)
	}
}

func TestNetworkError(t *testing.T) {
	client, err := NewClient("test-key", WithBaseURL("http://127.0.0.1:1"))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	_, err = client.Search(&SearchOptions{Query: "test"})
	if err == nil {
		t.Fatal("expected error for unreachable server, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 500 {
		t.Errorf("expected StatusCode=500, got %d", apiErr.StatusCode)
	}
}

func TestApiKeyRequired(t *testing.T) {
	_, err := NewClient("")
	if err == nil {
		t.Fatal("expected error for empty API key, got nil")
	}

	apiErr, ok := err.(*APIError)
	if !ok {
		t.Fatalf("expected *APIError, got %T", err)
	}
	if apiErr.StatusCode != 401 {
		t.Errorf("expected StatusCode=401, got %d", apiErr.StatusCode)
	}
}

func TestCustomBaseURL(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		resp := SearchResponse{
			TotalArticles: 1,
			CurrentPage:   1,
			Articles:      []Article{},
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	client, err := NewClient("test-key", WithBaseURL(server.URL))
	if err != nil {
		t.Fatalf("unexpected error creating client: %v", err)
	}

	// Verify that requests go to the custom base URL by successfully getting a response
	result, err := client.Search(&SearchOptions{Query: "test"})
	if err != nil {
		t.Fatalf("expected request to hit custom base URL, got error: %v", err)
	}
	if result.TotalArticles != 1 {
		t.Errorf("expected TotalArticles=1, got %d", result.TotalArticles)
	}
}
