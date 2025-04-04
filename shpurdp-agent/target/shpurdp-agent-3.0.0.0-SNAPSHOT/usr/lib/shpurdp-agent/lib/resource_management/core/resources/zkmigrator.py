#!/usr/bin/env python3
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Shpurdp Agent

"""

from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format


class ZkMigrator:
  def __init__(self, zk_host, java_exec, java_home, jaas_file, user):
    self.zk_host = zk_host
    self.java_exec = java_exec
    self.java_home = java_home
    self.jaas_file = jaas_file
    self.user = user
    self.zkmigrator_jar = "/var/lib/shpurdp-agent/tools/zkmigrator.jar"

  def set_acls(self, znode, acl, tries=3):
    Logger.info(format("Setting ACL on znode {znode} to {acl}"))
    Execute(
      self._acl_command(znode, acl),
      user=self.user,
      environment={"JAVA_HOME": self.java_home},
      logoutput=True,
      tries=tries,
    )

  def delete_node(self, znode, tries=3):
    Logger.info(format("Removing znode {znode}"))
    Execute(
      self._delete_command(znode),
      user=self.user,
      environment={"JAVA_HOME": self.java_home},
      logoutput=True,
      tries=tries,
    )

  def _acl_command(self, znode, acl):
    return (
      f"{self.java_exec} -Djava.security.auth.login.config={self.jaas_file} -jar {self.zkmigrator_jar}"
      f" -connection-string {self.zk_host} -znode {znode} -acl {acl}"
    )

  def _delete_command(self, znode):
    return (
      f"{self.java_exec} -Djava.security.auth.login.config={self.jaas_file} -jar {self.zkmigrator_jar}"
      f" -connection-string {self.zk_host} -znode {znode} -delete"
    )
