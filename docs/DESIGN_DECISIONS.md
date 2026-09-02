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

## 4. Two real classloader bugs found and fixed (the most valuable content in this file)

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

## 5. Not yet decided (deferred to when the relevant module actually gets built)

- DJL vs. ONNX Runtime Java — no ML code exists yet
- Graph representation — no provenance graph code exists yet
- Concurrency model for the event pipeline — `EventBuffer` today is a simple
  `CopyOnWriteArrayList` singleton, adequate for one sensor and a test; revisit once
  there's real throughput to measure
