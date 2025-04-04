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

# Shpurdp Commons & Resource Management Imports
from shpurdp_commons import OSCheck
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script


# a map of the Shpurdp role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  "HIVE_METASTORE": "hive-metastore",
  "HIVE_SERVER": "hive-server2",
  "HIVE_CLIENT": "hive-client",
}

# Either HIVE_METASTORE, HIVE_SERVER, HIVE_CLIENT
role = default("/role", None)
component_directory = Script.get_component_from_role(
  SERVER_ROLE_DIRECTORY_MAP, "HIVE_CLIENT"
)

config = Script.get_config()

stack_root = Script.get_stack_root()
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted_major = format_stack_version(stack_version_unformatted)

hive_pid_dir = config["configurations"]["hive-env"]["hive_pid_dir"]
hive_pid = format("{hive_pid_dir}/hive-server.pid")
hive_metastore_pid = format("{hive_pid_dir}/hive.pid")

process_name = "mysqld"

SERVICE_FILE_TEMPLATES = ["/etc/init.d/{0}", "/usr/lib/systemd/system/{0}.service"]
POSSIBLE_DAEMON_NAMES = ["mysql", "mysqld", "mariadb"]


# Security related/required params
hostname = config["agentLevelParams"]["hostname"]
security_enabled = config["configurations"]["cluster-env"]["security_enabled"]
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
tmp_dir = Script.get_tmp_dir()
hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hive_user = config["configurations"]["hive-env"]["hive_user"]

# hcat_pid_dir
hcat_pid_dir = config["configurations"]["hive-env"]["hcat_pid_dir"]
webhcat_pid_file = format("{hcat_pid_dir}/webhcat.pid")

stack_name = default("/clusterLevelParams/stack_name", None)
