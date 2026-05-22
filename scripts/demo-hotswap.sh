#!/usr/bin/env bash
# Interactive hot plug-and-play demo.
# Prerequisite: the app must already be running (scripts/build-plugins.sh was executed first).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PLUGINS_DIR="$ROOT/plugins"

V1_JAR="$ROOT/plugin-bundle/target/plugin-bundle-v1.jar"
V2_JAR="$ROOT/plugin-bundle/target/plugin-bundle-v2.jar"

for jar in "$V1_JAR" "$V2_JAR"; do
    if [[ ! -f "$jar" ]]; then
        echo "ERROR: $jar not found. Run scripts/build-plugins.sh first."
        exit 1
    fi
done

echo "============================================================"
echo "  Hot Plug-and-Play Demo"
echo "============================================================"
echo ""
echo "STEP 1 — Loading plugin-bundle v1 (original ASCII art)"
cp "$V1_JAR" "$PLUGINS_DIR/"
echo "  Copied plugin-bundle-v1.jar → plugins/"
echo "  PluginWatcher will detect it in ~200ms."
echo ""
echo "  Open http://localhost:8080 and send a few messages:"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'carrot'"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'rabbit'"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'cabbage'"
echo ""
read -rp "Press ENTER when you are ready to hot-swap to v2..."
echo ""
echo "STEP 2 — Hot-swapping to plugin-bundle v2 (enhanced ASCII art)"
cp "$V2_JAR" "$PLUGINS_DIR/"
echo "  Copied plugin-bundle-v2.jar → plugins/"
echo "  PluginWatcher replaces all three plugins instantly — no restart."
echo ""
echo "  Send the same messages again and watch the ASCII art change:"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'carrot'"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'rabbit'"
echo "    curl -s -X POST http://localhost:8080/api/messages -H 'Content-Type: text/plain' -d 'cabbage'"
echo ""
echo "  Verify active plugins:"
echo "    curl -s http://localhost:8080/api/plugins | python3 -m json.tool"
echo ""
echo "Demo complete."
