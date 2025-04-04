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
import sys
import logging

from shpurdp_commons.exceptions import FatalException
from shpurdp_server import serverConfiguration
from shpurdp_server import dbConfiguration
from shpurdp_server import setupSecurity
from shpurdp_commons import os_utils
from shpurdp_server import userInput
from shpurdp_server import serverUtils
from shpurdp_server.serverConfiguration import (
  configDefaults,
  get_java_exe_path,
  get_shpurdp_properties,
  read_shpurdp_user,
  parse_properties_file,
  JDBC_DATABASE_PROPERTY,
)
from shpurdp_commons.logging_utils import (
  print_info_msg,
  print_warning_msg,
  print_error_msg,
)
from shpurdp_server.dbConfiguration import (
  ensure_jdbc_driver_is_installed,
  LINUX_DBMS_KEYS_LIST,
)
from shpurdp_server.serverClassPath import ServerClassPath
from shpurdp_server.setupSecurity import (
  ensure_can_start_under_current_user,
  generate_env,
)
from shpurdp_commons.os_utils import run_os_command
from shpurdp_server.serverUtils import is_server_runing
from shpurdp_server.userInput import get_YN_input

logger = logging.getLogger(__name__)

CHECK_DATABASE_HELPER_CMD = (
  "{0} -cp {1} " + "org.apache.shpurdp.server.checks.DatabaseConsistencyChecker"
)


def check_database(options):
  logger.info("Check database consistency.")
  jdk_path = serverConfiguration.get_java_exe_path()

  if jdk_path is None:
    print_error_msg(
      'No JDK found, please run the "setup" '
      "command to install a JDK automatically or install any "
      "JDK manually to " + configDefaults.JDK_INSTALL_DIR
    )
    sys.exit(1)

  properties = serverConfiguration.get_shpurdp_properties()
  serverConfiguration.parse_properties_file(options)

  database_type = properties[JDBC_DATABASE_PROPERTY]
  if not database_type:
    print_error_msg(
      'Please run "shpurdp-server setup" command' " to initialize shpurdp db properties."
    )
    sys.exit(1)

  options.database_index = LINUX_DBMS_KEYS_LIST.index(
    properties[JDBC_DATABASE_PROPERTY]
  )

  dbConfiguration.ensure_jdbc_driver_is_installed(
    options, serverConfiguration.get_shpurdp_properties()
  )

  serverClassPath = ServerClassPath(
    serverConfiguration.get_shpurdp_properties(), options
  )
  class_path = serverClassPath.get_full_shpurdp_classpath_escaped_for_shell()

  command = CHECK_DATABASE_HELPER_CMD.format(jdk_path, class_path)

  shpurdp_user = serverConfiguration.read_shpurdp_user()
  current_user = setupSecurity.ensure_can_start_under_current_user(shpurdp_user)
  environ = setupSecurity.generate_env(options, shpurdp_user, current_user)

  (retcode, stdout, stderr) = os_utils.run_os_command(command, env=environ)

  if retcode > 0:
    raise FatalException(
      int(retcode),
      "Database check failed to complete: {0}. \nPlease check {1} and {2} for more "
      "information.".format(
        stdout + stderr, configDefaults.SERVER_LOG_FILE, configDefaults.DB_CHECK_LOG
      ),
    )
  else:
    print(str(stdout))
    if not stdout.startswith("No errors"):
      print("Shpurdp Server 'check-database' completed")
      sys.exit(1)
