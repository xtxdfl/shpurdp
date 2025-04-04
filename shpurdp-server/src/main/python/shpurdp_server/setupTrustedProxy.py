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

import shpurdp_simplejson as json
import http.client
import os
import re
import urllib.request, urllib.error, urllib.parse

from shpurdp_commons.exceptions import FatalException, NonFatalException
from shpurdp_commons.logging_utils import get_silent, print_info_msg
from shpurdp_server.serverConfiguration import get_shpurdp_properties
from shpurdp_server.serverUtils import (
  is_server_runing,
  get_shpurdp_admin_username_password_pair,
  get_cluster_name,
  perform_changes_via_rest_api,
  get_json_via_rest_api,
  get_value_from_dictionary,
)
from shpurdp_server.setupSecurity import REGEX_TRUE_FALSE
from shpurdp_server.userInput import get_validated_string_input, get_YN_input


TPROXY_SUPPORT_ENABLED = "shpurdp.tproxy.authentication.enabled"
PROXYUSER_HOSTS = "shpurdp.tproxy.proxyuser.{}.hosts"
PROXYUSER_USERS = "shpurdp.tproxy.proxyuser.{}.users"
PROXYUSER_GROUPS = "shpurdp.tproxy.proxyuser.{}.groups"

TPROXY_CONFIG_API_ENTRYPOINT = (
  "services/SHPURDP/components/SHPURDP_SERVER/configurations/tproxy-configuration"
)

REGEX_ANYTHING = ".*"
WILDCARD_FOR_ALL = "*"


def get_trusted_proxy_properties(shpurdp_properties, admin_login, admin_password):
  print_info_msg("Fetching Trusted Proxy configuration from DB")

  try:
    response_code, json_data = get_json_via_rest_api(
      shpurdp_properties, admin_login, admin_password, TPROXY_CONFIG_API_ENTRYPOINT
    )
  except urllib.error.HTTPError as http_error:
    if http_error.code == http.client.NOT_FOUND:
      # This means that there is no Trusted Proxy configuration in the database yet -> we can not fetch the properties; but this is NOT an error
      json_data = None
    else:
      raise http_error

  return json_data.get("Configuration", {}).get("properties", {}) if json_data else {}


def populate_tproxy_configuration_property(
  properties, tproxy_user_name, property_name, question_text_qualifier
):
  resolved_property_name = property_name.format(tproxy_user_name)
  resolved_property_value = get_value_from_dictionary(
    properties, resolved_property_name, WILDCARD_FOR_ALL
  )
  resolved_property_value = get_validated_string_input(
    f"Allowed {question_text_qualifier} for {tproxy_user_name} ({resolved_property_value})? ",
    resolved_property_value,
    REGEX_ANYTHING,
    "Invalid input",
    False,
  )
  properties[resolved_property_name] = resolved_property_value


def add_new_trusted_proxy_config(properties):
  tproxy_user_name = get_validated_string_input(
    "The proxy user's (local) username? ",
    None,
    REGEX_ANYTHING,
    "Invalid Trusted Proxy User Name",
    False,
    allowEmpty=False,
  )
  populate_tproxy_configuration_property(
    properties, tproxy_user_name, PROXYUSER_HOSTS, "hosts"
  )
  populate_tproxy_configuration_property(
    properties, tproxy_user_name, PROXYUSER_USERS, "users"
  )
  populate_tproxy_configuration_property(
    properties, tproxy_user_name, PROXYUSER_GROUPS, "groups"
  )
  return get_YN_input("Add another proxy user [y/n] (n)? ", False)


def parse_trusted_configuration_file(tproxy_configuration_file_path, properties):
  with open(tproxy_configuration_file_path) as tproxy_configuration_file:
    tproxy_configurations = json.loads(tproxy_configuration_file.read())

  if tproxy_configurations:
    for tproxy_configuration in tproxy_configurations:
      tproxy_user_name = tproxy_configuration["proxyuser"]
      properties[PROXYUSER_HOSTS.format(tproxy_user_name)] = tproxy_configuration[
        "hosts"
      ]
      properties[PROXYUSER_USERS.format(tproxy_user_name)] = tproxy_configuration[
        "users"
      ]
      properties[PROXYUSER_GROUPS.format(tproxy_user_name)] = tproxy_configuration[
        "groups"
      ]


def update_tproxy_conf(
  shpurdp_properties, tproxy_configuration_properties, admin_login, admin_password
):
  request_data = {
    "Configuration": {"category": "tproxy-configuration", "properties": {}}
  }
  request_data["Configuration"]["properties"] = tproxy_configuration_properties
  perform_changes_via_rest_api(
    shpurdp_properties,
    admin_login,
    admin_password,
    TPROXY_CONFIG_API_ENTRYPOINT,
    "PUT",
    request_data,
  )


def remove_tproxy_conf(shpurdp_properties, admin_login, admin_password):
  perform_changes_via_rest_api(
    shpurdp_properties,
    admin_login,
    admin_password,
    TPROXY_CONFIG_API_ENTRYPOINT,
    "DELETE",
  )


def validate_options(options):
  errors = []
  if options.tproxy_enabled and not re.match(REGEX_TRUE_FALSE, options.tproxy_enabled):
    errors.append("--tproxy-enabled should be to either 'true' or 'false'")

  if (
    options.tproxy_configuration_file_path
    and options.tproxy_configuration_file_path is not None
  ):
    if not os.path.isfile(options.tproxy_configuration_file_path):
      errors.append(
        f"--tproxy-configuration-file-path is set to a non-existing file: {options.tproxy_configuration_file_path}"
      )

  if len(errors) > 0:
    error_msg = "The following errors occurred while processing your request: {0}"
    raise FatalException(1, error_msg.format(str(errors)))


def setup_trusted_proxy(options):
  print_info_msg("Setup Trusted Proxy")

  server_status, pid = is_server_runing()
  if not server_status:
    err = "Shpurdp Server is not running."
    raise FatalException(1, err)

  if not get_silent():
    validate_options(options)

    shpurdp_properties = get_shpurdp_properties()

    admin_login, admin_password = get_shpurdp_admin_username_password_pair(options)
    properties = get_trusted_proxy_properties(
      shpurdp_properties, admin_login, admin_password
    )

    if not options.tproxy_enabled:
      tproxy_support_enabled = get_value_from_dictionary(
        properties, TPROXY_SUPPORT_ENABLED
      )

      if tproxy_support_enabled:
        if "true" == tproxy_support_enabled:
          tproxy_status = "enabled"
        else:
          tproxy_status = "disabled"
      else:
        tproxy_status = "not configured"
      print_info_msg(f"\nTrusted Proxy support is currently {tproxy_status}\n")

      if tproxy_status == "enabled":
        enable_tproxy = not get_YN_input(
          "Do you want to disable Trusted Proxy support [y/n] (n)? ", False
        )
      elif get_YN_input(
        "Do you want to configure Trusted Proxy Support [y/n] (y)? ", True
      ):
        enable_tproxy = True
      else:
        return False
    else:
      enable_tproxy = options.tproxy_enabled == "true"

    if enable_tproxy:
      properties[TPROXY_SUPPORT_ENABLED] = "true"
      if not options.tproxy_configuration_file_path:
        add_new_trusted_proxy = add_new_trusted_proxy_config(properties)
        while add_new_trusted_proxy:
          add_new_trusted_proxy = add_new_trusted_proxy_config(properties)
      else:
        parse_trusted_configuration_file(
          options.tproxy_configuration_file_path, properties
        )

      update_tproxy_conf(shpurdp_properties, properties, admin_login, admin_password)
    else:
      remove_tproxy_conf(shpurdp_properties, admin_login, admin_password)

  else:
    warning = "setup-trusted-proxy is not enabled in silent mode."
    raise NonFatalException(warning)
  pass
