#!/usr/bin/env bash
set -e

echo $1 > cnf/app.version
npm version $1 --no-git-tag-version