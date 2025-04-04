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

import os

# Local Imports
import mysql_users

# Shpurdp Commons & Resource Management Imports
from resource_management.core.resources.system import Execute


def mysql_configure():
  import params

  # required for running hive
  for mysql_configname in params.mysql_confignames:
    if os.path.isfile(mysql_configname):
      replace_bind_address = (
        "sed",
        "-i",
        "s|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|",
        mysql_configname,
      )
      Execute(
        replace_bind_address,
        sudo=True,
      )

  # this also will start mysql-server
  mysql_users.mysql_adduser()
