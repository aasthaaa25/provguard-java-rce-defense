package provguard.enforcement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Agent attached at JVM startup for the whole test run - see pom.xml and
// docs/DESIGN_DECISIONS.md section 4G.
class EnforcementEngineTest {

    @Test
    void allowDoesNotThrow() {
        assertDoesNotThrow(() -> EnforcementEngine.enforce(new Decision.Allow(), "test"));
    }

    @Test
    void logDoesNotThrow() {
        assertDoesNotThrow(() -> EnforcementEngine.enforce(new Decision.Log("reason"), "test"));
    }

    @Test
    void blockThrowsSinkBlockedException() {
        assertThrows(SinkBlockedException.class,
                () -> EnforcementEngine.enforce(new Decision.Block("reason"), "test"));
    }
}
