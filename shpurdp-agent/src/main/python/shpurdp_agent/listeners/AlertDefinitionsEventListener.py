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
import shpurdp_stomp

from shpurdp_agent.listeners import EventListener
from shpurdp_agent import Constants

logger = logging.getLogger(__name__)


class AlertDefinitionsEventListener(EventListener):
  """
  Listener of Constants.ALERTS_DEFINITIONS_TOPIC events from server.
  """

  def __init__(self, initializer_module):
    super(AlertDefinitionsEventListener, self).__init__(initializer_module)
    self.alert_definitions_cache = initializer_module.alert_definitions_cache
    self.alert_scheduler_handler = initializer_module.alert_scheduler_handler

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.ALERTS_DEFINITIONS_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    # this kind of response is received if hash was identical. And server does not need to change anything
    if message == {}:
      return

    event_type = message["eventType"]

    if event_type == "CREATE":
      self.alert_definitions_cache.rewrite_cache(message["clusters"], message["hash"])
    elif event_type == "UPDATE":
      self.alert_definitions_cache.cache_update(message["clusters"], message["hash"])
    elif event_type == "DELETE":
      self.alert_definitions_cache.cache_delete(message["clusters"], message["hash"])
    else:
      logger.error("Unknown event type '{0}' for alert event")

    self.alert_scheduler_handler.update_definitions(event_type)

  def get_handled_path(self):
    return Constants.ALERTS_DEFINITIONS_TOPIC

  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type.
    Usually should be used if full dict is too big for logs and should shortened or made more readable
    """
    try:
      for cluster_id in message_json["clusters"]:
        for alert_definition in message_json["clusters"][cluster_id][
          "alertDefinitions"
        ]:
          if "source" in alert_definition:
            alert_definition["source"] = "..."
    except KeyError:
      pass

    return super(AlertDefinitionsEventListener, self).get_log_message(
      headers, message_json
    )
