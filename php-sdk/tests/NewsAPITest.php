<?php

namespace AllNewsAPI\Tests;

use AllNewsAPI\NewsAPI;
use AllNewsAPI\NewsAPIException;
use PHPUnit\Framework\TestCase;

class NewsAPITest extends TestCase
{
    public function testApiKeyRequired(): void
    {
        $this->expectException(NewsAPIException::class);
        new NewsAPI('');
    }

    public function testApiKeyRequiredThrowsCorrectCode(): void
    {
        try {
            new NewsAPI('');
            $this->fail('Expected NewsAPIException');
        } catch (NewsAPIException $e) {
            $this->assertEquals(401, $e->getCode());
        }
    }

    public function testConstructorAcceptsValidApiKey(): void
    {
        $client = new NewsAPI('valid-api-key');
        $this->assertInstanceOf(NewsAPI::class, $client);
    }

    public function testConstructorAcceptsCustomBaseUrl(): void
    {
        $client = new NewsAPI('valid-api-key', ['baseUrl' => 'https://custom.api.com']);
        $this->assertInstanceOf(NewsAPI::class, $client);
    }

    public function testConstructorAcceptsCustomTimeout(): void
    {
        $client = new NewsAPI('valid-api-key', ['timeout' => 60]);
        $this->assertInstanceOf(NewsAPI::class, $client);
    }
}
