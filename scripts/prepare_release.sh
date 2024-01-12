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