import java.util.Objects;

/**
 * TOPIC: classes, objects, constructors, fields, static, final, access
 * modifiers, packages, enums, interfaces, abstract classes, encapsulation.
 *
 * WHY IT MATTERS: this is the vocabulary everything else in Java is built
 * from. Getting encapsulation and immutability right here is what makes
 * ProvGuard's ProvenanceEvent/SinkType (in src/main) safe to pass between
 * threads without defensive copying everywhere.
 *
 * WHERE PROVGUARD USES IT: SinkType (enum) and every class in
 * provguard.provenance use exactly this pattern - private final fields,
 * a small public API, no setters.
 */
public class Basics {

    interface Shape {
        double area();
    }

    abstract static class AbstractShape implements Shape {
        // template-ish helper shared by subclasses
        String describe() {
            return getClass().getSimpleName() + " area=" + area();
        }
    }

    static final class Circle extends AbstractShape {
        private final double radius; // encapsulated, immutable

        Circle(double radius) {
            if (radius <= 0) {
                throw new IllegalArgumentException("radius must be positive");
            }
            this.radius = radius;
        }

        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    static final class Rectangle extends AbstractShape {
        private final double width;
        private final double height;

        Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public double area() {
            return width * height;
        }
    }

    enum Access { PUBLIC, PACKAGE_PRIVATE, PROTECTED, PRIVATE }

    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Rectangle(3, 4) };
        for (Shape s : shapes) {
            System.out.println(((AbstractShape) s).describe());
        }

        for (Access a : Access.values()) {
            System.out.println("visibility level: " + a);
        }

        System.out.println("Objects.equals demo: " + Objects.equals(new Circle(1).area(), new Circle(1).area()));
    }
}
