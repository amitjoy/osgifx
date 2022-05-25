#!/usr/bin/env bash
set -e

echo $1 > cnf/app.version
echo $2 > cnf/baseline.version
npm version $1 --no-git-tag-version