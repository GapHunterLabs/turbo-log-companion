<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Turbo Log Companion Changelog

## [Unreleased]

## [0.1.2]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace reviews
  page instead of the vendor's generic plugin list.
- `LogStatementFinder`'s whole-file PSI walk (used by "Remove All Log
  Statements") now calls `ProgressManager.checkCanceled()` per
  element, matching the rest of the catalog's convention for any
  unbounded recursive traversal.

## [0.1.1]

### Added

- Review/star CTA: after 5 successful log-statement insertions (never
  counted for the validation-failed branch, and never for "Remove All
  Log Statements", which is cleanup, not the plugin's value
  proposition), a one-time notification asks whether to rate the
  plugin on Marketplace, with a permanent "Don't ask again" option.

### Fixed

Found via live interactive testing 2026-08-14, all 4 confirmed fixed
in the same live sandbox afterward:

- **Remove All Log Statements** threw `PsiInvalidElementAccessException`
  and only removed the first of several inserted statements. Now
  removes the statement and its leading whitespace as a single atomic
  operation.
- Inserting a log statement on a variable whose only appearance in the
  file was its own declaration (e.g. right after `String status =
  "PENDING";`) silently did nothing -- "Insert Log Statement" now works
  on declarations too, not just later usages.
- Inserting on the last statement in a block landed the new statement
  outside the block's closing brace (invalid code).
- Inserting on a variable inside a `return`/`throw` statement landed
  the log call AFTER it -- unreachable code, a real compile error. Now
  inserts before instead.

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

[Unreleased]: https://github.com/GapHunterLabs/turbo-log-companion/compare/0.1.2...HEAD
[0.1.2]: https://github.com/GapHunterLabs/turbo-log-companion/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/GapHunterLabs/turbo-log-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/turbo-log-companion/commits/0.1.0
