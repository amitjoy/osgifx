#!/usr/bin/env bash
set -e

echo "🏁 Publishing Bundles"

./gradlew :com.osgifx.console.agent:release --info
./gradlew :com.osgifx.console.agent.api:release --info
