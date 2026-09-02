# Core Java Coverage Matrix

Only concepts genuinely exercised by real code in this repository so far. This grows as
modules are actually built — see `README.md` "Current status" for what's not built yet.

**Broader concept coverage lives in `study/`** (14 self-contained, run-verified demos —
see `study/README.md` for the full index): generics, Collections, Streams/lambdas,
exceptions, NIO, concurrency primitives, CompletableFuture, virtual threads, custom
annotations/reflection, and design patterns. The table below is specifically what's
exercised in the *production* `src/main/java/provguard` code, not the study modules.

| Java Concept | Where Used | File/Class | Why Used | Test/Demo | Status |
|---|---|---|---|---|---|
| Records | Immutable provenance data | `ProvenanceEvent` | Value object for a captured sink event; immutability matters since it may cross thread boundaries (sensor thread -> reader) | `ProcessExecutionSensorTest` | Done |
| Enums | Sink categorization | `SinkType` | Closed, type-safe vocabulary for sink kinds | `ProcessExecutionSensorTest` | Done |
| `java.lang.instrument` (Java agents) | Agent entrypoint | `AgentBootstrap` (`premain`, `agentmain`) | Required mechanism for JVM-level instrumentation | Manual run via `-javaagent`, see README | Done |
| ByteBuddy `AgentBuilder` + `Advice` | Sink hooking | `InstrumentationManager`, `ProcessExecutionAdvice` | High-level, safer bytecode weaving than raw ASM | `ProcessExecutionSensorTest` | Done |
| `StackWalker` | Provenance capture | `StackWalkerCollector` | Lazy, low-overhead call-stack inspection (vs. `new Throwable().getStackTrace()`) | `ProcessExecutionSensorTest` (asserts stack contents) | Done |
| Class loaders (bootstrap vs. application) | Cross-classloader callback from woven JDK code | `BootstrapInjector` | Real, non-optional JVM constraint when instrumenting bootstrap-loaded classes and calling back into agent code — see `docs/DESIGN_DECISIONS.md` for 3 real bugs this caused and how they were fixed | `ProcessExecutionSensorTest` (implicitly — the test only passes because this works correctly) | Done |
| `java.util.jar` / `java.nio.file` (`Files.walk`, `Path`, try-with-resources) | Building the bootstrap-visible temp jar | `BootstrapInjector` | Needs to zip either a loose classes directory or filter an existing jar at runtime | Exercised every test run | Done |
| Concurrency: `CopyOnWriteArrayList` | Thread-safe event storage | `EventBuffer` | Sink hits can occur on multiple threads; safe concurrent iteration without external locking for a write-rare/read-more workload | `ProcessExecutionSensorTest` | Done (simple; not the final `provguard-runtime` design) |
| Singleton via `static final` (not an enum singleton, deliberately) | Shared event sink accessible from inlined Advice bytecode | `EventBuffer.INSTANCE` | Advice-inlined code can't receive constructor-injected dependencies; a static holder is the standard pattern here | `ProcessExecutionSensorTest` | Done |
| `Optional`-free defensive design / no null returns | `EventBuffer.getAll()` returns `List.copyOf(...)` | `EventBuffer` | Avoids exposing the mutable internal list or returning null | — | Done |
| Streams + `Collectors` | Turning `StackWalker` frames into a `List<String>` | `StackWalkerCollector` | Declarative frame-to-string mapping | `ProcessExecutionSensorTest` | Done |
| JUnit 5 (`@BeforeAll`, `@BeforeEach`) + dynamic self-attach (`ByteBuddyAgent.install()`) | Testing agent behavior without `-javaagent` | `ProcessExecutionSensorTest` | Proves real interception happens, not just that code compiles | Is the test | Done |

## Not yet covered (planned, not fabricated here)

Generics/bounded wildcards, most of the Collections framework beyond one list type,
functional interfaces beyond streams, checked/custom exceptions, NIO channels/buffers
(only `Files`/`Path` used so far), virtual threads, `ExecutorService`/`CompletableFuture`,
`ReentrantLock`/Atomics, reflection beyond what ByteBuddy does internally, custom
annotations, JPMS, JFR, design patterns beyond an implicit Strategy-ish shape in
`InstrumentationManager`. These land as the corresponding modules (`provguard-graph`,
`provguard-detection`, `provguard-enforcement`, `provguard-runtime`) get built.
