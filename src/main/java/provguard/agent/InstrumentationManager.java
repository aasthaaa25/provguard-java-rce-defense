package provguard.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.matcher.ElementMatchers;
import provguard.sensors.DeserializationAdvice;
import provguard.sensors.JndiAdvice;
import provguard.sensors.ProcessExecutionAdvice;

import javax.naming.InitialContext;
import java.io.ObjectInputStream;
import java.lang.instrument.Instrumentation;

/**
 * Installs ProvGuard's ByteBuddy-based sink hooks onto a live {@link Instrumentation}
 * instance. Called from both real agent attach paths ({@link AgentBootstrap#premain}
 * / {@link AgentBootstrap#agentmain}) and from tests (via
 * {@code net.bytebuddy.agent.ByteBuddyAgent.install()}), so it must work whether
 * each target class is already loaded (dynamic attach / test case) or not yet
 * loaded (premain, before the app's main() runs) —
 * {@link AgentBuilder.RedefinitionStrategy#RETRANSFORMATION} handles both.
 *
 * Three sinks are wired up: {@code ProcessBuilder.start()} (process execution),
 * {@code ObjectInputStream.resolveClass()} (deserialization), and
 * {@code InitialContext.lookup(String)} (JNDI lookup). Script evaluation
 * (the fourth category in the ProvGuard threat model) is NOT implemented:
 * this JDK ships no bundled {@code ScriptEngine} implementation (Nashorn was
 * removed after JDK 14), so there is nothing to genuinely hook and test on
 * this machine without adding an external scripting library — see
 * docs/DESIGN_DECISIONS.md.
 *
 * Each sink is installed via its OWN independent {@code AgentBuilder}
 * instance rather than chaining multiple {@code .type().transform()} pairs
 * onto one builder: chaining was tried first and, for reasons not fully
 * root-caused, silently caused earlier rules (ProcessBuilder, ObjectInputStream)
 * in the chain to never fire while only the last rule installed correctly —
 * see docs/DESIGN_DECISIONS.md section 4D. Three independent installs sidesteps
 * the issue entirely and is arguably simpler to reason about besides.
 */
public final class InstrumentationManager {

    private static volatile boolean installed = false;

    private InstrumentationManager() {
    }

    public static synchronized void install(Instrumentation instrumentation) {
        if (installed) {
            return;
        }

        // Resolve every Advice visitor BEFORE mutating the bootstrap classloader
        // search path below - see docs/DESIGN_DECISIONS.md section 4A for why
        // computing these lazily inside a transform callback intermittently
        // fails with "No advice defined by class ...".
        AsmVisitorWrapper.ForDeclaredMethods processAdvice = Advice.to(ProcessExecutionAdvice.class)
                .on(ElementMatchers.named("start")
                        .and(ElementMatchers.takesArguments(0))
                        .and(ElementMatchers.isPublic()));

        AsmVisitorWrapper.ForDeclaredMethods deserializationAdvice = Advice.to(DeserializationAdvice.class)
                .on(ElementMatchers.named("resolveClass")
                        .and(ElementMatchers.takesArguments(1))
                        .and(ElementMatchers.isProtected()));

        AsmVisitorWrapper.ForDeclaredMethods jndiAdvice = Advice.to(JndiAdvice.class)
                .on(ElementMatchers.named("lookup")
                        .and(ElementMatchers.takesArguments(1))
                        .and(ElementMatchers.takesArgument(0, String.class))
                        .and(ElementMatchers.isPublic()));

        // Use InstrumentationManager itself (not any provguard.provenance/
        // graph/detection/enforcement class) as the location marker - see
        // docs/DESIGN_DECISIONS.md section 4B for why the marker class must
        // never be one that's shared/consumed across the classloader boundary.
        BootstrapInjector.ensureVisible(instrumentation, InstrumentationManager.class);

        installOne(instrumentation, ProcessBuilder.class, processAdvice);
        installOne(instrumentation, ObjectInputStream.class, deserializationAdvice);
        installOne(instrumentation, InitialContext.class, jndiAdvice);

        installed = true;
    }

    private static void installOne(Instrumentation instrumentation, Class<?> targetType, AsmVisitorWrapper.ForDeclaredMethods advice) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.is(targetType))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder.visit(advice))
                .installOn(instrumentation);
    }
}
