import java.util.ArrayList;
import java.util.List;

/**
 * TOPIC: generic classes, generic methods, bounded type parameters, wildcards.
 *
 * WHY IT MATTERS: ProvGuard's planned ProvenanceGraph is generic over node type
 * so it can be reused/tested without depending on the full sensor pipeline.
 */
public class Generics {

    // Generic class with a bounded type parameter: T must be Comparable.
    static class Ranked<T extends Comparable<T>> {
        private final List<T> items = new ArrayList<>();

        void add(T item) {
            items.add(item);
        }

        T max() {
            T best = items.get(0);
            for (T item : items) {
                if (item.compareTo(best) > 0) {
                    best = item;
                }
            }
            return best;
        }
    }

    // Generic method, independent of any class type parameter.
    static <T> T firstOrDefault(List<T> list, T fallback) {
        return list.isEmpty() ? fallback : list.get(0);
    }

    // Wildcard: accepts a list of anything that IS-A Number, read-only.
    static double sumOf(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) {
            total += n.doubleValue();
        }
        return total;
    }

    public static void main(String[] args) {
        Ranked<Integer> ranked = new Ranked<>();
        ranked.add(3);
        ranked.add(7);
        ranked.add(1);
        System.out.println("max: " + ranked.max());

        System.out.println(firstOrDefault(List.of("a", "b"), "none"));
        System.out.println(firstOrDefault(List.<String>of(), "none"));

        System.out.println("sum: " + sumOf(List.of(1, 2, 3.5)));
    }
}
