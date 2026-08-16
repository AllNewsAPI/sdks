# frozen_string_literal: true

require_relative 'lib/allnewsapi/version'

Gem::Specification.new do |spec|
  spec.name          = 'allnewsapi'
  spec.version       = AllNewsAPI::VERSION
  spec.authors       = ['AllNewsAPI']
  spec.email         = ['contact@allnewsapi.com']

  spec.summary       = 'A Ruby SDK for the AllNewsAPI'
  spec.description   = 'A simple Ruby wrapper for the AllNewsAPI that allows you to search for news articles, get headlines, and check usage statistics.'
  spec.homepage      = 'https://github.com/AllNewsAPI/ruby-sdk'
  spec.license       = 'MIT'
  spec.required_ruby_version = '>= 3.0'

  spec.metadata = {
    'homepage_uri' => spec.homepage,
    'source_code_uri' => 'https://github.com/AllNewsAPI/ruby-sdk',
    'changelog_uri' => 'https://github.com/AllNewsAPI/ruby-sdk/blob/main/CHANGELOG.md',
    'documentation_uri' => 'https://www.rubydoc.info/gems/allnewsapi',
    'bug_tracker_uri' => 'https://github.com/AllNewsAPI/ruby-sdk/issues'
  }

  spec.files = Dir['lib/**/*.rb'] + ['README.md', 'LICENSE']

  # Development dependencies
  spec.add_development_dependency 'bundler', '~> 2.0'
  spec.add_development_dependency 'minitest', '~> 5.0'
  spec.add_development_dependency 'rake', '~> 13.0'
  spec.add_development_dependency 'webmock', '~> 3.14'
end
