#!/usr/bin/env bash
# run-newman.sh — EcoVolt Billing API test runner using Newman
# Usage: ./postman/scripts/run-newman.sh [environment] [base-url]
#
#   environment   "local" (default) or "ci"
#   base-url      override baseUrl, e.g. http://localhost:8080
#
# Uses global newman when available; otherwise falls back to npx.
# Exit codes: 0 = all tests pass, non-zero = failures or setup error

set -euo pipefail

# ── Resolve script and project paths ──────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POSTMAN_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd "${POSTMAN_DIR}/.." && pwd)"

COLLECTION="${POSTMAN_DIR}/collections/EcoVolt-Billing.postman_collection.json"
REPORTS_DIR="${PROJECT_DIR}/newman-reports"

# ── Arguments ─────────────────────────────────────────────────────────────────
ENV_NAME="${1:-local}"
BASE_URL_OVERRIDE="${2:-}"

case "${ENV_NAME}" in
  local) ENV_FILE="${POSTMAN_DIR}/environments/local.postman_environment.json" ;;
  ci)    ENV_FILE="${POSTMAN_DIR}/environments/ci.postman_environment.json"    ;;
  *)
    echo "ERROR: Unknown environment '${ENV_NAME}'. Use 'local' or 'ci'." >&2
    exit 1
    ;;
esac

# ── Validation ─────────────────────────────────────────────────────────────────
if command -v newman &>/dev/null; then
  NEWMAN_CMD=(newman)
elif command -v npx &>/dev/null; then
  NEWMAN_CMD=(npx --yes --package newman --package newman-reporter-htmlextra newman)
else
  echo "ERROR: neither newman nor npx is available. Install Node.js and Newman." >&2
  exit 1
fi

if [ ! -f "${COLLECTION}" ]; then
  echo "ERROR: Collection not found: ${COLLECTION}" >&2
  exit 1
fi

if [ ! -f "${ENV_FILE}" ]; then
  echo "ERROR: Environment file not found: ${ENV_FILE}" >&2
  exit 1
fi

# ── Prepare reports directory ──────────────────────────────────────────────────
mkdir -p "${REPORTS_DIR}"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
HTML_REPORT="${REPORTS_DIR}/report-${TIMESTAMP}.html"
JUNIT_REPORT="${REPORTS_DIR}/junit-${TIMESTAMP}.xml"

echo "══════════════════════════════════════════════════════"
echo "  EcoVolt Billing API — Newman Test Runner"
echo "  Environment : ${ENV_NAME}"
echo "  Collection  : ${COLLECTION}"
echo "  Env file    : ${ENV_FILE}"
echo "  HTML report : ${HTML_REPORT}"
echo "  JUnit report: ${JUNIT_REPORT}"
echo "══════════════════════════════════════════════════════"

# ── Build Newman arguments ─────────────────────────────────────────────────────
NEWMAN_ARGS=(
  run "${COLLECTION}"
  --environment "${ENV_FILE}"
  --reporters "cli,htmlextra,junit"
  --reporter-htmlextra-export "${HTML_REPORT}"
  --reporter-htmlextra-title "EcoVolt Billing API Test Report"
  --reporter-htmlextra-browserTitle "EcoVolt Billing"
  --reporter-junit-export "${JUNIT_REPORT}"
  --color on
  --bail
)

# Override baseUrl if provided via CLI argument or CI environment variable
if [ -n "${BASE_URL_OVERRIDE}" ]; then
  NEWMAN_ARGS+=(--env-var "baseUrl=${BASE_URL_OVERRIDE}")
elif [ -n "${BILLING_BASE_URL:-}" ]; then
  NEWMAN_ARGS+=(--env-var "baseUrl=${BILLING_BASE_URL}")
fi

# ── Run Newman ─────────────────────────────────────────────────────────────────
echo ""
"${NEWMAN_CMD[@]}" "${NEWMAN_ARGS[@]}"
EXIT_CODE=$?

echo ""
echo "══════════════════════════════════════════════════════"
if [ "${EXIT_CODE}" -eq 0 ]; then
  echo "  ✅  All tests passed."
else
  echo "  ❌  Test failures detected. Exit code: ${EXIT_CODE}"
fi
echo "  Reports saved to: ${REPORTS_DIR}/"
echo "══════════════════════════════════════════════════════"

exit "${EXIT_CODE}"
