package allnewsapi

// Source represents a news source.
type Source struct {
	Name string `json:"name"`
	URL  string `json:"url"`
}

// Entity represents an AI-extracted entity.
type Entity struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

// Article represents a news article returned by the API.
type Article struct {
	Title             string             `json:"title"`
	Description       string             `json:"description"`
	Category          string             `json:"category"`
	Content           string             `json:"content"`
	Country           string             `json:"country"`
	Region            string             `json:"region"`
	Lang              string             `json:"lang"`
	Authors           []string           `json:"authors"`
	AISentiment       string             `json:"ai_sentiment"`
	AISentimentScores map[string]float64 `json:"ai_sentiment_scores"`
	AIEntities        []Entity           `json:"ai_entities"`
	AISummary         string             `json:"ai_summary"`
	URL               string             `json:"url"`
	Image             string             `json:"image"`
	PublishedAt       string             `json:"publishedAt"`
	Source            Source             `json:"source"`
}

// SearchResponse represents the response from search/headlines endpoints.
type SearchResponse struct {
	TotalArticles int       `json:"totalArticles"`
	CurrentPage   int       `json:"currentPage"`
	NextPage      *int      `json:"nextPage"`
	Articles      []Article `json:"articles"`
}

// UsageResponse represents the response from the usage endpoint.
type UsageResponse struct {
	Plan                     string `json:"plan"`
	RequestsUsed24Hours      int    `json:"requestsUsed24Hours"`
	RequestsLimit24Hours     int    `json:"requestsLimit24Hours"`
	RequestsRemaining24Hours int    `json:"requestsRemaining24Hours"`
	RequestsUsed30Days       int    `json:"requestsUsed30Days"`
}
