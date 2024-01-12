#!/usr/bin/env bash
#-------------------------------------------------------------------------------
# Copyright 2021-2024 Amit Kumar Mondal
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.
#-------------------------------------------------------------------------------
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