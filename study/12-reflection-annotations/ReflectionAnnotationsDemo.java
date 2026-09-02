import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * TOPIC: custom annotations + reflection.
 *
 * WHY IT MATTERS: ProvGuard's planned SinkRegistry can use a @Sink annotation
 * exactly like this to declaratively mark which methods are sensors, then
 * discover them via reflection at agent startup instead of hand-maintaining
 * a registration list.
 */
public class ReflectionAnnotationsDemo {

    // Custom annotation: RUNTIME retention so it's visible to reflection,
    // METHOD target since it marks sensor methods specifically.
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Sink {
        String value(); // the sink's display name
    }

    static class DemoSensors {
        @Sink("PROCESS_EXECUTION")
        void onProcessBuilderStart() {
        }

        @Sink("JNDI_LOOKUP")
        void onJndiLookup() {
        }

        void notASink() {
        }
    }

    public static void main(String[] args) {
        List<String> discovered = new ArrayList<>();

        // Reflection: scan a class's methods for our annotation, exactly the
        // pattern a real SinkRegistry would use at agent startup.
        for (Method method : DemoSensors.class.getDeclaredMethods()) {
            Sink sink = method.getAnnotation(Sink.class);
            if (sink != null) {
                discovered.add(sink.value() + " -> " + method.getName());
            }
        }

        discovered.forEach(System.out::println);
        System.out.println("discovered " + discovered.size() + " sink method(s) via reflection");
    }
}
