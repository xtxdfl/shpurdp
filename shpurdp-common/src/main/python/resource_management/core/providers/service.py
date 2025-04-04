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

import os

from resource_management.core import shell
from resource_management.core.base import Fail
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger


class ServiceProvider(Provider):
  def action_start(self):
    if not self.status():
      self._exec_cmd("start", 0)

  def action_stop(self):
    if self.status():
      self._exec_cmd("stop", 0)

  def action_restart(self):
    if not self.status():
      self._exec_cmd("start", 0)
    else:
      self._exec_cmd("restart", 0)

  def action_reload(self):
    if not self.status():
      self._exec_cmd("start", 0)
    else:
      self._exec_cmd("reload", 0)

  def status(self):
    return self._exec_cmd("status") == 0

  def _exec_cmd(self, command, expect=None):
    if command != "status":
      Logger.info(f"{self.resource} command '{command}'")

    custom_cmd = getattr(self.resource, f"{command}_command", None)
    if custom_cmd:
      Logger.debug(f"{self.resource} executing '{custom_cmd}'")
      if hasattr(custom_cmd, "__call__"):
        if custom_cmd():
          ret = 0
        else:
          ret = 1
      else:
        ret, out = shell.call(custom_cmd)
    else:
      ret, out = self._init_cmd(command)

    if expect is not None and expect != ret:
      raise Fail(
        "%r command %s for service %s failed with return code: %d. %s"
        % (self, command, self.resource.service_name, ret, out)
      )
    return ret

  def _init_cmd(self, command):
    if self._upstart:
      if command == "status":
        ret, out = shell.call(["/sbin/" + command, self.resource.service_name])
        _proc, state = out.strip().split(" ", 1)
        ret = 0 if state != "stop/waiting" else 1
      else:
        ret, out = shell.call(["/sbin/" + command, self.resource.service_name])
    else:
      ret, out = shell.call([f"/etc/init.d/{self.resource.service_name}", command])
    return ret, out

  @property
  def _upstart(self):
    try:
      return self.__upstart
    except AttributeError:
      self.__upstart = os.path.exists("/sbin/start") and os.path.exists(
        f"/etc/init/{self.resource.service_name}.conf"
      )
    return self.__upstart


class ServiceConfigProvider(Provider):
  pass
