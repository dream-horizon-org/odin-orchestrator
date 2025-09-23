#!/usr/bin/env bash
set -euo pipefail

JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError"
export JVM_OPTS=${JVM_OPTS:-""}
exec java -jar "${JAVA_OPTS}" "${JVM_OPTS}" odin-orchestrator-fat.jar
