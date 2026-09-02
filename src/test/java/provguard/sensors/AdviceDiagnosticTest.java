package provguard.sensors;

import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Test;

class AdviceDiagnosticTest {
    @Test
    void adviceToDoesNotThrow() {
        Advice advice = Advice.to(ProcessExecutionAdvice.class);
        System.out.println("ADVICE_OK: " + advice);
    }
}
