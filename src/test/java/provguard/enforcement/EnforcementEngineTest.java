package provguard.enforcement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
