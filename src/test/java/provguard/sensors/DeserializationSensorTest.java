package provguard.sensors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;
import provguard.provenance.SinkType;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Proves the deserialization sensor intercepts a REAL
 * ObjectInputStream#resolveClass call - this test actually serializes and
 * then deserializes an object, it does not mock ObjectInputStream. The agent
 * is attached at JVM startup for the whole test run - see pom.xml and
 * docs/DESIGN_DECISIONS.md section 4G.
 */
class DeserializationSensorTest {

    @BeforeEach
    void clearBuffer() {
        EventBuffer.INSTANCE.clear();
    }

    @Test
    void capturesProvenanceWhenResolveClassIsInvoked() throws IOException, ClassNotFoundException {
        // Uses a custom Serializable class rather than a bare String: the JDK
        // serialization protocol has a fast path for well-known types (TC_STRING)
        // that does NOT go through resolveClass(), so a String payload alone
        // does not exercise the sink this test is meant to verify.
        byte[] serialized = serialize(new Payload("provguard-deserialization-test-payload"));

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            Payload result = (Payload) in.readObject(); // triggers resolveClass() internally
            assertEquals("provguard-deserialization-test-payload", result.value);
        }

        List<ProvenanceEvent> events = EventBuffer.INSTANCE.getAll();
        assertFalse(events.isEmpty(), "Expected the sensor to capture at least one event for ObjectInputStream#resolveClass");
        assertEquals(SinkType.DESERIALIZATION, events.get(0).sinkType());
    }

    private static byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            out.flush();
            return bytes.toByteArray();
        }
    }

    static final class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        final String value;

        Payload(String value) {
            this.value = value;
        }
    }
}
