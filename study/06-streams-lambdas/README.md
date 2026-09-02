# 06 — Lambdas, Functional Interfaces, Streams, Optional

**Concept:** lambda expressions, `Predicate`/`Function`/`Consumer`/`Supplier`, method
references, the `Stream` API + `Collectors`, `Optional`.

**Example:** `StreamsLambdas.java` — filters/maps/groups a list of call-stack frames
using each functional interface by name, then an `Optional`-returning `findFirst()`.

**Where ProvGuard uses it today:** `provguard.provenance.StackWalkerCollector`
(`frames.limit(...).map(...).collect(...)`) is this exact pattern, applied to real
`StackWalker.StackFrame`s instead of the demo `Frame` record here.

Run: `java StreamsLambdas.java`
