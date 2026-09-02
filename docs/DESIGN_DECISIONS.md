# Design Decisions

Documented as they were actually made, in order, including the bugs that forced them —
this is more useful to a future contributor than a tidied-up rationale written after the fact.

## 1. Build tool: Maven

No Maven or Gradle was installed on the dev machine. Maven was chosen and installed
manually (downloaded from `archive.apache.org`, since `dlcdn.apache.org` only serves the
latest release and 3.9.9 had aged out of it). A single-module `pom.xml` was used for this
first slice rather than the full multi-module layout planned in `ARCHITECTURE.md`, to
minimize POM complexity while proving the core mechanism works at all. Splitting into
`provguard-agent` / `provguard-sensors` / `provguard-provenance` / etc. modules is
follow-up work once there's more than one sensor to justify the boundaries.

## 2. JDK version: built against JDK 26, NOT the recommended JDK 21 LTS

The only JDK present on the dev machine was Eclipse Adoptium **26.0.2** (a very recent,
non-LTS release). The ProvGuard brief explicitly asks for Java 21 LTS. Under time
pressure, the decision was made to build against what was actually installed rather than
spend the budget installing another JDK, and to document the consequences honestly
instead of silently deviating from the brief.

**Real consequence hit during this session:** ByteBuddy 1.15.11 (current stable at the
time) does not officially recognize JDK 26 by version-check, though in practice
`-Dnet.bytebuddy.experimental=true` was sufficient to get past that — no ByteBuddy code
changes were needed. Since JDK 26 support in ByteBuddy is best-effort/unofficial as of
this build, **this project should be re-verified against JDK 21 LTS before being relied
upon**, and `-Dnet.bytebuddy.experimental=true` is currently required (see `pom.xml`
surefire `argLine`, and the `-D` flag on the `DemoMain` command in `README.md`).

## 3. Bytecode instrumentation: ByteBuddy, no ASM

Only ByteBuddy was needed for this slice (an `Advice`-based hook on
`ProcessBuilder#start()`). No case for dropping to raw ASM has come up yet — this
matches the brief's instruction not to use ASM just to inflate the technology list.

## 4. Seven real bugs found and fixed while building this (the most valuable content in this file)

Both bugs are specific to instrumenting a **bootstrap-classloader-loaded** JDK class
(`java.lang.ProcessBuilder`) with `Advice`-inlined bytecode that calls back into our own
classes — this is a genuine, non-optional JVM constraint (see `BootstrapInjector`'s
Javadoc for the mechanism), not an implementation mistake, and it's exactly the kind of
thing `docs/THREAT_MODEL.md` / class-loading coverage in the original brief was
anticipating.

### Bug A — `Advice.to(...)` must be resolved *before* `appendToBootstrapClassLoaderSearch`

**Symptom:** `java.lang.IllegalArgumentException: No advice defined by class
provguard.sensors.ProcessExecutionAdvice`, thrown from inside the `AgentBuilder`
transform callback, even though `javap -v` confirmed the compiled class had the correct
`RuntimeVisibleAnnotations: net.bytebuddy.asm.Advice$OnMethodEnter` on the right method.
Calling `Advice.to(ProcessExecutionAdvice.class)` in isolation (no `AgentBuilder`
involved) worked fine.

**Root cause (best understanding from the evidence, not fully traced into ByteBuddy's
internals):** the advice visitor was originally being (re-)computed *inside* the
`AgentBuilder` transform lambda, which runs during retransformation — i.e., *after*
`instrumentation.appendToBootstrapClassLoaderSearch(...)` had already mutated the
bootstrap classloader's search path. Moving `Advice.to(ProcessExecutionAdvice.class).on(...)`
to execute once, *before* the bootstrap injection call, and capturing the resulting
`AsmVisitorWrapper` in a local variable for the lambda to reuse, fixed it completely and
reproducibly.

**Takeaway:** resolve all ByteBuddy `Advice`/`AgentBuilder` wiring before doing anything
that changes classloader search paths.

### Bug B — the bootstrap-injection *marker class* must not be the shared/consumed class

**Symptom:** After fixing Bug A, weaving succeeded (`[Byte Buddy] TRANSFORM
java.lang.ProcessBuilder ... loaded=true` with no error) and `pb.start()` no longer
threw — but the test's `EventBuffer.INSTANCE` was still empty. No exception, just silent
data loss.

**Root cause:** `BootstrapInjector.ensureVisible(instrumentation, EventBuffer.class)` was
being called with `EventBuffer.class` as the marker used to locate the jar/directory to
append to the bootstrap search path. But *resolving* `EventBuffer.class` to read its
`ProtectionDomain`/`CodeSource` **forces the JVM to load `EventBuffer` via the
application classloader right then** — before the bootstrap append has happened, so
there's nothing yet for it to delegate to. Later, when the *woven* `ProcessBuilder`
bytecode (loaded by the bootstrap classloader) references `provguard.provenance.EventBuffer`,
the bootstrap classloader — which cannot see classes loaded by a child classloader —
loads its **own, second, independent copy** of `EventBuffer`, with its own separate
`INSTANCE` static field. The sensor was writing to one `EventBuffer` class; the test was
reading from a different one with the same fully-qualified name.

**Fix:** use a marker class that never needs to be shared across the classloader
boundary — `InstrumentationManager.class` itself — to locate the jar for bootstrap
injection. `EventBuffer` is then never touched by application code before the bootstrap
search path already includes it, so its first-ever classload (whichever side triggers it
first) resolves via standard parent-first delegation to the bootstrap classloader, and
every consumer ends up sharing the exact same class/singleton.

**Takeaway:** any class that must be shared between bootstrap-woven code and normal
application code must not be loaded by the application classloader before the bootstrap
classloader can see it. The class used to *discover* the injection jar's location is not
neutral — picking the wrong one silently defeats the whole point of the injection.

### Bug C — filter the injected jar to our own package only (packaged/shaded jar case)

**Symptom:** Bugs A and B were fixed and verified in the `mvn test` (dynamic self-attach)
scenario. Running the *packaged* agent for real (`-javaagent:target/provguard-agent.jar`)
then failed differently:
```
java.lang.LinkageError: loader constraint violation: when resolving method
'net.bytebuddy.matcher.ElementMatcher$Junction net.bytebuddy.matcher.BooleanMatcher.of(boolean)'
the class loader 'app' ... and the class loader 'bootstrap' ... have different Class
objects for the type net.bytebuddy.matcher.ElementMatcher$Junction
```

**Root cause:** the packaged jar is a *shaded* (fat) jar containing not just our
`provguard.*` classes but all of ByteBuddy's own classes too (via
`maven-shade-plugin`). `InstrumentationManager.class`'s `CodeSource` location in this
scenario is the *entire* fat jar, so the original `BootstrapInjector` appended the whole
thing — including ByteBuddy's own classes — to the bootstrap search path. That makes
ByteBuddy's classes loadable from both the application classloader (already loaded,
since our own agent code uses them) and the bootstrap classloader (now also possible),
producing two incompatible definitions of the same class and tripping the JVM's loader
constraint check at link time.

**Fix:** `BootstrapInjector` now always builds a *filtered* temp jar containing only
entries under `provguard/`, whether the origin is a loose `target/classes` directory
(test scenario) or an existing packaged jar (real agent scenario). Every third-party
class, ByteBuddy included, is then loaded exactly once, by its normal classloader.

### Bug D — chaining multiple `.type().transform()` rules on one `AgentBuilder` silently dropped earlier rules

**Symptom:** after adding a second and third sink (deserialization, JNDI) by chaining
`.type(A).transform(x).type(B).transform(y).type(C).transform(z)` on a single
`AgentBuilder.Default()` instance, only the *last* registered rule (`C`) actually fired.
`ProcessExecutionSensorTest`, which passed before this change, started failing with 0
events captured — the earlier-working `ProcessBuilder` sensor stopped firing entirely,
with no exception anywhere in the (very verbose) `AgentBuilder.Listener` output.

**Root cause:** not fully traced into ByteBuddy's internals in the time available — the
listener showed the last rule's target class going through `DISCOVERY`/`TRANSFORM`/
`COMPLETE` cleanly, while the earlier rules' target classes never appeared in the log at
all, as if those rules were never registered.

**Fix:** stopped chaining. `InstallationManager` now creates and installs **one
independent `AgentBuilder.Default()` per sink** (`installOne(instrumentation, targetType,
advice)`), each with its own single `.type().transform().installOn()` call. This sidesteps
the issue entirely, and arguably reads more simply besides — worth knowing this pattern
is safer than the chained-rules form the ByteBuddy docs also show as valid syntax.

### Bug E — the "immediate caller" heuristic breaks for sinks that call themselves internally

**Symptom:** once the deserialization sensor's weaving itself worked, `DeserializationSensorTest`
started throwing `SinkBlockedException` unexpectedly: `"caller java.io.ObjectInputStream is
NOT on the trusted allowlist"`.

**Root cause:** `ObjectInputStream#resolveClass()` is invoked internally by *other methods
of the same class* (`readObject -> readOrdinaryObject -> resolveClass`, all within
`ObjectInputStream`). `ProvenanceGraph.callerClassName()`'s original implementation
naively returned "whatever frame is one above the sink frame" — for `ProcessBuilder`
(no internal recursion) that's the real caller, but for `ObjectInputStream` it's just
`ObjectInputStream` itself again.

**Fix:** `callerClassName()` now walks up the captured frames and returns the first one
whose class name *differs* from the sink's own class name, correctly skipping past any
number of internal same-class frames to find the real external caller. Documented
directly in `ProvenanceGraph`'s Javadoc since it's a non-obvious requirement, not an
implementation detail.

### Bug F — SinkBlockedException identity mismatch across the classloader boundary in tests

**Symptom:** `assertThrows(SinkBlockedException.class, ...)` failed with `"Unexpected
exception type thrown, expected: <SinkBlockedException@X> but was: <SinkBlockedException@Y>"`
— both objects were genuinely instances of a class named `provguard.enforcement.SinkBlockedException`,
just not the *same* `Class` object.

**Root cause:** the exception is thrown from inside Advice-woven, bootstrap-loaded sink
code, so the thrown instance's class was loaded via the bootstrap classloader. The test's
own `SinkBlockedException.class` literal, however, can end up resolved via the test's own
classloader context depending on exactly when the JVM resolves that particular constant —
a timing-sensitive detail not fully pinned down (unlike Bug B, this wasn't traced to a
single provably-avoidable ordering mistake in our own code).

**Fix — a more robust pattern than "get the ordering exactly right" again:**
`SinkBlockedException` now extends `SecurityException` (a bootstrap-native `java.lang`
type). Tests assert against `SecurityException.class` — guaranteed to be the exact same
`Class` object everywhere in the JVM regardless of which classloader loaded the specific
subclass — and separately check `thrown.getClass().getName()` (a `String` comparison,
immune to identity issues) to confirm it's genuinely our exception. This is also more
semantically correct: a blocked security-sensitive operation is exactly what
`SecurityException` is for.

### Bug G — pure unit tests touching provguard.* types could be "first" and pin them to the application classloader

**Symptom:** after adding a new test class in `provguard.detection`, previously-passing
pure unit tests (`AllowlistDetectorTest`, `PolicyEngineTest`, `EnforcementEngineTest`,
`GraphBuilderTest` - none of which install the agent themselves) started failing with
`LinkageError: loader constraint violation` and `IncompatibleClassChangeError`, the same
family of error as Bug B/F but now hitting test-only code, not production code.

**Root cause:** these pure unit tests reference `provguard.graph`/`detection`/`enforcement`
types directly (as field types, method parameter types, etc.) and never called
`InstrumentationManager.install(...)` themselves - they relied on *some other* test class
(a sensor test) happening to run first in the same forked JVM and installing the agent
before they touched those types. Adding a new test class changed Surefire's execution
order enough that a pure unit test became the *first* code in the JVM to reference some
of these types - at which point they got loaded via the application classloader, before
bootstrap injection had ever run. When a sensor test's `@BeforeAll` installed the agent
afterwards, the woven code got its own, separate, bootstrap-loaded copies of the same
classes, and the two sides disagreed on class identity from then on for the rest of the
JVM's life. Adding `@BeforeAll` calls to the affected test classes individually did NOT
fix this: JUnit must fully load (and verify) a test class - which can eagerly resolve its
field types - before it can even run that class's own `@BeforeAll`, so the pinning could
happen before any in-class fix had a chance to run.

**Fix:** stopped relying on any test's `@BeforeAll` for this entirely. The agent is now
attached via a real `-javaagent` at JVM startup for the whole `mvn test` run (see
`pom.xml`'s `maven-jar-plugin` `early-agent-jar-for-tests` execution, bound to
`process-test-classes` - before the `test` phase - producing an early, unshaded jar
containing just `provguard.*`, with ByteBuddy resolved via Surefire's normal test
classpath; and the surefire `argLine`, which points `-javaagent` at that jar). This
guarantees bootstrap injection happens before Surefire loads even the *first* test class,
regardless of execution order, permanently closing this entire class of bug rather than
patching it per-class. All the redundant per-class `ByteBuddyAgent.install()` dynamic
self-attach calls were then removed as dead weight.

**A real side effect this surfaced:** with the agent genuinely active from JVM startup,
`ByteBuddyAgent`'s own internal self-attach machinery (which spawns a helper process via
`ProcessBuilder`) started getting **genuinely blocked** by our own enforcement, since
`net.bytebuddy.agent.ByteBuddyAgent` was never on the trusted allowlist. This is a real
demonstration that enforcement works exactly as designed - it just meant the (now
unnecessary) dynamic self-attach calls needed to go regardless.

## 6. A known, unresolved limitation (documented, not hidden)

**`InitialContext#lookup(String)` (the JNDI sink) does not actually intercept calls at
runtime, despite `AgentBuilder`'s listener reporting a clean, successful `TRANSFORM`.**
The real `lookup()` body runs unmodified — no `Advice.OnMethodEnter` code executes, no
`ProvenanceEvent` is captured, confirmed with a raw `System.out.println` placed as the
very first line of `JndiAdvice.onEnter()`: it never printed, across multiple runs, even
though `javap -p javax.naming.InitialContext` on this JDK confirms the exact target
method exists as expected (`public java.lang.Object lookup(java.lang.String) throws
javax.naming.NamingException`) and the element matcher
(`named("lookup").and(takesArguments(1)).and(takesArgument(0, String.class)).and(isPublic())`)
matches it correctly on inspection.

**Narrowed, not fully root-caused:** the one concrete difference between this sink and
the two working ones is that `InitialContext` lives in the `java.naming` platform module,
while `ProcessBuilder` and `ObjectInputStream` both live in `java.base`. `BootstrapInjector`
makes `provguard.*` classes visible to the bootstrap classloader's *unnamed* module; it is
plausible that `java.naming`'s module boundary doesn't implicitly "read" that unnamed
module the way `java.base` does (nearly everything reads `java.base` implicitly; that is
not true of other platform modules), and that this silently prevents the woven advice
from linking/executing even though ByteBuddy's own retransformation bookkeeping reports
success. **Fix attempt 1 (ruled out):** added an explicit `Instrumentation.redefineModule(...)`
call in `InstrumentationManager`, granting `java.naming` a "reads" edge to the module our
bootstrap-injected classes actually end up in (obtained via
`provguard.provenance.SinkType.class.getModule()`, referenced *after* the bootstrap
append so it resolves through the bootstrap classloader rather than the application one).
This compiles and runs without error but did **not** fix the issue — the advice still
never fires. The module-read grant code was left in place (it's harmless, and may still
be a real prerequisite even if not sufficient alone) but the root cause is evidently
something else, or something more than a missing reads edge.

**Fix attempt 2, follow-up (also inconclusive):** tried enabling ByteBuddy's bytecode-dump
feature (`-Dnet.bytebuddy.dump=<dir>`, set via the surefire `argLine`) to compare the
actual retransformed bytecode of `InitialContext` against `ProcessBuilder`'s. The dump
directory stayed empty for *both* classes — the dump mechanism itself never activated in
this configuration (likely needs to be wired through `AgentBuilder`'s own API rather than
only the system property, which apparently isn't sufficient for this ByteBuddy
version/setup). This didn't produce new evidence either way; it just means this
particular diagnostic technique needs more setup than was tried before it can be useful.

**Fix attempt 3 (ruled out):** once the test infrastructure was changed to attach the
agent via a real static `-javaagent` at JVM startup instead of dynamic self-attach (see
section 4G above), the JNDI sensor was re-tested under this genuinely different attach
mechanism, on the theory that static vs. dynamic attach might matter for a
platform-module class. It did not fix it - the woven Advice still never executes when
`lookup()` is genuinely invoked, exactly as before.

Given four concrete, reasoned diagnostic/fix attempts have now been tried without
resolving it, further debugging is left as documented follow-up rather than continued
open-ended guessing. A reasonable next step for whoever picks this up: get the ByteBuddy
dump working properly (via `AgentBuilder`'s dump configuration API, not just the system
property) to see whether the `Advice` bytecode is actually inlined into the retransformed
`InitialContext` class at all — that would show definitively whether this is a
weaving-time problem (bytecode never actually changed) or a link/execution-time one
(bytecode changed but something prevents it from running).
`JndiSensorTest.capturesProvenanceWhenLookupIsInvoked`
is marked `@Disabled` with this explanation rather than deleted, silently left failing, or
"fixed" by weakening the assertion — the sensor code is real and the failure is real;
follow-up work should start by comparing `javax.naming.InitialContext`'s and
`java.io.ObjectInputStream`'s module/classloader metadata at retransform time.

## 7. Why detection/enforcement are separate from the sensors, and why AllowlistDetector first

`SensorPipeline.captureAndEnforce()` deliberately chains four independently-testable
stages (capture → graph → detect → decide-and-enforce) instead of one big method, mirroring
the full architecture's Sensor → Provenance → Graph → Detection → Enforcement pipeline at
a smaller scale. `AllowlistDetector` (not a learned model) was built first because it's
explicitly the comparison baseline the ProvGuard research plan calls for — a future
learned model has to demonstrably beat it (generalizing to callers never explicitly
listed) to justify its complexity. Building the baseline first, for real, with real
enforcement wired to it, is more valuable than stub detector code waiting for ML that
doesn't exist yet.

## 8. Not yet decided (deferred to when the relevant module actually gets built)

- DJL vs. ONNX Runtime Java — no ML code exists yet
- Graph representation beyond the current simple node/edge model — no feature-extraction-for-ML
  code exists yet; today's `ProvenanceGraph` only needs to support `AllowlistDetector`
- Concurrency model for the event pipeline — `EventBuffer` today is a simple
  `CopyOnWriteArrayList` singleton, adequate for three sensors and a test suite; revisit
  once there's real throughput to measure
