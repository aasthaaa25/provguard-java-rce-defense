# Contributing

This is currently a solo college research project, not yet accepting external
contributions — but the conventions below are how the repository is kept honest and
consistent, and apply to any future contributor (including future-you).

## Ground rules

1. **Never claim something works unless you actually ran it.** Every "Done"/✅ in this
   repo's docs corresponds to a real test run or a real command someone actually
   executed — see `docs/DESIGN_DECISIONS.md` for what that looks like in practice
   (including documenting bugs that were found, not just fixes that landed clean).
2. **No placeholder/stub classes pretending to work.** If a component isn't built yet,
   say so in `README.md`'s status table and `docs/WEEKLY_ROADMAP.md` — don't ship an
   empty class with a misleading name.
3. **Build must pass from a clean checkout.** `mvn clean test` before every commit that
   claims a milestone is done.
4. **Meaningful commits, not one giant commit.** Conventional-commit-style prefixes
   (`feat(agent): ...`, `test(graph): ...`, `docs(java): ...`) — see `git log` for
   examples already in this repo.
5. **Document bugs, don't hide them.** `docs/DESIGN_DECISIONS.md` exists specifically to
   record real problems hit and how (or whether) they were resolved — a documented open
   bug is more valuable to the next person than a silently deleted failing test.

## Setup

See `README.md` "Requirements" and `docs/TROUBLESHOOTING.md` for this machine's specific
setup quirks (Maven not on `PATH` by default, a TLS-intercepting antivirus breaking
Maven Central access, and building against a non-LTS JDK).

## Where things live

See `README.md` "Project layout" and `docs/ARCHITECTURE.md`.
