#!/usr/bin/env shpurdp-python-wrap
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

# Python imports
from ast import Param
import imp
import os
import traceback
import re
import socket
import fnmatch


from resource_management.core.logger import Logger

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

try:
  if "BASE_SERVICE_ADVISOR" in os.environ:
    PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = imp.load_module(
      "service_advisor", fp, PARENT_FILE, (".py", "rb", imp.PY_SOURCE)
    )
except Exception as e:
  traceback.print_exc()
  print("Failed to load parent")


class AlluxioServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    self.as_super = super(AlluxioServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    # Always call these methods
    self.modifyMastersWithMultipleInstances()
    self.modifyCardinalitiesDict()
    self.modifyHeapSizeProperties()
    self.modifyNotValuableComponents()
    self.modifyComponentsNotPreferableOnServer()
    self.modifyComponentLayoutSchemes()

  def modifyMastersWithMultipleInstances(self):
    """
    Modify the set of masters with multiple instances.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """

    # Nothing to do
    pass

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return self.getServiceComponentCardinalityValidations(services, hosts, "ALLUXIO")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    """
    Entry point.
    Must be overriden in child class.
    """
    # Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = AlluxioRecommender()
    recommender.recommendAlluxioConfigurationsFromHDP33(
      configurations, clusterData, services, hosts
    )

  # def getServiceConfigurationRecommendationsForSSO(self, configurations, clusterData, services, hosts):
  #   """
  #   Entry point.
  #   Must be overriden in child class.
  #   """
  #   recommender = AlluxioRecommender()
  #   recommender.recommendConfigurationsForSSO(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    # Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    return []

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determines if security is enabled by testing the value of core-site/hadoop.security.authentication enabled.
    If the property exists and is equal to "kerberos", then is it enabled; otherwise is it assumed to be
    disabled.

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    if (
      configurations
      and "core-site" in configurations
      and "hadoop.security.authentication" in configurations["core-site"]["properties"]
    ):
      return (
        configurations["core-site"]["properties"][
          "hadoop.security.authentication"
        ].lower()
        == "kerberos"
      )
    elif (
      services
      and "core-site" in services["configurations"]
      and "hadoop.security.authentication"
      in services["configurations"]["core-site"]["properties"]
    ):
      return (
        services["configurations"]["core-site"]["properties"][
          "hadoop.security.authentication"
        ].lower()
        == "kerberos"
      )
    else:
      return False


class AlluxioRecommender(service_advisor.ServiceAdvisor):
  """
  Alluxio Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(AlluxioRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendAlluxioConfigurationsFromHDP33(
    self, configurations, clusterData, services, hosts
  ):
    """
    Recommend configurations for this service based on HDP 3.3.
    """
