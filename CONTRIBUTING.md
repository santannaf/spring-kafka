# Contributing to Tuned Kafka Library

Thank you for your interest in contributing! This document explains the process for contributing to this project.

## How to Contribute

### 1. Open an Issue

Before writing any code, **open an issue** describing what you want to do:

- **Bug report**: describe the behavior, expected behavior, and steps to reproduce
- **Feature request**: describe the use case and proposed solution

Wait for feedback from the maintainer before starting work.

### 2. Fork and Branch

1. Fork this repository
2. Create a feature branch from `master`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Keep your branch focused on a single change

### 3. Code Guidelines

- Follow the existing code style (2-space indent, LF line endings, max 140 chars)
- Base package: `io.github.santannaf.kafka`
- Add tests for new features or bug fixes
- Make sure all existing tests pass:
  ```bash
  ./mvnw clean verify -pl kafka
  ```

### 4. Commit Messages

- Use clear, descriptive commit messages
- Start with a verb in imperative mood (e.g., "add", "fix", "update")
- Keep the first line under 72 characters

### 5. Open a Pull Request

1. Push your branch to your fork
2. Open a Pull Request against `master`
3. Fill in the PR description explaining **what** and **why**
4. Ensure CI checks pass
5. Wait for review from the maintainer

### What We Accept

- Bug fixes with tests
- Performance improvements with benchmarks
- New features that align with the library's scope (Kafka auto-configuration for Spring Boot)
- Documentation improvements

### What We Don't Accept

- Breaking changes without prior discussion
- Changes unrelated to the library's scope
- PRs without a corresponding issue

## Requirements

- JDK 21+
- Maven (wrapper included: `./mvnw`)
- Docker (for integration tests)

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
