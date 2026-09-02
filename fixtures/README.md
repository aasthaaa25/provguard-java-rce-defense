# Fixtures

Minimal, intentionally vulnerable LOCAL-ONLY programs used to exercise ProvGuard against
real (if harmless) vulnerable call patterns. These never contact any external system,
never run anything destructive, and are not real exploit chains (no `ysoserial` or
similar payload generator is used or bundled) — they exist solely so ProvGuard has
something genuine to intercept.

Both are single-file Java programs (no build step — `java Foo.java` runs them directly)
and are **not** on ProvGuard's trusted allowlist (`provguard.enforcement.Policy`), so
running them with the agent attached in blocking mode demonstrates a real block.

## vulnerable-command-execution/VulnerableCommandRunner.java

Passes a caller-supplied string directly into `ProcessBuilder` — a stand-in for OS
command injection.

```
cd fixtures/vulnerable-command-execution

# Unprotected — runs the command normally:
java VulnerableCommandRunner.java

# Protected — genuinely blocked (verified output below):
java -Dnet.bytebuddy.experimental=true -javaagent:../../target/provguard-agent.jar VulnerableCommandRunner.java
```

Verified actual output when run with the agent attached:
```
Running (unsafely constructed) command: echo vulnerable-fixture-ran
[ProvGuard][BLOCK] PROCESS_EXECUTION via VulnerableCommandRunner -- caller VulnerableCommandRunner is NOT on the trusted allowlist
Exception in thread "main" provguard.enforcement.SinkBlockedException: PROCESS_EXECUTION via VulnerableCommandRunner: caller VulnerableCommandRunner is NOT on the trusted allowlist
	at provguard.enforcement.EnforcementEngine.enforce(EnforcementEngine.java:24)
	at provguard.sensors.SensorPipeline.captureAndEnforce(SensorPipeline.java:38)
	at java.base/java.lang.ProcessBuilder.start(ProcessBuilder.java:1046)
	at VulnerableCommandRunner.main(VulnerableCommandRunner.java:24)
```
The `echo` command genuinely never ran — the stack trace shows the block happening
inside `ProcessBuilder.start()` itself, before its real body executes.

## vulnerable-deserialization/VulnerableDeserializer.java

Deserializes a byte array directly via `ObjectInputStream` with no validation — a
stand-in for a Java deserialization gadget chain (CWE-502).

```
cd fixtures/vulnerable-deserialization
java -Dnet.bytebuddy.experimental=true -javaagent:../../target/provguard-agent.jar VulnerableDeserializer.java
```

Verified actual output when run with the agent attached (the deserialization genuinely
never completes — the block happens inside `ObjectInputStream.resolveClass()`, visible
in the stack trace):
```
Deserializing untrusted-looking payload (134 bytes)...
[ProvGuard][BLOCK] DESERIALIZATION via VulnerableDeserializer -- caller VulnerableDeserializer is NOT on the trusted allowlist
Exception in thread "main" provguard.enforcement.SinkBlockedException: ...
	at java.base/java.io.ObjectInputStream.resolveClass(ObjectInputStream.java:743)
	...
	at VulnerableDeserializer.main(VulnerableDeserializer.java:23)
```

## Not included

`vulnerable-jndi/` and `vulnerable-script-evaluation/` fixtures from the original plan
are not built: the JNDI sensor doesn't actually intercept calls yet (a real, documented,
unresolved bug — see `docs/DESIGN_DECISIONS.md` §6), and there is no script sensor at all
(no `ScriptEngine` implementation on this JDK to test against). A fixture for either
would have nothing genuine to demonstrate right now.
