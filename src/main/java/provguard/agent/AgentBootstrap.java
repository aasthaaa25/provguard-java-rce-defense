package provguard.agent;

import java.lang.instrument.Instrumentation;

/**
 * Entry point required by {@code java.lang.instrument}. Two attach modes are
 * supported:
 * <ul>
 *   <li>{@link #premain} — static attach, when the JVM is launched with
 *       {@code -javaagent:provguard-agent.jar}, before the target app's
 *       {@code main()} runs.</li>
 *   <li>{@link #agentmain} — dynamic attach to an already-running JVM via the
 *       Attach API ({@code com.sun.tools.attach.VirtualMachine}).</li>
 * </ul>
 * Both simply hand off to {@link InstrumentationManager}, which is idempotent.
 */
public final class AgentBootstrap {

    private AgentBootstrap() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[ProvGuard] agent attached via premain (static attach at JVM startup)");
        InstrumentationManager.install(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[ProvGuard] agent attached via agentmain (dynamic attach to a running JVM)");
        InstrumentationManager.install(instrumentation);
    }
}
