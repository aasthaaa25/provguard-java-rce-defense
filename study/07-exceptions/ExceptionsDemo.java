import java.io.Closeable;
import java.io.IOException;

/**
 * TOPIC: checked vs. unchecked exceptions, custom exceptions, exception
 * chaining, try-with-resources.
 *
 * WHY IT MATTERS: ProvGuard's planned enforcement layer needs a custom
 * unchecked exception (e.g. SinkBlockedException) to distinguish "blocked by
 * policy, on purpose" from "something is actually broken".
 */
public class ExceptionsDemo {

    // Custom UNCHECKED exception: callers are not forced to catch it, matching
    // how a security policy decision should propagate (it's exceptional, but
    // callers who don't care about it shouldn't be forced to handle it).
    static final class SinkBlockedException extends RuntimeException {
        SinkBlockedException(String sinkName, Throwable cause) {
            super("blocked dangerous sink: " + sinkName, cause); // exception chaining
        }
    }

    // Custom CHECKED exception: forces callers to explicitly decide how to
    // handle a recoverable failure.
    static final class PolicyLoadException extends Exception {
        PolicyLoadException(String message) {
            super(message);
        }
    }

    static void loadPolicy(boolean corrupt) throws PolicyLoadException {
        if (corrupt) {
            throw new PolicyLoadException("policy file is corrupt");
        }
    }

    // AutoCloseable resource, used to demonstrate try-with-resources.
    static final class FakeHandle implements Closeable {
        @Override
        public void close() {
            System.out.println("FakeHandle closed");
        }
    }

    public static void main(String[] args) {
        // try-with-resources: FakeHandle.close() is guaranteed to run.
        try (FakeHandle handle = new FakeHandle()) {
            System.out.println("using handle: " + handle);
        }

        try {
            loadPolicy(true);
        } catch (PolicyLoadException e) {
            System.out.println("caught checked exception: " + e.getMessage());
        }

        try {
            try {
                throw new IllegalStateException("underlying JDK failure");
            } catch (IllegalStateException cause) {
                // Exception chaining: wrap the low-level cause in a domain-specific one.
                throw new SinkBlockedException("ProcessBuilder.start()", cause);
            }
        } catch (SinkBlockedException e) {
            System.out.println("caught: " + e.getMessage() + " caused by: " + e.getCause());
        }
    }
}
