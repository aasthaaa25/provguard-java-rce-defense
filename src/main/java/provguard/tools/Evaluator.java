package provguard.tools;

import provguard.detection.AllowlistDetector;
import provguard.detection.AnomalyDetector;
import provguard.detection.DetectionResult;
import provguard.detection.OneClassDistanceDetector;
import provguard.graph.FeatureExtractor;
import provguard.graph.FeatureVector;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes REAL precision/recall/F1 for AllowlistDetector and
 * OneClassDistanceDetector against dataset/benign and dataset/malicious -
 * actual numbers from actually running the detectors against the real
 * captured dataset, not estimates. See docs/EVALUATION.md for the honest
 * interpretation and dataset/README.md for what "malicious" does and doesn't
 * mean here.
 *
 * Run (no agent needed - this only reads files and calls detector code directly):
 *   java -cp target/classes provguard.tools.Evaluator
 */
public final class Evaluator {

    private record Trace(String label, String sinkType, int depth, int edgeCount, String caller) {
        boolean isMalicious() {
            return !label.equals("benign");
        }
    }

    public static void main(String[] args) throws IOException {
        List<Trace> benign = parse(Path.of("dataset/benign/local-benign-traces.jsonl"));
        List<Trace> malicious = parse(Path.of("dataset/malicious/local-malicious-shaped-traces.jsonl"));

        System.out.println("Loaded " + benign.size() + " benign and " + malicious.size() + " malicious-shaped traces.\n");

        evaluateAllowlist(benign, malicious);
        System.out.println();
        evaluateOneClassPooled(benign, malicious);
        System.out.println();
        evaluateOneClassPerSinkType(benign, malicious);
    }

    private static void evaluateAllowlist(List<Trace> benign, List<Trace> malicious) {
        System.out.println("=== AllowlistDetector (the detector actually wired into enforcement) ===");
        // Uses the SAME allowlist as production Policy, so this reflects what's actually deployed.
        AnomalyDetector detector = new AllowlistDetector(Set.of(
                "provguard.cli.DemoMain",
                "provguard.tools.DatasetGenerator"
        ));
        Metrics metrics = evaluate(detector, benign, malicious);
        metrics.print();
    }

    private static void evaluateOneClassPooled(List<Trace> benign, List<Trace> malicious) {
        System.out.println("=== OneClassDistanceDetector, POOLED across sink types (structural baseline) ===");
        System.out.println("(deliberately naive - trains on ALL sink types mixed together, to demonstrate why that's wrong)");
        Metrics metrics = trainAndEvaluate(benign, malicious);
        metrics.print();
    }

    private static void evaluateOneClassPerSinkType(List<Trace> benign, List<Trace> malicious) {
        System.out.println("=== OneClassDistanceDetector, trained PER SINK TYPE (correct methodology) ===");
        System.out.println("(see OneClassDistanceDetectorTest and docs/DESIGN_DECISIONS.md for why pooling is wrong: "
                + "DESERIALIZATION's natural depth (~10, ObjectInputStream's own internal call chain) is wildly "
                + "different from PROCESS_EXECUTION's (~3-4), so a pooled model's tolerance band is too wide to "
                + "catch a real deviation within either sink type individually)");

        int truePositive = 0, falsePositive = 0, trueNegative = 0, falseNegative = 0;
        for (String sinkType : List.of("PROCESS_EXECUTION", "DESERIALIZATION")) {
            List<Trace> benignForSink = benign.stream().filter(t -> t.sinkType().equals(sinkType)).toList();
            List<Trace> maliciousForSink = malicious.stream().filter(t -> t.sinkType().equals(sinkType)).toList();
            Metrics m = trainAndEvaluate(benignForSink, maliciousForSink);
            System.out.println("  " + sinkType + ": TP=" + m.truePositive() + " FP=" + m.falsePositive()
                    + " TN=" + m.trueNegative() + " FN=" + m.falseNegative());
            truePositive += m.truePositive();
            falsePositive += m.falsePositive();
            trueNegative += m.trueNegative();
            falseNegative += m.falseNegative();
        }
        new Metrics(truePositive, falsePositive, trueNegative, falseNegative).print();
    }

    private static Metrics trainAndEvaluate(List<Trace> benign, List<Trace> malicious) {
        int splitIndex = Math.max(1, (int) (benign.size() * 0.75));
        List<Trace> trainBenign = benign.subList(0, splitIndex);
        List<Trace> testBenign = benign.subList(splitIndex, benign.size());

        List<FeatureVector> trainingSet = trainBenign.stream()
                .map(t -> new FeatureVector(t.depth(), t.edgeCount()))
                .toList();
        AnomalyDetector detector = new OneClassDistanceDetector(trainingSet, 2.0);
        return evaluate(detector, testBenign, malicious);
    }

    private static Metrics evaluate(AnomalyDetector detector, List<Trace> benign, List<Trace> malicious) {
        int truePositive = 0, falseNegative = 0, trueNegative = 0, falsePositive = 0;

        for (Trace t : malicious) {
            boolean flagged = detector.detect(toGraph(t)).anomalous();
            if (flagged) truePositive++; else falseNegative++;
        }
        for (Trace t : benign) {
            boolean flagged = detector.detect(toGraph(t)).anomalous();
            if (flagged) falsePositive++; else trueNegative++;
        }

        return new Metrics(truePositive, falsePositive, trueNegative, falseNegative);
    }

    /**
     * Rebuilds a ProvenanceGraph faithful to the recorded depth/edgeCount/caller
     * from a dataset row: node[0]=a generic sink placeholder, node[1]=the
     * recorded caller (a genuinely different class name, so
     * ProvenanceGraph#callerClassName() resolves it correctly), plus filler
     * frames to reach the recorded depth exactly - GraphBuilder then derives
     * the same depth/edgeCount FeatureExtractor would have recorded originally.
     */
    private static ProvenanceGraph toGraph(Trace t) {
        List<String> frames = new ArrayList<>();
        frames.add("Sink#invoke");
        if (t.caller() != null) {
            frames.add(t.caller() + "#invoke");
        }
        for (int i = frames.size(); i < t.depth(); i++) {
            frames.add("filler.Frame" + i + "#invoke");
        }
        return GraphBuilder.build(t.sinkType(), frames);
    }

    private static List<Trace> parse(Path file) throws IOException {
        List<Trace> traces = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) continue;
            traces.add(parseLine(line));
        }
        return traces;
    }

    private static Trace parseLine(String line) {
        String sinkType = extract(line, "sinkType");
        String label = extract(line, "label");
        int depth = Integer.parseInt(extract(line, "depth"));
        int edgeCount = Integer.parseInt(extract(line, "edgeCount"));
        String rawCaller = extract(line, "caller");
        String caller = "null".equals(rawCaller) ? null : rawCaller;
        return new Trace(label, sinkType, depth, edgeCount, caller);
    }

    private static String extract(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\":(?:\"([^\"]*)\"|([a-zA-Z0-9]+))");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Field '" + field + "' not found in: " + json);
        }
        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    private record Metrics(int truePositive, int falsePositive, int trueNegative, int falseNegative) {
        double precision() {
            return truePositive + falsePositive == 0 ? 0 : (double) truePositive / (truePositive + falsePositive);
        }

        double recall() {
            return truePositive + falseNegative == 0 ? 0 : (double) truePositive / (truePositive + falseNegative);
        }

        double f1() {
            double p = precision(), r = recall();
            return p + r == 0 ? 0 : 2 * p * r / (p + r);
        }

        double falsePositiveRate() {
            return falsePositive + trueNegative == 0 ? 0 : (double) falsePositive / (falsePositive + trueNegative);
        }

        void print() {
            System.out.printf("TP=%d FP=%d TN=%d FN=%d%n", truePositive, falsePositive, trueNegative, falseNegative);
            System.out.printf("Precision: %.3f%n", precision());
            System.out.printf("Recall:    %.3f%n", recall());
            System.out.printf("F1:        %.3f%n", f1());
            System.out.printf("FPR:       %.3f%n", falsePositiveRate());
        }
    }
}
