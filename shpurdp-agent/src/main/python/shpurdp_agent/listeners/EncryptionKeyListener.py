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

import logging

from shpurdp_agent.listeners import EventListener
from shpurdp_agent import Constants

logger = logging.getLogger(__name__)


class EncryptionKeyListener(EventListener):
  """
  Listener of Constants.ENCRYPTION_KEY_TOPIC events from server.
  """

  def __init__(self, initializer_module):
    super(EncryptionKeyListener, self).__init__(initializer_module)

  def on_event(self, headers, message):
    logger.info("EncryptionKey received")
    self.initializer_module.customServiceOrchestrator.encryption_key = message[
      "encryptionKey"
    ]

  def get_handled_path(self):
    return Constants.ENCRYPTION_KEY_TOPIC
