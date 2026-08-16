# frozen_string_literal: true

module AllNewsAPI
  # Custom error class for AllNewsAPI errors
  class Error < StandardError
    attr_reader :status_code

    # @param status_code [Integer] HTTP status code
    # @param message [String] Error message
    def initialize(status_code, message)
      @status_code = status_code
      super(message)
    end
  end
end
