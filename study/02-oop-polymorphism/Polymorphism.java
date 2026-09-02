import java.util.List;

/**
 * TOPIC: inheritance, polymorphism, method overriding, method overloading,
 * composition over inheritance.
 *
 * WHY IT MATTERS: ProvGuard's future AnomalyDetector implementations
 * (OneClassDetector, AutoencoderDetector, GnnDetector) will all be called
 * through one polymorphic interface - the caller never needs an if/else on
 * concrete type.
 */
public class Polymorphism {

    interface Sensor {
        String sinkName();
    }

    // Composition over inheritance: Detector HAS-A Sensor, doesn't extend it.
    static class Detector {
        private final Sensor sensor; // composed, not inherited

        Detector(Sensor sensor) {
            this.sensor = sensor;
        }

        String report() {
            return "watching sink: " + sensor.sinkName();
        }
    }

    static class ProcessSensor implements Sensor {
        @Override
        public String sinkName() {
            return "ProcessBuilder.start()";
        }
    }

    static class DeserializationSensor implements Sensor {
        @Override
        public String sinkName() {
            return "ObjectInputStream.resolveClass()";
        }
    }

    // Overloading: same method name, different parameter lists.
    static String describe(int x) {
        return "int: " + x;
    }

    static String describe(String x) {
        return "String: " + x;
    }

    static String describe(int x, int y) {
        return "pair: " + x + "," + y;
    }

    public static void main(String[] args) {
        List<Sensor> sensors = List.of(new ProcessSensor(), new DeserializationSensor());
        for (Sensor s : sensors) {
            // Polymorphism: sinkName() dispatches to the right override at runtime.
            System.out.println(new Detector(s).report());
        }

        System.out.println(describe(1));
        System.out.println(describe("x"));
        System.out.println(describe(1, 2));
    }
}
