import java.io.*;

/**
 * Deliberately vulnerable LOCAL fixture: deserializes a byte array directly
 * via ObjectInputStream with no validation of its own - a stand-in for a
 * classic Java deserialization gadget-chain sink (CWE-502). Exists ONLY to
 * exercise ProvGuard locally; the "payload" here is a harmless custom object,
 * not a real exploit chain (no ysoserial or similar is used or bundled).
 *
 * Not on ProvGuard's trusted allowlist - running WITH the agent attached in
 * blocking mode demonstrates a real block.
 *
 * Usage:
 *   java VulnerableDeserializer.java
 *   java -Dnet.bytebuddy.experimental=true -javaagent:../../target/provguard-agent.jar VulnerableDeserializer.java
 */
public class VulnerableDeserializer {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        byte[] payload = buildSamplePayload();
        System.out.println("Deserializing untrusted-looking payload (" + payload.length + " bytes)...");
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            Object result = in.readObject();
            System.out.println("Deserialized: " + result);
        }
        System.out.println("(if you see this, the call was NOT blocked)");
    }

    private static byte[] buildSamplePayload() throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new SamplePayload());
            return bytes.toByteArray();
        }
    }

    static final class SamplePayload implements Serializable {
        private static final long serialVersionUID = 1L;
        final String note = "stand-in for an attacker-supplied object graph";

        @Override
        public String toString() {
            return "SamplePayload[" + note + "]";
        }
    }
}
