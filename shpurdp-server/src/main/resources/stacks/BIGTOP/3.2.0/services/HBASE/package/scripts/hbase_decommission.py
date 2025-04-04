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

"""

from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from resource_management.libraries.functions.format import format
from shpurdp_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from shpurdp_commons import OSConst
from resource_management.core.logger import Logger


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hbase_decommission(env):
  import params

  env.set_params(params)
  File(
    params.region_drainer,
    content=StaticFile("draining_servers.rb"),
    owner=params.hbase_user,
    mode="f",
  )

  hosts = params.hbase_excluded_hosts.split(",")
  for host in hosts:
    if host:
      if params.hbase_drain_only == True:
        regiondrainer_cmd = format(
          "cmd /c {hbase_executable} org.jruby.Main {region_drainer} remove {host}"
        )
        Execute(regiondrainer_cmd, user=params.hbase_user, logoutput=True)
      else:
        regiondrainer_cmd = format(
          "cmd /c {hbase_executable} org.jruby.Main {region_drainer} add {host}"
        )
        regionmover_cmd = format(
          "cmd /c {hbase_executable} org.jruby.Main {region_mover} -m 24 -o unload -r {host}"
        )
        Execute(regiondrainer_cmd, user=params.hbase_user, logoutput=True)
        Execute(regionmover_cmd, user=params.hbase_user, logoutput=True)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hbase_decommission(env):
  import params

  env.set_params(params)
  kinit_cmd = params.kinit_cmd_master

  File(params.region_drainer, content=StaticFile("draining_servers.rb"), mode=0o755)
  File(params.region_drainer2, content=StaticFile("draining_servers2.rb"), mode=0o755)

  if params.hbase_excluded_hosts and params.hbase_excluded_hosts.split(","):
    hosts = params.hbase_excluded_hosts.split(",")
  elif params.hbase_included_hosts and params.hbase_included_hosts.split(","):
    hosts = params.hbase_included_hosts.split(",")

  if params.hbase_drain_only:
    for host in hosts:
      if host:
        regiondrainer_cmd = format(
          '{kinit_cmd} HBASE_SERVER_JAAS_OPTS="{master_security_config}" {hbase_cmd} --config {hbase_conf_dir} {hbase_decommission_auth_config} org.jruby.Main {region_drainer2} remove {host}'
        )
        Execute(regiondrainer_cmd, user=params.hbase_user, logoutput=True)
        pass
    pass

  else:
    for host in hosts:
      if host:
        regiondrainer_cmd = format(
          '{kinit_cmd} HBASE_SERVER_JAAS_OPTS="{master_security_config}" {hbase_cmd} --config {hbase_conf_dir} {hbase_decommission_auth_config} org.jruby.Main {region_drainer2} add {host}'
        )
        regionmover_cmd = format(
          '{kinit_cmd} HBASE_SERVER_JAAS_OPTS="{master_security_config}" {hbase_cmd} --config {hbase_conf_dir} {hbase_decommission_auth_config} org.jruby.Main {region_mover} -m 24 -o unload -r {host}'
        )

        Execute(regiondrainer_cmd, user=params.hbase_user, logoutput=True)

        Execute(regionmover_cmd, user=params.hbase_user, logoutput=True)
      pass
    pass
  pass
