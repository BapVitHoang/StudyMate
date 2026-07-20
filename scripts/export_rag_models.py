#!/usr/bin/env python3
"""Export Phase 2 RAG ONNX models for StudyMate (all-MiniLM-L6-v2 + ms-marco MiniLM).

NOTE: Filenames historically used the *_int8.onnx suffix for app compatibility.
Optimum's default ORT export here is typically FP32/ONNX (not quantized INT8).
If you need true INT8, run a separate onnxruntime quantization pass and keep the
same destination filenames expected by ModelManager.

Usage:
  pip install sentence-transformers optimum[onnxruntime] onnx onnxruntime
  python scripts/export_rag_models.py --out models_export

Then upload:
  models_export/minilm_embed_int8.onnx
  models_export/minilm_rerank_int8.onnx
  models_export/vocab.txt

to Firebase Storage / CDN and set URLs in app/src/main/assets/rag_model_urls.json
(or copy the three files into the Android device path: files/models/).
If URLs are empty, ModelManager falls back to Constants.DEFAULT_*_URL.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


def export_embedding(out_dir: Path) -> None:
    from optimum.onnxruntime import ORTModelForFeatureExtraction
    from transformers import AutoTokenizer

    model_id = "sentence-transformers/all-MiniLM-L6-v2"
    print(f"Exporting embedding model: {model_id}")
    model = ORTModelForFeatureExtraction.from_pretrained(model_id, export=True)
    tokenizer = AutoTokenizer.from_pretrained(model_id)

    embed_dir = out_dir / "embed"
    embed_dir.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(embed_dir)
    tokenizer.save_pretrained(embed_dir)

    onnx_candidates = list(embed_dir.glob("*.onnx"))
    if not onnx_candidates:
        raise RuntimeError("No ONNX file produced for embedding model")
    shutil.copyfile(onnx_candidates[0], out_dir / "minilm_embed_int8.onnx")
    shutil.copyfile(embed_dir / "vocab.txt", out_dir / "vocab.txt")
    print(f"Wrote {out_dir / 'minilm_embed_int8.onnx'}")


def export_reranker(out_dir: Path) -> None:
    from optimum.onnxruntime import ORTModelForSequenceClassification
    from transformers import AutoTokenizer

    model_id = "cross-encoder/ms-marco-MiniLM-L-6-v2"
    print(f"Exporting rerank model: {model_id}")
    model = ORTModelForSequenceClassification.from_pretrained(model_id, export=True)
    tokenizer = AutoTokenizer.from_pretrained(model_id)

    rerank_dir = out_dir / "rerank"
    rerank_dir.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(rerank_dir)
    tokenizer.save_pretrained(rerank_dir)

    onnx_candidates = list(rerank_dir.glob("*.onnx"))
    if not onnx_candidates:
        raise RuntimeError("No ONNX file produced for rerank model")
    shutil.copyfile(onnx_candidates[0], out_dir / "minilm_rerank_int8.onnx")
    print(f"Wrote {out_dir / 'minilm_rerank_int8.onnx'}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="models_export", help="Output directory")
    args = parser.parse_args()
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    export_embedding(out_dir)
    export_reranker(out_dir)
    print("Done. Fill app/src/main/assets/rag_model_urls.json with hosted file URLs.")


if __name__ == "__main__":
    main()
