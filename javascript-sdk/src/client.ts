import { NewsAPIError } from './errors';
import type { NewsAPIConfig, SearchOptions, SearchResponse, UsageResponse } from './types';

const DEFAULT_ERROR_MESSAGES: Record<number, string> = {
  400: 'Bad Request - Your request is invalid',
  401: 'Unauthorized - Invalid API Key or Account status is inactive',
  403: 'Forbidden - Your account is not authorized to make that request',
  429: 'Too Many Requests - You have reached your daily request limit',
  500: 'Internal Server Error - We had a problem with our server',
  503: 'Service Unavailable - We\'re temporarily offline for maintenance',
};

export class NewsAPI {
  private readonly apiKey: string;
  private readonly baseUrl: string;
  private readonly timeout: number;

  constructor(apiKey: string, config: NewsAPIConfig = {}) {
    if (!apiKey) {
      throw new NewsAPIError(401, 'API key is required');
    }
    this.apiKey = apiKey;
    this.baseUrl = (config.baseUrl || 'https://api.allnewsapi.com').replace(/\/$/, '');
    this.timeout = config.timeout || 30000;
  }

  async search(options: SearchOptions = {}): Promise<SearchResponse | ArrayBuffer> {
    return this.request('/search', options);
  }

  async headlines(options: SearchOptions = {}): Promise<SearchResponse | ArrayBuffer> {
    return this.request('/headlines', options);
  }

  async usage(): Promise<UsageResponse> {
    const params = new URLSearchParams({ apikey: this.apiKey });
    const url = `${this.baseUrl}/usage?${params.toString()}`;
    const data = await this.makeRequest(url);
    return JSON.parse(new TextDecoder().decode(data)) as UsageResponse;
  }

  private async request(endpoint: string, options: SearchOptions): Promise<SearchResponse | ArrayBuffer> {
    const format = options.format || 'json';
    const queryString = this.serializeParams(options);
    const url = `${this.baseUrl}${endpoint}?${queryString}`;
    const data = await this.makeRequest(url);

    if (format === 'csv' || format === 'xlsx') {
      return data;
    }

    return JSON.parse(new TextDecoder().decode(data)) as SearchResponse;
  }

  private serializeParams(options: SearchOptions): string {
    const params = new URLSearchParams();
    params.set('apikey', this.apiKey);

    for (const [key, value] of Object.entries(options)) {
      if (value === null || value === undefined) continue;

      if (Array.isArray(value)) {
        params.set(key, value.join(','));
      } else if (value instanceof Date) {
        params.set(key, value.toISOString());
      } else if (typeof value === 'boolean') {
        params.set(key, value ? 'true' : 'false');
      } else {
        params.set(key, String(value));
      }
    }

    return params.toString();
  }

  private async makeRequest(url: string): Promise<ArrayBuffer> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await fetch(url, { signal: controller.signal });

      if (!response.ok) {
        const message = await this.extractErrorMessage(response);
        throw new NewsAPIError(response.status, message);
      }

      return await response.arrayBuffer();
    } catch (error) {
      if (error instanceof NewsAPIError) throw error;
      throw new NewsAPIError(500, `Request failed: ${(error as Error).message}`);
    } finally {
      clearTimeout(timeoutId);
    }
  }

  private async extractErrorMessage(response: Response): Promise<string> {
    try {
      const body = await response.json();
      const message = body?.detail?.message;
      if (message) return message;
    } catch {
      // JSON parsing failed, use default
    }
    return DEFAULT_ERROR_MESSAGES[response.status] || `HTTP Error ${response.status}`;
  }
}
