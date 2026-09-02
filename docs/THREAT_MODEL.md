# Threat Model

## In scope today (what's actually built and tested)

ProvGuard currently detects and can enforce against exactly two call patterns:

1. Any code path that invokes `new ProcessBuilder(...).start()` (process execution / OS
   command injection)
2. Any code path that deserializes an object via `ObjectInputStream` in a way that
   reaches `resolveClass()` (Java deserialization gadget chains, CWE-502)

Detection is by **caller identity**, not by structural/behavioral analysis: `AllowlistDetector`
walks the captured call stack past any frames belonging to the sink's own class and checks
whether the first genuinely different class is on a small, explicit allowlist. It fails
closed — an unknown or unexpected caller is treated as anomalous.

## Out of scope today (honestly, not silently)

- **JNDI injection** (Log4Shell-style, CVE-2021-44228): sensor code exists
  (`provguard.sensors.JndiAdvice`) but does not actually intercept calls at runtime — a
  real, unresolved bug (`docs/DESIGN_DECISIONS.md` §6). Treat JNDI lookups as
  **completely unprotected** by this project as it stands.
- **Script/expression-language injection** (SpEL, OGNL, etc.): not attempted. This JDK
  ships no bundled `ScriptEngine` implementation to hook and genuinely test against.
- **Structural/behavioral anomaly detection**: there is no learned model. A malicious
  call that originates from an *already-allowlisted* class (e.g. because that code was
  itself compromised, or because a gadget chain happens to route through a call site
  that looks like a trusted caller) would not be caught. This is precisely the gap a
  future graph-structure-aware learned model is meant to close — see
  `docs/ARCHITECTURE.md`.
- **Evasion resistance**: no adversarial/evasion testing has been done (reflection-invoked
  sinks, child-JVM bypass, obfuscated call chains — none of these have been tried against
  this implementation). Assume a motivated attacker who knows this tool is present could
  likely evade it.
- **Any sink not explicitly listed above.** ProvGuard hooks exactly the methods named in
  this document — it does not provide general-purpose RCE protection.

## Assumptions

- Runs with `-javaagent` (or dynamic attach) and the privileges that implies — a
  compromised JVM that can disable/detach the agent, or that runs code before the agent
  attaches, is not defended against.
- The allowlist in `provguard.enforcement.Policy` is trusted, hand-maintained
  configuration — anyone who can modify it can disable enforcement for any caller they add.
- No claim of protection against zero-day sinks not yet hooked, or against attacks that
  don't route through the three hooked methods at all.

## Honest summary

This is a real, working prevention mechanism for two specific, well-understood RCE sink
categories, using a simple and explainable (not learned) detection strategy. It is not,
and does not claim to be, comprehensive RCE protection. See `README.md` "Current status"
for the itemized build state.
