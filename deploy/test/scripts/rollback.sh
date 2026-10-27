#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PGPASSWORD="${POSTGRES_TEST_PASSWORD:-timetracker_test_pass}"
psql -v ON_ERROR_STOP=1 -h "${POSTGRES_TEST_HOST:-localhost}" -p "${POSTGRES_TEST_PORT:-5434}" -U "${POSTGRES_TEST_USER:-timetracker_test}" -d "${POSTGRES_TEST_DB:-timetracker_test}" -f "${SCRIPT_DIR}/rollback.sql"
psql -v ON_ERROR_STOP=1 -h "${POSTGRES_TEST_HOST:-localhost}" -p "${POSTGRES_TEST_PORT:-5434}" -U "${POSTGRES_TEST_USER:-timetracker_test}" -d "${POSTGRES_TEST_DB:-timetracker_test}" -f "${SCRIPT_DIR}/02_seed.sql"
