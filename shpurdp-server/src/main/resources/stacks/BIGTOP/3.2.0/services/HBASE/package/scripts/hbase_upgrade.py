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

import sys
from resource_management.libraries.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute


class HbaseMasterUpgrade(Script):
  def take_snapshot(self, env):
    import params

    snap_cmd = f"echo 'snapshot_all' | {params.hbase_cmd} shell"

    exec_cmd = f"{params.kinit_cmd} {snap_cmd}"

    Execute(exec_cmd, user=params.hbase_user)

  def restore_snapshot(self, env):
    import params

    print("TODO SHPURDP-12698")


if __name__ == "__main__":
  HbaseMasterUpgrade().execute()
