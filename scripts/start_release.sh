#!/usr/bin/env bash
set -e

echo "Updating OSGi.fx Version to $1"
./scripts/update_version.sh $1

echo "Committing the changes to current branch"
git add .
git commit -m "Release Preparation for $1"

echo "Creating tag: v$1"
git tag v$1