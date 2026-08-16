#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative '../lib/allnewsapi'

client = AllNewsAPI::Client.new('bcsYSbIeGBgCQUW7KmWZQA')

begin
  # Search
  puts '--- Search for "bitcoin" ---'
  results = client.search(q: 'bitcoin', max: 3)
  puts "Total articles: #{results['totalArticles']}"
  results['articles'].each do |article|
    puts "  #{article['title']}"
    puts "  Source: #{article['source']['name']}"
    puts "  URL: #{article['url']}"
    puts
  end

  # Headlines
  puts '--- Top Headlines ---'
  headlines = client.headlines(max: 3)
  puts "Total articles: #{headlines['totalArticles']}"
  headlines['articles'].each do |article|
    puts "  #{article['title']}"
  end
  puts

  # Usage
  puts '--- API Usage ---'
  usage = client.usage
  puts "Plan: #{usage['plan']}"
  puts "Requests today: #{usage['requestsUsed24Hours']}/#{usage['requestsLimit24Hours']}"

rescue AllNewsAPI::Error => e
  puts "Error #{e.status_code}: #{e.message}"
end
