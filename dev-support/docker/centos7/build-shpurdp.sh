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

echo -e "\033[32mStarting container shpurdp-rpm-build\033[0m"
if [ `docker inspect --format '{{.State.Running}}' shpurdp-rpm-build` == true ];then
  docker exec shpurdp-rpm-build bash -c "pkill -KILL -f maven"
else
  docker start shpurdp-rpm-build
fi

echo -e "\033[32mCompiling shpurdp\033[0m"
docker exec shpurdp-rpm-build bash -c "mvn clean install rpm:rpm -DskipTests -Drat.skip=true"
docker stop shpurdp-rpm-build

echo -e "\033[32mRestarting shpurdp-server\033[0m"
docker exec shpurdp-server bash -c "shpurdp-server stop"
docker exec shpurdp-server bash -c "shpurdp-agent stop"
docker exec shpurdp-server bash -c "yum -y remove shpurdp-server"
docker exec shpurdp-server bash -c "yum -y remove shpurdp-agent"
docker cp ../../../shpurdp-server/target/rpm/shpurdp-server/RPMS/x86_64/shpurdp-server*.rpm shpurdp-server:/root/shpurdp-server.rpm
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-server:/root/shpurdp-agent.rpm
docker exec shpurdp-server bash -c "yum -y install /root/shpurdp-server.rpm"
docker exec shpurdp-server bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-server bash -c "shpurdp-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"
docker exec shpurdp-server bash -c "shpurdp-server setup --java-home=/usr/lib/jvm/java --database=mysql --databasehost=localhost --databaseport=3306 --databasename=shpurdp --databaseusername=root --databasepassword=root -s"
docker exec shpurdp-server bash -c "shpurdp-server restart --debug"
docker exec shpurdp-server bash -c "shpurdp-agent restart"

echo -e "\033[32mRestarting shpurdp-agent-01\033[0m"
docker exec shpurdp-agent-01 bash -c "shpurdp-agent stop"
docker exec shpurdp-agent-01 bash -c "yum -y remove shpurdp-agent"
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-agent-01:/root/shpurdp-agent.rpm
docker exec shpurdp-agent-01 bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-agent-01 bash -c "shpurdp-agent restart"

echo -e "\033[32mRestarting shpurdp-agent-02\033[0m"
docker exec shpurdp-agent-02 bash -c "shpurdp-agent stop"
docker exec shpurdp-agent-02 bash -c "yum -y remove shpurdp-agent"
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-agent-02:/root/shpurdp-agent.rpm
docker exec shpurdp-agent-02 bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-agent-02 bash -c "shpurdp-agent restart"

echo -e "\033[32mDone!\033[0m"