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

import shpurdp_simplejson as json
import os
import sys
import traceback

RECOMMEND_COMPONENT_LAYOUT_ACTION = "recommend-component-layout"
VALIDATE_COMPONENT_LAYOUT_ACTION = "validate-component-layout"
RECOMMEND_CONFIGURATIONS = "recommend-configurations"
RECOMMEND_CONFIGURATIONS_FOR_SSO = "recommend-configurations-for-sso"
RECOMMEND_CONFIGURATIONS_FOR_LDAP = "recommend-configurations-for-ldap"
RECOMMEND_CONFIGURATIONS_FOR_KERBEROS = "recommend-configurations-for-kerberos"
RECOMMEND_CONFIGURATION_DEPENDENCIES = "recommend-configuration-dependencies"
VALIDATE_CONFIGURATIONS = "validate-configurations"

ALL_ACTIONS = [
  RECOMMEND_COMPONENT_LAYOUT_ACTION,
  VALIDATE_COMPONENT_LAYOUT_ACTION,
  RECOMMEND_CONFIGURATIONS,
  RECOMMEND_CONFIGURATIONS_FOR_SSO,
  RECOMMEND_CONFIGURATIONS_FOR_LDAP,
  RECOMMEND_CONFIGURATIONS_FOR_KERBEROS,
  RECOMMEND_CONFIGURATION_DEPENDENCIES,
  VALIDATE_CONFIGURATIONS,
]
USAGE = f"Usage: <action> <hosts_file> <services_file>\nPossible actions are: {str(ALL_ACTIONS)}\n"

SCRIPT_DIRECTORY = os.path.dirname(os.path.abspath(__file__))
STACKS_DIRECTORY = os.path.join(SCRIPT_DIRECTORY, "../stacks")
STACK_ADVISOR_PATH = os.path.join(STACKS_DIRECTORY, "stack_advisor.py")
SHPURDP_CONFIGURATION_PATH = os.path.join(STACKS_DIRECTORY, "shpurdp_configuration.py")
STACK_ADVISOR_DEFAULT_IMPL_CLASS = "DefaultStackAdvisor"
STACK_ADVISOR_IMPL_PATH_TEMPLATE = os.path.join(
  STACKS_DIRECTORY, "{0}/{1}/services/stack_advisor.py"
)
STACK_ADVISOR_IMPL_CLASS_TEMPLATE = "{0}{1}StackAdvisor"

ADVISOR_CONTEXT = "advisor_context"
CALL_TYPE = "call_type"


class StackAdvisorException(Exception):
  pass


def loadJson(path):
  try:
    with open(path, "r") as f:
      return json.load(f)
  except Exception as err:
    traceback.print_exc()
    raise StackAdvisorException(f"Error loading file at: {path}")


def dumpJson(json_object, dump_file):
  try:
    with open(dump_file, "w") as out:
      json.dump(json_object, out, indent=1)
  except Exception as err:
    traceback.print_exc()
    raise StackAdvisorException(f"Error writing to file {dump_file} : {str(err)}")


def main(argv=None):
  args = argv[1:]

  if len(args) < 3:
    sys.stderr.write(USAGE)
    sys.exit(2)

  action = args[0]
  if action not in ALL_ACTIONS:
    sys.stderr.write(USAGE)
    sys.exit(2)

  hostsFile = args[1]
  servicesFile = args[2]

  # Parse hostsFile and servicesFile
  hosts = loadJson(hostsFile)
  services = loadJson(servicesFile)

  # Instantiate StackAdvisor and call action related method
  stackName = services["Versions"]["stack_name"]
  stackVersion = services["Versions"]["stack_version"]
  parentVersions = []

  if "stack_hierarchy" in services["Versions"]:
    parentVersions = services["Versions"]["stack_hierarchy"]["stack_versions"]

  stackAdvisor = instantiateStackAdvisor(stackName, stackVersion, parentVersions)

  # Perform action
  actionDir = os.path.realpath(os.path.dirname(args[1]))

  # filter
  hosts = stackAdvisor.filterHostMounts(hosts, services)

  if action == RECOMMEND_COMPONENT_LAYOUT_ACTION:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendComponentLayout"}
    result = stackAdvisor.recommendComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout.json")
  elif action == VALIDATE_COMPONENT_LAYOUT_ACTION:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "validateComponentLayout"}
    result = stackAdvisor.validateComponentLayout(services, hosts)
    result_file = os.path.join(actionDir, "component-layout-validation.json")
  elif action == RECOMMEND_CONFIGURATIONS:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendConfigurations"}
    result = stackAdvisor.recommendConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATIONS_FOR_SSO:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendConfigurationsForSSO"}
    result = stackAdvisor.recommendConfigurationsForSSO(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATIONS_FOR_LDAP:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendConfigurationsForLDAP"}
    result = stackAdvisor.recommendConfigurationsForLDAP(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATIONS_FOR_KERBEROS:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendConfigurationsForKerberos"}
    result = stackAdvisor.recommendConfigurationsForKerberos(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  elif action == RECOMMEND_CONFIGURATION_DEPENDENCIES:
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "recommendConfigurationDependencies"}
    result = stackAdvisor.recommendConfigurationDependencies(services, hosts)
    result_file = os.path.join(actionDir, "configurations.json")
  else:  # action == VALIDATE_CONFIGURATIONS
    services[ADVISOR_CONTEXT] = {CALL_TYPE: "validateConfigurations"}
    result = stackAdvisor.validateConfigurations(services, hosts)
    result_file = os.path.join(actionDir, "configurations-validation.json")

  dumpJson(result, result_file)


def instantiateStackAdvisor(stackName, stackVersion, parentVersions):
  """Instantiates StackAdvisor implementation for the specified Stack"""
  import imp

  with open(SHPURDP_CONFIGURATION_PATH, "r") as fp:
    imp.load_module(
      "shpurdp_configuration",
      fp,
      SHPURDP_CONFIGURATION_PATH,
      (".py", "rb", imp.PY_SOURCE),
    )

  with open(STACK_ADVISOR_PATH, "r") as fp:
    default_stack_advisor = imp.load_module(
      "stack_advisor", fp, STACK_ADVISOR_PATH, (".py", "rb", imp.PY_SOURCE)
    )
  className = STACK_ADVISOR_DEFAULT_IMPL_CLASS
  stack_advisor = default_stack_advisor

  versions = [stackVersion]
  versions.extend(parentVersions)

  for version in reversed(versions):
    try:
      path = STACK_ADVISOR_IMPL_PATH_TEMPLATE.format(stackName, version)

      if os.path.isfile(path):
        with open(path, "r") as fp:
          stack_advisor = imp.load_module(
            "stack_advisor_impl", fp, path, (".py", "rb", imp.PY_SOURCE)
          )
        className = STACK_ADVISOR_IMPL_CLASS_TEMPLATE.format(
          stackName, version.replace(".", "")
        )
        print(
          f"StackAdvisor implementation for stack {stackName}, version {version} was loaded"
        )
    except IOError:  # file not found
      traceback.print_exc()
      print(
        f"StackAdvisor implementation for stack {stackName}, version {version} was not found"
      )

  try:
    clazz = getattr(stack_advisor, className)
    print("Returning " + className + " implementation")
    return clazz()
  except Exception as e:
    traceback.print_exc()
    print("Returning default implementation")
    return default_stack_advisor.DefaultStackAdvisor()


if __name__ == "__main__":
  try:
    main(sys.argv)
  except StackAdvisorException as stack_exception:
    traceback.print_exc()
    print(f"Error occured in stack advisor.\nError details: {str(stack_exception)}")
    sys.exit(1)
  except Exception as e:
    traceback.print_exc()
    print(f"Error occured in stack advisor.\nError details: {str(e)}")
    sys.exit(2)
