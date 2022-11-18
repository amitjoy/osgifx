#!/usr/bin/env bash
set -e

echo "🏁 Updating OSGi.fx Application Version to $1"
echo "🏁 Updating OSGi.fx Baseline Version to $2"

echo $1 > cnf/version/app.version
echo $2 > cnf/version/baseline.version
npm version $1 --no-git-tag-version

echo "🏁 Committing the changes to current branch"

git add .
git commit -m "🏁 Next Development Cycle Preparation"