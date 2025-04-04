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

__all__ = ["TerminateStrategy", "terminate_process"]

import os
import time
import signal
from resource_management.core.base import Fail

GRACEFUL_PG_KILL_TIMEOUT_SECONDS = 5


class TerminateStrategy:
  """
  0 - kill parent process with SIGTERM (is perfect if all children handle SIGTERM signal). Otherwise children will survive.
  1 - kill process GROUP with SIGTERM and if not effective with SIGKILL
  2 - send SIGTERM to every process in the tree
  """

  TERMINATE_PARENT = 0
  KILL_PROCESS_GROUP = 1
  KILL_PROCESS_TREE = 2


def terminate_process(proc, terminate_strategy):
  if terminate_strategy == TerminateStrategy.TERMINATE_PARENT:
    terminate_parent_process(proc)
  elif terminate_strategy == TerminateStrategy.KILL_PROCESS_GROUP:
    killpg_gracefully(proc)
  elif terminate_strategy == TerminateStrategy.KILL_PROCESS_TREE:
    kill_process_tree(proc)
  else:
    raise Fail(
      f"Invalid timeout_kill_strategy = '{terminate_strategy}'. Use TerminateStrategy class constants as a value."
    )


def killpg_gracefully(proc, timeout=GRACEFUL_PG_KILL_TIMEOUT_SECONDS):
  """
  Tries to kill pgroup (process group) of process with SIGTERM.
  If the process is still alive after waiting for timeout, SIGKILL is sent to the pgroup.
  """
  from resource_management.core import sudo
  from resource_management.core.logger import Logger

  if proc.poll() == None:
    try:
      pgid = os.getpgid(proc.pid)
      sudo.kill(-pgid, signal.SIGTERM.value)

      for i in range(10 * timeout):
        if proc.poll() is not None:
          break
        time.sleep(0.1)
      else:
        Logger.info(
          f"Cannot gracefully kill process group {pgid}. Resorting to SIGKILL."
        )
        sudo.kill(-pgid, signal.SIGKILL.value)
        proc.wait()
    # catch race condition if proc already dead
    except OSError:
      pass


def terminate_parent_process(proc):
  if proc.poll() == None:
    try:
      proc.terminate()
      proc.wait()
    # catch race condition if proc already dead
    except OSError:
      pass


def kill_process_tree(proc):
  from resource_management.core import shell

  current_directory = os.path.dirname(os.path.abspath(__file__))
  kill_tree_script = f"{current_directory}/files/killtree.sh"
  if proc.poll() == None:
    shell.checked_call(["bash", kill_tree_script, str(proc.pid), str(signal.SIGKILL)])
