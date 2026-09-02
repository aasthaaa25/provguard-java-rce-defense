# 02 — Polymorphism, Overriding, Overloading, Composition

**Concept:** runtime polymorphism via interface dispatch, method overriding vs
overloading, composition over inheritance.

**Example:** `Polymorphism.java` — a `Detector` that is *composed with* a `Sensor`
(not inherited from it), plus overloaded `describe(...)` methods.

**Why it matters:** ProvGuard's planned `AnomalyDetector` interface (with
`OneClassDetector`/`AutoencoderDetector`/`GnnDetector` implementations) will be called
polymorphically — the caller never branches on concrete type.

**Where ProvGuard uses it today:** `provguard.sensors` vs `provguard.agent` is already
a composition relationship (`InstrumentationManager` uses a `ProcessExecutionAdvice`,
doesn't extend it).

Run: `java Polymorphism.java`
