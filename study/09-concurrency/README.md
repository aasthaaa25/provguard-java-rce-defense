# 09 — Concurrency Basics

**Concept:** `Thread`/`Runnable`, `ExecutorService`, `Future`, `synchronized` vs
`ReentrantLock`, `AtomicInteger`, `volatile`.

**Example:** `ConcurrencyDemo.java` — 100 tasks concurrently increment three counters
(atomic, `synchronized`, `ReentrantLock`) via a thread pool, plus a `volatile`
shutdown flag read by a separate watcher thread.

**Why it matters:** ProvGuard's design explicitly requires detection to never stall the
monitored application's threads — this is the concurrency toolbox that makes that
possible.

**Where ProvGuard uses it today:** `provguard.provenance.EventBuffer` uses
`CopyOnWriteArrayList` (a concurrent collection) for exactly this reason — safe writes
from whichever thread happens to trigger a sink.

Run: `java ConcurrencyDemo.java`
