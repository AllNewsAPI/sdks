# Contributing

Thanks for your interest in contributing to the AllNewsAPI SDKs. This guide covers the basics for getting started.

## Commit Conventions

We use [Conventional Commits](https://www.conventionalcommits.org/). All commit messages must follow this format:

- `feat:` — new feature
- `fix:` — bug fix
- `chore:` — maintenance (deps, configs)
- `docs:` — documentation only
- `refactor:` — code change that neither fixes a bug nor adds a feature

Scope commits to the SDK when relevant, e.g. `feat(python-sdk): add usage endpoint`.

## Branching and Pull Requests

- Branch from `main`
- One logical change per PR
- Keep PRs focused and reviewable

## Project Structure

Each SDK lives in its own directory with its own dependencies and test setup:

```
allnewsapi-sdks/
├── python-sdk/
├── javascript-sdk/
├── go-sdk/
├── java-sdk/
├── php-sdk/
└── ruby-sdk/
```

## Per-SDK Development Setup

### Python

```bash
cd python-sdk
pip install -e ".[dev]"
pytest
```

### TypeScript

```bash
cd javascript-sdk
npm install
npm test
```

### Go

```bash
cd go-sdk
go test ./...
```

### Java

```bash
cd java-sdk
mvn test
```

### PHP

```bash
cd php-sdk
composer install
vendor/bin/phpunit
```

### Ruby

```bash
cd ruby-sdk
bundle install
rake test
```

## Code Quality

- All PRs are validated by CI (linting + tests) before merge
- Follow the existing code style in each SDK
- Add tests for new functionality
