# 07 — Exceptions

**Concept:** checked vs. unchecked exceptions, custom exception types, exception
chaining (`cause`), try-with-resources.

**Example:** `ExceptionsDemo.java` — an unchecked `SinkBlockedException` (policy
decisions shouldn't force every caller to catch them), a checked `PolicyLoadException`
(recoverable, callers must decide), and a `try`-with-resources `Closeable`.

**Why it matters:** ProvGuard's planned enforcement layer needs exactly this
distinction — `SinkBlockedException` should propagate as "blocked on purpose", not be
confused with a real bug.

Run: `java ExceptionsDemo.java`
