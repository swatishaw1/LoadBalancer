#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

"$ROOT/Light_Task_Server/mvnw" -f "$ROOT/Light_Task_Server/pom.xml" package -DskipTests
"$ROOT/Heavy_Task_Server/mvnw" -f "$ROOT/Heavy_Task_Server/pom.xml" package -DskipTests

java -jar "$ROOT/Light_Task_Server/target/instance-1-0.0.1-SNAPSHOT.jar" > "$ROOT/light-instance.log" 2>&1 &
LIGHT_PID=$!
java -jar "$ROOT/Heavy_Task_Server/target/instance-2-0.0.1-SNAPSHOT.jar" > "$ROOT/heavy-instance.log" 2>&1 &
HEAVY_PID=$!

printf 'Light instance started (PID %s, port 8080); log: light-instance.log\n' "$LIGHT_PID"
printf 'Heavy instance started (PID %s, port 8081); log: heavy-instance.log\n' "$HEAVY_PID"
printf 'Stop them with: kill %s %s\n' "$LIGHT_PID" "$HEAVY_PID"
wait
