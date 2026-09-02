# 15 — Java Agents

No separate toy demo here on purpose: ProvGuard's own production code IS the real,
working example, and duplicating a second toy agent would teach less than pointing
directly at working code with a real test.

**Read, in this order:**
1. `src/main/java/provguard/agent/AgentBootstrap.java` — `premain`/`agentmain` entrypoints
2. `src/main/java/provguard/agent/InstrumentationManager.java` — wires up the ByteBuddy hook
3. `src/main/java/provguard/agent/BootstrapInjector.java` — the classloader mechanics (see
   also `study/14-classloaders/` for the underlying concept in isolation)
4. `src/test/java/provguard/sensors/ProcessExecutionSensorTest.java` — proves it actually
   intercepts a real JDK call

**Run it for real:**
```
mvn clean package
java -Dnet.bytebuddy.experimental=true -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.cli.DemoMain
```

**Why it matters:** `premain` (static attach, before `main()` runs) and `agentmain`
(dynamic attach to an already-running JVM) are the two ways the JVM allows external code
to instrument a running application without modifying its source — the foundation
everything else in this project depends on.
