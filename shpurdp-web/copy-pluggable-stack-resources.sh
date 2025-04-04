#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

if [[ $# -eq 0 ]] ; then
  echo 'Usage: copy-pluggable-stack-resources.sh <stack.distribution>'
  exit 1
fi

resources_dir="../shpurdp-common/src/main/python/pluggable_stack_definition/resources/$1"
if [ ! -d $resources_dir ];
then
  echo "No resources to copy for [ $1 ]"
  exit 0
fi

echo "Copying pluggable stack resources for [ $1 ]"
if [ -f "$resources_dir/custom_stack_map.js" ];
then
  echo cp $resources_dir/custom_stack_map.js app/data/custom_stack_map.js
  cp $resources_dir/custom_stack_map.js app/data/custom_stack_map.js
fi
if [ -f "$resources_dir/custom-ui.less" ]
then
  echo cp $resources_dir/custom-ui.less app/styles/custom-ui.less
  cp $resources_dir/custom-ui.less app/styles/custom-ui.less
fi
if [ -f "$resources_dir/messages.js" ]
then
  echo cp $resources_dir/messages.js app/messages.js
  cp $resources_dir/messages.js app/messages.js
fi
