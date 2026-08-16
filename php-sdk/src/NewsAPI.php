<?php

namespace AllNewsAPI;

class NewsAPI
{
    private string $apiKey;
    private string $baseUrl;
    private int $timeout;

    public function __construct(string $apiKey, array $config = [])
    {
        if (empty($apiKey)) {
            throw new NewsAPIException('API key is required', 401);
        }

        $this->apiKey = $apiKey;
        $this->baseUrl = rtrim($config['baseUrl'] ?? 'https://api.allnewsapi.com', '/');
        $this->timeout = $config['timeout'] ?? 30;
    }

    public function search(array $options = []): array|string
    {
        return $this->request('/search', $options);
    }

    public function headlines(array $options = []): array|string
    {
        return $this->request('/headlines', $options);
    }

    public function usage(): array
    {
        $url = $this->baseUrl . '/usage?' . http_build_query(['apikey' => $this->apiKey]);
        $response = $this->makeRequest($url);
        return json_decode($response, true);
    }

    private function request(string $endpoint, array $options): array|string
    {
        $format = $options['format'] ?? 'json';
        $queryString = $this->serializeParams($options);
        $url = $this->baseUrl . $endpoint . '?' . $queryString;
        $response = $this->makeRequest($url);

        if ($format === 'csv' || $format === 'xlsx') {
            return $response;
        }

        $decoded = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new NewsAPIException('Invalid JSON response', 500);
        }

        return $decoded;
    }

    private function serializeParams(array $options): string
    {
        $params = ['apikey' => $this->apiKey];

        foreach ($options as $key => $value) {
            if ($value === null) {
                continue;
            }

            if (is_array($value)) {
                $params[$key] = implode(',', $value);
            } elseif ($value instanceof \DateTimeInterface) {
                $params[$key] = $value->format('c');
            } elseif (is_bool($value)) {
                $params[$key] = $value ? 'true' : 'false';
            } else {
                $params[$key] = (string) $value;
            }
        }

        return http_build_query($params);
    }

    private function makeRequest(string $url): string
    {
        $ch = curl_init();

        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_TIMEOUT => $this->timeout,
            CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        ]);

        $response = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);

        curl_close($ch);

        if ($error) {
            throw new NewsAPIException("Request failed: {$error}", 500);
        }

        if ($statusCode >= 400) {
            $message = $this->extractErrorMessage($response, $statusCode);
            throw new NewsAPIException($message, $statusCode);
        }

        return $response;
    }

    private function extractErrorMessage(string $body, int $statusCode): string
    {
        $decoded = json_decode($body, true);
        if (is_array($decoded) && isset($decoded['detail']['message'])) {
            return $decoded['detail']['message'];
        }

        return $this->defaultErrorMessage($statusCode);
    }

    private function defaultErrorMessage(int $statusCode): string
    {
        $defaults = [
            400 => 'Bad Request - Your request is invalid',
            401 => 'Unauthorized - Invalid API Key or Account status is inactive',
            403 => 'Forbidden - Your account is not authorized to make that request',
            429 => 'Too Many Requests - You have reached your daily request limit',
            500 => 'Internal Server Error - We had a problem with our server',
            503 => "Service Unavailable - We're temporarily offline for maintenance",
        ];

        return $defaults[$statusCode] ?? "HTTP Error {$statusCode}";
    }
}
