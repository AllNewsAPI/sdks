# Publishing Setup Guide

## Overview

This guide explains how to configure the automated publish workflows for the AllNewsAPI SDKs monorepo. Each SDK publishes to its language's package registry when Release-Please creates a version tag (e.g., `python-sdk-v1.0.0`). You need to create accounts and API tokens on each registry, then store them as GitHub repository secrets.

## Prerequisites

- Admin access to the GitHub repository
- Accounts on each package registry you want to publish to

## GitHub Secrets Configuration

Go to: **Repository → Settings → Secrets and variables → Actions → New repository secret**

---

## 1. Python → PyPI

**Registry:** https://pypi.org
**Workflow:** `.github/workflows/publish-python.yml`
**Tag trigger:** `python-sdk-v*`

### Steps

1. Create an account at https://pypi.org/account/register/
2. Go to **Account Settings → API tokens**
3. Create a token scoped to the `allnewsapi` project (or all projects for first publish)
4. Copy the token (starts with `pypi-`)

### GitHub Secret

| Name | Value |
|------|-------|
| `PYPI_TOKEN` | The API token (starts with `pypi-`) |

### First publish note

For the very first release, you may need to manually upload using `twine upload dist/*` since PyPI requires the project to exist before you can scope a token to it. Alternatively, use an unscoped token for the first release, then create a project-scoped token afterward.

### Manual first release

```bash
cd python-sdk
pip install build twine
python -m build
twine upload dist/*
```

---

## 2. TypeScript/JavaScript → npm

**Registry:** https://www.npmjs.com
**Workflow:** `.github/workflows/publish-javascript.yml`
**Tag trigger:** `javascript-sdk-v*`

### Steps

1. Create an account at https://www.npmjs.com/signup
2. Go to **Access Tokens** (avatar → Access Tokens)
3. Click **Generate New Token** → select **"Automation"** type (bypasses 2FA for CI)
4. Copy the token

### GitHub Secret

| Name | Value |
|------|-------|
| `NPM_TOKEN` | The automation token |

### Package name

The package is published as `allnewsapi`. Ensure this name is available or you own it on npm.

**Scoped package alternative:** If `allnewsapi` is taken, use `@allnewsapi/sdk` and update `package.json`:

```json
{
  "name": "@allnewsapi/sdk",
  "publishConfig": {
    "access": "public"
  }
}
```

---

## 3. Go → pkg.go.dev

**Registry:** https://pkg.go.dev (auto-indexed from GitHub tags)
**Workflow:** `.github/workflows/publish-go.yml`
**Tag trigger:** `go-sdk-v*`

### Steps

No secrets needed! Go modules are published by pushing a Git tag. The publish workflow:
1. Validates `go.mod` with `go mod verify`
2. Requests indexing from the Go module proxy (`proxy.golang.org`)

### Requirements

- The `go.mod` module path must match the GitHub repository: `github.com/AllNewsAPI/go-sdk`
- Tags must follow the `go-sdk-v{version}` format (e.g., `go-sdk-v1.0.0`)

### Module path note

The current `go.mod` declares `module github.com/AllNewsAPI/go-sdk`. Users import the package as:

```go
import allnewsapi "github.com/AllNewsAPI/go-sdk"
```

If you move to a monorepo import path (`github.com/AllNewsAPI/allnewsapi-sdks/go-sdk`), update `go.mod` accordingly and ensure the publish workflow references the correct module path.

### GitHub Secret

None required.

---

## 4. Java → Maven Central (via Central Portal)

**Registry:** https://central.sonatype.com
**Workflow:** `.github/workflows/publish-java.yml`
**Tag trigger:** `java-sdk-v*`

### Prerequisites

✅ Account created on https://central.sonatype.com/
✅ Namespace `com.allnewsapi` claimed and verified

### Steps

1. **Generate a user token** — In the Central Portal, go to your account → **View Account** → **Generate User Token**
   - This gives you a **token username** and **token password** (different from your login credentials)
   - These are used in `settings.xml` as the server credentials

2. **Generate a GPG key** for artifact signing (required by Maven Central):

```bash
# Generate a new key pair
gpg --gen-key

# List keys to find your KEY_ID (the long hex after "sec")
gpg --list-secret-keys --keyid-format=long

# Export the private key in ASCII armor format
gpg --export-secret-keys --armor YOUR_KEY_ID > private-key.asc

# Upload public key to a keyserver (required for verification)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

3. Note the passphrase you set during GPG key generation

### GitHub Secrets

| Name | Value |
|------|-------|
| `CENTRAL_TOKEN_USERNAME` | Token username from Central Portal (not your login) |
| `CENTRAL_TOKEN_PASSWORD` | Token password from Central Portal |
| `MAVEN_GPG_KEY` | Full contents of `private-key.asc` |
| `MAVEN_GPG_PASSPHRASE` | The passphrase for the GPG key |

### pom.xml setup

The `java-sdk/pom.xml` needs the `central-publishing-maven-plugin` (replaces the old `nexus-staging-maven-plugin`):

```xml
<groupId>com.allnewsapi</groupId>
<artifactId>allnewsapi</artifactId>
<version>0.1.0</version>

<build>
  <plugins>
    <!-- Central Portal publishing plugin -->
    <plugin>
      <groupId>org.sonatype.central</groupId>
      <artifactId>central-publishing-maven-plugin</artifactId>
      <version>0.9.0</version>
      <extensions>true</extensions>
      <configuration>
        <publishingServerId>central</publishingServerId>
        <autoPublish>true</autoPublish>
        <waitUntil>published</waitUntil>
      </configuration>
    </plugin>

    <!-- GPG signing -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-gpg-plugin</artifactId>
      <version>3.2.7</version>
      <executions>
        <execution>
          <id>sign-artifacts</id>
          <phase>verify</phase>
          <goals><goal>sign</goal></goals>
          <configuration>
            <gpgArguments>
              <arg>--pinentry-mode</arg>
              <arg>loopback</arg>
            </gpgArguments>
          </configuration>
        </execution>
      </executions>
    </plugin>

    <!-- Javadoc (required by Maven Central) -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-javadoc-plugin</artifactId>
      <version>3.6.3</version>
      <executions>
        <execution>
          <id>attach-javadocs</id>
          <goals><goal>jar</goal></goals>
        </execution>
      </executions>
    </plugin>

    <!-- Sources (required by Maven Central) -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-source-plugin</artifactId>
      <version>3.3.0</version>
      <executions>
        <execution>
          <id>attach-sources</id>
          <goals><goal>jar-no-fork</goal></goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The workflow creates a `settings.xml` with server ID `central` matching the plugin's `publishingServerId`.

### How it works

Unlike the old OSSRH approach, the Central Portal plugin:
- Bundles artifacts into a ZIP and uploads directly to `central.sonatype.com`
- With `autoPublish: true`, automatically publishes after validation passes
- With `waitUntil: published`, the CI job blocks until the artifact is live on Maven Central
- No manual staging/release step needed

### Timeline

Once published, artifacts appear on Maven Central search within ~30 minutes.

---

## 5. PHP → Packagist

**Registry:** https://packagist.org
**Workflow:** `.github/workflows/publish-php.yml`
**Tag trigger:** `php-sdk-v*`

### How it works

A root-level `composer.json` in the monorepo points the PSR-4 autoload to `php-sdk/src/`. Packagist reads this directly from the monorepo — no split repo needed.

When users run `composer require allnewsapi/allnewsapi`, they get the full monorepo. The PHP source is at `php-sdk/src/` and autoloads correctly.

### Steps

1. Create an account at https://packagist.org/register/
2. Submit the package at https://packagist.org/packages/submit
   - Enter the monorepo URL: `https://github.com/AllNewsAPI/sdks`
3. **(Recommended) Set up auto-update webhook:**
   - Packagist → your package → Settings → Show API Token
   - GitHub `AllNewsAPI/sdks` → Settings → Webhooks → Add webhook
   - Payload URL: the Packagist hook URL, Content type: `application/json`
   - Secret: the Packagist secret
   - Events: Just the push event

### GitHub Secrets

| Name | Value |
|------|-------|
| `PACKAGIST_USERNAME` | (Optional) Your Packagist username for API notification |
| `PACKAGIST_TOKEN` | (Optional) Your Packagist API token for API notification |

If the webhook is configured (recommended), the secrets are optional — Packagist updates automatically on push.

---

## 6. Ruby → RubyGems

**Registry:** https://rubygems.org
**Workflow:** `.github/workflows/publish-ruby.yml`
**Tag trigger:** `ruby-sdk/v*`

### Steps

1. Create an account at https://rubygems.org/sign_up
2. Go to **Settings → API Keys → New API Key**
3. Name it (e.g., "github-actions")
4. Select scope: **"Push rubygem"**
5. Restrict to the `allnewsapi` gem if desired
6. Copy the API key

### GitHub Secret

| Name | Value |
|------|-------|
| `RUBYGEMS_API_KEY` | The API key value |

### Gem name

The gem is published as `allnewsapi`. Verify availability at https://rubygems.org/gems/allnewsapi before first publish.

---

## Summary of GitHub Secrets

| Secret Name | Used By | Registry |
|-------------|---------|----------|
| `PYPI_TOKEN` | `publish-python.yml` | PyPI |
| `NPM_TOKEN` | `publish-javascript.yml` | npm |
| `CENTRAL_TOKEN_USERNAME` | `publish-java.yml` | Maven Central |
| `CENTRAL_TOKEN_PASSWORD` | `publish-java.yml` | Maven Central |
| `MAVEN_GPG_KEY` | `publish-java.yml` | Maven Central |
| `MAVEN_GPG_PASSPHRASE` | `publish-java.yml` | Maven Central |
| `PACKAGIST_USERNAME` | `publish-php.yml` | Packagist |
| `PACKAGIST_TOKEN` | `publish-php.yml` | Packagist |
| `RUBYGEMS_API_KEY` | `publish-ruby.yml` | RubyGems |

**Go SDK** requires no secrets — it publishes via Git tags automatically.

**Total secrets to configure: 9** (assuming all registries are set up)

---

## How Publishing Works (End-to-End Flow)

1. A developer merges a PR to `main` with conventional commit messages (e.g., `feat(python-sdk): add retry logic`)
2. The **Release-Please** workflow (`release.yml`) detects the changes and creates a release PR with changelog updates
3. A maintainer reviews and merges the Release-Please PR
4. Release-Please creates Git tags in the format `{sdk-name}-v{semver}` (e.g., `python-sdk-v0.2.0`)
5. The corresponding publish workflow triggers based on the tag pattern and publishes to the registry

### Tag format reference

| SDK | Tag Pattern | Example |
|-----|-------------|---------|
| Python | `python-sdk-v*` | `python-sdk-v1.0.0` |
| JavaScript | `javascript-sdk-v*` | `javascript-sdk-v1.0.0` |
| Go | `go-sdk-v*` | `go-sdk-v1.0.0` |
| Java | `java-sdk-v*` | `java-sdk-v1.0.0` |
| PHP | `php-sdk-v*` | `php-sdk-v1.0.0` |
| Ruby | `ruby-sdk/v*` | `ruby-sdk/v1.0.0` |

---

## Testing a Release (Dry Run)

Before publishing to production registries, you can test the build and publish flow:

| SDK | Dry Run Method |
|-----|----------------|
| Python | Use TestPyPI: set `TWINE_REPOSITORY_URL=https://test.pypi.org/legacy/` with a separate `TEST_PYPI_TOKEN` |
| JavaScript | Run `npm publish --dry-run` locally to verify package contents |
| Go | Push a pre-release tag (e.g., `go-sdk-v0.0.1-alpha.1`) and check `pkg.go.dev` |
| Java | Sonatype staging is the default — artifacts are staged and require manual release at first |
| PHP | Run `composer validate --strict` locally; Packagist pulls automatically |
| Ruby | Run `gem build allnewsapi.gemspec` locally to verify the gem builds cleanly |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Package name already taken" | Check name availability before first publish; use a scoped name if needed |
| "401 Unauthorized" on publish | Verify the secret value is correct; check the token hasn't expired |
| "403 Forbidden" on npm | Ensure the token type is "Automation" (not "Read-only" or "Publish" which requires 2FA) |
| "GPG signing failed" (Java) | Ensure the GPG public key is uploaded to `keyserver.ubuntu.com`; verify the passphrase matches |
| Packagist not updating | Check the webhook is configured correctly; verify `PACKAGIST_TOKEN` is valid |
| Go module not indexed | Wait 5-10 minutes; verify with `GOPROXY=https://proxy.golang.org go list -m module@version` |
| Release-Please not creating PRs | Ensure commits follow [Conventional Commits](https://www.conventionalcommits.org/) format |
| Tag not triggering workflow | Verify the tag pattern matches the `on.push.tags` filter in the workflow file |
| Maven Central publish failed | Check token is valid in Central Portal → Account → User Token; verify GPG key is on keyserver |

---

## Security Best Practices

- Use **project-scoped tokens** where possible (PyPI, RubyGems) to limit blast radius
- Rotate tokens periodically (every 6-12 months)
- Use npm **Automation** tokens instead of Publish tokens to avoid 2FA issues in CI
- Never commit secrets to the repository — always use GitHub Actions secrets
- Consider enabling **GitHub Environments** with approval rules for production publish workflows
- Audit secret access in **Settings → Secrets → Access** to ensure only needed workflows can read them
