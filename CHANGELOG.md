# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0-alpha1] - 2026-05-28

The first release under the new coordinates. The `1.0.0` major bump
marks the coordinate / package / module rename, not API
stabilisation; subsequent alpha releases may continue to evolve the
API. The `1.0.0` milestone proper is reserved for the API-stable
cut.

### Changed
- **Maven coordinates moved from `org.javai` to `org.mavai`.** New
  coordinate: `org.mavai:outcome:1.0.0-alpha1`. The artifactId
  (`outcome`) is unchanged.
- **Java package renamed `org.javai.outcome` → `org.mavai.outcome`.**
  Affects every public type. Consumers must update their imports.
- **JPMS module name renamed `org.javai.outcome` → `org.mavai.outcome`.**
  Modular consumers must update `requires org.javai.outcome` →
  `requires org.mavai.outcome` in their own `module-info.java`.
- **Repository moved `github.com/javai-org/outcome` →
  `github.com/mavai-org/outcome`.** The old URL 301-redirects forever;
  Maven `<scm>` and POM `<url>` now reference the new home.
- **Version bumped to `1.0.0`.** The coordinate/package/module rename
  is a breaking change for consumers; the major bump marks the
  relocation, not an API change. The sealed-type shape
  (`Outcome<T>` / `Ok<T>` / `Fail<T>`) is preserved verbatim.

### Migration
A final `org.javai:outcome:0.3.99` release ships a Maven relocation
POM that auto-redirects to `org.mavai:outcome:1.0.0-alpha1` with a
deprecation warning. Consumers should update their dependency
coordinates explicitly; the relocation POM is a safety net, not a
permanent solution.

## [0.3.0] - 2026-05-11

### Added
- **`module-info.java`** — the library now ships a real JPMS module
  descriptor. The module name is `org.javai.outcome`; every public
  package is exported (`org.javai.outcome`, `.boundary`, `.ops`,
  `.ops.log4j`, `.ops.metrics`, `.ops.slack`, `.ops.teams`,
  `.retry`). Requires `java.net.http`, `java.sql`, and
  `static org.slf4j`. Modular consumers can now depend on the
  published artifact directly via `requires org.javai.outcome`
  without the `extra-java-module-info` shim previously needed.

### Consumer impact
- **Classpath consumers** — no change. The descriptor is invisible
  to non-modular builds.
- **Modular consumers** — add `requires org.javai.outcome` to their
  own `module-info.java`. Sibling projects that previously wrapped
  outcome as an automatic module (notably `punit-core`'s
  `extraJavaModuleInfo { automaticModule("org.javai:outcome",
  "outcome") }` block) can drop the shim once they upgrade to
  0.3.0; the module is now named `org.javai.outcome`.

## [0.2.0] - 2026-03-10

### Changed
- `Failure` record fields `exception`, `retryAfter`, and `correlationId` now use `Optional` instead of nullable types
- `RetryPolicy` implementations now respect the failure's `retryAfter` hint via a shared `withDelayStrategy` method, removing duplication between `fixed` and `backoff` policies

### Added
- `HttpStatusException` — checked exception carrying HTTP status code and parsed `Retry-After` hint
- `HttpResponses.requireSuccess(HttpResponse)` — utility to convert non-2xx responses into `HttpStatusException`
- `BoundaryFailureClassifier` support for `HttpStatusException`: classifies 429 and 5xx as transient, other 4xx as permanent, with `Retry-After` propagation

## [0.1.0] - 2025-12-15

Initial release of Outcome — a framework for building action plans based on
natural language inputs.

### Added
- `Outcome` type with success/failure semantics and tracking keys
- `Boundary` for structured failure classification with transient/permanent/correctable types
- Retry support via `Retrier` with configurable strategies and corrective retry
- Operator reporting: `Log4jOpReporter`, `MetricsOpReporter`, `SlackOpReporter`, `TeamsOpReporter`
- `CompositeReporter` for combining multiple reporters
- `DefectClassifier` for mapping exceptions to failure types
- Static convenience methods for low-ceremony usage

[Unreleased]: https://github.com/mavai-org/outcome/compare/v1.0.0-alpha1...HEAD
[1.0.0-alpha1]: https://github.com/mavai-org/outcome/compare/v0.3.0...v1.0.0-alpha1
[0.3.0]: https://github.com/javai-org/outcome/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/javai-org/outcome/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/javai-org/outcome/releases/tag/v0.1.0
