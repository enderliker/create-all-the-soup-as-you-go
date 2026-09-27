#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
if [[ -z "${JAVA_HOME:-}" ]]; then
    for soup_jdk in "$HOME"/.gradle/jdks/*; do
        if [[ -x "$soup_jdk/bin/java" ]] && "$soup_jdk/bin/java" -version 2>&1 | head -1 | rg -q 'version "21\.'; then
            export JAVA_HOME="$soup_jdk"
            break
        fi
    done
fi
exec ./gradlew runShowcase -Pshowcase --no-daemon "$@"
