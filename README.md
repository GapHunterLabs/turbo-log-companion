# Turbo Log Companion

IntelliJ-family plugin. Place the caret on a variable, parameter, or
field, run **Insert Log Statement**, and get a real `println`
(Kotlin) / `System.out.println` (Java) inserted right after the
enclosing statement — class, method, and line number already baked in
as literal text. **Remove All Log Statements** finds and deletes every
one this plugin has added to the current file, in one action.

## Why it exists

Ports a pattern that's genuinely popular elsewhere — VS Code's Turbo
Console Log has millions of installs — with **no equivalent anywhere
in JetBrains Marketplace** (confirmed by search before building this,
not assumed: zero results for "turbo console log", "turbo log",
"console log" as a code-generation concept). This is a deliberate
"port a proven concept" bet, not a competitor-complaint-driven build —
the same documented-exception discipline this follows (same treatment
as Refactor Simulator/Bean Copy Companion).

## Why built this way

- **Class/method/line baked in as literal text, never runtime
  reflection.** `getClass().getSimpleName()` would break in a static
  method; a Kotlin top-level function has no `this` at all. Since the
  plugin already knows the real class/method/line from PSI at
  generation time, it embeds them directly as string literal text —
  correct in every context, no runtime cost, no reflection dependency.
- **Always anchors on a real block-level statement.** A reference
  inside a braceless `if (x > 0) foo(x);` anchors on the whole
  if-statement, not mid-expression — slightly less precise placement
  in that rare case, but the new statement is always a real sibling of
  a real statement, never inserted where it could produce a syntax
  error.
- **In-memory PSI validation before every insertion.** The rendered
  statement is parsed in a throwaway PSI copy and checked for syntax
  errors before it ever touches the real file — if it can't be
  inserted safely, nothing is written, and you get an honest
  notification instead of broken code. Same discipline already proven
  in Test Scaffold Companion and Bean Copy Companion.
- **A single, consistent `TCLC` marker on every inserted line**, so
  "Remove All Log Statements" can find exactly (and only) what this
  plugin added — never touches a `println` you wrote yourself.
- **Real Java AND Kotlin support** — the concept this plugin ports is
  JS/TS-only everywhere else it exists.
- **100% local** — no network call, no account, no telemetry.

## Usage

Place the caret on a variable/parameter/field reference → right-click
→ **Turbo Log Companion → Insert Log Statement**. To clean up: right-
click anywhere in the file → **Turbo Log Companion → Remove All Log
Statements**.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
