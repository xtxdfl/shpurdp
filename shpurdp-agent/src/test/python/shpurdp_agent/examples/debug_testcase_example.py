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

from unittest import TestCase

# from Register import Register
from shpurdp_agent.Controller import Controller
from shpurdp_agent.Heartbeat import Heartbeat
from shpurdp_agent.ActionQueue import ActionQueue
from shpurdp_agent import ShpurdpConfig
from shpurdp_agent.NetUtil import NetUtil
import socket, configparser, logging
import os, pprint, json, sys, unittest
from threading import Thread
import time
import queue

logger = logging.getLogger()


class TestController(TestCase):
  # This file should be put to shpurdp-agent/src/main/python/shpurdp-agent/debug_testcase_example.py.
  # After installing python plugin and adjusting test,
  # it may be run in IntelliJ IDEA debugger

  def setUp(self):
    # logger.disabled = True
    self.defaulttimeout = -1.0
    if hasattr(socket, "getdefaulttimeout"):
      # Set the default timeout on sockets
      self.defaulttimeout = socket.getdefaulttimeout()

  def tearDown(self):
    if (
      self.defaulttimeout is not None
      and self.defaulttimeout > 0
      and hasattr(socket, "setdefaulttimeout")
    ):
      # Set the default timeout on sockets
      socket.setdefaulttimeout(self.defaulttimeout)
      # logger.disabled = False

  def test_custom(self):
    """
    test to make sure if we can get a re register command, we register with the server
    """
    pass


def main(argv=None):
  logger.setLevel(logging.INFO)
  formatter = logging.Formatter(
    "%(asctime)s %(filename)s:%(lineno)d - \
      %(message)s"
  )
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)

  unittest.main()


if __name__ == "__main__":
  main()
