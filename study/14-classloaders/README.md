# 14 — Classloaders and Delegation

**Concept:** the bootstrap → platform → system/application classloader delegation
chain, and the rule that a parent classloader can never see classes loaded by a child.

**Example:** `ClassLoadersDemo.java` — prints the real delegation chain for this JVM,
and shows that `java.lang.String` is bootstrap-loaded while our own `Marker` class is
system-loaded.

**Why it matters — this is not a toy example:** this exact mechanism caused **three
real bugs** while building `provguard.agent.BootstrapInjector`, documented in full in
`docs/DESIGN_DECISIONS.md` §4. Woven `Advice` bytecode inside a bootstrap-loaded JDK
class (`java.lang.ProcessBuilder`) needed to call back into
`provguard.provenance.EventBuffer` (a class loaded by the application classloader) —
which is impossible without explicitly appending our classes to the bootstrap
classloader's search path first.

Run: `java ClassLoadersDemo.java`
