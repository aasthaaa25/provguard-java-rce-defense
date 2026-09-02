# Week 1 — Notes

Three real classloader bugs were the actual content of this week, not incidental noise:

1. **Advice resolution ordering.** `Advice.to(...)` must be resolved before
   `appendToBootstrapClassLoaderSearch(...)` runs, or it intermittently fails to find the
   `@Advice.OnMethodEnter` method. Root cause not fully traced into ByteBuddy internals,
   but the fix (resolve first, inject second) is 100% reproducible.
2. **Bootstrap injection marker class matters.** Using `EventBuffer.class` itself to
   locate the injection jar forced `EventBuffer` to load via the application classloader
   *before* the bootstrap classloader could see it — so the woven code and the test ended
   up talking to two different `EventBuffer` classes with separate `INSTANCE` statics.
   Fixed by using a marker class (`InstrumentationManager`) that never needs to be shared
   across the classloader boundary.
3. **Filter the injected jar to our own package.** In the packaged/shaded-jar scenario,
   injecting the *whole* fat jar (including ByteBuddy's own classes) into the bootstrap
   search path caused a `LinkageError` — ByteBuddy's classes became loadable from two
   classloaders at once. Fixed by always filtering to `provguard/` entries only.

Environment friction ate real time too: Avast's antivirus does TLS interception, which
broke both `winget` and Maven Central access until its root cert was imported into a
writable copy of the JDK's `cacerts`. Full details in `docs/TROUBLESHOOTING.md`.

See `docs/DESIGN_DECISIONS.md` for the complete, detailed writeup of all of the above.
