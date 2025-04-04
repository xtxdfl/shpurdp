#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo -e "\033[32mStopping container shpurdp-rpm-build and maven process\033[0m"
if [ `docker inspect --format '{{.State.Running}}' shpurdp-rpm-build` == true ];then
  docker exec shpurdp-rpm-build bash -c "pkill -KILL -f maven"
  docker stop shpurdp-rpm-build
fi

echo -e "\033[32mRemoving container shpurdp-server\033[0m"
docker rm -f shpurdp-server

echo -e "\033[32mRemoving container shpurdp-agent-01\033[0m"
docker rm -f shpurdp-agent-01

echo -e "\033[32mRemoving container shpurdp-agent-02\033[0m"
docker rm -f shpurdp-agent-02

echo -e "\033[32mRemoving network shpurdp\033[0m"
docker network rm shpurdp