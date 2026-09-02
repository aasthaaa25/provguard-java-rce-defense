package provguard.sensors;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import provguard.agent.InstrumentationManager;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;
import provguard.provenance.SinkType;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the sensor actually intercepts a real JDK call, end to end:
 * dynamically attaches an agent to this very test JVM (no -javaagent flag
 * needed), installs the ProcessBuilder hook, invokes ProcessBuilder.start()
 * for real, and asserts a ProvenanceEvent was captured with a call stack that
 * genuinely includes this test method.
 */
class ProcessExecutionSensorTest {

    @BeforeAll
    static void installAgent() {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        InstrumentationManager.install(instrumentation);
    }

    @BeforeEach
    void clearBuffer() {
        EventBuffer.INSTANCE.clear();
    }

    @Test
    void capturesProvenanceWhenProcessBuilderStartIsInvoked() throws IOException, InterruptedException {
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
}
