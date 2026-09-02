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
import java.util.Set;

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

        // Referenced here, AFTER the bootstrap append above, so that standard
        // parent-first delegation resolves this class via the bootstrap
        // classloader (which can now find it) rather than the application
        // classloader - giving us a live reference to the actual Module that
        // provguard.* classes end up in once bootstrap-injected. See the
        // module-read grant below and docs/DESIGN_DECISIONS.md section 6.
        Module provguardBootstrapModule = provguard.provenance.SinkType.class.getModule();

        grantModuleRead(instrumentation, ProcessBuilder.class.getModule(), provguardBootstrapModule);
        grantModuleRead(instrumentation, ObjectInputStream.class.getModule(), provguardBootstrapModule);
        grantModuleRead(instrumentation, InitialContext.class.getModule(), provguardBootstrapModule);

        installOne(instrumentation, ProcessBuilder.class, processAdvice);
        installOne(instrumentation, ObjectInputStream.class, deserializationAdvice);
        installOne(instrumentation, InitialContext.class, jndiAdvice);

        installed = true;
    }

    /**
     * Grants {@code targetModule} an explicit "reads" edge to the module our
     * bootstrap-injected classes live in. {@code java.base} (ProcessBuilder,
     * ObjectInputStream) already reads essentially everything implicitly, so
     * this is a no-op for those two in practice; it is NOT a no-op for
     * {@code java.naming} (InitialContext), which does not automatically read
     * the bootstrap classloader's unnamed module the way java.base does. This
     * is the concrete fix attempt for the JNDI sensor issue described in
     * docs/DESIGN_DECISIONS.md section 6.
     */
    private static void grantModuleRead(Instrumentation instrumentation, Module targetModule, Module moduleToRead) {
        if (targetModule.canRead(moduleToRead)) {
            return;
        }
        instrumentation.redefineModule(
                targetModule,
                Set.of(moduleToRead),
                java.util.Map.of(),
                java.util.Map.of(),
                Set.of(),
                java.util.Map.of()
        );
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
