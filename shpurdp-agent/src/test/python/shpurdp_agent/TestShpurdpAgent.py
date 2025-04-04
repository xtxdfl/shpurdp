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

import unittest
import subprocess
import os
import sys
import ShpurdpConfig
from mock.mock import MagicMock, patch, ANY

with patch("distro.linux_distribution", return_value=("Suse", "11", "Final")):
  from shpurdp_agent import ShpurdpAgent


class TestShpurdpAgent(unittest.TestCase):
  @patch.object(subprocess, "Popen")
  @patch("os.path.isfile")
  @patch("os.remove")
  def test_main(self, os_remove_mock, os_path_isfile_mock, subprocess_popen_mock):
    facter1 = MagicMock()
    facter2 = MagicMock()
    subprocess_popen_mock.side_effect = [facter1, facter2]
    facter1.returncode = 77
    facter2.returncode = 55
    os_path_isfile_mock.return_value = True
    if not ("PYTHON" in os.environ):
      os.environ["PYTHON"] = "test/python/path"
    sys.argv[0] = "test data"
    ShpurdpAgent.main()

    self.assertTrue(subprocess_popen_mock.called)
    self.assertTrue(subprocess_popen_mock.call_count == 2)
    self.assertTrue(facter1.communicate.called)
    self.assertTrue(facter2.communicate.called)
    self.assertTrue(os_path_isfile_mock.called)
    self.assertTrue(os_path_isfile_mock.call_count == 2)
    self.assertTrue(os_remove_mock.called)

  #
  # Test ShpurdpConfig.getLogFile() for shpurdp-agent
  #
  def test_logfile_location(self):
    #
    # Test without $SHPURDP_AGENT_LOG_DIR
    #
    log_folder = "/var/log/shpurdp-agent"
    log_file = "shpurdp-agent.log"
    with patch.dict("os.environ", {}):
      self.assertEqual(
        os.path.join(log_folder, log_file), ShpurdpConfig.ShpurdpConfig.getLogFile()
      )

    #
    # Test with $SHPURDP_AGENT_LOG_DIR
    #
    log_folder = "/myloglocation/log"
    log_file = "shpurdp-agent.log"
    with patch.dict("os.environ", {"SHPURDP_AGENT_LOG_DIR": log_folder}):
      self.assertEqual(
        os.path.join(log_folder, log_file), ShpurdpConfig.ShpurdpConfig.getLogFile()
      )
    pass

  #
  # Test ShpurdpConfig.getOutFile() for shpurdp-agent
  #
  def test_outfile_location(self):
    #
    # Test without $SHPURDP_AGENT_OUT_DIR
    #
    out_folder = "/var/log/shpurdp-agent"
    out_file = "shpurdp-agent.out"
    with patch.dict("os.environ", {}):
      self.assertEqual(
        os.path.join(out_folder, out_file), ShpurdpConfig.ShpurdpConfig.getOutFile()
      )

    #
    # Test with $SHPURDP_AGENT_OUT_DIR
    #
    out_folder = "/myoutlocation/out"
    out_file = "shpurdp-agent.out"
    with patch.dict("os.environ", {"SHPURDP_AGENT_LOG_DIR": out_folder}):
      self.assertEqual(
        os.path.join(out_folder, out_file), ShpurdpConfig.ShpurdpConfig.getOutFile()
      )
    pass
