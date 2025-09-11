# Contributing to dist-diff

Thank you for considering contributing to `wildfly/dist-diff`! This document describes the recommended workflow for reporting issues, proposing improvements, and submitting pull requests so that your contributions can be reviewed and merged smoothly.

> **TL;DR:** Fork → branch → commit → push → open a Pull Request (PR).

---

## Table of Contents

1. [Where to report issues and request features](#where-to-report-issues-and-request-features)
2. [Setting up the development environment](#setting-up-the-development-environment)
3. [Working with branches and pull requests](#working-with-branches-and-pull-requests)
4. [Commit message format and sign-off](#commit-message-format-and-sign-off)
5. [Code style and quality checks](#code-style-and-quality-checks)
6. [Testing and local verification](#testing-and-local-verification)
7. [Security reporting](#security-reporting)
8. [License and copyright](#license-and-copyright)

---

## Where to report issues and request features

- Use GitHub Issues for bug reports and feature requests: [https://github.com/wildfly/dist-diff/issues](https://github.com/wildfly/dist-diff/issues)
- When reporting a bug, please provide as much detail as possible:
    - version (`dist-diff2-$VERSION` or commit hash)
    - steps to reproduce
    - expected vs. actual behavior
    - tool output and/or minimal reproducible example
- For security-related issues, please refer to the repository’s `SECURITY.md`. **Do not** open public issues for security vulnerabilities.

## Setting up the development environment

1. Fork the repository and clone it locally:

```bash
git clone https://github.com/<your-username>/dist-diff.git
cd dist-diff
```

2. The project uses Maven. Build locally with:

```bash
mvn clean install
```

3. Check `checkstyle.xml` in the root directory and make sure your IDE follows the same rules.

> Note: See the `README.md` for usage instructions (CLI arguments, options) before making changes.

## Working with branches and pull requests

- Always create a dedicated branch for your work, named according to the change type, e.g. `fix/short-description`, `feat/new-phase`, `docs/update-readme`.
- Rebase/update your branch on top of `main` before opening a PR to keep history clean.
- Open a PR against `main` with a clear title and description.
- In the PR description, include:
    - a summary of the changes
    - the motivation for the change
    - how to verify/test (steps, examples)
    - any CLI or API changes

### Suggested PR template

```
Resolves: #<issue-number>  # if applicable

### Description
Short summary of what this PR does.

### Testing
- What has been added/tested

### Notes
- backward-incompatible changes? yes/no
- any additional notes for reviewers
```

## Commit message format and sign-off

- Write clear commit messages: a short summary line (~50 chars max) and optional detailed description in the body.
- Sign your commits using the Developer Certificate of Origin (DCO):

```bash
git commit -s
```

This adds a line such as:

```
Signed-off-by: Your Name <you@example.org>
```

Adding this line indicates that you agree to the terms described in [`dco.txt`](dco.txt).

## Code style and quality checks

- Follow the existing coding style.
- Ensure your code passes `mvn checkstyle:check`.
- Avoid unnecessary dependencies.
- Add JavaDoc and comments where appropriate.

## Testing and local verification

- Run unit tests locally with:

```bash
mvn test
```

- Add new tests for any new features or bug fixes.
- Verify CLI compatibility if you change command-line options.

## Security reporting

- Do not disclose security issues publicly.
- Report them according to the instructions in [`SECURITY.md`](SECURITY.md).

## License and copyright

By contributing, you agree that your contributions will be licensed under the same license as the project (see [`LICENSE`](LICENSE)).
