#!/usr/bin/env bash
# Download the Olist Brazilian E-commerce dataset from Kaggle.
# Requires: kaggle CLI authenticated (pip install kaggle + ~/.kaggle/kaggle.json)
set -euo pipefail

RAW_DIR="$(cd "$(dirname "$0")/../raw-data/olist" && pwd 2>/dev/null || echo "$(dirname "$0")/../raw-data/olist")"
mkdir -p "$RAW_DIR"

echo "Downloading Olist dataset to $RAW_DIR ..."
kaggle datasets download -d olistbr/brazilian-ecommerce -p "$RAW_DIR" --unzip

echo "Done. Files:"
ls -lh "$RAW_DIR"/*.csv 2>/dev/null || echo "(no CSVs found)"
