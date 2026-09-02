import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.Map;

/**
 * TOPIC: Strategy, Factory, Builder, Observer design patterns - each
 * included only because ProvGuard's architecture genuinely benefits from it
 * (see docs/DESIGN_DECISIONS.md - patterns are not forced in where they
 * don't belong).
 */
public class DesignPatternsDemo {

    // --- Strategy: interchangeable detection algorithms behind one interface. ---
    interface AnomalyStrategy {
        double score(int featureValue);
    }

    static final class FixedThresholdStrategy implements AnomalyStrategy {
        public double score(int featureValue) {
            return featureValue > 10 ? 1.0 : 0.0;
        }
    }

    static final class LinearStrategy implements AnomalyStrategy {
        public double score(int featureValue) {
            return featureValue / 20.0;
        }
    }

    // --- Factory: centralizes "which sensor for which sink type" decisions. ---
    enum SinkKind { PROCESS, JNDI }

    interface SensorFactory {
        String create(SinkKind kind);
    }

    static final class DefaultSensorFactory implements SensorFactory {
        public String create(SinkKind kind) {
            return switch (kind) {
                case PROCESS -> "ProcessExecutionSensor";
                case JNDI -> "JndiSensor (not yet implemented)";
            };
        }
    }

    // --- Builder: readable construction of a config object with many optional fields. ---
    static final class PolicyConfig {
        final boolean blockingEnabled;
        final double threshold;
        final String modelVersion;

        private PolicyConfig(Builder b) {
            this.blockingEnabled = b.blockingEnabled;
            this.threshold = b.threshold;
            this.modelVersion = b.modelVersion;
        }

        @Override
        public String toString() {
            return "PolicyConfig{blocking=%s, threshold=%s, model=%s}".formatted(blockingEnabled, threshold, modelVersion);
        }

        static final class Builder {
            private boolean blockingEnabled = false;
            private double threshold = 0.8;
            private String modelVersion = "v0";

            Builder blockingEnabled(boolean v) { this.blockingEnabled = v; return this; }
            Builder threshold(double v) { this.threshold = v; return this; }
            Builder modelVersion(String v) { this.modelVersion = v; return this; }
            PolicyConfig build() { return new PolicyConfig(this); }
        }
    }

    // --- Observer: event listeners notified when a sink fires. ---
    interface SinkListener {
        void onSinkHit(String sinkName);
    }

    static final class SinkEventBus {
        private final List<SinkListener> listeners = new ArrayList<>();

        void subscribe(SinkListener listener) {
            listeners.add(listener);
        }

        void publish(String sinkName) {
            for (SinkListener l : listeners) {
                l.onSinkHit(sinkName);
            }
        }
    }

    public static void main(String[] args) {
        // Strategy in action:
        Map<String, AnomalyStrategy> strategies = Map.of(
                "fixed", new FixedThresholdStrategy(),
                "linear", new LinearStrategy()
        );
        strategies.forEach((name, strategy) -> System.out.println(name + " -> " + strategy.score(15)));

        // Factory in action:
        SensorFactory factory = new DefaultSensorFactory();
        System.out.println(factory.create(SinkKind.PROCESS));

        // Builder in action:
        PolicyConfig config = new PolicyConfig.Builder()
                .blockingEnabled(true)
                .threshold(0.9)
                .modelVersion("v1-autoencoder")
                .build();
        System.out.println(config);

        // Observer in action:
        SinkEventBus bus = new SinkEventBus();
        bus.subscribe(sink -> System.out.println("[logger] sink hit: " + sink));
        bus.subscribe(sink -> System.out.println("[metrics] incrementing counter for: " + sink));
        bus.publish("ProcessBuilder.start()");
    }
}
