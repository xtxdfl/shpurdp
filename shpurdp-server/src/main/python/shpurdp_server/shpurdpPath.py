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

import os
import re

SHPURDP_SERVER_ROOT_ENV_VARIABLE = "ROOT"


class ShpurdpPath:
  root_directory = os.getenv(SHPURDP_SERVER_ROOT_ENV_VARIABLE, "/")

  @staticmethod
  def get(path):
    """
    Any paths which to shpurdp-server files, should be wrapped with this function call.
    Which is needed for the situations when shpurdp-server installed not in / but in other folder like /opt.
    Not shpurdp paths like /var/run/postgresql SHOULD NOT wrapped by this call though.
    """
    # return os.path.realpath(ShpurdpPath.root_directory + os.sep + path) # realpath won't replace slashes for python2.6
    return re.sub("/+", "/", ShpurdpPath.root_directory + os.sep + path)
