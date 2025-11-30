#!/usr/bin/env bash
set -euo pipefail

# Fetch the latest app-debug-apk artifact from the repository's Actions runs
# Requires: gh (GitHub CLI) authenticated

REPO="lukedodd12/AirHand-Control"
ARTIFACT_NAME="app-debug-apk"
OUT_DIR="APK"

echo "Looking for latest successful run with artifact '$ARTIFACT_NAME' in $REPO..."

# Get latest successful run id
RUN_ID=$(gh run list --repo "$REPO" --json databaseId,status,conclusion,event --jq '.[] | select(.status=="completed" and .conclusion=="success") | .databaseId' | head -n 1)

if [ -z "$RUN_ID" ]; then
  echo "No successful workflow runs found. Trigger the workflow first or check Actions page." >&2
  exit 1
fi

echo "Found run: $RUN_ID — downloading artifact '$ARTIFACT_NAME'..."

mkdir -p "$OUT_DIR"

# gh run download supports specifying run id and artifact name
gh run download "$RUN_ID" --repo "$REPO" --name "$ARTIFACT_NAME" --dir "$OUT_DIR"

echo "Downloaded artifact to $OUT_DIR. If the artifact is a zip, unzip it and move the APK into this folder." 
