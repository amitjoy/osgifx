#!/usr/bin/env bash
set -e

echo "🏁 Retrieving Version"
version=$(cat cnf/version/app.version)

echo "🏁 Updating Version"
version_without_snapshot=${version%".SNAPSHOT"}
echo $version_without_snapshot > cnf/version/app.version

echo "🏁 Committing Changes"
git add .
git commit -m "🏁 REL v$version_without_snapshot Preparation"

echo "🏁 Creating Tag: v$version_without_snapshot"
git tag v$version_without_snapshot