#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
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

# this script will install Shpurdp agent on Centos 7

yum install -y -q wget
wget -q -O /etc/yum.repos.d/shpurdp.repo http://s3.amazonaws.com/dev.hortonworks.com/shpurdp/centos7/2.x/latest/2.1.1/shpurdpbn.repo
yum install -y -q epel-release
yum install -y -q shpurdp-agent
yum install -y -q java-1.8.0-openjdk.x86_64
yum install -y -q java-1.8.0-openjdk-devel.x86_64