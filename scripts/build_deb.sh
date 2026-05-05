#!/bin/bash
# build_deb.sh — Builds StockFlow .deb package using an Ubuntu container.
# Run from the project root: ./build_deb.sh
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="$PROJECT_DIR/build/compose/binaries/main/deb"

echo "═══════════════════════════════════════════"
echo " StockFlow .deb builder (Podman + Ubuntu)"
echo "═══════════════════════════════════════════"
echo "Project: $PROJECT_DIR"
echo ""

mkdir -p "$OUTPUT_DIR"

podman run --rm \
  --volume "$PROJECT_DIR:/project:Z" \
  --volume "$HOME/.gradle:/root/.gradle:Z" \
  --workdir /project \
  docker.io/library/ubuntu:22.04 \
  bash -c '
    set -e
    echo "[1/4] Installing build tools..."
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq binutils fakeroot dpkg-dev wget curl > /dev/null

    echo "[2/4] Installing JDK 21 (Liberica)..."
    wget -q https://download.bell-sw.com/java/21.0.5+11/bellsoft-jdk21.0.5+11-linux-amd64.deb
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq ./bellsoft-jdk21.0.5+11-linux-amd64.deb > /dev/null
    export JAVA_HOME=/usr/lib/jvm/bellsoft-java21-amd64
    export PATH="$JAVA_HOME/bin:$PATH"
    java -version

    echo "[3/4] Building .deb with Gradle..."
    ./gradlew packageDeb --no-daemon

    echo "[4/4] Done!"
  '

echo ""
echo "✅  .deb package ready in: $OUTPUT_DIR"
ls -lh "$OUTPUT_DIR"/*.deb 2>/dev/null || echo "(No .deb found — check logs above)"
