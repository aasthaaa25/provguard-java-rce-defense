package provguard.sensors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;
import provguard.provenance.SinkType;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the sensor actually intercepts a real JDK call, end to end. The
 * agent is attached via -javaagent at JVM startup for the whole test run
 * (see pom.xml's surefire argLine and docs/DESIGN_DECISIONS.md section 4G)
 * rather than per-class dynamic self-attach, so no setup is needed here
 * beyond what @BeforeEach already does.
 */
class ProcessExecutionSensorTest {

    @BeforeEach
    void clearBuffer() {
        EventBuffer.INSTANCE.clear();
    }

    @Test
    void capturesProvenanceWhenProcessBuilderStartIsInvoked() throws IOException, InterruptedException {
        // ProcessExecutionSensorTest is on the Policy allowlist, so this is allowed.
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo provguard-test");
        Process process = pb.start();
        process.waitFor();

        List<ProvenanceEvent> events = EventBuffer.INSTANCE.getAll();

        assertFalse(events.isEmpty(), "Expected the sensor to capture at least one event for ProcessBuilder.start()");
        assertEquals(SinkType.PROCESS_EXECUTION, events.get(0).sinkType());
        assertTrue(
                events.get(0).callStack().stream().anyMatch(frame -> frame.contains("ProcessExecutionSensorTest")),
                "Expected the captured call stack to include the test method that triggered the sink, got: "
                        + events.get(0).callStack()
        );
    }

    @Test
    void doesNotCaptureAnythingBeforeStartIsCalled() {
        assertEquals(0, EventBuffer.INSTANCE.size(), "Buffer should be empty until a sink is actually invoked");
    }

    @Test
    void blocksSinkCallFromCallerNotOnTheAllowlist() {
        // UntrustedCaller is NOT on the Policy allowlist, so this must be blocked -
        // and blocked means the real ProcessBuilder#start() body never executes.
        // Asserted against SecurityException (SinkBlockedException's JDK-native
        // superclass), not the exact provguard.enforcement.SinkBlockedException
        // class object: that class is loaded via the bootstrap classloader when
        // thrown from woven sink code, and comparing against a Class literal
        // resolved by the test's own classloader is exactly the kind of
        // cross-classloader identity trap documented in docs/DESIGN_DECISIONS.md.
        UntrustedCaller untrusted = new UntrustedCaller();

        SecurityException thrown = assertThrows(SecurityException.class, untrusted::runProcess);
        assertEquals("provguard.enforcement.SinkBlockedException", thrown.getClass().getName());
        assertTrue(thrown.getMessage().contains("PROCESS_EXECUTION"));

        List<ProvenanceEvent> events = EventBuffer.INSTANCE.getAll();
        assertFalse(events.isEmpty(), "The sensor should still have captured provenance before blocking");
    }

    /** A caller deliberately absent from Policy's trusted allowlist. */
    static final class UntrustedCaller {
        void runProcess() throws IOException {
            new ProcessBuilder("cmd", "/c", "echo should-be-blocked").start();
        }
    }
}
