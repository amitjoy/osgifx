#!/usr/bin/env bash
set -e

echo "Retrieving version"
version=$(cat cnf/app.version)

echo "Creating tag: v$version"
git tag v$version