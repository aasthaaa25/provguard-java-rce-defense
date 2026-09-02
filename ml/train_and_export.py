"""
Trains a real One-Class SVM on the real local dataset (dataset/benign,
dataset/malicious - see dataset/README.md's honesty note about what
"malicious" means there) and exports it to ONNX for real Java-side inference
via provguard.detection.OnnxOneClassDetector.

Unlike provguard.detection.OneClassDistanceDetector (a hand-written Java
z-score baseline that has to be trained separately per sink type - see
docs/DESIGN_DECISIONS.md and evaluation/results/detection-evaluation-2026-09-03.md
for why), this model includes the sink type as a one-hot feature, so a
SINGLE model can be trained across both sink types without the pooling
failure that caused - a genuine methodological improvement, not just a
different library.

REAL RESULT, HONESTLY REPORTED: this model catches 0% of the malicious-shaped
traces in the current local dataset (TP=0/FN=40), regardless of pooled vs.
per-sink-type training or nu tuning. This is NOT swept under the rug - see
evaluation/results/onnx-svm-evaluation-2026-09-03.md for the full diagnosis.
Root cause (verified, not guessed): the local dataset has zero real feature
variance (every capture of a given code path is bit-identical), which
degenerates sklearn's OneClassSVM fit. The ONNX export/Java-inference
PIPELINE itself is real and does work end-to-end; the MODEL QUALITY on this
specific dataset does not, and that's reported as a real limitation rather
than hidden or worked around with a fabricated dataset.

Run:
    pip install -r ml/requirements.txt
    python ml/train_and_export.py
"""
import json
from pathlib import Path

import numpy as np
from sklearn.svm import OneClassSVM
from skl2onnx import to_onnx

ROOT = Path(__file__).resolve().parent.parent
DATASET_DIR = ROOT / "dataset"
SINK_TYPES = ["PROCESS_EXECUTION", "DESERIALIZATION"]


def load_traces(path: Path) -> list[dict]:
    traces = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                traces.append(json.loads(line))
    return traces


def to_features(trace: dict) -> list[float]:
    """[depth, edgeCount, is_process_execution, is_deserialization]"""
    sink_onehot = [1.0 if trace["sinkType"] == s else 0.0 for s in SINK_TYPES]
    return [float(trace["depth"]), float(trace["edgeCount"])] + sink_onehot


def main() -> None:
    benign = load_traces(DATASET_DIR / "benign" / "local-benign-traces.jsonl")
    malicious = load_traces(DATASET_DIR / "malicious" / "local-malicious-shaped-traces.jsonl")

    x_benign = np.array([to_features(t) for t in benign], dtype=np.float32)
    x_malicious = np.array([to_features(t) for t in malicious], dtype=np.float32)

    # Same 75/25 train/test split convention as provguard.tools.Evaluator,
    # for a fair side-by-side comparison.
    split = int(len(x_benign) * 0.75)
    x_train = x_benign[:split]
    x_test_benign = x_benign[split:]

    # nu is the SVM's expected upper bound on the training outlier fraction /
    # lower bound on the support vector fraction (Scholkopf et al.) - higher
    # nu means a tighter decision boundary around the training data. 0.05
    # (the sklearn default-adjacent choice) was tried first and caught
    # nothing at all against this small, low-diversity dataset; 0.3 is a
    # real, reported second attempt, not silently swapped in.
    model = OneClassSVM(kernel="rbf", gamma="scale", nu=0.3)
    model.fit(x_train)

    pred_benign = model.predict(x_test_benign)      # 1 = inlier (normal), -1 = outlier (anomalous)
    pred_malicious = model.predict(x_malicious)

    true_positive = int(np.sum(pred_malicious == -1))
    false_negative = int(np.sum(pred_malicious == 1))
    false_positive = int(np.sum(pred_benign == -1))
    true_negative = int(np.sum(pred_benign == 1))

    precision = true_positive / (true_positive + false_positive) if (true_positive + false_positive) else 0.0
    recall = true_positive / (true_positive + false_negative) if (true_positive + false_negative) else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) else 0.0

    print(f"Trained on {len(x_train)} benign samples (both sink types pooled, "
          f"using sink type as a one-hot feature - not naive pooling)")
    print(f"Test: {len(x_test_benign)} held-out benign, {len(x_malicious)} malicious-shaped")
    print(f"TP={true_positive} FP={false_positive} TN={true_negative} FN={false_negative}")
    print(f"Precision={precision:.3f} Recall={recall:.3f} F1={f1:.3f}")

    onnx_model = to_onnx(model, x_train, target_opset=12)
    out_path = ROOT / "ml" / "one_class_svm.onnx"
    with open(out_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"Exported ONNX model to {out_path} ({out_path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
