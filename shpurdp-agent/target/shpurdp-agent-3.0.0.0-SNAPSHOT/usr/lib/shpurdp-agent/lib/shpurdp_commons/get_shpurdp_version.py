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
import configparser
from resource_management.core.logger import Logger

"""
returns the shpurdp version on an agent host
"""


def get_shpurdp_version_agent():
  shpurdp_version = None
  SHPURDP_AGENT_CONF = "/etc/shpurdp-agent/conf/shpurdp-agent.ini"
  if os.path.exists(SHPURDP_AGENT_CONF):
    try:
      shpurdp_agent_config = configparser.RawConfigParser()
      shpurdp_agent_config.read(SHPURDP_AGENT_CONF)
      data_dir = shpurdp_agent_config.get("agent", "prefix")
      ver_file = os.path.join(data_dir, "version")
      with open(ver_file, "r") as f:
        shpurdp_version = f.read().strip()
    except Exception as e:
      Logger.info("Unable to determine shpurdp version from the agent version file.")
      Logger.debug(f"Exception: {str(e)}")
      pass
    pass
  return shpurdp_version
