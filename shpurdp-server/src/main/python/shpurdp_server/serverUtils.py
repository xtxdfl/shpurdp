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

import base64
import os
import socket
import ssl
import urllib.request, urllib.error, urllib.parse
from contextlib import closing

import time
from shpurdp_commons.exceptions import FatalException, NonFatalException
from shpurdp_commons.logging_utils import get_verbose, print_info_msg, get_debug_mode
from shpurdp_commons.os_check import OSConst
from shpurdp_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from shpurdp_commons.os_utils import run_os_command

# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import shpurdp_simplejson as json
from shpurdp_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
from shpurdp_server.serverConfiguration import (
  configDefaults,
  PID_NAME,
  get_resources_location,
  get_stack_location,
  CLIENT_API_PORT,
  CLIENT_API_PORT_PROPERTY,
  SSL_API,
  DEFAULT_SSL_API_PORT,
  SSL_API_PORT,
)
from shpurdp_server.userInput import get_validated_string_input

# Shpurdp server API properties
SERVER_API_HOST = "127.0.0.1"
SERVER_API_PROTOCOL = "http"
SERVER_API_SSL_PROTOCOL = "https"


@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def is_server_runing():
  pid_file_path = os.path.join(configDefaults.PID_DIR, PID_NAME)

  if os.path.exists(pid_file_path):
    try:
      f = open(pid_file_path, "r")
    except IOError as ex:
      raise FatalException(1, str(ex))

    pid = f.readline().strip()

    if not pid.isdigit():
      err = f"'{pid}' is incorrect PID value. {pid_file_path} is corrupt. Removing"
      f.close()
      run_os_command("rm -f " + pid_file_path)
      raise NonFatalException(err)

    f.close()
    retcode, out, err = run_os_command("ps -p " + pid)
    if retcode == 0:
      return True, int(pid)
    else:
      return False, None
  else:
    return False, None


def wait_for_server_to_stop(wait_timeout):
  start_time = time.time()
  is_timeout = lambda: time.time() - start_time > wait_timeout

  while is_server_runing()[0] and not is_timeout():
    time.sleep(0.1)

  return not is_timeout()


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def is_server_runing():
  from shpurdp_commons.os_windows import (
    SERVICE_STATUS_STARTING,
    SERVICE_STATUS_RUNNING,
    SERVICE_STATUS_STOPPING,
    SERVICE_STATUS_STOPPED,
    SERVICE_STATUS_NOT_INSTALLED,
  )
  from shpurdp_windows_service import ShpurdpServerService

  statusStr = ShpurdpServerService.QueryStatus()
  if statusStr in (
    SERVICE_STATUS_STARTING,
    SERVICE_STATUS_RUNNING,
    SERVICE_STATUS_STOPPING,
  ):
    return True, ""
  elif statusStr == SERVICE_STATUS_STOPPED:
    return False, SERVICE_STATUS_STOPPED
  elif statusStr == SERVICE_STATUS_NOT_INSTALLED:
    return False, SERVICE_STATUS_NOT_INSTALLED
  else:
    return False, None


#
# Performs HDP stack housekeeping
#
def refresh_stack_hash(properties):
  resources_location = get_resources_location(properties)
  stacks_location = get_stack_location(properties)
  resource_files_keeper = ResourceFilesKeeper(resources_location, stacks_location)

  try:
    print(
      "Organizing resource files at {0}...".format(
        resources_location, verbose=get_verbose()
      )
    )
    resource_files_keeper.perform_housekeeping()
  except KeeperException as ex:
    msg = f"Can not organize resource files at {resources_location}: {str(ex)}"
    raise FatalException(-1, msg)


#
# Builds shpurdp-server API base url
# Reads server protocol/port from configuration
# And returns something like
# http://127.0.0.1:8080/api/v1/
# or if using ssl https://hostname.domain:8443/api/v1
#
def get_shpurdp_server_api_base(properties):
  api_host = SERVER_API_HOST
  api_protocol = SERVER_API_PROTOCOL
  api_port = CLIENT_API_PORT
  api_port_prop = properties.get_property(CLIENT_API_PORT_PROPERTY)
  if api_port_prop is not None and api_port_prop != "":
    api_port = api_port_prop

  api_ssl = is_api_ssl_enabled(properties)

  if api_ssl:
    api_host = socket.getfqdn()
    api_protocol = SERVER_API_SSL_PROTOCOL
    api_port = DEFAULT_SSL_API_PORT
    api_port_prop = properties.get_property(SSL_API_PORT)
    if api_port_prop is not None:
      api_port = api_port_prop
  return f"{api_protocol}://{api_host}:{api_port!s}/api/v1/"


def get_shpurdp_admin_username_password_pair(options):
  """
  Returns the Shpurdp administrator credential.
  If not supplied via command line options, the user is queried for the username and password.
  :param options: the collected command line options
  :return: the Shpurdp admin credentials
  """
  admin_login = (
    options.shpurdp_admin_username
    if hasattr(options, "shpurdp_admin_username")
    and options.shpurdp_admin_username is not None
    else get_validated_string_input(
      "Enter Shpurdp Admin login: ", None, None, None, False, False
    )
  )
  admin_password = (
    options.shpurdp_admin_password
    if hasattr(options, "shpurdp_admin_password")
    and options.shpurdp_admin_password is not None
    else get_validated_string_input(
      "Enter Shpurdp Admin password: ", None, None, None, True, False
    )
  )
  return admin_login, admin_password


def get_cluster_name(properties, admin_login, admin_password):
  """
  Fetches the name of the first cluster (in case there are more)
  from the response of host:port/api/v1/clusters call
  """
  print_info_msg("Fetching cluster name")

  cluster_name = None
  response_code, json_data = get_json_via_rest_api(
    properties, admin_login, admin_password, "clusters"
  )

  if json_data and "items" in json_data:
    items = json_data["items"]
    if len(items) > 0:
      cluster_name = items[0]["Clusters"]["cluster_name"]
      print_info_msg(f"Found cluster name: {cluster_name}")

  return cluster_name


def get_json_via_rest_api(properties, admin_login, admin_password, entry_point):
  """
  Fetches the data from a given REST API entry point

  :param properties: the properties from the shpurdp.properties file
  :param admin_login: an administrator's username used to log in to Shpurdp
  :param admin_password: an administrator's password used to log in to Shpurdp
  :param entry_point: the relative entry point to query (the base URL will be generated using the shpurdp.properties data)
  :return: HTTP status, JSON data
  """
  url = get_shpurdp_server_api_base(properties) + entry_point
  admin_auth = (
    base64.encodebytes(f"{admin_login}:{admin_password}".encode())
    .decode()
    .replace("\n", "")
  )
  request = urllib.request.Request(url)
  request.add_header("Authorization", f"Basic {admin_auth}")
  request.add_header("X-Requested-By", "shpurdp")
  request.get_method = lambda: "GET"

  print_info_msg("Fetching information from Shpurdp's REST API")

  with closing(
    urllib.request.urlopen(request, context=get_ssl_context(properties))
  ) as response:
    response_status_code = response.getcode()
    json_data = None
    print_info_msg(
      f"Received HTTP {response_status_code} while fetching information from Shpurdp's REST API"
    )
    if response_status_code == 200:
      json_data = json.loads(response.read())
      if get_debug_mode():
        print_info_msg("Received JSON:\n" + json_data)
    return response_status_code, json_data


def perform_changes_via_rest_api(
  properties, admin_login, admin_password, url_postfix, get_method, request_data=None
):
  url = get_shpurdp_server_api_base(properties) + url_postfix
  admin_auth = (
    base64.encodebytes(f"{admin_login}:{admin_password}".encode())
    .decode()
    .replace("\n", "")
  )
  request = urllib.request.Request(url)
  request.add_header("Authorization", f"Basic {admin_auth}")
  request.add_header("X-Requested-By", "shpurdp")
  if request_data is not None:
    request.data = json.dumps(request_data)
  request.get_method = lambda: get_method

  with closing(
    urllib.request.urlopen(request, context=get_ssl_context(properties))
  ) as response:
    response_status_code = response.getcode()
    if response_status_code not in (200, 201):
      err = (
        "Error while performing changes via Shpurdp REST API. Http status code - "
        + str(response_status_code)
      )
      raise FatalException(1, err)


def get_ssl_context(properties, requested_protocol=None):
  """
  If needed, creates an SSL context that does not validate the SSL certificate provided by the server.

  If api.ssl is not True, then return None, else create a new SSL context with either the requested
  protocol or the best one that is available for the version of Python being used.

  :param properties the Shpurdp server configuration data
  :param requested_protocol: the requested SSL/TLS protocol; None to choose the protocol dynamically
  :rtype ssl.SSLContext
  :return: a permissive SSLContext or None
  """

  if not is_api_ssl_enabled(properties) or not hasattr(ssl, "SSLContext"):
    return None

  if requested_protocol:
    protocol = requested_protocol
  else:
    if hasattr(ssl, "PROTOCOL_TLS"):
      # https://docs.python.org/2/library/ssl.html#ssl.PROTOCOL_TLS
      # Selects the highest protocol version that both the client and server support.
      protocol = ssl.PROTOCOL_TLS
    elif hasattr(ssl, "PROTOCOL_TLSv1_2"):
      # https://docs.python.org/2/library/ssl.html#ssl.PROTOCOL_TLSv1_2
      # Selects TLS version 1.2 as the channel encryption protocol.
      protocol = ssl.PROTOCOL_TLSv1_2
    elif hasattr(ssl, "PROTOCOL_TLSv1_1"):
      # https://docs.python.org/2/library/ssl.html#ssl.PROTOCOL_TLSv1_1
      # Selects TLS version 1.1 as the channel encryption protocol
      protocol = ssl.PROTOCOL_TLSv1_1
    elif hasattr(ssl, "PROTOCOL_TLSv1"):
      # https://docs.python.org/2/library/ssl.html#ssl.PROTOCOL_TLSv1
      # Selects TLS version 1.0 as the channel encryption protocol
      protocol = ssl.PROTOCOL_TLSv1
    else:
      protocol = None

  if protocol:
    context = ssl.SSLContext(protocol)
  else:
    context = ssl.create_default_context()

  # if _https_verify_certificates is vaild, force this to be False
  if hasattr(context, "_https_verify_certificates"):
    context._https_verify_certificates(False)

  return context


def is_api_ssl_enabled(properties):
  """
  Determines if the Shpurdp REST API uses SSL or not.

  :param properties: the Shpurdp server configuration data
  :return: True, if the Shpurdp REST API uses SSL; otherwise False
  """
  ssl_enabled = False
  api_ssl_prop = properties.get_property(SSL_API)
  if api_ssl_prop is not None:
    ssl_enabled = api_ssl_prop.lower() == "true"

  return ssl_enabled


def eligible(service_info, is_sso_integration):
  if is_sso_integration:
    return service_info["sso_integration_supported"] and (
      not service_info["sso_integration_requires_kerberos"]
      or service_info["kerberos_enabled"]
    )
  else:
    return service_info["ldap_integration_supported"]


def get_eligible_services(
  properties, admin_login, admin_password, cluster_name, entry_point, service_qualifier
):
  print_info_msg(f"Fetching {service_qualifier} enabled services")

  safe_cluster_name = urllib.parse.quote(cluster_name)

  response_code, json_data = get_json_via_rest_api(
    properties, admin_login, admin_password, entry_point % safe_cluster_name
  )

  services = []

  if json_data and "items" in json_data:
    services = [
      item["ServiceInfo"]["service_name"]
      for item in json_data["items"]
      if eligible(item["ServiceInfo"], "SSO" == service_qualifier)
    ]

    if len(services) > 0:
      print_info_msg(
        f"Found {service_qualifier} enabled services: {', '.join(services)}"
      )
    else:
      print_info_msg(f"No {service_qualifier} enabled services were found")

  return services


def get_value_from_dictionary(properties, key, default_value=None):
  return properties[key] if properties and key in properties else default_value


def get_boolean_from_dictionary(properties, key, default_value=False):
  value = get_value_from_dictionary(properties, key, None)
  return "true" == value.lower() if value else default_value
