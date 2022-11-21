#!/usr/bin/env bash
set -e

echo "🏁 Retrieving version"
version=$(cat cnf/version/app.version)

echo "🏁 Updating Version"
version_without_snapshot=${version%".SNAPSHOT"}
echo $version_without_snapshot > cnf/version/app.version

echo "🏁 Creating tag: v$version"
git tag v$version