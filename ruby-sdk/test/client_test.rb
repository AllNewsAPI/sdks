# frozen_string_literal: true

require_relative 'test_helper'

class ClientTest < Minitest::Test
  def setup
    @api_key = 'test-api-key-123'
    @client = AllNewsAPI::Client.new(@api_key)
  end

  def test_search_json_response
    json_body = {
      'totalArticles' => 100,
      'currentPage' => 1,
      'nextPage' => 2,
      'articles' => [
        {
          'title' => 'Test Article',
          'description' => 'A test article',
          'category' => 'technology',
          'content' => 'Full content here',
          'country' => 'us',
          'region' => 'north_america',
          'lang' => 'en',
          'authors' => ['Author One'],
          'ai_sentiment' => 'positive',
          'ai_sentiment_scores' => { 'positive' => 0.85, 'negative' => 0.05, 'neutral' => 0.10 },
          'ai_entities' => [{ 'name' => 'OpenAI', 'type' => 'organization' }],
          'ai_summary' => 'Summary text',
          'url' => 'https://example.com/article',
          'image' => 'https://example.com/image.jpg',
          'publishedAt' => '2024-01-15T10:30:00Z',
          'source' => { 'name' => 'Example News', 'url' => 'https://example.com' }
        }
      ]
    }.to_json

    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_return(body: json_body, status: 200, headers: { 'Content-Type' => 'application/json' })

    result = @client.search(q: 'technology')

    assert_instance_of Hash, result
    assert_equal 100, result['totalArticles']
    assert_equal 1, result['currentPage']
    assert_equal 2, result['nextPage']
    assert_equal 1, result['articles'].length
    assert_equal 'Test Article', result['articles'][0]['title']
    assert_equal 'positive', result['articles'][0]['ai_sentiment']
  end

  def test_headlines_json_response
    json_body = {
      'totalArticles' => 50,
      'currentPage' => 1,
      'nextPage' => nil,
      'articles' => [
        {
          'title' => 'Headline Article',
          'description' => 'Top news',
          'category' => 'general',
          'content' => '',
          'country' => 'gb',
          'region' => 'europe',
          'lang' => 'en',
          'authors' => [],
          'ai_sentiment' => 'neutral',
          'ai_sentiment_scores' => {},
          'ai_entities' => [],
          'ai_summary' => '',
          'url' => 'https://example.com/headline',
          'image' => '',
          'publishedAt' => '2024-02-01T08:00:00Z',
          'source' => { 'name' => 'BBC', 'url' => 'https://bbc.co.uk' }
        }
      ]
    }.to_json

    stub_request(:get, /api\.allnewsapi\.com\/headlines/)
      .to_return(body: json_body, status: 200, headers: { 'Content-Type' => 'application/json' })

    result = @client.headlines(lang: 'en', country: 'gb')

    assert_instance_of Hash, result
    assert_equal 50, result['totalArticles']
    assert_equal 'Headline Article', result['articles'][0]['title']
    assert_nil result['nextPage']
  end

  def test_usage_response
    json_body = {
      'plan' => 'pro',
      'requestsUsed24Hours' => 150,
      'requestsLimit24Hours' => 1000,
      'requestsRemaining24Hours' => 850,
      'requestsUsed30Days' => 4500
    }.to_json

    stub_request(:get, /api\.allnewsapi\.com\/usage/)
      .to_return(body: json_body, status: 200, headers: { 'Content-Type' => 'application/json' })

    result = @client.usage

    assert_instance_of Hash, result
    assert_equal 'pro', result['plan']
    assert_equal 150, result['requestsUsed24Hours']
    assert_equal 1000, result['requestsLimit24Hours']
    assert_equal 850, result['requestsRemaining24Hours']
    assert_equal 4500, result['requestsUsed30Days']
  end

  def test_csv_returns_raw_string
    csv_body = "title,description,url\nTest,A test,https://example.com\n"

    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_return(body: csv_body, status: 200, headers: { 'Content-Type' => 'text/csv' })

    result = @client.search(q: 'test', format: 'csv')

    assert_instance_of String, result
    assert_equal csv_body, result
  end

  def test_error_400_with_json_body
    error_body = { 'detail' => { 'message' => 'Missing required parameter: q' } }.to_json

    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_return(body: error_body, status: 400, headers: { 'Content-Type' => 'application/json' })

    error = assert_raises(AllNewsAPI::Error) do
      @client.search
    end

    assert_equal 400, error.status_code
    assert_equal 'Missing required parameter: q', error.message
  end

  def test_error_401_default_message
    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_return(body: 'Not JSON', status: 401, headers: { 'Content-Type' => 'text/plain' })

    error = assert_raises(AllNewsAPI::Error) do
      @client.search(q: 'test')
    end

    assert_equal 401, error.status_code
    assert_equal 'Unauthorized - Invalid API Key or Account status is inactive', error.message
  end

  def test_error_429
    error_body = { 'detail' => { 'message' => 'Rate limit exceeded' } }.to_json

    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_return(body: error_body, status: 429, headers: { 'Content-Type' => 'application/json' })

    error = assert_raises(AllNewsAPI::Error) do
      @client.search(q: 'test')
    end

    assert_equal 429, error.status_code
    assert_equal 'Rate limit exceeded', error.message
  end

  def test_network_error
    stub_request(:get, /api\.allnewsapi\.com\/search/)
      .to_raise(SocketError.new('getaddrinfo: Name or service not known'))

    error = assert_raises(AllNewsAPI::Error) do
      @client.search(q: 'test')
    end

    assert_equal 500, error.status_code
    assert_includes error.message, 'getaddrinfo: Name or service not known'
  end

  def test_api_key_required
    error = assert_raises(AllNewsAPI::Error) do
      AllNewsAPI::Client.new(nil)
    end
    assert_equal 401, error.status_code

    error = assert_raises(AllNewsAPI::Error) do
      AllNewsAPI::Client.new('')
    end
    assert_equal 401, error.status_code

    error = assert_raises(AllNewsAPI::Error) do
      AllNewsAPI::Client.new('   ')
    end
    assert_equal 401, error.status_code
  end

  def test_custom_base_url
    custom_url = 'https://custom-api.example.com'
    client = AllNewsAPI::Client.new(@api_key, base_url: custom_url)

    json_body = { 'totalArticles' => 0, 'currentPage' => 1, 'nextPage' => nil, 'articles' => [] }.to_json

    stub_request(:get, /custom-api\.example\.com\/search/)
      .to_return(body: json_body, status: 200, headers: { 'Content-Type' => 'application/json' })

    result = client.search(q: 'test')

    assert_instance_of Hash, result
    assert_equal 0, result['totalArticles']
  end
end
