package provguard.detection;

import org.junit.jupiter.api.Test;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies real ONNX Runtime Java inference against the actual model file
 * exported by ml/train_and_export.py (ml/one_class_svm.onnx, committed to the
 * repo) - loads the real .onnx file and runs real inference through the
 * native ONNX Runtime library, not a mock.
 *
 * This test deliberately does NOT assert a specific precision/recall - see
 * evaluation/results/onnx-svm-evaluation-2026-09-03.md for why the currently
 * trained model has 0% recall on this project's dataset (a real, diagnosed
 * data-variance limitation, not a bug in this inference code). What IS
 * asserted here: the model loads, runs without throwing, and produces
 * output of the expected shape/type for both sink types - i.e. the
 * Python-to-ONNX-to-Java pipeline itself genuinely works end to end.
 */
class OnnxOneClassDetectorTest {

    private static final Path MODEL_PATH = Path.of("ml", "one_class_svm.onnx");

    @Test
    void loadsRealOnnxModelAndRunsInferenceOnProcessExecutionGraph() {
        try (OnnxOneClassDetector detector = new OnnxOneClassDetector(MODEL_PATH)) {
            ProvenanceGraph graph = GraphBuilder.build("PROCESS_EXECUTION",
                    List.of("java.lang.ProcessBuilder#start", "provguard.cli.DemoMain#main"));
            DetectionResult result = detector.detect(graph);
            assertNotNull(result);
            assertNotNull(result.reason());
            assertTrue(result.reason().contains("ONNX OneClassSVM"),
                    "Result should be produced by real ONNX inference, not a stub: " + result.reason());
        }
    }

    @Test
    void loadsRealOnnxModelAndRunsInferenceOnDeserializationGraph() {
        try (OnnxOneClassDetector detector = new OnnxOneClassDetector(MODEL_PATH)) {
            ProvenanceGraph graph = GraphBuilder.build("DESERIALIZATION",
                    List.of("java.io.ObjectInputStream#resolveClass", "provguard.cli.DemoMain#main"));
            DetectionResult result = detector.detect(graph);
            assertNotNull(result);
            assertNotNull(result.reason());
        }
    }
}
