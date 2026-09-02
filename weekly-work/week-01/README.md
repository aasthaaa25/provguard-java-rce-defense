# Week 1

Objective: prove the hardest technical risk works before building anything else on top
of it — that ProvGuard's core mechanism (weaving into a bootstrap-loaded JDK class and
calling back into agent code) actually functions on this machine's JDK.

See `TASKS.md`, `NOTES.md`, `TEST_RESULTS.md` in this folder for the details. Summary:
delivered a working `ProcessBuilder#start()` sensor, end to end, with real `StackWalker`
provenance capture — found and fixed three genuine classloader bugs along the way (full
writeups in `docs/DESIGN_DECISIONS.md` §4A-4C).
