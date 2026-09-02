package provguard.sensors;

import net.bytebuddy.asm.Advice;
import provguard.provenance.SinkType;

/**
 * Woven into {@code javax.naming.InitialContext#lookup(String)} - the
 * Log4Shell-style (CVE-2021-44228) JNDI injection sink. Only the
 * String-argument overload is hooked (not the Name-argument one) since
 * attacker-controlled JNDI URLs arrive as strings; see
 * docs/DESIGN_DECISIONS.md for this and other known-limitation notes.
 * Only {@code InitialContext} itself is hooked, not every possible
 * {@code javax.naming.Context} implementation - a real known limitation,
 * documented rather than silently assumed away.
 */
public final class JndiAdvice {

    private JndiAdvice() {
    }

    @Advice.OnMethodEnter
    public static void onEnter() {
        SensorPipeline.captureAndEnforce(SinkType.JNDI_LOOKUP);
    }
}
