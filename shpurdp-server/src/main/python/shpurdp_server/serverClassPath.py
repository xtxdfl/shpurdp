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

import glob

import os

import shpurdp_server
import re
from shpurdp_commons.exceptions import FatalException
from shpurdp_commons.logging_utils import print_info_msg, print_warning_msg
from resource_management.core.shell import quote_bash_args

SHPURDP_CONF_VAR = "SHPURDP_CONF_DIR"
SERVER_CLASSPATH_KEY = "SERVER_CLASSPATH"
LIBRARY_PATH_KEY = "LD_LIBRARY_PATH"
SHPURDP_SERVER_LIB = "SHPURDP_SERVER_LIB"
JDBC_DRIVER_PATH_PROPERTY = "server.jdbc.driver.path"
JAR_FILE_PATTERN = re.compile(r"^(.*)(-\d.*\.jar$)")
SHPURDP_SERVER_JAR_FILE_PATTERN = re.compile(r"^shpurdp-server(-\d.*\.jar$)")
JAR_DUPLICATES_TO_IGNORE = [
  "javax.servlet.jsp.jstl",  # org.eclipse.jetty dependency requires two different libraries with this name
]


class ServerClassPath:
  properties = None
  options = None
  configDefaults = None

  def __init__(self, properties, options):
    self.properties = properties
    self.options = options
    self.configDefaults = shpurdp_server.serverConfiguration.ServerConfigDefaults()

  def _get_shpurdp_jars(self):
    try:
      conf_dir = os.environ[SHPURDP_SERVER_LIB]
      return conf_dir
    except KeyError:
      default_jar_location = self.configDefaults.DEFAULT_LIBS_DIR
      print_info_msg(
        SHPURDP_SERVER_LIB + " is not set, using default " + default_jar_location
      )
      return default_jar_location

  def _get_jdbc_cp(self):
    jdbc_jar_path = ""
    if self.properties != -1:
      jdbc_jar_path = self.properties[JDBC_DRIVER_PATH_PROPERTY]
    return jdbc_jar_path

  def _get_shpurdp_classpath(self):
    shpurdp_class_path = os.path.abspath(self._get_shpurdp_jars() + os.sep + "*")

    # Add classpath from server.jdbc.driver.path property
    jdbc_cp = self._get_jdbc_cp()
    if len(jdbc_cp) > 0:
      shpurdp_class_path = shpurdp_class_path + os.pathsep + jdbc_cp

    # Add classpath from environment (SERVER_CLASSPATH)
    if SERVER_CLASSPATH_KEY in os.environ:
      shpurdp_class_path = (
        os.environ[SERVER_CLASSPATH_KEY] + os.pathsep + shpurdp_class_path
      )

    # Add jdbc driver classpath
    if self.options:
      jdbc_driver_path = shpurdp_server.dbConfiguration.get_jdbc_driver_path(
        self.options, self.properties
      )
      if jdbc_driver_path not in shpurdp_class_path:
        shpurdp_class_path = shpurdp_class_path + os.pathsep + jdbc_driver_path

    # Add conf_dir to class_path
    conf_dir = shpurdp_server.serverConfiguration.get_conf_dir()
    shpurdp_class_path = conf_dir + os.pathsep + shpurdp_class_path

    return shpurdp_class_path

  def get_full_shpurdp_classpath_escaped_for_shell(self, validate_classpath=False):
    class_path = self._get_shpurdp_classpath()
    if validate_classpath:
      self._validate_classpath(class_path)
    # When classpath is required we should also set native libs os env variable
    # This is required for some jdbc (ex. sqlAnywhere)
    self.set_native_libs_path()

    return quote_bash_args(class_path)

  #
  # Set native libs os env
  #
  def set_native_libs_path(self):
    if self.options:
      native_libs_path = shpurdp_server.dbConfiguration.get_native_libs_path(
        self.options, self.properties
      )
      if native_libs_path is not None:
        if LIBRARY_PATH_KEY in os.environ:
          native_libs_path = (
            os.environ[LIBRARY_PATH_KEY] + os.pathsep + native_libs_path
          )
        os.environ[LIBRARY_PATH_KEY] = native_libs_path

  def _validate_classpath(self, classpath):
    """
    Check if  java class path contains multiple versions of the same jar
    archives, if yes - warn user.
    If multiple versions of the shpurdp-server jar found, raise FatalException()

    Keyword arguments:
      classpath - java class path
    """
    jars = self._find_all_jars(classpath)
    jar_names = {}

    for jar in jars:
      match = JAR_FILE_PATTERN.match(os.path.basename(jar))
      if match:
        for group in match.groups():
          if group in JAR_DUPLICATES_TO_IGNORE:
            break

          if group in jar_names:
            err = (
              "Multiple versions of {0}.jar found in java class path "
              "({1} and {2}). \n Make sure that you include only one "
              "{0}.jar in the java class path '{3}'.".format(
                group, jar, jar_names[group], classpath
              )
            )
            if SHPURDP_SERVER_JAR_FILE_PATTERN.match(os.path.basename(jar)):
              raise FatalException(1, err)
            else:
              print_warning_msg(err)
          else:
            jar_names[group] = jar
          break

  def _find_all_jars(self, classpath):
    """
    Return the list of absolute paths to jars in classpath.
    Raise FatalException() if classpath isn't set

    Keyword arguments:
      classpath - java class path
    """
    if classpath:
      jars = []
      for wildcard in classpath.split(os.pathsep):
        for path in glob.glob(wildcard):
          if os.path.isfile(path) and path.endswith(".jar"):
            jars.append(path)
      return jars
    else:
      raise FatalException(1, "No classpath specified.")
    pass
