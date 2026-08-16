# Contributing

Thanks for your interest in contributing to the AllNewsAPI SDKs.

## How to Contribute

1. **Report bugs or request features** — Open an [issue](https://github.com/AllNewsAPI/allnewsapi-sdks/issues) describing the problem or idea. This is the best starting point for any contribution.
2. **Submit a pull request** — PRs are welcome from anyone. Fork the repo, make your changes, and open a PR against `main`.
3. **Review** — A project collaborator will review your PR. Merge permissions are restricted to collaborators.

For larger changes, open an issue first to discuss the approach before investing time in a PR.

## Commit Conventions

We use [Conventional Commits](https://www.conventionalcommits.org/) enforced by commitlint on pre-commit. All commit messages must follow this format:

- `feat:` — new feature
- `fix:` — bug fix
- `chore:` — maintenance (deps, configs)
- `docs:` — documentation only
- `refactor:` — code change that neither fixes a bug nor adds a feature

Scope commits to the SDK when relevant, e.g. `feat(python-sdk): add retry logic`.

Valid scopes: `python-sdk`, `javascript-sdk`, `go-sdk`, `java-sdk`, `php-sdk`, `ruby-sdk`, `ci`, `docs`, `deps`.

## Branching and Pull Requests

- Fork the repository and branch from `main`
- One logical change per PR
- Keep PRs focused and reviewable
- Ensure CI passes before requesting review
- PRs that modify GitHub Actions workflows require extra scrutiny from maintainers

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
PYTHONPATH=src pytest tests/
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
mkdir -p target/classes
find src/main -name "*.java" -exec javac -d target/classes {} +
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
bundle exec rake test
```

## Pre-commit Hooks

This repo uses [husky](https://typicode.github.io/husky/) for pre-commit hooks. After cloning, run:

```bash
npm install
```

This installs husky which will:
- **Pre-commit**: Run lint-staged (auto-formats/lints staged files per SDK)
- **Commit-msg**: Validate your commit message follows Conventional Commits

## Code Quality

- All PRs are validated by CI (linting + tests) before merge
- Follow the existing code style in each SDK
- Add tests for new functionality
- Don't introduce external dependencies — all SDKs use stdlib-only HTTP
