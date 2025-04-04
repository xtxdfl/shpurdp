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

from shpurdp_agent import main

main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
import os
import tempfile
import shutil
from unittest import TestCase

from shpurdp_agent.security import CertificateManager
from shpurdp_agent import ShpurdpConfig
from mock.mock import patch, MagicMock
from shpurdp_commons import OSCheck
from only_for_platform import os_distro_value


class TestCertGeneration(TestCase):
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def setUp(self):
    self.tmpdir = tempfile.mkdtemp()
    config = ShpurdpConfig.ShpurdpConfig()
    config.set("server", "hostname", "example.com")
    config.set("server", "url_port", "777")
    config.set("security", "keysdir", self.tmpdir)
    config.set("security", "server_crt", "ca.crt")
    server_hostname = config.get("server", "hostname")
    self.certMan = CertificateManager(config, server_hostname)

  @patch.object(os, "chmod")
  def test_generation(self, chmod_mock):
    self.certMan.genAgentCrtReq("/dummy_dir/hostname.key")
    self.assertTrue(chmod_mock.called)
    self.assertTrue(os.path.exists(self.certMan.getAgentKeyName()))
    self.assertTrue(os.path.exists(self.certMan.getAgentCrtReqName()))

  def tearDown(self):
    shutil.rmtree(self.tmpdir)
