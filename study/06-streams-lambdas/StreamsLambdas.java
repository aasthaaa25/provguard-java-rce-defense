import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TOPIC: lambda expressions, functional interfaces (Predicate/Function/
 * Consumer/Supplier), method references, the Stream API, Collectors, Optional.
 *
 * WHY IT MATTERS: provguard.provenance.StackWalkerCollector already uses a
 * Stream (frames.limit(...).map(...).collect(...)) - this is that pattern in
 * more depth.
 */
public class StreamsLambdas {

    record Frame(String className, String methodName, boolean isSink) {}

    public static void main(String[] args) {
        List<Frame> stack = List.of(
                new Frame("DemoMain", "main", false),
                new Frame("ProcessBuilder", "start", true),
                new Frame("StackWalkerCollector", "captureCallStack", false)
        );

        // Predicate: a reusable boolean test.
        Predicate<Frame> isSink = Frame::isSink;

        // Function: transforms one type into another.
        Function<Frame, String> toLabel = f -> f.className() + "#" + f.methodName();

        // Consumer: does something with a value, returns nothing.
        Consumer<String> printer = System.out::println;

        // Supplier: produces a value with no input.
        Supplier<String> fallbackLabel = () -> "<no sink frame>";

        List<String> sinkLabels = stack.stream()
                .filter(isSink)
                .map(toLabel)
                .collect(Collectors.toList());
        sinkLabels.forEach(printer);

        // Optional: avoids returning null when there might be no match.
        Optional<Frame> firstSink = stack.stream().filter(isSink).findFirst();
        String result = firstSink.map(toLabel).orElseGet(fallbackLabel);
        System.out.println("first sink frame: " + result);

        // A fuller stream pipeline: group frame names by whether they are a sink.
        var grouped = stack.stream()
                .collect(Collectors.groupingBy(Frame::isSink, Collectors.mapping(Frame::methodName, Collectors.toList())));
        System.out.println("grouped: " + grouped);

        long sinkCount = stack.stream().filter(isSink).count();
        System.out.println("sink frame count: " + sinkCount);
    }
}
