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

import sys
import os.path
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.source import InlineTemplate, StaticFile
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.stack_features import check_stack_feature
from shpurdp_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.functions.setup_atlas_hook import (
  has_atlas_in_cluster,
  setup_atlas_hook,
)
from shpurdp_commons import OSConst
from shpurdp_commons.constants import SERVICE


def webhcat():
  import params

  Directory(
    params.templeton_pid_dir,
    owner=params.webhcat_user,
    mode=0o755,
    group=params.user_group,
    create_parents=True,
  )

  Directory(
    params.templeton_log_dir,
    owner=params.webhcat_user,
    mode=0o755,
    group=params.user_group,
    create_parents=True,
  )

  Directory(
    params.webhcat_conf_dir,
    create_parents=True,
    owner=params.webhcat_user,
    group=params.user_group,
    cd_access="a",
  )

  # Replace _HOST with hostname in relevant principal-related properties
  webhcat_site = params.config["configurations"]["webhcat-site"].copy()
  for prop_name in ["templeton.hive.properties", "templeton.kerberos.principal"]:
    if prop_name in webhcat_site:
      webhcat_site[prop_name] = webhcat_site[prop_name].replace(
        "_HOST", params.hostname
      )

  XmlConfig(
    "webhcat-site.xml",
    conf_dir=params.webhcat_conf_dir,
    configurations=webhcat_site,
    configuration_attributes=params.config["configurationAttributes"]["webhcat-site"],
    owner=params.webhcat_user,
    group=params.user_group,
  )

  # if we're in an upgrade of a secure cluster, make sure hive-site and yarn-site are created
  if (
    check_stack_feature(
      StackFeature.CONFIG_VERSIONING, params.stack_version_formatted_major
    )
    and params.version
    and params.stack_root
  ):
    XmlConfig(
      "hive-site.xml",
      conf_dir=format("{stack_root}/{version}/hive/conf"),
      configurations=params.config["configurations"]["hive-site"],
      configuration_attributes=params.config["configurationAttributes"]["hive-site"],
      owner=params.hive_user,
      group=params.user_group,
    )

    XmlConfig(
      "yarn-site.xml",
      conf_dir=format("{stack_root}/{version}/hadoop/conf"),
      configurations=params.config["configurations"]["yarn-site"],
      configuration_attributes=params.config["configurationAttributes"]["yarn-site"],
      owner=params.yarn_user,
      group=params.user_group,
    )

  File(
    format("{webhcat_conf_dir}/webhcat-env.sh"),
    owner=params.webhcat_user,
    group=params.user_group,
    content=InlineTemplate(params.webhcat_env_sh_template),
  )

  Directory(params.webhcat_conf_dir, cd_access="a", create_parents=True)

  log4j_webhcat_filename = "webhcat-log4j.properties"
  if params.log4j_webhcat_props != None:
    File(
      format("{webhcat_conf_dir}/{log4j_webhcat_filename}"),
      mode=0o644,
      group=params.user_group,
      owner=params.webhcat_user,
      content=InlineTemplate(params.log4j_webhcat_props),
    )
  elif os.path.exists("{webhcat_conf_dir}/{log4j_webhcat_filename}.template"):
    File(
      format("{webhcat_conf_dir}/{log4j_webhcat_filename}"),
      mode=0o644,
      group=params.user_group,
      owner=params.webhcat_user,
      content=StaticFile(
        format("{webhcat_conf_dir}/{log4j_webhcat_filename}.template")
      ),
    )

  # Generate atlas-application.properties.xml file
  if params.enable_atlas_hook:
    # WebHCat uses a different config dir than the rest of the daemons in Hive.
    atlas_hook_filepath = os.path.join(
      params.webhcat_conf_dir, params.atlas_hook_filename
    )
    setup_atlas_hook(
      SERVICE.HIVE,
      params.hive_atlas_application_properties,
      atlas_hook_filepath,
      params.hive_user,
      params.user_group,
    )
