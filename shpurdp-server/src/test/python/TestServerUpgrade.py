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

os.environ["ROOT"] = ""
import io
import sys
from shpurdp_commons.exceptions import FatalException
from unittest import TestCase
from mock.mock import patch, MagicMock
from shpurdp_commons import os_utils
import platform
import distro
import urllib.request, urllib.parse, urllib.error

import shutil

project_dir = os.path.join(
  os.path.abspath(os.path.dirname(__file__)), os.path.normpath("../../../../")
)
shutil.copyfile(
  project_dir + "/shpurdp-server/conf/unix/shpurdp.properties", "/tmp/shpurdp.properties"
)

_search_file = os_utils.search_file
os_utils.search_file = MagicMock(return_value="/tmp/shpurdp.properties")
with patch.object(
  distro,
  "linux_distribution",
  return_value=MagicMock(return_value=("Redhat", "6.4", "Final")),
):
  with patch("os.path.isdir", return_value=MagicMock(return_value=True)):
    with patch("os.access", return_value=MagicMock(return_value=True)):
      with patch.object(
        os_utils,
        "parse_log4j_file",
        return_value={"shpurdp.log.dir": "/var/log/shpurdp-server"},
      ):
        from shpurdp_server.serverUpgrade import set_current, SetCurrentVersionOptions
        import shpurdp_server

os_utils.search_file = _search_file


@patch.object(
  distro, "linux_distribution", new=MagicMock(return_value=("Redhat", "6.4", "Final"))
)
@patch("os.path.isdir", new=MagicMock(return_value=True))
@patch("os.access", new=MagicMock(return_value=True))
class TestServerUpgrade(TestCase):
  @patch("shpurdp_server.serverUpgrade.is_server_runing")
  @patch("shpurdp_server.serverUpgrade.SetCurrentVersionOptions.no_finalize_options_set")
  @patch("shpurdp_server.serverUpgrade.get_validated_string_input")
  @patch("shpurdp_server.serverUpgrade.get_shpurdp_properties")
  @patch("shpurdp_server.serverUtils.get_shpurdp_server_api_base")
  @patch("shpurdp_commons.logging_utils.get_verbose")
  @patch("urllib.request.urlopen")
  def test_set_current(
    self,
    urlopen_mock,
    get_verbose_mock,
    get_shpurdp_server_api_base_mock,
    get_shpurdp_properties_mock,
    get_validated_string_input_mock,
    no_finalize_options_set_mock,
    is_server_runing_mock,
  ):
    options = MagicMock()
    options.cluster_name = "cc"
    options.desired_repo_version = "HDP-2.2.2.0-2561"
    options.force_repo_version = None

    # Case when server is not running
    is_server_runing_mock.return_value = False, None
    try:
      set_current(options)
      self.fail("Server is not running - should error out")
    except FatalException:
      pass  # expected

    is_server_runing_mock.return_value = True, 11111

    # Test insufficient options case
    no_finalize_options_set_mock.return_value = True
    try:
      set_current(options)
      self.fail("Should error out")
    except FatalException:
      pass  # expected

    no_finalize_options_set_mock.return_value = False

    # Test normal flow
    get_validated_string_input_mock.return_value = "dummy_string"

    p = get_shpurdp_properties_mock.return_value
    p.get_property.side_effect = ["8080", "false", "false"]

    get_shpurdp_server_api_base_mock.return_value = "http://127.0.0.1:8080/api/v1/"
    get_verbose_mock.retun_value = False

    set_current(options)

    self.assertTrue(urlopen_mock.called)
    request = urlopen_mock.call_args_list[0][0][0]
    self.assertEqual(
      request.get_full_url(), "http://127.0.0.1:8080/api/v1/clusters/cc/stack_versions"
    )
    self.assertEqual(
      request.data,
      '{"ClusterStackVersions": {"repository_version": "HDP-2.2.2.0-2561", "state": "CURRENT", "force": false}}',
    )
    self.assertEqual(request.origin_req_host, "127.0.0.1")
    self.assertEqual(
      request.headers,
      {
        "X-requested-by": "shpurdp",
        "Authorization": "Basic ZHVtbXlfc3RyaW5nOmR1bW15X3N0cmluZw==",
      },
    )

  @patch("shpurdp_server.serverUpgrade.is_server_runing")
  @patch("shpurdp_server.serverUpgrade.SetCurrentVersionOptions.no_finalize_options_set")
  @patch("shpurdp_server.serverUpgrade.get_validated_string_input")
  @patch("shpurdp_server.serverUpgrade.get_shpurdp_properties")
  @patch("shpurdp_server.serverUtils.get_shpurdp_server_api_base")
  @patch("shpurdp_commons.logging_utils.get_verbose")
  @patch("urllib.request.urlopen")
  def test_set_current_with_force(
    self,
    urlopen_mock,
    get_verbose_mock,
    get_shpurdp_server_api_base_mock,
    get_shpurdp_properties_mock,
    get_validated_string_input_mock,
    no_finalize_options_set_mock,
    is_server_runing_mock,
  ):
    options = MagicMock()
    options.cluster_name = "cc"
    options.desired_repo_version = "HDP-2.2.2.0-2561"
    options.force_repo_version = True

    # Case when server is not running
    is_server_runing_mock.return_value = False, None
    try:
      set_current(options)
      self.fail("Server is not running - should error out")
    except FatalException:
      pass  # expected

    is_server_runing_mock.return_value = True, 11111

    # Test insufficient options case
    no_finalize_options_set_mock.return_value = True
    try:
      set_current(options)
      self.fail("Should error out")
    except FatalException:
      pass  # expected

    no_finalize_options_set_mock.return_value = False

    # Test normal flow
    get_validated_string_input_mock.return_value = "dummy_string"

    p = get_shpurdp_properties_mock.return_value
    p.get_property.side_effect = ["8080", "false", "false"]

    get_shpurdp_server_api_base_mock.return_value = "http://127.0.0.1:8080/api/v1/"
    get_verbose_mock.retun_value = False

    set_current(options)

    self.assertTrue(urlopen_mock.called)
    request = urlopen_mock.call_args_list[0][0][0]
    self.assertEqual(
      request.get_full_url(), "http://127.0.0.1:8080/api/v1/clusters/cc/stack_versions"
    )
    self.assertEqual(
      request.data,
      '{"ClusterStackVersions": {"repository_version": "HDP-2.2.2.0-2561", "state": "CURRENT", "force": true}}',
    )
    self.assertEqual(request.origin_req_host, "127.0.0.1")
    self.assertEqual(
      request.headers,
      {
        "X-requested-by": "shpurdp",
        "Authorization": "Basic ZHVtbXlfc3RyaW5nOmR1bW15X3N0cmluZw==",
      },
    )

  def testCurrentVersionOptions(self):
    # Negative test cases
    options = MagicMock()
    options.cluster_name = None
    options.desired_repo_version = "HDP-2.2.2.0-2561"
    cvo = SetCurrentVersionOptions(options)
    self.assertTrue(cvo.no_finalize_options_set())

    options = MagicMock()
    options.cluster_name = "cc"
    options.desired_repo_version = None
    cvo = SetCurrentVersionOptions(options)
    self.assertTrue(cvo.no_finalize_options_set())

    # Positive test case
    options = MagicMock()
    options.cluster_name = "cc"
    options.desired_repo_version = "HDP-2.2.2.0-2561"
    cvo = SetCurrentVersionOptions(options)
    self.assertFalse(cvo.no_finalize_options_set())
