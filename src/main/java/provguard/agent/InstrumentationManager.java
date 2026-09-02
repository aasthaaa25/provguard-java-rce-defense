package provguard.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import provguard.sensors.ProcessExecutionAdvice;

import java.lang.instrument.Instrumentation;

/**
 * Installs ProvGuard's ByteBuddy-based sink hooks onto a live {@link Instrumentation}
 * instance. Called from both real agent attach paths ({@link AgentBootstrap#premain}
 * / {@link AgentBootstrap#agentmain}) and from tests (via
 * {@code net.bytebuddy.agent.ByteBuddyAgent.install()}), so it must work whether
 * the target class ({@code java.lang.ProcessBuilder}) is already loaded (dynamic
 * attach / test case) or not yet loaded (premain, before the app's main() runs) —
 * {@link AgentBuilder.RedefinitionStrategy#RETRANSFORMATION} handles both.
 *
 * Only one sink is wired up in this slice: {@code ProcessBuilder.start()}
 * (process execution — one of the four sink categories in the ProvGuard threat
 * model). Deserialization, JNDI lookup, and script evaluation sensors are not
 * yet implemented; see docs/ARCHITECTURE.md and docs/WEEKLY_ROADMAP.md for the
 * planned order.
 */
public final class InstrumentationManager {

    private static volatile boolean installed = false;

    private InstrumentationManager() {
    }

    public static synchronized void install(Instrumentation instrumentation) {
        if (installed) {
            return;
        }

        // Resolve the Advice visitor BEFORE mutating the bootstrap classloader search
        // path below. Advice.to(...) re-analyzes the advice class's methods/annotations
        // each time it is called; if that resolution instead happened lazily inside the
        // transform callback (i.e. after appendToBootstrapClassLoaderSearch has already
        // run), it intermittently fails to find the @Advice.OnMethodEnter method.
        net.bytebuddy.asm.AsmVisitorWrapper.ForDeclaredMethods adviceVisitor = Advice
                .to(ProcessExecutionAdvice.class)
                .on(ElementMatchers.named("start")
                        .and(ElementMatchers.takesArguments(0))
                        .and(ElementMatchers.isPublic()));

        // Use InstrumentationManager itself (not EventBuffer) as the location marker.
        // EventBuffer must remain UNLOADED until after this call: if anything forced it
        // to load via the application classloader first, the bootstrap classloader would
        // later load its OWN separate copy when the woven ProcessBuilder code references
        // it - two distinct classes with two distinct EventBuffer.INSTANCE statics, so the
        // sensor and the test/demo consumer would silently talk to different buffers.
        BootstrapInjector.ensureVisible(instrumentation, InstrumentationManager.class);

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())
                .with(AgentBuilder.InstallationListener.StreamWriting.toSystemOut())
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.is(ProcessBuilder.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .visit(adviceVisitor))
                .installOn(instrumentation);

        installed = true;
    }
}
