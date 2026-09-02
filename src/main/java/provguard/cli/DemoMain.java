package provguard.cli;

import provguard.provenance.EventBuffer;

/**
 * Command-line demo. Run with the agent attached to see interception happen live:
 *
 * <pre>
 *   mvn package
 *   java -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.cli.DemoMain
 * </pre>
 *
 * This intentionally triggers a real {@code ProcessBuilder.start()} call — the
 * same JDK API used in real command-injection RCE — so the sensor has something
 * genuine to intercept. It runs a harmless local `echo` command only.
 */
public final class DemoMain {

    private DemoMain() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ProvGuard hour-1 demo ===");
        System.out.println("Triggering ProcessBuilder.start() — a real RCE-relevant sink...");
        System.out.println();

        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo hello-from-provguard-demo");
        Process process = pb.start();
        process.waitFor();

        System.out.println();
        System.out.println("Captured " + EventBuffer.INSTANCE.size() + " provenance event(s):");
        EventBuffer.INSTANCE.getAll().forEach(e -> System.out.println("  " + e.summary()));

        if (EventBuffer.INSTANCE.size() == 0) {
            System.out.println();
            System.out.println("NOTE: 0 events captured. Did you run this WITH -javaagent:target/provguard-agent.jar ?");
        }
    }
}
