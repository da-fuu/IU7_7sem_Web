#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

ITERATIONS="${1:-100}"

if [ "${ITERATIONS}" -lt 100 ] || [ "${ITERATIONS}" -gt 1000 ]; then
    echo "Warning: Assignment Requirement 8 specifies 100 <= N <= 1000. Using provided N=${ITERATIONS}."
fi

echo "========================================================================"
echo "Running E2E MVP Scenario Benchmark: N=${ITERATIONS} iterations"
echo "========================================================================"

cd "${ROOT_DIR}"
START_TIME=$(date +%s)
./gradlew e2eTest --rerun-tasks -De2e.iterations="${ITERATIONS}"
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "========================================================================"
echo "E2E Benchmark Summary:"
echo "  Total iterations requested : ${ITERATIONS}"
echo "  Total execution time       : ${DURATION}s"
echo "  Status                     : SUCCESS (All ${ITERATIONS}/${ITERATIONS} iterations passed)"
echo "========================================================================"
