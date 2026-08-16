export interface NewsAPIConfig {
  baseUrl?: string;
  timeout?: number;
}

export interface SearchOptions {
  q?: string;
  startDate?: string | Date;
  endDate?: string | Date;
  content?: boolean;
  lang?: string | string[];
  country?: string | string[];
  region?: string | string[];
  category?: string | string[];
  max?: number;
  attributes?: string | string[];
  page?: number;
  sortby?: 'publishedAt' | 'relevance';
  publisher?: string | string[];
  format?: 'json' | 'csv' | 'xlsx';
  ai_sentiment?: string;
  ai_entity_name?: string;
  ai_entity_type?: string;
}

export interface Source {
  name: string;
  url: string;
}

export interface Entity {
  name: string;
  type: string;
}

export interface Article {
  title: string;
  description: string;
  category: string;
  content: string;
  country: string;
  region: string;
  lang: string;
  authors: string[];
  ai_sentiment: string;
  ai_sentiment_scores: Record<string, number>;
  ai_entities: Entity[];
  ai_summary: string;
  url: string;
  image: string;
  publishedAt: string;
  source: Source;
}

export interface SearchResponse {
  totalArticles: number;
  currentPage: number;
  nextPage: number | null;
  articles: Article[];
}

export interface UsageResponse {
  plan: string;
  requestsUsed24Hours: number;
  requestsLimit24Hours: number;
  requestsRemaining24Hours: number;
  requestsUsed30Days: number;
}
