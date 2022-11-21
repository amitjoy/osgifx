#!/usr/bin/env bash
set -e

echo "🏁 Publishing OSGi.fx Agent Bundles (API and Implementation)"

./gradlew :com.osgifx.console.agent:release --info
./gradlew :com.osgifx.console.agent.api:release --info
