#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
docker run --rm --network host \
  -v "$PWD:/workspace" -w /workspace \
  -v gestao-verbas-maven-cache:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=localhost \
  maven:3.9.9-eclipse-temurin-21 sh ./mvnw -B -Pintegration "$@" verify
