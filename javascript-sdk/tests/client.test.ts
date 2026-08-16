import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NewsAPI } from '../src/client';
import { NewsAPIError } from '../src/errors';

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

beforeEach(() => {
  mockFetch.mockReset();
});

// Helper: successful JSON response
function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

// Helper: error response
function errorResponse(status: number, body?: unknown): Response {
  return new Response(body ? JSON.stringify(body) : 'error', {
    status,
    headers: { 'Content-Type': body ? 'application/json' : 'text/plain' },
  });
}

const sampleArticle = {
  title: 'Test Article',
  description: 'A test article description',
  category: 'technology',
  content: 'Full article content here',
  country: 'us',
  region: 'north_america',
  lang: 'en',
  authors: ['John Doe'],
  ai_sentiment: 'positive',
  ai_sentiment_scores: { positive: 0.85, negative: 0.05, neutral: 0.10 },
  ai_entities: [{ name: 'OpenAI', type: 'organization' }],
  ai_summary: 'A summary of the article',
  url: 'https://example.com/article',
  image: 'https://example.com/image.jpg',
  publishedAt: '2024-01-15T10:30:00Z',
  source: { name: 'Example News', url: 'https://example.com' },
};

const sampleSearchResponse = {
  totalArticles: 100,
  currentPage: 1,
  nextPage: 2,
  articles: [sampleArticle],
};

const sampleUsageResponse = {
  plan: 'pro',
  requestsUsed24Hours: 150,
  requestsLimit24Hours: 1000,
  requestsRemaining24Hours: 850,
  requestsUsed30Days: 4500,
};

describe('NewsAPI Client', () => {
  describe('search', () => {
    it('test_search_json_response', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(sampleSearchResponse));

      const client = new NewsAPI('test-api-key');
      const result = await client.search({ q: 'technology' });

      expect(result).toEqual(sampleSearchResponse);
      expect((result as typeof sampleSearchResponse).totalArticles).toBe(100);
      expect((result as typeof sampleSearchResponse).currentPage).toBe(1);
      expect((result as typeof sampleSearchResponse).nextPage).toBe(2);
      expect((result as typeof sampleSearchResponse).articles).toHaveLength(1);
      expect((result as typeof sampleSearchResponse).articles[0].title).toBe('Test Article');
      expect((result as typeof sampleSearchResponse).articles[0].ai_sentiment).toBe('positive');
      expect((result as typeof sampleSearchResponse).articles[0].ai_entities[0].name).toBe('OpenAI');
      expect((result as typeof sampleSearchResponse).articles[0].source.name).toBe('Example News');
    });
  });

  describe('headlines', () => {
    it('test_headlines_json_response', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(sampleSearchResponse));

      const client = new NewsAPI('test-api-key');
      const result = await client.headlines({ q: 'breaking' });

      expect(result).toEqual(sampleSearchResponse);
      expect((result as typeof sampleSearchResponse).totalArticles).toBe(100);
      expect((result as typeof sampleSearchResponse).currentPage).toBe(1);
      expect((result as typeof sampleSearchResponse).articles[0].title).toBe('Test Article');

      // Verify the URL targets /headlines
      const fetchCall = mockFetch.mock.calls[0];
      expect(fetchCall[0]).toContain('/headlines');
    });
  });

  describe('usage', () => {
    it('test_usage_response', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(sampleUsageResponse));

      const client = new NewsAPI('test-api-key');
      const result = await client.usage();

      expect(result.plan).toBe('pro');
      expect(result.requestsUsed24Hours).toBe(150);
      expect(result.requestsLimit24Hours).toBe(1000);
      expect(result.requestsRemaining24Hours).toBe(850);
      expect(result.requestsUsed30Days).toBe(4500);
    });
  });

  describe('binary formats', () => {
    it('test_csv_returns_arraybuffer', async () => {
      const csvContent = 'title,description\nTest,A test article';
      const encoder = new TextEncoder();
      const csvBuffer = encoder.encode(csvContent);

      mockFetch.mockResolvedValueOnce(
        new Response(csvBuffer, {
          status: 200,
          headers: { 'Content-Type': 'text/csv' },
        })
      );

      const client = new NewsAPI('test-api-key');
      const result = await client.search({ format: 'csv' });

      expect(result).toBeInstanceOf(ArrayBuffer);
      const decoded = new TextDecoder().decode(result as ArrayBuffer);
      expect(decoded).toBe(csvContent);
    });

    it('test_xlsx_returns_arraybuffer', async () => {
      const xlsxBytes = new Uint8Array([0x50, 0x4b, 0x03, 0x04]);

      mockFetch.mockResolvedValueOnce(
        new Response(xlsxBytes, {
          status: 200,
          headers: { 'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
        })
      );

      const client = new NewsAPI('test-api-key');
      const result = await client.search({ format: 'xlsx' });

      expect(result).toBeInstanceOf(ArrayBuffer);
      const view = new Uint8Array(result as ArrayBuffer);
      expect(view[0]).toBe(0x50);
      expect(view[1]).toBe(0x4b);
    });
  });

  describe('error handling', () => {
    it('test_error_400_with_json_body', async () => {
      mockFetch.mockResolvedValueOnce(
        errorResponse(400, { detail: { message: 'Invalid query parameter' } })
      );

      const client = new NewsAPI('test-api-key');
      await expect(client.search({ q: '' })).rejects.toThrow(NewsAPIError);

      try {
        await client.search({ q: '' });
      } catch (error) {
        // reset mock for second call
      }

      mockFetch.mockResolvedValueOnce(
        errorResponse(400, { detail: { message: 'Invalid query parameter' } })
      );

      try {
        await client.search({ q: '' });
      } catch (error) {
        expect(error).toBeInstanceOf(NewsAPIError);
        expect((error as NewsAPIError).statusCode).toBe(400);
        expect((error as NewsAPIError).message).toBe('Invalid query parameter');
      }
    });

    it('test_error_401_default_message', async () => {
      mockFetch.mockResolvedValueOnce(
        new Response('not json', { status: 401 })
      );

      const client = new NewsAPI('test-api-key');

      try {
        await client.search();
      } catch (error) {
        expect(error).toBeInstanceOf(NewsAPIError);
        expect((error as NewsAPIError).statusCode).toBe(401);
        expect((error as NewsAPIError).message).toContain('Unauthorized');
      }
    });

    it('test_error_429', async () => {
      mockFetch.mockResolvedValueOnce(
        errorResponse(429, { detail: { message: 'Rate limit exceeded' } })
      );

      const client = new NewsAPI('test-api-key');

      try {
        await client.search();
      } catch (error) {
        expect(error).toBeInstanceOf(NewsAPIError);
        expect((error as NewsAPIError).statusCode).toBe(429);
        expect((error as NewsAPIError).message).toBe('Rate limit exceeded');
      }
    });

    it('test_network_error', async () => {
      mockFetch.mockRejectedValueOnce(new TypeError('fetch failed'));

      const client = new NewsAPI('test-api-key');

      try {
        await client.search();
      } catch (error) {
        expect(error).toBeInstanceOf(NewsAPIError);
        expect((error as NewsAPIError).statusCode).toBe(500);
        expect((error as NewsAPIError).message).toContain('fetch failed');
      }
    });
  });

  describe('client initialization', () => {
    it('test_api_key_required', () => {
      expect(() => new NewsAPI('')).toThrow(NewsAPIError);

      try {
        new NewsAPI('');
      } catch (error) {
        expect(error).toBeInstanceOf(NewsAPIError);
        expect((error as NewsAPIError).statusCode).toBe(401);
      }
    });

    it('test_custom_base_url', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(sampleSearchResponse));

      const client = new NewsAPI('test-api-key', {
        baseUrl: 'https://custom.api.com',
      });
      await client.search({ q: 'test' });

      const fetchCall = mockFetch.mock.calls[0];
      expect(fetchCall[0]).toContain('https://custom.api.com/search');
    });
  });
});
