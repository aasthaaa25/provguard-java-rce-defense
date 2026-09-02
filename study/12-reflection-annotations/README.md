# 12 — Custom Annotations + Reflection

**Concept:** defining a custom runtime-retained annotation and scanning for it via
reflection.

**Example:** `ReflectionAnnotationsDemo.java` — a `@Sink("...")` annotation, applied to
two methods, discovered at runtime by scanning `getDeclaredMethods()`.

**Why it matters:** ProvGuard's planned `SinkRegistry` can use exactly this pattern —
declaratively mark sensor methods with `@Sink`, then discover them at agent startup
instead of hand-maintaining a registration list.

Run: `java ReflectionAnnotationsDemo.java`
