package provguard.runtime;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import provguard.provenance.EventBuffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Actually starts a real JFR recording (jdk.jfr.Recording, not a mock),
 * triggers a real ProcessBuilder call through the live agent, stops the
 * recording, and reads the ACTUAL .jfr file back with RecordingFile to
 * confirm our custom SinkHitJfrEvent genuinely appears with the right fields.
 */
class JfrIntegrationTest {

    @Test
    void sinkHitEventIsRecordedAndReadableFromARealJfrFile() throws Exception {
        EventBuffer.INSTANCE.clear();

        Recording recording = new Recording();
        recording.enable("ProvGuard.SinkHit");
        recording.start();

        try {
            // JfrIntegrationTest is not on Policy's allowlist, so this call
            // gets BLOCKED - that's fine, and actually useful: it proves the
            // JFR event fires even for a call that's about to be blocked
            // (SensorPipeline emits it before calling EnforcementEngine).
            new ProcessBuilder("cmd", "/c", "echo jfr-test").start();
        } catch (SecurityException expectedBlock) {
            // expected - see comment above
        } finally {
            recording.stop();
        }

        Path dumpFile = Files.createTempFile("provguard-jfr-test-", ".jfr");
        try {
            recording.dump(dumpFile);
            recording.close();

            List<RecordedEvent> events = RecordingFile.readAllEvents(dumpFile).stream()
                    .filter(e -> e.getEventType().getName().equals("ProvGuard.SinkHit"))
                    .toList();

            assertFalse(events.isEmpty(), "Expected at least one real ProvGuard.SinkHit JFR event to have been recorded");
            RecordedEvent event = events.get(0);
            assertEquals("PROCESS_EXECUTION", event.getValue("sinkType"));
            assertEquals("BLOCK", event.getValue("decision"));
            assertEquals("provguard.runtime.JfrIntegrationTest",
                    ((String) event.getValue("caller")));
        } finally {
            Files.deleteIfExists(dumpFile);
        }
    }
}
