<?php

require_once __DIR__ . '/../vendor/autoload.php';

use AllNewsAPI\NewsAPI;
use AllNewsAPI\NewsAPIException;

try {
    $client = new NewsAPI('bcsYSbIeGBgCQUW7KmWZQA');

    // Search
    echo "--- Search for 'bitcoin' ---\n";
    $results = $client->search(['q' => 'bitcoin', 'max' => 3]);
    echo "Total articles: " . $results['totalArticles'] . "\n";
    foreach ($results['articles'] as $article) {
        echo "  " . $article['title'] . "\n";
        echo "  Source: " . $article['source']['name'] . "\n";
        echo "  URL: " . $article['url'] . "\n\n";
    }

    // Headlines
    echo "--- Top Headlines ---\n";
    $headlines = $client->headlines(['max' => 3]);
    echo "Total articles: " . $headlines['totalArticles'] . "\n";
    foreach ($headlines['articles'] as $article) {
        echo "  " . $article['title'] . "\n";
    }
    echo "\n";

    // Usage
    echo "--- API Usage ---\n";
    $usage = $client->usage();
    echo "Plan: " . $usage['plan'] . "\n";
    echo "Requests today: " . $usage['requestsUsed24Hours'] . "/" . $usage['requestsLimit24Hours'] . "\n";

} catch (NewsAPIException $e) {
    echo "Error " . $e->getStatusCode() . ": " . $e->getMessage() . "\n";
}
