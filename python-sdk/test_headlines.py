#!/usr/bin/env python3
"""
Test script for the AllNewsAPI Python SDK headlines functionality.
This script demonstrates that both search and headlines endpoints work correctly.
"""

import sys
import os

# Add the current directory to the Python path so we can import the module
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from index import NewsAPI, NewsAPIError

def test_headlines_implementation():
    """Test that the headlines method is properly implemented."""
    
    # Initialize with a dummy API key for testing
    api_key = "test-api-key"
    news_api = NewsAPI(api_key)
    
    # Test 1: Check that headlines endpoint is properly set
    expected_headlines_endpoint = "https://api.allnewsapi.com/v1/headlines"
    assert hasattr(news_api, 'headlines_endpoint'), "headlines_endpoint attribute missing"
    assert news_api.headlines_endpoint == expected_headlines_endpoint, f"Expected {expected_headlines_endpoint}, got {news_api.headlines_endpoint}"
    
    # Test 2: Check that headlines method exists
    assert hasattr(news_api, 'headlines'), "headlines method missing"
    assert callable(getattr(news_api, 'headlines')), "headlines is not callable"
    
    # Test 3: Test URL building for headlines endpoint
    test_params = {
        'country': 'us',
        'category': 'business',
        'max': 5
    }
    
    # Test the _build_url method with headlines endpoint
    url = news_api._build_url(test_params.copy(), news_api.headlines_endpoint)
    
    # Check that the URL contains the headlines endpoint
    assert "/v1/headlines" in url, f"Headlines endpoint not found in URL: {url}"
    
    # Check that parameters are properly encoded
    assert "country=us" in url, f"Country parameter not found in URL: {url}"
    assert "category=business" in url, f"Category parameter not found in URL: {url}"
    assert "max=5" in url, f"Max parameter not found in URL: {url}"
    assert "apikey=test-api-key" in url, f"API key not found in URL: {url}"
    
    # Test 4: Compare search and headlines URL building
    search_url = news_api._build_url(test_params.copy(), news_api.search_endpoint)
    headlines_url = news_api._build_url(test_params.copy(), news_api.headlines_endpoint)
    
    # URLs should be different (different endpoints)
    assert search_url != headlines_url, "Search and headlines URLs should be different"
    
    # But they should have the same parameters
    search_params = search_url.split('?')[1]
    headlines_params = headlines_url.split('?')[1]
    assert search_params == headlines_params, "Parameters should be the same for both endpoints"
    
    print("✅ All tests passed!")
    print(f"✅ Headlines endpoint: {news_api.headlines_endpoint}")
    print(f"✅ Search endpoint: {news_api.search_endpoint}")
    print(f"✅ Sample headlines URL: {headlines_url}")
    
    return True

def test_method_signatures():
    """Test that search and headlines methods have the same signature."""
    
    news_api = NewsAPI("test-key")
    
    # Both methods should accept the same keyword arguments
    test_kwargs = {
        'q': 'bitcoin',
        'lang': ['en'],
        'country': 'us',
        'category': 'technology',
        'max': 10,
        'sortby': 'relevance'
    }
    
    # Test that both methods can be called with the same parameters
    # (They will fail with network errors, but that's expected without a real API key)
    try:
        news_api.search(**test_kwargs)
    except NewsAPIError as e:
        # Expected - we're using a fake API key
        assert "401" in str(e) or "400" in str(e) or "Request failed" in str(e), f"Unexpected error for search: {e}"
    
    try:
        news_api.headlines(**test_kwargs)
    except NewsAPIError as e:
        # Expected - we're using a fake API key
        assert "401" in str(e) or "400" in str(e) or "Request failed" in str(e), f"Unexpected error for headlines: {e}"
    
    print("✅ Method signatures are compatible!")
    return True

if __name__ == "__main__":
    print("Testing AllNewsAPI headlines implementation...")
    print("=" * 50)
    
    try:
        test_headlines_implementation()
        test_method_signatures()
        print("=" * 50)
        print("🎉 All tests completed successfully!")
        print("\nThe headlines endpoint has been successfully added to the AllNewsAPI Python SDK.")
        print("Both search() and headlines() methods now support the same parameters and functionality.")
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        sys.exit(1)