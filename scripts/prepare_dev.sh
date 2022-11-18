#!/usr/bin/env bash
set -e

baseline_version=$(cat cnf/version/app.version)
echo "🏁 Updating OSGi.fx Application Version to $1"
echo "🏁 Updating OSGi.fx Baseline Version to $baseline_version"

echo $1.SNAPSHOT > cnf/version/app.version
echo $baseline_version > cnf/version/baseline.version

npm version $1 --no-git-tag-version

echo "🏁 Committing the changes to current branch"

git add .
git commit -m "🏁 Next Development Cycle Preparation"