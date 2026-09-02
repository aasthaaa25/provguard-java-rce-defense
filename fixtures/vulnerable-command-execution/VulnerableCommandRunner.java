import java.io.IOException;

/**
 * Deliberately vulnerable LOCAL fixture: passes a caller-supplied string
 * directly into ProcessBuilder with no validation - a stand-in for a classic
 * OS command injection sink. Exists ONLY to exercise ProvGuard locally
 * against a real (if harmless) vulnerable pattern; it never contacts any
 * external system and never runs anything destructive.
 *
 * This class is NOT on ProvGuard's trusted allowlist
 * (provguard.enforcement.Policy), so running it WITH the agent attached in
 * blocking mode demonstrates a real block of a genuinely vulnerable call
 * pattern - not a simulation. Run it directly (no agent) to see the
 * unprotected behavior for comparison.
 *
 * Usage:
 *   java VulnerableCommandRunner.java
 *   java -Dnet.bytebuddy.experimental=true -javaagent:../../target/provguard-agent.jar VulnerableCommandRunner.java
 */
public class VulnerableCommandRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        String command = args.length > 0 ? args[0] : "echo vulnerable-fixture-ran";
        System.out.println("Running (unsafely constructed) command: " + command);
        new ProcessBuilder("cmd", "/c", command).start().waitFor();
        System.out.println("(if you see this, the call was NOT blocked)");
    }
}
