package provguard.sensors;

import net.bytebuddy.asm.Advice;
import provguard.provenance.SinkType;

/**
 * ByteBuddy {@link Advice} woven directly into {@code java.lang.ProcessBuilder#start()}.
 *
 * IMPORTANT: this class is never invoked via a normal method call. ByteBuddy
 * copies ("inlines") the bytecode of {@link #onEnter()} directly into
 * ProcessBuilder's compiled start() method. That means every class referenced
 * from inside this method (transitively, everything {@link SensorPipeline}
 * touches) must be resolvable by whatever classloader loaded ProcessBuilder —
 * the bootstrap classloader (null). See {@link provguard.agent.BootstrapInjector}
 * for how ProvGuard's own classes are made visible there.
 *
 * If {@link SensorPipeline} decides to BLOCK, it throws — since that throw
 * happens here, at method entry, the original ProcessBuilder#start() body
 * never runs. That's what makes blocking mode a real prevention mechanism.
 */
public final class ProcessExecutionAdvice {

    private ProcessExecutionAdvice() {
    }

    @Advice.OnMethodEnter
    public static void onEnter() {
        SensorPipeline.captureAndEnforce(SinkType.PROCESS_EXECUTION);
    }
}
