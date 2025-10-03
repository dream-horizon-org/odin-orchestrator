#!/usr/bin/env bash
set -euo pipefail

JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS=${JVM_OPTS:-""}
#shellcheck disable=SC2086
exec java -jar "${JAVA_OPTS}" ${JVM_OPTS} odin-orchestrator-fat.jar
