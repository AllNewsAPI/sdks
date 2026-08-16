package main

import (
	"fmt"
	"log"

	allnewsapi "github.com/AllNewsAPI/go-sdk"
)

func main() {
	client, err := allnewsapi.NewClient("bcsYSbIeGBgCQUW7KmWZQA")
	if err != nil {
		log.Fatalf("Error creating client: %v", err)
	}

	// Search
	fmt.Println("--- Search for 'bitcoin' ---")
	response, err := client.Search(&allnewsapi.SearchOptions{
		Query: "bitcoin",
		Max:   3,
	})
	if err != nil {
		log.Fatalf("Error searching: %v", err)
	}

	fmt.Printf("Total articles: %d\n", response.TotalArticles)
	for _, article := range response.Articles {
		fmt.Printf("  %s\n", article.Title)
		fmt.Printf("  Source: %s\n", article.Source.Name)
		fmt.Printf("  URL: %s\n", article.URL)
		fmt.Println()
	}

	// Headlines
	fmt.Println("--- Top Headlines ---")
	headlines, err := client.Headlines(&allnewsapi.SearchOptions{
		Max: 3,
	})
	if err != nil {
		log.Fatalf("Error getting headlines: %v", err)
	}

	fmt.Printf("Total articles: %d\n", headlines.TotalArticles)
	for _, article := range headlines.Articles {
		fmt.Printf("  %s\n", article.Title)
	}
	fmt.Println()

	// Usage
	fmt.Println("--- API Usage ---")
	usage, err := client.Usage()
	if err != nil {
		log.Fatalf("Error getting usage: %v", err)
	}
	fmt.Printf("Plan: %s\n", usage.Plan)
	fmt.Printf("Requests today: %d/%d\n", usage.RequestsUsed24Hours, usage.RequestsLimit24Hours)
}
