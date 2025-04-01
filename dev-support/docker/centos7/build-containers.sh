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
if [[ -z $(docker ps -a --format "table {{.Names}}" | grep "shpurdp-rpm-build") ]];then
  docker run -it -d --name shpurdp-rpm-build --privileged=true -e "container=docker" \
    -v /sys/fs/cgroup:/sys/fs/cgroup:ro -v $PWD/../../../:/opt/shpurdp/ \
    -w /opt/shpurdp \
    shpurdp/develop:trunk-centos-7
else
  docker start shpurdp-rpm-build
fi

echo -e "\033[32mCompiling shpurdp\033[0m"
docker exec shpurdp-rpm-build bash -c "mvn clean install rpm:rpm -DskipTests -Drat.skip=true"
docker stop shpurdp-rpm-build

echo -e "\033[32mCreating network shpurdp\033[0m"
docker network create --driver bridge shpurdp

echo -e "\033[32mCreating container shpurdp-server\033[0m"
docker run -d -p 3306:3306 -p 5005:5005 -p 8080:8080 --name shpurdp-server --hostname shpurdp-server --network shpurdp --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro shpurdp/develop:trunk-centos-7 /usr/sbin/init
docker cp ../../../shpurdp-server/target/rpm/shpurdp-server/RPMS/x86_64/shpurdp-server*.rpm shpurdp-server:/root/shpurdp-server.rpm
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-server:/root/shpurdp-agent.rpm
SERVER_PUB_KEY=`docker exec shpurdp-server /bin/cat /root/.ssh/id_rsa.pub`
docker exec shpurdp-server bash -c "yum -y install /root/shpurdp-server.rpm"
docker exec shpurdp-server bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-server bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec shpurdp-server /bin/systemctl enable sshd
docker exec shpurdp-server /bin/systemctl start sshd

echo -e "\033[32mSetting up mariadb-server\033[0m"
docker exec shpurdp-server /bin/systemctl enable mariadb
docker exec shpurdp-server /bin/systemctl start mariadb
docker exec shpurdp-server bash -c "mysql -e \"UPDATE mysql.user SET Password = PASSWORD('root') WHERE User = 'root'\""
docker exec shpurdp-server bash -c "mysql -e \"GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION\""
docker exec shpurdp-server bash -c "mysql -e \"DROP USER ''@'localhost'\""
docker exec shpurdp-server bash -c "mysql -e \"DROP USER ''@'shpurdp-server'\""
docker exec shpurdp-server bash -c "mysql -e \"DROP DATABASE test\""
docker exec shpurdp-server bash -c "mysql -e \"CREATE DATABASE shpurdp\""
docker exec shpurdp-server bash -c "mysql --database=shpurdp -e  \"source /var/lib/shpurdp-server/resources/Shpurdp-DDL-MySQL-CREATE.sql\""

docker exec shpurdp-server bash -c "mysql -e \"CREATE USER 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec shpurdp-server bash -c "mysql  -e \"GRANT ALL PRIVILEGES ON *.* TO 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec shpurdp-server bash -c "mysql -e \"CREATE DATABASE hive\""

docker exec shpurdp-server bash -c "mysql -e \"FLUSH PRIVILEGES\""

echo -e "\033[32mSetting up shpurdp-server\033[0m"
docker exec shpurdp-server bash -c "shpurdp-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"
docker exec shpurdp-server bash -c "shpurdp-server setup --java-home=/usr/lib/jvm/java --database=mysql --databasehost=localhost --databaseport=3306 --databasename=shpurdp --databaseusername=root --databasepassword=root -s"

echo -e "\033[32mCreating container shpurdp-agent-01\033[0m"
docker run -d -p 9995:9995 --name shpurdp-agent-01 --hostname shpurdp-agent-01 --network shpurdp --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro shpurdp/develop:trunk-centos-7 /usr/sbin/init
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-agent-01:/root/shpurdp-agent.rpm
docker exec shpurdp-agent-01 bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-agent-01 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec shpurdp-agent-01 /bin/systemctl enable sshd
docker exec shpurdp-agent-01 /bin/systemctl start sshd

echo -e "\033[32mCreating container shpurdp-agent-02\033[0m"
docker run -d -p 8088:8088 --name shpurdp-agent-02 --hostname shpurdp-agent-02 --network shpurdp --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro shpurdp/develop:trunk-centos-7 /usr/sbin/init
docker cp ../../../shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64/shpurdp-agent*.rpm shpurdp-agent-02:/root/shpurdp-agent.rpm
docker exec shpurdp-agent-02 bash -c "yum -y install /root/shpurdp-agent.rpm"
docker exec shpurdp-agent-02 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec shpurdp-agent-02 /bin/systemctl enable sshd
docker exec shpurdp-agent-02 /bin/systemctl start sshd

echo -e "\033[32mConfiguring hosts file\033[0m"
SHPURDP_SERVER_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' shpurdp-server`
SHPURDP_AGENT_01_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' shpurdp-agent-01`
SHPURDP_AGENT_02_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' shpurdp-agent-02`
docker exec shpurdp-server bash -c "echo '$SHPURDP_AGENT_01_IP      shpurdp-agent-01' >> /etc/hosts"
docker exec shpurdp-server bash -c "echo '$SHPURDP_AGENT_02_IP      shpurdp-agent-02' >> /etc/hosts"
docker exec shpurdp-agent-01 bash -c "echo '$SHPURDP_SERVER_IP      shpurdp-server' >> /etc/hosts"
docker exec shpurdp-agent-01 bash -c "echo '$SHPURDP_AGENT_02_IP      shpurdp-agent-02' >> /etc/hosts"
docker exec shpurdp-agent-02 bash -c "echo '$SHPURDP_SERVER_IP      shpurdp-server' >> /etc/hosts"
docker exec shpurdp-agent-02 bash -c "echo '$SHPURDP_AGENT_01_IP      shpurdp-agent-01' >> /etc/hosts"


echo -e "\033[32mConfiguring Kerberos\033[0m"
docker cp ./krb5.conf shpurdp-server:/etc/krb5.conf
docker cp ./krb5.conf shpurdp-agent-01:/etc/krb5.conf
docker cp ./krb5.conf shpurdp-agent-02:/etc/krb5.conf
docker exec shpurdp-server bash -c "echo -e \"admin\nadmin\" | kdb5_util create -s -r EXAMPLE.COM"
docker exec shpurdp-server bash -c "echo -e \"admin\nadmin\" | kadmin.local -q \"addprinc admin/admin\""
docker exec shpurdp-server bash -c "systemctl start krb5kdc"
docker exec shpurdp-server bash -c "systemctl enable krb5kdc"
docker exec shpurdp-server bash -c "systemctl start kadmin"
docker exec shpurdp-server bash -c "systemctl enable kadmin"

echo -e "\033[32mSynchronize Chrony\033[0m"
docker exec shpurdp-server bash -c "systemctl enable chronyd; systemctl start chronyd; chronyc tracking"
docker exec shpurdp-agent-01 bash -c "systemctl enable chronyd; systemctl start chronyd; chronyc tracking"
docker exec shpurdp-agent-02 bash -c "systemctl enable chronyd; systemctl start chronyd; chronyc tracking"

docker exec shpurdp-server bash -c "shpurdp-server restart --debug"

echo -e "\033[32mPrint Shpurdp Server RSA Private Key\033[0m"
docker exec shpurdp-server bash -c "cat ~/.ssh/id_rsa"

# KDC HOST: shpurdp-server
# REALM NAME: EXAMPLE.COM
# ADMIN PRINCIPAL: admin/admin@EXAMPLE.COM
# ADMIN PASSWORD: admin

# MySQL HOST: shpurdp-server
# MySQL PORT: 3306
# DATABASE NAME: hive
# DATABASE USER NAME: hive
# DATABASE PASSWORD: hive
