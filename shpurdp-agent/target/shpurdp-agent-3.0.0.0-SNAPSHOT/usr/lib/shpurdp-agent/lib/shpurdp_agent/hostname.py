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

import socket
import subprocess
import urllib.request, urllib.error, urllib.parse
import logging
import traceback
import sys

logger = logging.getLogger(__name__)

cached_hostname = None
cached_public_hostname = None
cached_server_hostnames = []


def arrayFromCsvString(str):
  CSV_DELIMITER = ","

  result_array = []
  items = str.lower().split(CSV_DELIMITER)

  for item in items:
    result_array.append(item.strip())
  return result_array


def hostname(config):
  global cached_hostname
  if cached_hostname is not None:
    return cached_hostname

  try:
    scriptname = config.get("agent", "hostname_script")
    try:
      osStat = subprocess.Popen(
        [scriptname],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
      )
      out, err = osStat.communicate()
      if 0 == osStat.returncode and 0 != len(out.strip()):
        cached_hostname = out.strip()
        logger.info(
          f"Read hostname '{cached_hostname}' using agent:hostname_script '{scriptname}'"
        )
      else:
        logger.warn(
          f"Execution of '{scriptname}' failed with exit code {osStat.returncode}. err='{err.strip()}'\nout='{out.strip()}'"
        )
        cached_hostname = socket.getfqdn()
        logger.info(
          f"Read hostname '{cached_hostname}' using socket.getfqdn() as '{scriptname}' failed"
        )
    except:
      cached_hostname = socket.getfqdn()
      logger.warn(
        f"Unexpected error while retrieving hostname: '{sys.exc_info()}', defaulting to socket.getfqdn()"
      )
      logger.info(f"Read hostname '{cached_hostname}' using socket.getfqdn().")
  except:
    cached_hostname = socket.getfqdn()
    logger.info(
      f"agent:hostname_script configuration not defined thus read hostname '{cached_hostname}' using socket.getfqdn()."
    )

  cached_hostname = cached_hostname.lower()
  return cached_hostname


def public_hostname(config):
  global cached_public_hostname
  if cached_public_hostname is not None:
    return cached_public_hostname

  out = ""
  err = ""
  try:
    if config.has_option("agent", "public_hostname_script"):
      scriptname = config.get("agent", "public_hostname_script")
      output = subprocess.Popen(
        scriptname,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        shell=True,
        universal_newlines=True,
      )
      out, err = output.communicate()
      if 0 == output.returncode and 0 != len(out.strip()):
        cached_public_hostname = out.strip().lower()
        logger.info(
          "Read public hostname '"
          + cached_public_hostname
          + "' using agent:public_hostname_script"
        )
        return cached_public_hostname
      else:
        logger.warn(
          f"Execution of '{scriptname}' returned {output.returncode}. {err.strip()}\n{out.strip()}"
        )
  except:
    # ignore for now.
    trace_info = traceback.format_exc()
    logger.info(
      "Error using the scriptname:" + trace_info + " :out " + out + " :err " + err
    )
    logger.info("Defaulting to fqdn.")

  try:
    handle = urllib.request.urlopen(
      "http://169.254.169.254/latest/meta-data/public-hostname", "", 2
    )
    str = handle.read()
    handle.close()
    cached_public_hostname = str.lower()
    logger.info(
      "Read public hostname '"
      + cached_public_hostname
      + "' from http://169.254.169.254/latest/meta-data/public-hostname"
    )
  except:
    cached_public_hostname = socket.getfqdn().lower()
    logger.info(
      "Read public hostname '" + cached_public_hostname + "' using socket.getfqdn()"
    )
  return cached_public_hostname


def server_hostnames(config):
  """
  Reads the shpurdp server name from the config or using the supplied script
  """
  global cached_server_hostnames
  if cached_server_hostnames != []:
    return cached_server_hostnames

  if config.has_option("server", "hostname_script"):
    scriptname = config.get("server", "hostname_script")
    try:
      osStat = subprocess.Popen(
        [scriptname],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
      )
      out, err = osStat.communicate()
      if 0 == osStat.returncode and 0 != len(out.strip()):
        cached_server_hostnames = arrayFromCsvString(out)
        logger.info(
          "Read server hostname '"
          + cached_server_hostnames
          + "' using server:hostname_script"
        )
    except Exception as err:
      logger.info("Unable to execute hostname_script for server hostname. " + str(err))

  if not cached_server_hostnames:
    cached_server_hostnames = arrayFromCsvString(config.get("server", "hostname"))
  return cached_server_hostnames


def main(argv=None):
  print(hostname())
  print(public_hostname())
  print(server_hostnames())


if __name__ == "__main__":
  main()
