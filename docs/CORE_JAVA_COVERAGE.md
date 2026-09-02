# Core Java Coverage Matrix

Only concepts genuinely exercised by real code in this repository. This grows as modules
are actually built — see `README.md` "Current status" for what's not built yet.

**Broader concept coverage lives in `study/`** (14 self-contained, run-verified demos —
see `study/README.md` for the full index): generics, Collections, Streams/lambdas,
exceptions, NIO, concurrency primitives, CompletableFuture, virtual threads, custom
annotations/reflection, and design patterns. The table below is specifically what's
exercised in the *production* `src/main/java/provguard` code.

| Java Concept | Where Used | File/Class | Why | Test/Demo | Status |
|---|---|---|---|---|---|
| Records | Immutable value types | `ProvenanceEvent`, `DetectionResult` | Value objects that may cross thread boundaries (sensor thread → reader) | Multiple tests | Done |
| Sealed interfaces + pattern-matching `switch` | Enforcement outcomes | `Decision` (`Allow`/`Log`/`Block`), switched exhaustively in `EnforcementEngine` | Compiler-checked exhaustiveness for a security decision type — adding a 4th outcome forces every call site to be updated | `EnforcementEngineTest` | Done |
| Enums | Sink categorization | `SinkType` | Closed, type-safe vocabulary | Multiple tests | Done |
| `java.lang.instrument` (Java agents) | Agent entrypoint | `AgentBootstrap` (`premain`, `agentmain`) | Required mechanism for JVM-level instrumentation | Manual run via `-javaagent`, see README | Done |
| ByteBuddy `AgentBuilder` + `Advice` | Sink hooking (×3) | `InstrumentationManager`, `ProcessExecutionAdvice`, `DeserializationAdvice`, `JndiAdvice` | High-level, safer bytecode weaving than raw ASM | Sensor tests | Done (2 of 3 actually intercept at runtime — see `docs/DESIGN_DECISIONS.md` §6) |
| `StackWalker` | Provenance capture | `StackWalkerCollector` | Lazy, low-overhead call-stack inspection | Sensor tests | Done |
| Class loaders (bootstrap vs. application) | Cross-classloader callback from woven JDK code | `BootstrapInjector` | Real, non-optional JVM constraint — see `docs/DESIGN_DECISIONS.md` for six real bugs this class of problem caused and how each was fixed | All sensor tests (implicitly — they only pass because this works) | Done |
| `java.util.jar` / `java.nio.file` | Building the bootstrap-visible temp jar | `BootstrapInjector` | Zips either a loose classes directory or filters an existing (possibly shaded) jar at runtime | Exercised every test run | Done |
| Concurrency: `CopyOnWriteArrayList` | Thread-safe event storage | `EventBuffer` | Sink hits can occur on multiple threads; safe concurrent iteration, write-rare/read-more workload | Sensor tests | Done (simple; not the final `provguard-runtime` design) |
| Static holder pattern (deliberately, not enum singleton) | Shared config accessible from inlined Advice bytecode | `EventBuffer.INSTANCE`, `Policy.detector`/`blockingEnabled` | Advice-inlined code can't receive constructor-injected dependencies | Sensor tests | Done |
| Interfaces as a Strategy point | Pluggable detection algorithm | `AnomalyDetector` (implemented by `AllowlistDetector`; future ML detectors implement the same interface) | Callers never branch on concrete detector type | `AllowlistDetectorTest` | Done |
| Custom unchecked exception extending a JDK type | Enforcement veto | `SinkBlockedException extends SecurityException` | Both semantically correct (a real security denial) and practically important — see `docs/DESIGN_DECISIONS.md` §4F for why extending a bootstrap-native JDK type avoids a real cross-classloader identity bug | `EnforcementEngineTest`, sensor tests | Done |
| `equals()`/`hashCode()` contract | Graph node/edge deduplication | `GraphNode`, `GraphEdge` | Needed for correct `Set` membership when building a provenance graph | `GraphBuilderTest` | Done |
| Streams + `Collectors` | Turning `StackWalker` frames into a `List<String>` | `StackWalkerCollector` | Declarative frame-to-string mapping | Sensor tests | Done |
| JUnit 5 (`@BeforeEach`, `@Disabled`) + real `-javaagent` attached for the whole test JVM | Testing agent behavior | All tests | Proves real interception happens, not just that code compiles; `@Disabled` used honestly on the one known-broken sensor. Attaching at true JVM startup (rather than per-class dynamic self-attach) was itself the fix for a real classloader-ordering bug — see `docs/DESIGN_DECISIONS.md` §4G | Is the test suite | Done |
| Simple statistical modeling (mean/variance/z-score) in plain Java, no ML framework | A real, honest first "learned model" baseline | `OneClassDistanceDetector`, `FeatureVector`, `FeatureExtractor` | Trained on genuinely captured data, not invented numbers; its real, tested limitation (can't distinguish same-shaped calls from different callers) is what motivates the caller-identity-based `AllowlistDetector` actually being the one wired into enforcement | `OneClassDistanceDetectorTest` | Done |

## Not yet covered (planned, not fabricated here)

Generics/bounded wildcards, most of the Collections framework beyond `Set`/`List`,
checked exceptions, NIO channels/buffers beyond `Files`/`Path` (used in `BootstrapInjector`),
virtual threads, `ExecutorService`/`CompletableFuture`, `ReentrantLock`/Atomics, reflection
beyond what ByteBuddy does internally, custom annotations, JPMS, JFR, and design patterns
like Factory/Builder/Observer — all of these exist in the `study/` modules (see
`study/README.md`), but not yet in the production `provguard.*` code, because no
production module has genuinely needed them yet.
