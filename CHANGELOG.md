<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Turbo Log Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- **Insert Log Statement**: place the caret on a variable, parameter,
  or field and generate a `println`/`System.out.println` with class,
  method, and line baked in as literal text -- works identically in
  static, instance, and top-level contexts.
- **Remove All Log Statements**: finds and removes every statement
  this plugin previously inserted into the current file, in one
  action, via a consistent `TCLC` marker.
- Real Java and Kotlin support.
- In-memory PSI validation before every insertion -- never writes a
  statement that wouldn't parse.

[Unreleased]: https://github.com/GapHunterLabs/turbo-log-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/turbo-log-companion/commits/0.1.0
