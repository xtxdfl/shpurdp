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

import configparser
import os
import optparse
import sys

import win32serviceutil
import win32api
import win32event
import win32service

from shpurdp_commons.shpurdp_service import ShpurdpService, ENV_PYTHON_PATH
from shpurdp_commons.exceptions import *
from shpurdp_commons.logging_utils import *
from shpurdp_commons.os_windows import WinServiceController
from shpurdp_commons.os_utils import find_in_path
from shpurdp_agent.ShpurdpConfig import ShpurdpConfig, updateConfigServerHostname
from shpurdp_agent.HeartbeatHandlers import HeartbeatStopHandlers

SHPURDP_VERSION_VAR = "SHPURDP_VERSION_VAR"

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESET_ACTION = "reset"
STATUS_ACTION = "status"
DEBUG_ACTION = "debug"


def parse_options():
  # parse env cmd
  with open(os.path.join(os.getcwd(), "shpurdp-env.cmd"), "r") as env_cmd:
    content = env_cmd.readlines()
  for line in content:
    if line.startswith("set"):
      name, value = line[4:].split("=")
      os.environ[name] = value.rstrip()
  # checking env variables, and fallback to working dir if no env var was founded
  if "SHPURDP_AGENT_CONF_DIR" not in os.environ:
    os.environ["SHPURDP_AGENT_CONF_DIR"] = os.getcwd()
  if "SHPURDP_AGENT_LOG_DIR" not in os.environ:
    os.environ["SHPURDP_AGENT_LOG_DIR"] = os.path.join(
      "\\", "var", "log", "shpurdp-agent"
    )
  if not os.path.exists(os.environ["SHPURDP_AGENT_LOG_DIR"]):
    os.makedirs(os.environ["SHPURDP_AGENT_LOG_DIR"])
  if "PYTHON_EXE" not in os.environ:
    os.environ["PYTHON_EXE"] = find_in_path("python.exe")


class ShpurdpAgentService(ShpurdpService):
  ShpurdpService._svc_name_ = "Shpurdp Agent"
  ShpurdpService._svc_display_name_ = "Shpurdp Agent"
  ShpurdpService._svc_description_ = "Shpurdp Agent"

  heartbeat_stop_handler = None

  # Adds the necessary script dir to the Python's modules path
  def _adjustPythonPath(self, current_dir):
    iPos = 0
    python_path = os.path.join(current_dir, "sbin")
    sys.path.insert(iPos, python_path)

    # Add the alerts and apscheduler subdirs to the path, for the imports to work correctly without
    #  having to modify the files in these 2 subdirectories
    agent_path = os.path.join(current_dir, "sbin", "shpurdp_agent")
    iPos += 1
    sys.path.insert(iPos, agent_path)
    for subdir in os.listdir(agent_path):
      full_subdir = os.path.join(agent_path, subdir)
      iPos += 1
      sys.path.insert(iPos, full_subdir)

  def SvcDoRun(self):
    parse_options()
    self.redirect_output_streams()

    # Soft dependency on the Windows Time service
    ensure_time_service_is_started()

    self.heartbeat_stop_handler = HeartbeatStopHandlers(
      ShpurdpAgentService._heventSvcStop
    )

    self.ReportServiceStatus(win32service.SERVICE_RUNNING)

    from shpurdp_agent import main

    main.main(self.heartbeat_stop_handler)

  def _InitOptionsParser(self):
    return init_options_parser()

  def redirect_output_streams(self):
    self._RedirectOutputStreamsToFile(ShpurdpConfig.getOutFile())
    pass


def ensure_time_service_is_started():
  ret = WinServiceController.EnsureServiceIsStarted("W32Time")
  if 0 != ret:
    raise FatalException(-1, "Error starting Windows Time service: " + str(ret))
  pass


def ctrlHandler(ctrlType):
  ShpurdpAgentService.DefCtrlCHandler()
  return True


#
# Configures the Shpurdp Agent settings and registers the Windows service.
#
def setup(options):
  config = ShpurdpConfig()
  # TODO SHPURDP-18733, need to read home_dir to get correct config file.
  configFile = config.getConfigFile()

  updateConfigServerHostname(configFile, options.host_name)

  ShpurdpAgentService.set_ctrl_c_handler(ctrlHandler)
  ShpurdpAgentService.Install()


#
# Starts the Shpurdp Agent as a service.
# Start the Agent in normal mode, as a Windows service. If the Shpurdp Agent is
# not registered as a service, the function fails. By default, only one instance of the service can
#     possibly run.
#
def svcstart(options):
  (ret, msg) = ShpurdpAgentService.Start(15)
  if 0 != ret:
    options.exit_message = msg
  pass


#
# Stops the Shpurdp Agent.
#
def svcstop(options):
  (ret, msg) = ShpurdpAgentService.Stop()
  if 0 != ret:
    options.exit_message = msg


#
# The Shpurdp Agent status.
#
def svcstatus(options):
  options.exit_message = None

  statusStr = ShpurdpAgentService.QueryStatus()
  print("Shpurdp Agent is " + statusStr)


def svcdebug(options):
  sys.frozen = "windows_exe"  # Fake py2exe so we can debug

  ShpurdpAgentService.set_ctrl_c_handler(ctrlHandler)
  win32serviceutil.HandleCommandLine(ShpurdpAgentService, options)


def init_options_parser():
  parser = optparse.OptionParser(
    usage="usage: %prog action [options]",
  )
  parser.add_option(
    "-r",
    "--hostname",
    dest="host_name",
    default="localhost",
    help="Use specified Shpurdp server host for registration.",
  )
  parser.add_option(
    "-j",
    "--java-home",
    dest="java_home",
    default=None,
    help="Use specified java_home.  Must be valid on all hosts",
  )
  parser.add_option(
    "-v",
    "--verbose",
    action="store_true",
    dest="verbose",
    default=False,
    help="Print verbose status messages",
  )
  parser.add_option(
    "-s",
    "--silent",
    action="store_true",
    dest="silent",
    default=False,
    help="Silently accepts default prompt values",
  )
  parser.add_option(
    "--jdbc-driver",
    default=None,
    help="Specifies the path to the JDBC driver JAR file for the "
    "database type specified with the --jdbc-db option. Used only with --jdbc-db option.",
    dest="jdbc_driver",
  )
  return parser


#
# Main.
#
def agent_main():
  parser = init_options_parser()
  (options, args) = parser.parse_args()

  options.warnings = []

  if len(args) == 0:
    print(parser.print_help())
    parser.error("No action entered")

  action = args[0]
  possible_args_numbers = [1]

  matches = 0
  for args_number_required in possible_args_numbers:
    matches += int(len(args) == args_number_required)

  if matches == 0:
    print(parser.print_help())
    possible_args = " or ".join(str(x) for x in possible_args_numbers)
    parser.error(
      "Invalid number of arguments. Entered: "
      + str(len(args))
      + ", required: "
      + possible_args
    )

  options.exit_message = f"Shpurdp Agent '{action}' completed successfully."
  try:
    if action == SETUP_ACTION:
      setup(options)
    elif action == START_ACTION:
      svcstart(options)
    elif action == DEBUG_ACTION:
      svcdebug(options)
    elif action == STOP_ACTION:
      svcstop(options)
    elif action == STATUS_ACTION:
      svcstatus(options)
    else:
      parser.error("Invalid action")

    if options.warnings:
      for warning in options.warnings:
        print_warning_msg(warning)
        pass
      options.exit_message = f"Shpurdp Agent '{action}' completed with warnings."
      pass
  except FatalException as e:
    if e.reason is not None:
      print_error_msg(f"Exiting with exit code {e.code}. \nREASON: {e.reason}")
    sys.exit(e.code)
  except NonFatalException as e:
    options.exit_message = f"Shpurdp Agent '{action}' completed with warnings."
    if e.reason is not None:
      print_warning_msg(e.reason)

  if options.exit_message is not None:
    print(options.exit_message)


if __name__ == "__main__":
  try:
    agent_main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
