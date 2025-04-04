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
from resource_management.core.logger import Logger
from resource_management.libraries.execution_command import execution_command

import sys
import json

command_data_file = "TestExecutionCommand_command.json"


class TestExecutionCommand(TestCase):
  def setUp(self):
    Logger.initialize_logger()
    try:
      with open(command_data_file) as f:
        self.__execution_command = execution_command.ExecutionCommand(json.load(f))
        from resource_management.libraries.script import Script

        Script.execution_command = self.__execution_command
        Script.module_configs = Script.get_module_configs()
        Script.stack_settings = Script.get_stack_settings()
        Script.cluster_settings = Script.get_cluster_settings()
    except IOError:
      Logger.error("Can not read json file with command parameters: ")
      sys.exit(1)

  def test_get_module_name(self):
    module_name = self.__execution_command.get_module_name()
    self.assertEqual(module_name, "HDFS")

  # TODO : Check if this will be part of _hosts info in clusterHostInfo.
  """
    def test_get_oozie_server_hosts(self):
        oozie_server = self.__execution_command.get_component_hosts('oozie_server')
        self.assertEqual(oozie_server, 'host2')

    def test_get_ganglia_server_hosts(self):
        ganglia_server_hosts = self.__execution_command.get_component_hosts('ganglia_server')
        self.assertEqual(ganglia_server_hosts, 'host1')
    """

  def test_get_java_version(self):
    java_version = self.__execution_command.get_java_version()
    self.assertEqual(java_version, 8)

  def test_get_module_configs(self):
    module_configs = self.__execution_command.get_module_configs()
    self.assertNotEqual(module_configs, None)

    zookeeper_client_port = module_configs.get_property_value(
      "zookeeper", "zoo.cfg", "clientPort"
    )
    self.assertEqual(int(zookeeper_client_port), 2181)

    zookeeper_client_port_fake = module_configs.get_property_value(
      "zookeeper", "zoo.cfg", "clientPort1"
    )
    self.assertEqual(zookeeper_client_port_fake, None)

    zookeeper_client_port_default_value = module_configs.get_property_value(
      "zookeeper", "zoo.cfg", "clientPort1", 1111
    )
    self.assertEqual(int(zookeeper_client_port_default_value), 1111)

    zookeeper_empty_value = module_configs.get_all_properties("zookeeper", "zoo_fake")
    self.assertEqual(zookeeper_empty_value, {})

    zookeeper_log_max_backup_size = module_configs.get_property_value(
      "zookeeper", "zookeeper-log4j", "zookeeper_log_max_backup_size", 10
    )
    self.assertEqual(zookeeper_log_max_backup_size, "10")

    properties = module_configs.get_properties(
      "zookeeper", "zoo.cfg", ["clientPort", "dataDir", "fake"]
    )
    self.assertEqual(int(properties.get("clientPort")), 2181)
    self.assertEqual(properties.get("fake"), None)

    sqoop = bool(module_configs.get_all_properties("zookeeper", "sqoop-env"))
    self.assertFalse(sqoop)

  def test_access_to_module_configs(self):
    module_configs = self.__execution_command.get_module_configs()

    is_zoo_cfg_there = bool(module_configs.get_all_properties("zookeeper", "zoo.cfg"))
    self.assertTrue(is_zoo_cfg_there)

    zoo_cfg = module_configs.get_all_properties("zookeeper", "zoo.cfg")
    self.assertTrue(isinstance(zoo_cfg, dict))

  def test_null_value(self):
    versions = self.__execution_command.get_value("Versions")
    self.assertEqual(versions, None)

    versions = self.__execution_command.get_value("Versions", "1.1.1.a")
    self.assertEqual(versions, "1.1.1.a")

    module_configs = self.__execution_command.get_module_configs()
    version = module_configs.get_property_value("zookeeper", "zoo.cfg", "version")
    self.assertEqual(version, None)

    version = module_configs.get_property_value(
      "zookeeper", "zoo.cfg", "version", "3.0.b"
    )
    self.assertEqual(version, "3.0.b")

  def test_access_to_stack_settings(self):
    stack_settings = self.__execution_command.get_stack_settings()

    stack_name = stack_settings.get_mpack_name()
    self.assertEqual(stack_name, "HDP")

    stack_features = stack_settings.get_stack_features()
    self.assertTrue("snappy" in stack_features)

    stack_package = stack_settings.get_stack_packages()
    self.assertTrue("ACCUMULO" in stack_package)

    stack_tools = stack_settings.get_stack_tools()
    self.assertTrue("conf_selector" in stack_tools)

  def test_access_to_cluster_settings(self):
    cluster_settings = self.__execution_command.get_cluster_settings()

    security_enabled = cluster_settings.is_cluster_security_enabled()
    self.assertFalse(security_enabled)

    recovery_count = cluster_settings.get_recovery_max_count()
    self.assertEqual(recovery_count, 6)

    recovery_enabled = cluster_settings.check_recovery_enabled()
    self.assertTrue(recovery_enabled)

    recovery_type = cluster_settings.get_recovery_type()
    self.assertEqual(recovery_type, "AUTO_START")

    kerberos_domain = cluster_settings.get_kerberos_domain()
    self.assertEqual(kerberos_domain, "EXAMPLE.COM")

    smoke_user = cluster_settings.get_smokeuser()
    self.assertEqual(smoke_user, "shpurdp-qa")

    user_group = cluster_settings.get_user_group()
    self.assertEqual(user_group, "hadoop")

    suse_rhel_template = cluster_settings.get_repo_suse_rhel_template()
    self.assertTrue("if mirror_list" in suse_rhel_template)

    ubuntu_template = cluster_settings.get_repo_ubuntu_template()
    self.assertTrue("package_type" in ubuntu_template)

    override_uid = cluster_settings.check_override_uid()
    self.assertTrue(override_uid)

    skip_copy = cluster_settings.check_sysprep_skip_copy_fast_jar_hdfs()
    self.assertFalse(skip_copy)

    skip_lzo = cluster_settings.check_sysprep_skip_lzo_package_operations()
    self.assertFalse(skip_lzo)

    skip_setup_jce = cluster_settings.check_sysprep_skip_setup_jce()
    self.assertFalse(skip_setup_jce)

    ignored = cluster_settings.check_ignore_groupsusers_create()
    self.assertFalse(ignored)

  def test_access_to_execution_command(self):
    exec_cmd = self.__execution_command

    comp_type = exec_cmd.get_component_type()
    self.assertEqual(comp_type, "NAMENODE")

    comp_name = exec_cmd.get_component_instance_name()
    self.assertEqual(comp_name, "HDFS")

    cluster_name = exec_cmd.get_cluster_name()
    self.assertEqual(cluster_name, "c1")

    repo_file = exec_cmd.get_repository_file()
    expected = {
      "resolved": True,
      "repoVersion": "3.0.1.0-187",
      "repositories": [
        {
          "mirrorsList": None,
          "tags": [],
          "shpurdpManaged": True,
          "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos7/3.x/BUILDS/3.0.1.0-187",
          "repoName": "HDP",
          "components": None,
          "distribution": None,
          "repoId": "HDP-3.0-repo-1",
          "applicableServices": [],
        },
        {
          "mirrorsList": None,
          "tags": ["GPL"],
          "shpurdpManaged": True,
          "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/HDP-GPL/centos7/3.x/BUILDS/3.0.1.0-187",
          "repoName": "HDP-GPL",
          "components": None,
          "distribution": None,
          "repoId": "HDP-3.0-GPL-repo-1",
          "applicableServices": [],
        },
        {
          "mirrorsList": None,
          "tags": [],
          "shpurdpManaged": True,
          "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos7",
          "repoName": "HDP-UTILS",
          "components": None,
          "distribution": None,
          "repoId": "HDP-UTILS-1.1.0.22-repo-1",
          "applicableServices": [],
        },
      ],
      "feature": {"preInstalled": False, "scoped": True},
      "stackName": "HDP",
      "repoVersionId": 1,
      "repoFileName": "shpurdp-hdp-1",
    }
    self.assertDictEqual(expected, repo_file)

    local_components = exec_cmd.get_local_components()
    expected = ["NAMENODE", "ZOOKEEPER_SERVER"]
    self.assertListEqual(expected, local_components)

    role_cmd = exec_cmd.get_role_command()
    self.assertEqual(role_cmd, "START")

    jdk_loc = exec_cmd.get_jdk_location()
    self.assertEqual(jdk_loc, "http://host-1.openstacklocal:8080/resources")

    jdk_name = exec_cmd.get_jdk_name()
    self.assertEqual(jdk_name, "jdk-8u112-linux-x64.tar.gz")

    java_ver = exec_cmd.get_java_version()
    self.assertEqual(java_ver, 8)

    jce_name = exec_cmd.get_jce_name()
    self.assertEqual(jce_name, "jce_policy-8.zip")

    db_driver_file_name = exec_cmd.get_db_driver_file_name()
    self.assertEqual(db_driver_file_name, "mysql-connector-java.jar")

    db_name = exec_cmd.get_db_name()
    self.assertEqual(db_name, "shpurdp")

    oracle_jdbc_url = exec_cmd.get_oracle_jdbc_url()
    self.assertEqual(
      oracle_jdbc_url, "http://host-1.openstacklocal:8080/resources/ojdbc6.jar"
    )

    mysql_jdbc_url = exec_cmd.get_mysql_jdbc_url()
    self.assertEqual(
      mysql_jdbc_url,
      "http://host-1.openstacklocal:8080/resources/mysql-connector-java.jar",
    )

    agent_stack_retry_count = exec_cmd.get_agent_stack_retry_count()
    self.assertEqual(agent_stack_retry_count, 5)

    agent_stack_want_retry_on_unavailability = (
      exec_cmd.check_agent_stack_want_retry_on_unavailability()
    )
    self.assertEqual(agent_stack_want_retry_on_unavailability, "false")

    shpurdp_server_host = exec_cmd.get_shpurdp_server_host()
    self.assertEqual(shpurdp_server_host, "host-1.openstacklocal")

    shpurdp_server_port = exec_cmd.get_shpurdp_server_port()
    self.assertEqual(shpurdp_server_port, "8080")

    shpurdp_server_use_ssl = exec_cmd.is_shpurdp_server_use_ssl()
    self.assertEqual(shpurdp_server_use_ssl, "false")

    gpl_license_accepted = exec_cmd.is_gpl_license_accepted()
    self.assertEqual(gpl_license_accepted, "false")

    mpack_name = exec_cmd.get_mpack_name()
    self.assertEqual(mpack_name, "HDP")

    mpack_version = exec_cmd.get_mpack_version()
    self.assertEqual(mpack_version, "3.0")

    user_groups = exec_cmd.get_user_groups()
    self.assertEqual(
      user_groups,
      '{"zookeeper":["hadoop"],"shpurdp-qa":["hadoop","users"],"hdfs":["hdfs","hadoop"]}',
    )
    group_list = exec_cmd.get_group_list()
    self.assertEqual(group_list, '["hdfs","hadoop","users"]')

    user_list = exec_cmd.get_user_list()
    self.assertEqual(user_list, '["zookeeper","shpurdp-qa","hdfs"]')

    host_name = exec_cmd.get_host_name()
    self.assertEqual(host_name, "host-1.openstacklocal")

    agent_cache_dir = exec_cmd.get_agent_cache_dir()
    self.assertEqual(agent_cache_dir, "/var/lib/shpurdp-agent/cache")

    agent_config_execute_in_parallel = exec_cmd.check_agent_config_execute_in_parallel()
    self.assertEqual(agent_config_execute_in_parallel, 0)

    agent_proxy_settings = exec_cmd.check_agent_proxy_settings()
    self.assertEqual(agent_proxy_settings, True)

    unlimited_key_jce_required = exec_cmd.check_unlimited_key_jce_required()
    self.assertEqual(unlimited_key_jce_required, "false")

    new_mpack_version_for_upgrade = exec_cmd.get_new_mpack_version_for_upgrade()
    self.assertEqual(new_mpack_version_for_upgrade, "3.0.1.0-187")

    command_retry_enabled = exec_cmd.check_command_retry_enabled()
    self.assertEqual(command_retry_enabled, "false")

    dfs_type = exec_cmd.get_dfs_type()
    self.assertEqual(dfs_type, "HDFS")

    module_package_folder = exec_cmd.get_module_package_folder()
    self.assertEqual(module_package_folder, "stacks/HDP/3.0/services/HDFS/package")

    component_hosts = exec_cmd.get_component_hosts("secondary_namenode")
    self.assertEqual(component_hosts, ["host-2.openstacklocal"])

    component_hosts1 = exec_cmd.get_component_hosts("zookeeper_server")
    self.assertListEqual(
      component_hosts1, ["host-2.openstacklocal", "host-1.openstacklocal"]
    )

    all_hosts = exec_cmd.get_all_hosts()
    self.assertEqual(all_hosts, ["host-2.openstacklocal", "host-1.openstacklocal"])

    all_racks = exec_cmd.get_all_racks()
    self.assertEqual(all_racks, ["/default-rack", "/default-rack"])

    all_ipv4_ips = exec_cmd.get_all_ipv4_ips()
    self.assertEqual(all_ipv4_ips, ["10.10.10.10", "10.10.10.11"])
