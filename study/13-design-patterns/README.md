# 13 — Design Patterns: Strategy, Factory, Builder, Observer

**Concept:** four patterns, each shown only in a form that maps to a real, justified
ProvGuard need — not forced in for their own sake.

**Example:** `DesignPatternsDemo.java`:
- **Strategy** — interchangeable `AnomalyStrategy` implementations behind one interface
  (maps to `AnomalyDetector`/`OneClassDetector`/`AutoencoderDetector`)
- **Factory** — `SensorFactory` centralizes "which sensor class for which sink type"
- **Builder** — `PolicyConfig.Builder` for readable construction of a many-optional-field
  config object (maps to enforcement thresholds/config)
- **Observer** — `SinkEventBus` notifies multiple listeners (logger, metrics) when a sink
  fires, without the sensor knowing who's listening

**Why each one is here:** see `docs/DESIGN_DECISIONS.md` — the project's explicit rule
is patterns must map to a real justified need, not be inserted to "cover more concepts".

Run: `java DesignPatternsDemo.java`
