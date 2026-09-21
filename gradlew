#!/bin/sh
# Self-contained Gradle launcher for environments where Gradle is not preinstalled.
set -eu
# Do not use a system Gradle: Fabric Loom 1.7 is tested with this pinned version.
GRADLE_VERSION=8.10.2
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
DIST="$CACHE/gradle-${GRADLE_VERSION}"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE"
  ZIP="$CACHE/gradle.zip"
  if command -v curl >/dev/null 2>&1; then curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP"; else wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O "$ZIP"; fi
  unzip -q -o "$ZIP" -d "$CACHE"
  rm -f "$ZIP"
fi
exec "$DIST/bin/gradle" "$@"
