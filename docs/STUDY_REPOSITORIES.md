# Study Repositories

Repositories worth studying for continuing this project, drawn from the original
ProvGuard research/planning document (Part 2). None of these are dependencies of this
project's code today — they are reading/learning references. Respect each project's own
license; nothing here has been copied from them.

## A. Java security tooling (directly relevant to the flagship)

| Repo | Why study it |
|---|---|
| `frohoff/ysoserial` | The canonical deserialization gadget-chain payload generator (~30 named chains: CommonsCollections1-7, BeanShell1, C3P0, Clojure, etc.). Study the gadget classes to understand exactly what a real deserialization sensor needs to catch. |
| `JackOfMostTrades/gadgetinspector` | Bytecode-level static taint + BFS gadget search. Good for learning ASM-based bytecode analysis (this project uses ByteBuddy, not raw ASM, but the analysis techniques transfer). |
| `tabby-sec/tabby` | Builds Code Property Graphs via Soot, stores them in Neo4j. Study for graph-based program analysis techniques (relevant to a future `provguard.graph` feature-extraction upgrade). |
| `soot-oss/soot` / `soot-oss/SootUp` | The classic Java static-analysis framework and its modern rewrite. Backbone for call-graph/CFG extraction if static enrichment is ever added. |
| `wala/WALA` | IBM's static analysis libraries (pointer analysis, call graphs, slicing). Heavier than Soot, more powerful. |
| `spotbugs/spotbugs` | Production static bug/security finder built on bytecode analysis. Study its detector architecture for how a mature tool structures many independent checks. |
| `raphw/byte-buddy` | The library this project's agent is built on. Its own codebase is the best reference for advanced `Advice`/`AgentBuilder` usage beyond what's used here. |

## B. Java machine-learning libraries (for the future ML work — none of this exists in-repo yet)

| Repo | Why study it |
|---|---|
| `deepjavalibrary/djl` | Engine-agnostic deep learning for Java — one candidate for in-JVM ML inference (the other being ONNX Runtime Java; see `docs/DESIGN_DECISIONS.md` for the not-yet-made decision). |
| `oracle/tribuo` | Pure-Java ML with built-in anomaly detection — a good fit for a future One-Class SVM/Isolation-Forest baseline written entirely in Java (no Python round-trip). |
| `deeplearning4j/deeplearning4j` | Full JVM deep-learning suite (ND4J, SameDiff, ONNX/Keras import). |

## C. Advanced Java concurrency / JVM internals

| Repo | Why study it |
|---|---|
| `JCTools/JCTools` | High-performance lock-free queues for the JVM. Relevant if `provguard.provenance.EventBuffer`'s simple `CopyOnWriteArrayList` ever needs to be replaced under real measured throughput pressure (see `docs/DESIGN_DECISIONS.md` §8 — not yet needed). |
| `google/guava` | Gold standard for collections, caching, concurrency (`ListenableFuture`), and API design generally. |
| `netty/netty` | The reference for NIO, buffers, and high-performance async concurrency. |
| `apache/dubbo` | Large RPC framework; useful for studying SPI/class loading and serialization in a real large codebase (and it has had real deserialization CVEs relevant to this project's threat model). |

## D. Curated "learn advanced Java" / concepts repos

| Repo | Why study it |
|---|---|
| `iluwatar/java-design-patterns` | The definitive design-patterns catalog with runnable examples — a good comparison point for how `study/13-design-patterns/DesignPatternsDemo.java` chose to demonstrate Strategy/Factory/Builder/Observer minimally. |
| `TheAlgorithms/Java` | Broad algorithms/data-structures implementations for general study. |
| `doocs/advanced-java` | High-concurrency, distributed-systems, JVM, and interview-depth Java topics (Chinese with translations). |
| `akullpp/awesome-java` | Curated index of the Java ecosystem — a map to everything else not listed here. |

## Caveat

This area moves fast and star counts / repo states change. Verify current state on each
repo's own page before relying on anything above as current fact — this list is a
starting point for study, not a live inventory.
