#!/usr/bin/env bash
set -e

./gradlew :com.osgifx.console.agent.api:release --info
./gradlew :com.osgifx.console.agent:release --info
./gradlew :com.osgifx.console.api:release --info
./gradlew :com.osgifx.console.smartgraph:release --info
./gradlew :com.osgifx.console.util:release --info