# Security

## Reporting

This is a personal/academic research project, not a maintained production tool. If you
find a security issue in ProvGuard itself (as opposed to in the JDK/libraries it depends
on), open an issue in this private repository.

## What this project does and does not claim

See `docs/THREAT_MODEL.md` for the full, itemized in-scope/out-of-scope breakdown. In
short: ProvGuard genuinely intercepts and can block two real RCE-relevant JDK sinks
(`ProcessBuilder#start()`, `ObjectInputStream#resolveClass()`) using a simple allowlist
baseline. It does **not** claim complete RCE prevention, zero-day protection, or
resistance to a motivated attacker who knows the tool is present and has not been
evasion-tested against it.

## Ethics and scope of testing

All testing in this repository is against **local, self-contained code only** —
`ProcessBuilder` calls that run harmless local commands (`echo`), and deserialization of
harmless in-memory objects created by the test itself. No external systems are targeted,
no destructive payloads exist anywhere in this repository, and no real exploit chains
(e.g. from `ysoserial`) have been integrated or attempted.

## Secrets

No credentials, API keys, tokens, or other secrets are present in this repository.
`.gitignore` excludes common secret-bearing file patterns (`.env`, `*.pem`, `*.jks`,
`*.keystore`) as a standing precaution, not because any currently exist.

## Known limitations that are security-relevant

- The JNDI sensor does not work (`docs/DESIGN_DECISIONS.md` §6) — JNDI lookups are
  currently unprotected by this tool, full stop.
- No adversarial/evasion testing has been performed against the working sensors.
- `provguard.enforcement.Policy`'s allowlist is a static, hardcoded `Set` in this
  prototype — not yet externally configurable, and not itself protected against
  tampering by anything that can modify the running JVM's classes.
