#!/usr/bin/env bash
set -e

echo "🏁 Updating OSGi.fx Application Version to $1"
echo "🏁 Updating OSGi.fx Baseline Version to $2"

./scripts/update_version.sh $1 $2

echo "🏁 Committing the changes to current branch"

git add .
git commit -m "🏁 Next Development Sprint Preparation"