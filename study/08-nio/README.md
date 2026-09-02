# 08 — NIO (Path, Files)

**Concept:** `java.nio.file.Path`/`Files`, try-with-resources over a lazily-read
`Stream<String>`.

**Example:** `NioDemo.java` — writes a few lines to a temp file, reads them back
lazily, checks the size, cleans up.

**Why it matters:** ProvGuard's planned `TraceWriter` persists provenance events to
disk using this modern NIO API rather than the older `java.io.File`.

Run: `java NioDemo.java`
