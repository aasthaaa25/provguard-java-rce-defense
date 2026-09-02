package provguard.sensors;

import net.bytebuddy.asm.Advice;
import provguard.provenance.SinkType;

/**
 * Woven into {@code java.io.ObjectInputStream#resolveClass(ObjectStreamClass)}
 * - the method every Java deserialization gadget chain must pass through to
 * resolve the class it's about to instantiate (CWE-502). See
 * ProcessExecutionAdvice's Javadoc for why this class only works because of
 * BootstrapInjector, and how BLOCK mode genuinely prevents the call.
 */
public final class DeserializationAdvice {

    private DeserializationAdvice() {
    }

    @Advice.OnMethodEnter
    public static void onEnter() {
        SensorPipeline.captureAndEnforce(SinkType.DESERIALIZATION);
    }
}
