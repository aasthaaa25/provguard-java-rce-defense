package provguard.detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import provguard.graph.FeatureExtractor;
import provguard.graph.FeatureVector;
import provguard.graph.ProvenanceGraph;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;

/**
 * Real ONNX Runtime Java inference against a model trained and exported by
 * ml/train_and_export.py (scikit-learn OneClassSVM -> skl2onnx). This class
 * is genuine, working inference plumbing - it loads the actual .onnx file
 * and calls the actual ONNX Runtime native library, not a stub.
 *
 * IMPORTANT, NOT HIDDEN: the currently-committed ml/one_class_svm.onnx model
 * has 0% recall on the local dataset (see
 * evaluation/results/onnx-svm-evaluation-2026-09-03.md for the full,
 * verified diagnosis - the local dataset has no real feature variance for
 * this model to learn from). This class is therefore NOT wired into
 * provguard.enforcement.Policy, the same way OneClassDistanceDetector isn't:
 * the inference pipeline works, the currently-trained model is not good
 * enough to enforce with. See OnnxOneClassDetectorTest for what IS verified:
 * that the model loads, runs, and produces output of the expected shape.
 *
 * Feature order MUST match ml/train_and_export.py's to_features():
 * [depth, edgeCount, is_process_execution, is_deserialization].
 */
public final class OnnxOneClassDetector implements AnomalyDetector, AutoCloseable {

    private final OrtEnvironment environment;
    private final OrtSession session;

    public OnnxOneClassDetector(Path onnxModelPath) {
        try {
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(onnxModelPath.toString(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to load ONNX model from " + onnxModelPath, e);
        }
    }

    @Override
    public DetectionResult detect(ProvenanceGraph graph) {
        FeatureVector fv = FeatureExtractor.extract(graph);
        String sinkType = graph.sinkType();
        float[] features = {
                fv.depth(),
                fv.edgeCount(),
                "PROCESS_EXECUTION".equals(sinkType) ? 1f : 0f,
                "DESERIALIZATION".equals(sinkType) ? 1f : 0f
        };

        try (OnnxTensor input = OnnxTensor.createTensor(environment, FloatBuffer.wrap(features), new long[]{1, 4})) {
            try (OrtSession.Result result = session.run(Map.of("X", input))) {
                long[][] labels = (long[][]) result.get("label").orElseThrow().getValue();
                float score = extractScore(result);
                boolean anomalous = labels[0][0] == -1L;
                return anomalous
                        ? DetectionResult.anomaly("ONNX OneClassSVM: label=-1 (outlier), decision_function score=%.4f".formatted(score))
                        : DetectionResult.allow("ONNX OneClassSVM: label=1 (inlier), decision_function score=%.4f".formatted(score));
            }
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static float extractScore(OrtSession.Result result) throws OrtException {
        OnnxValue scoresValue = result.get("scores").orElseThrow();
        Object value = scoresValue.getValue();
        if (value instanceof float[][] arr) {
            return arr[0][0];
        }
        if (value instanceof float[] arr) {
            return arr[0];
        }
        throw new IllegalStateException("Unexpected ONNX 'scores' output shape: " + value.getClass());
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            System.err.println("Failed to close ONNX session: " + e.getMessage());
        }
    }
}
