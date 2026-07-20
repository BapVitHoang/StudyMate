# StudyMate Phase 2 — one-shot local setup helper (PowerShell)
# Usage: .\scripts\setup_phase2_local.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "==> Export ONNX models (if missing)..."
if (-not (Test-Path "models_export\minilm_embed_int8.onnx")) {
    python scripts/export_rag_models.py --out models_export
}

Write-Host "==> Copy into app assets/models/..."
New-Item -ItemType Directory -Force -Path "app\src\main\assets\models" | Out-Null
Copy-Item "models_export\minilm_embed_int8.onnx" "app\src\main\assets\models\" -Force
Copy-Item "models_export\minilm_rerank_int8.onnx" "app\src\main\assets\models\" -Force
Copy-Item "models_export\vocab.txt" "app\src\main\assets\models\" -Force

Write-Host "==> Done. Next (requires your Google account):"
Write-Host "  firebase login"
Write-Host "  firebase use studymate-56c96"
Write-Host "  firebase functions:secrets:set GEMINI_API_KEY"
Write-Host "  firebase deploy --only functions,firestore"
Write-Host "Then open Android Studio -> Sync -> Run app -> long-press Ask to load models."
