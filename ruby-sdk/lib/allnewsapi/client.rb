# frozen_string_literal: true

require 'net/http'
require 'uri'
require 'json'
require 'date'

module AllNewsAPI
  # Main SDK client for interacting with AllNewsAPI
  class Client
    # Default base URL for the AllNewsAPI
    DEFAULT_BASE_URL = 'https://api.allnewsapi.com'

    # Default HTTP timeout in seconds
    DEFAULT_TIMEOUT = 30

    # Parameters that should NOT be converted from snake_case to camelCase
    AI_PARAMS = %w[ai_sentiment ai_entity_name ai_entity_type].freeze

    # Create a new AllNewsAPI client
    #
    # @param api_key [String] Your AllNewsAPI key
    # @param options [Hash] Optional configuration options
    # @option options [String] :base_url The base URL for the API
    # @option options [Integer] :timeout HTTP timeout in seconds (default: 30)
    def initialize(api_key, options = {})
      if api_key.nil? || api_key.to_s.strip.empty?
        raise Error.new(401, 'Unauthorized - Invalid API Key or Account status is inactive')
      end

      @api_key = api_key
      @base_url = options[:base_url] || DEFAULT_BASE_URL
      @timeout = options[:timeout] || DEFAULT_TIMEOUT
      @search_endpoint = "#{@base_url}/search"
      @headlines_endpoint = "#{@base_url}/headlines"
      @usage_endpoint = "#{@base_url}/usage"
    end

    # Search for news articles
    #
    # @param options [Hash] Search options
    # @option options [String] :q Keywords to search for
    # @option options [String, Date] :start_date Start date (YYYY-MM-DD or Date object)
    # @option options [String, Date] :end_date End date (YYYY-MM-DD or Date object)
    # @option options [Boolean] :content Whether to include full content
    # @option options [String, Array<String>] :lang Language(s) to filter by
    # @option options [String, Array<String>] :country Country/countries to filter by
    # @option options [String, Array<String>] :region Region(s) to filter by
    # @option options [String, Array<String>] :category Category/categories to filter by
    # @option options [Integer] :max Maximum number of results (1-100)
    # @option options [String, Array<String>] :attributes Attributes to search in
    # @option options [Integer] :page Page number for pagination
    # @option options [String] :sortby Sort by 'publishedAt' or 'relevance'
    # @option options [String, Array<String>] :publisher Publisher(s) to filter by
    # @option options [String] :format Response format (json, csv, xlsx)
    # @option options [String] :ai_sentiment AI sentiment filter
    # @option options [String] :ai_entity_name AI entity name filter
    # @option options [String] :ai_entity_type AI entity type filter
    #
    # @return [Hash, String] Search results (Hash for JSON, String for CSV/XLSX)
    def search(options = {})
      params = prepare_params(options)
      make_request(params, @search_endpoint)
    end

    # Get top headlines
    #
    # @param options [Hash] Headlines options (same as search)
    #
    # @return [Hash, String] Headlines results (Hash for JSON, String for CSV/XLSX)
    def headlines(options = {})
      params = prepare_params(options)
      make_request(params, @headlines_endpoint)
    end

    # Get account usage statistics
    #
    # @return [Hash] Usage statistics
    def usage
      make_request({}, @usage_endpoint)
    end

    private

    # Prepare parameters by converting dates to ISO 8601 format
    #
    # @param options [Hash] Raw options from the user
    # @return [Hash] Prepared options with dates converted
    def prepare_params(options)
      params = options.dup
      params[:start_date] = params[:start_date].iso8601 if params[:start_date].is_a?(Date)
      params[:end_date] = params[:end_date].iso8601 if params[:end_date].is_a?(Date)
      params
    end

    # Build the URL with query parameters for the API request
    #
    # @param params [Hash] Query parameters for the request
    # @param endpoint [String] The API endpoint to use
    # @return [URI] The complete URI for the API request
    def build_url(params, endpoint)
      uri = URI(endpoint)
      query_params = []
      query_params << ['apikey', @api_key]

      params.each do |key, value|
        next if value.nil?

        # Convert parameter name
        api_key_name = convert_param_name(key)

        # Handle array values by joining them with commas
        param_value = value.is_a?(Array) ? value.join(',') : value.to_s

        query_params << [api_key_name, param_value]
      end

      uri.query = URI.encode_www_form(query_params)
      uri
    end

    # Convert Ruby snake_case parameter name to API camelCase
    # AI params are kept as-is since they already match the API format
    #
    # @param key [Symbol, String] Ruby parameter name
    # @return [String] API parameter name
    def convert_param_name(key)
      key_str = key.to_s

      # AI params stay as-is
      return key_str if AI_PARAMS.include?(key_str)

      # Convert snake_case to camelCase
      key_str.gsub(/_([a-z])/) { ::Regexp.last_match(1).upcase }
    end

    # Make a request to the API
    #
    # @param params [Hash] Query parameters for the request
    # @param endpoint [String] The API endpoint to use
    # @return [Hash, String] The API response
    def make_request(params, endpoint)
      uri = build_url(params, endpoint)

      begin
        http = Net::HTTP.new(uri.host, uri.port)
        http.use_ssl = (uri.scheme == 'https')
        http.open_timeout = @timeout
        http.read_timeout = @timeout

        request = Net::HTTP::Get.new(uri)
        response = http.start { |h| h.request(request) }

        # Handle HTTP errors
        unless response.is_a?(Net::HTTPSuccess)
          error_message = parse_error_message(response)
          raise Error.new(response.code.to_i, error_message)
        end

        # Handle different formats
        format = params[:format] || 'json'

        case format.to_s
        when 'json'
          JSON.parse(response.body)
        else
          response.body
        end
      rescue Error => e
        raise e
      rescue SocketError, Timeout::Error, Errno::ECONNREFUSED => e
        raise Error.new(500, "Request failed: #{e.message}")
      rescue StandardError => e
        raise Error.new(500, "Request failed: #{e.message}")
      end
    end

    # Parse error message from API response
    #
    # @param response [Net::HTTPResponse] The error response
    # @return [String] The error message
    def parse_error_message(response)
      error_data = JSON.parse(response.body)
      error_data.dig('detail', 'message') || default_error_message(response.code.to_i)
    rescue JSON::ParserError, TypeError
      default_error_message(response.code.to_i)
    end

    # Get a default error message based on status code
    #
    # @param status_code [Integer] HTTP status code
    # @return [String] Default error message
    def default_error_message(status_code)
      messages = {
        400 => 'Bad Request - Your request is invalid',
        401 => 'Unauthorized - Invalid API Key or Account status is inactive',
        403 => 'Forbidden - Your account is not authorized to make that request',
        429 => 'Too Many Requests - You have reached your daily request limit',
        500 => 'Internal Server Error - We had a problem with our server',
        503 => "Service Unavailable - We're temporarily offline for maintenance"
      }

      messages[status_code] || 'Unknown error occurred'
    end
  end
end
