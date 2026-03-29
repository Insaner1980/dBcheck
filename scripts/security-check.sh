#!/usr/bin/env bash
set -euo pipefail

# Security check: Semgrep (code security) + OWASP Dependency-Check (CVE)
# Results saved to reports/

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="$PROJECT_DIR/reports"

mkdir -p "$REPORTS_DIR"

echo "=== Semgrep (code security) ==="
semgrep scan --config auto \
  --exclude='build' --exclude='.gradle' \
  --output "$REPORTS_DIR/security-code.txt" \
  --text \
  "$PROJECT_DIR/app/src/main/java" 2>&1 || true
echo "Results: $REPORTS_DIR/security-code.txt"

echo ""
echo "=== OWASP Dependency-Check (CVE) ==="
dependency-check \
  --project "$(basename "$PROJECT_DIR")" \
  --scan "$PROJECT_DIR/app/build.gradle.kts" \
  --scan "$PROJECT_DIR/gradle/libs.versions.toml" \
  --format JSON --format TXT \
  --out "$REPORTS_DIR" \
  2>&1 | tee "$REPORTS_DIR/security-deps.txt" || true
echo "Results: $REPORTS_DIR/security-deps.txt"

echo ""
echo "=== Done ==="
