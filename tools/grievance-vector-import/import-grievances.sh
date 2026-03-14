#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <grievances.json> [--replace]" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
IMPORT_FILE="$1"
REPLACE_EXISTING="false"

if [[ ! -f "${IMPORT_FILE}" ]]; then
  echo "Import file not found: ${IMPORT_FILE}" >&2
  exit 1
fi

if [[ $# -ge 2 && "$2" == "--replace" ]]; then
  REPLACE_EXISTING="true"
fi

cd "${PROJECT_DIR}"
./gradlew importGrievanceVectors \
  -PgrievanceImportFile="${IMPORT_FILE}" \
  -PgrievanceImportReplaceExisting="${REPLACE_EXISTING}"
