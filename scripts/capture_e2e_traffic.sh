#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPORT_DIR="${ROOT_DIR}/build/reports/traffic"
TARGET_PORT="${TARGET_PORT:-8080}"
TARGET_URL="${TARGET_URL:-http://localhost:${TARGET_PORT}}"

mkdir -p "${REPORT_DIR}"
PCAP_FILE="${REPORT_DIR}/e2e_traffic.pcap"
LOG_FILE="${REPORT_DIR}/traffic_dissection.log"

rm -f "${PCAP_FILE}" "${LOG_FILE}"

echo "=================================================="
echo "Starting E2E Network Traffic Capture"
echo "Target: ${TARGET_URL} (Port: ${TARGET_PORT})"
echo "Report Dir: ${REPORT_DIR}"
echo "=================================================="

if ! curl -fsS "${TARGET_URL}/health" >/dev/null; then
    echo "Error: test backend is unavailable at ${TARGET_URL}" >&2
    exit 1
fi

CAPTURE_PID=""

cleanup() {
    if [ -n "${CAPTURE_PID}" ]; then
        kill -2 "${CAPTURE_PID}" 2>/dev/null || true
        wait "${CAPTURE_PID}" 2>/dev/null || true
        CAPTURE_PID=""
    fi
}
trap cleanup EXIT

CAPTURE_TOOL=""
if command -v tshark >/dev/null 2>&1; then
    CAPTURE_TOOL="tshark"
    tshark -i lo -f "tcp port ${TARGET_PORT}" -w "${PCAP_FILE}" >/dev/null 2>&1 &
    CAPTURE_PID=$!
elif command -v tcpdump >/dev/null 2>&1; then
    CAPTURE_TOOL="tcpdump"
    tcpdump -i lo -w "${PCAP_FILE}" "tcp port ${TARGET_PORT}" >/dev/null 2>&1 &
    CAPTURE_PID=$!
else
    echo "Error: Neither tshark nor tcpdump is installed for traffic capture." >&2
    exit 1
fi

sleep 1
if ! kill -0 "${CAPTURE_PID}" 2>/dev/null; then
    echo "Error: traffic capture failed; check interface and capture permissions" >&2
    wait "${CAPTURE_PID}" || true
    exit 1
fi

python3 "${SCRIPT_DIR}/run_e2e_scenario.py" "${TARGET_URL}"

sleep 2

if [ -n "${CAPTURE_PID}" ]; then
    kill -2 "${CAPTURE_PID}" 2>/dev/null || true
    wait "${CAPTURE_PID}" 2>/dev/null || true
    CAPTURE_PID=""
fi


sleep 1

if [ -f "${PCAP_FILE}" ]; then
    echo "Dissecting captured packets..."
    if [ "${CAPTURE_TOOL}" = "tshark" ]; then
        tshark -r "${PCAP_FILE}" -V > "${LOG_FILE}" 2>/dev/null || tshark -r "${PCAP_FILE}" > "${LOG_FILE}"
    else
        tcpdump -A -r "${PCAP_FILE}" "tcp port ${TARGET_PORT}" > "${LOG_FILE}" 2>/dev/null
    fi
    echo "Traffic capture recorded in ${PCAP_FILE}"
    echo "Dissected traffic log saved in ${LOG_FILE}"
else
    echo "Error: Packet capture file ${PCAP_FILE} was not created." >&2
    exit 1
fi

echo "Traffic capture complete."
