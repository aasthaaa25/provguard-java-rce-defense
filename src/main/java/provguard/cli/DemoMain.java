package provguard.cli;

import provguard.provenance.EventBuffer;

import java.io.*;

/**
 * Command-line demo. Run with the agent attached to see interception happen live:
 *
 * <pre>
 *   mvn package
 *   java -Dnet.bytebuddy.experimental=true -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.cli.DemoMain
 * </pre>
 *
 * Demonstrates the full pipeline for two real sinks: an ALLOWED call (from
 * this trusted class), a BLOCKED call (from an untrusted caller), and a real
 * deserialization interception. All calls are genuine JDK API invocations,
 * not simulated - the only thing "fake" here is that the untrusted class
 * exists purely to demonstrate detection, not to actually attack anything.
 */
public final class DemoMain {

    private DemoMain() {
    }

    public static void main(String[] args) throws Exception {
        section("1. ALLOWED process execution (trusted caller: DemoMain)");
        new ProcessBuilder("cmd", "/c", "echo hello-from-provguard-demo").start().waitFor();
        printCapturedEvents();

        section("2. BLOCKED process execution (untrusted caller)");
        try {
            new UntrustedInvoker().runProcess();
            System.out.println("(unexpected: this should have been blocked)");
        } catch (SecurityException blocked) {
            System.out.println("Blocked as expected: " + blocked.getMessage());
        }
        printCapturedEvents();

        section("3. ALLOWED deserialization (trusted caller: DemoMain)");
        byte[] payload = serialize(new Payload("demo-payload"));
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            Payload result = (Payload) in.readObject();
            System.out.println("deserialized: " + result.value);
        }
        printCapturedEvents();

        section("4. BLOCKED deserialization (untrusted caller)");
        try {
            new UntrustedInvoker().runDeserialization(payload);
            System.out.println("(unexpected: this should have been blocked)");
        } catch (SecurityException blocked) {
            System.out.println("Blocked as expected: " + blocked.getMessage());
        }
        printCapturedEvents();

        System.out.println();
        System.out.println("Total provenance events captured this run: " + EventBuffer.INSTANCE.size());
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void printCapturedEvents() {
        int count = EventBuffer.INSTANCE.size();
        System.out.println("(captured " + count + " provenance event(s) so far)");
    }

    private static byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            out.flush();
            return bytes.toByteArray();
        }
    }

    static final class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        final String value;

        Payload(String value) {
            this.value = value;
        }
    }

    /** Deliberately absent from Policy's trusted allowlist. */
    static final class UntrustedInvoker {
        void runProcess() throws IOException {
            new ProcessBuilder("cmd", "/c", "echo should-be-blocked").start();
        }

        void runDeserialization(byte[] payload) throws IOException, ClassNotFoundException {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
                in.readObject();
            }
        }
    }
}
