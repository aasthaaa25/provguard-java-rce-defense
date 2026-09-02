# 11 — Virtual Threads

**Concept:** virtual threads (JEP 444) — `Executors.newVirtualThreadPerTaskExecutor()`,
`Thread.ofVirtual()`.

**Example:** `VirtualThreadsDemo.java` — runs 20 short "detection" tasks concurrently,
each on its own virtual thread, with no fixed pool size to exhaust.

**Why it matters:** ProvGuard's architecture explicitly calls for detection to run on
virtual threads so a burst of sink hits (e.g. a fast loop calling `ProcessBuilder`)
never blocks application threads waiting for a bounded platform-thread pool to free up.

Run: `java --enable-preview VirtualThreadsDemo.java` (or plain `java VirtualThreadsDemo.java`
on JDK 21+, where virtual threads are no longer preview).
