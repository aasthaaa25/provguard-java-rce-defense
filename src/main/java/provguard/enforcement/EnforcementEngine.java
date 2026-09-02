package provguard.enforcement;

/**
 * Carries out a Decision. ALLOW is a no-op, LOG prints and continues, BLOCK
 * prints and throws SinkBlockedException.
 *
 * Because this is called from Advice.OnMethodEnter code woven directly into
 * the sink method (see provguard.sensors.SensorPipeline), a thrown exception
 * here propagates out of the sink call entirely and the sink's real body
 * never executes - this is what makes BLOCK mode a genuine prevention
 * mechanism rather than just an alert.
 */
public final class EnforcementEngine {

    private EnforcementEngine() {
    }

    public static void enforce(Decision decision, String sinkDescription) {
        switch (decision) {
            case Decision.Allow ignored -> { }
            case Decision.Log log -> System.out.println("[ProvGuard][LOG] " + sinkDescription + " -- " + log.reason());
            case Decision.Block block -> {
                System.out.println("[ProvGuard][BLOCK] " + sinkDescription + " -- " + block.reason());
                throw new SinkBlockedException(sinkDescription + ": " + block.reason());
            }
        }
    }
}
