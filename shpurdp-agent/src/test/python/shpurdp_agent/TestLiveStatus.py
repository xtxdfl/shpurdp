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
from shpurdp_agent.LiveStatus import LiveStatus
from shpurdp_agent.ShpurdpConfig import ShpurdpConfig
import os, sys, io
from shpurdp_agent import ActualConfigHandler
from mock.mock import patch, MagicMock
import pprint
from shpurdp_commons import OSCheck
from only_for_platform import os_distro_value


class TestLiveStatus(TestCase):
  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(ActualConfigHandler.ActualConfigHandler, "read_actual_component")
  def test_build_predefined(self, read_actual_component_mock):
    read_actual_component_mock.return_value = "actual_component"
    """
    Tests that if live status us defined (using default parameter),
    then no StatusCheck is executed
    """
    config = ShpurdpConfig().getConfig()
    config.set("agent", "prefix", "shpurdp_agent" + os.sep + "dummy_files")
    livestatus = LiveStatus(
      "", "SOME_UNKNOWN_SERVICE", "SOME_UNKNOWN_COMPONENT", {}, config, {}
    )
    result = livestatus.build(component_status="STARTED")
    result_str = pprint.pformat(result)
    self.assertEqual(
      result_str,
      "{'clusterName': '',\n "
      "'componentName': 'SOME_UNKNOWN_COMPONENT',\n "
      "'configurationTags': 'actual_component',\n "
      "'msg': '',\n 'serviceName': 'SOME_UNKNOWN_SERVICE',\n "
      "'stackVersion': '',\n 'status': 'STARTED'}",
    )
