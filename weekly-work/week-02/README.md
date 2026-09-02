# Week 2

Objective: turn the single-sink proof-of-concept into a real, working prevention
pipeline — graph, detection, enforcement — and add a second sink to stress-test the
design. Also: find and fix whatever breaks when a second sink and more tests are added
(a lot did), and add a first honest step toward the ML side.

See `TASKS.md`, `NOTES.md`, `TEST_RESULTS.md`. Summary: real `ALLOW`/`LOG`/`BLOCK`
enforcement that genuinely prevents a sink from executing, a second working sink
(deserialization), a third sink attempted but not working (JNDI — documented, not
hidden), a real (if simple) statistical detection baseline trained on genuinely captured
data, two working local vulnerable fixtures, and a serious classloader-ordering bug found
and permanently fixed.
