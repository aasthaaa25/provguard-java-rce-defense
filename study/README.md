# ProvGuard Study Repository

Self-contained, runnable Java examples, kept deliberately separate from the production
`src/main/java/provguard/...` code. Each file is a single-file source program
(`java <File>.java` runs it directly, no build step — JEP 330).

Each folder's `README.md` says: what the concept is, why it matters, and where (if
anywhere) the real ProvGuard production code in `src/main/java` uses it.

| # | Topic | File |
|---|---|---|
| 01 | Classes, objects, constructors, static/final, access modifiers, enums | `01-basics/Basics.java` |
| 02 | Inheritance, polymorphism, overriding/overloading, composition | `02-oop-polymorphism/Polymorphism.java` |
| 03 | Records, sealed interfaces, pattern-matching switch, text blocks | `03-records-sealed/RecordsSealed.java` |
| 04 | Generics: classes, methods, bounded types, wildcards | `04-generics/Generics.java` |
| 05 | Collections: List/Set/Map/Deque/PriorityQueue, equals/hashCode, Comparator | `05-collections/CollectionsDemo.java` |
| 06 | Lambdas, functional interfaces, method references, Streams, Optional | `06-streams-lambdas/StreamsLambdas.java` |
| 07 | Checked/unchecked/custom exceptions, chaining, try-with-resources | `07-exceptions/ExceptionsDemo.java` |
| 08 | NIO: Path, Files, channels | `08-nio/NioDemo.java` |
| 09 | Threads, ExecutorService, Future, synchronized/Lock, Atomics, volatile | `09-concurrency/ConcurrencyDemo.java` |
| 10 | CompletableFuture composition | `10-completable-future/CompletableFutureDemo.java` |
| 11 | Virtual threads (Project Loom) | `11-virtual-threads/VirtualThreadsDemo.java` |
| 12 | Custom annotations + reflection | `12-reflection-annotations/ReflectionAnnotationsDemo.java` |
| 13 | Design patterns: Strategy, Factory, Builder, Observer | `13-design-patterns/DesignPatternsDemo.java` |
| 14 | Classloaders and delegation | `14-classloaders/ClassLoadersDemo.java` |
| 15 | Java agents (pointer to the real one) | `15-java-agents/README.md` |

Run any of them directly, e.g.:
```
java study/06-streams-lambdas/StreamsLambdas.java
```
