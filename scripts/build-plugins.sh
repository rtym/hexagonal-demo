#!/usr/bin/env bash
# Build both versions of the plugin bundle.
# Output: plugin-bundle/target/plugin-bundle-v1.jar
#         plugin-bundle/target/plugin-bundle-v2.jar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Building plugin-bundle v1 (original ASCII art)..."
mvn clean package -DskipTests -pl plugin-bundle -am -Pbundle-v1

# Stash v1 JAR before the next clean wipes target/
cp plugin-bundle/target/plugin-bundle-v1.jar /tmp/plugin-bundle-v1.jar.tmp

# Clean required so the v2 META-INF/services file overwrites the v1 one in target/classes
echo "==> Building plugin-bundle v2 (enhanced ASCII art)..."
mvn clean package -DskipTests -pl plugin-bundle -am -Pbundle-v2

# Restore v1 JAR alongside v2
cp /tmp/plugin-bundle-v1.jar.tmp plugin-bundle/target/plugin-bundle-v1.jar

echo ""
echo "Built JARs:"
ls -lh plugin-bundle/target/plugin-bundle-v*.jar
echo ""
echo "Next steps:"
echo "  cp plugin-bundle/target/plugin-bundle-v1.jar plugins/   # start with v1"
echo "  # ... run the app, send messages, observe v1 art ..."
echo "  cp plugin-bundle/target/plugin-bundle-v2.jar plugins/   # hot-swap to v2"
