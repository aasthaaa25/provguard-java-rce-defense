package provguard.sensors;

import net.bytebuddy.asm.Advice;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;
import provguard.provenance.SinkType;
import provguard.provenance.StackWalkerCollector;

import java.time.Instant;

/**
 * ByteBuddy {@link Advice} woven directly into {@code java.lang.ProcessBuilder#start()}.
 *
 * IMPORTANT: this class is never invoked via a normal method call. ByteBuddy
 * copies ("inlines") the bytecode of {@link #onEnter()} directly into
 * ProcessBuilder's compiled start() method. That means every class referenced
 * from inside this method (EventBuffer, ProvenanceEvent, SinkType,
 * StackWalkerCollector) must be resolvable by whatever classloader loaded
 * ProcessBuilder — the bootstrap classloader (null). See
 * {@link provguard.agent.BootstrapInjector} for how ProvGuard's own classes are
 * made visible there.
 */
public final class ProcessExecutionAdvice {

    private ProcessExecutionAdvice() {
    }

    @Advice.OnMethodEnter
    public static void onEnter() {
        ProvenanceEvent event = new ProvenanceEvent(
                SinkType.PROCESS_EXECUTION,
                Instant.now(),
                Thread.currentThread().getName(),
                StackWalkerCollector.captureCallStack()
        );
        EventBuffer.INSTANCE.record(event);
    }
}
