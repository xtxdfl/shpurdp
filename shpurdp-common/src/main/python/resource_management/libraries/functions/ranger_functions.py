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

import re
import time
import sys
import urllib.request, urllib.error, urllib.parse
import base64
import http.client

# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import shpurdp_simplejson as json

from io import StringIO as BytesIO
from resource_management.core.logger import Logger
from shpurdp_commons.inet_utils import openurl
from shpurdp_commons.exceptions import TimeoutError
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.decorator import safe_retry


class Rangeradmin:
  sInstance = None

  def __init__(self, url="http://localhost:6080", skip_if_rangeradmin_down=True):
    if url.endswith("/"):
      url = url.rstrip("/")
    self.baseUrl = url
    self.urlLogin = self.baseUrl + "/login.jsp"
    self.urlLoginPost = self.baseUrl + "/j_spring_security_check"
    self.urlRepos = self.baseUrl + "/service/assets/assets"
    self.urlReposPub = self.baseUrl + "/service/public/api/repository"
    self.urlPolicies = self.baseUrl + "/service/public/api/policy"
    self.urlGroups = self.baseUrl + "/service/xusers/groups"
    self.urlUsers = self.baseUrl + "/service/xusers/users"
    self.urlSecUsers = self.baseUrl + "/service/xusers/secure/users"
    self.skip_if_rangeradmin_down = skip_if_rangeradmin_down

    if self.skip_if_rangeradmin_down:
      Logger.info("Rangeradmin: Skip ranger admin if it's down !")

  @safe_retry(
    times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None
  )
  def get_repository_by_name_urllib2(self, name, component, status, usernamepassword):
    """
    :param name: name of the component, from which, function will search in list of repositories
    :param component: component for which repository has to be checked
    :param status: active or inactive
    :param usernamepassword: user credentials using which repository needs to be searched
    :return Returns Ranger repository dict if found otherwise None
    """
    try:
      searchRepoURL = (
        self.urlReposPub + "?name=" + name + "&type=" + component + "&status=" + status
      )
      request = urllib.request.Request(searchRepoURL)
      base64string = (
        base64.b64encode(usernamepassword.encode()).decode().replace("\n", "")
      )
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", f"Basic {base64string}")
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response["vXRepositories"]) > 0:
        for repo in response["vXRepositories"]:
          repoDump = json.loads(json.JSONEncoder().encode(repo))
          if repoDump["name"].lower() == name.lower():
            return repoDump
        return None
      else:
        return None
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(
          f"Error getting {name} repository for component {component}. Http status code - {e.code}. \n {e.read()}"
        )
      else:
        raise Fail(
          f"Error getting {name} repository for component {component}. Reason - {e.reason}."
        )
    except http.client.BadStatusLine:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  def create_ranger_repository(
    self,
    component,
    repo_name,
    repo_properties,
    shpurdp_ranger_admin,
    shpurdp_ranger_password,
    admin_uname,
    admin_password,
    policy_user,
  ):
    """
    :param component: name of the component, from which it will get or create repository
    :param repo_name: name of the repository to be get or create
    :param repo_properties: dict of repository to be create if not exist
    :param shpurdp_ranger_admin: shpurdp admin user creation username
    :param shpurdp_ranger_password: shpurdp admin user creation password
    :param admin_uname: ranger admin username
    :param admin_password: ranger admin password
    :param policy_user: use this policy user for policies that will be used during repository creation
    """
    response_code = self.check_ranger_login_urllib2(self.baseUrl)
    repo_data = json.dumps(repo_properties)
    shpurdp_ranger_password = str(shpurdp_ranger_password)
    admin_password = str(admin_password)
    shpurdp_username_password_for_ranger = format(
      "{shpurdp_ranger_admin}:{shpurdp_ranger_password}"
    )

    if response_code is not None and response_code == 200:
      user_resp_code = self.create_shpurdp_admin_user(
        shpurdp_ranger_admin,
        shpurdp_ranger_password,
        format("{admin_uname}:{admin_password}"),
      )
      if user_resp_code is not None and user_resp_code == 200:
        retryCount = 0
        while retryCount <= 5:
          repo = self.get_repository_by_name_urllib2(
            repo_name, component, "true", shpurdp_username_password_for_ranger
          )
          if repo is not None:
            Logger.info(f'{component.title()} Repository {repo["name"]} exist')
            break
          else:
            response = self.create_repository_urllib2(
              repo_data, shpurdp_username_password_for_ranger, policy_user
            )
            if response is not None:
              Logger.info(f"{component.title()} Repository created in Ranger admin")
              break
            else:
              if retryCount < 5:
                Logger.info("Retry Repository Creation is being called")
                time.sleep(15)  # delay for 15 seconds
                retryCount += 1
              else:
                Logger.error(
                  f"{component.title()} Repository creation failed in Ranger admin"
                )
                break
      else:
        Logger.error("Shpurdp admin user creation failed")
    elif not self.skip_if_rangeradmin_down:
      Logger.error("Connection to Ranger Admin failed !")

  @safe_retry(
    times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None
  )
  def create_repository_urllib2(self, data, usernamepassword, policy_user):
    """
    :param data: repository dict
    :param usernamepassword: user credentials using which repository needs to be created
    :param policy_user: use this policy user for policies that will be used during repository creation
    :return Returns created repository response else None
    """
    try:
      searchRepoURL = self.urlReposPub
      base64string = (
        base64.b64encode(f"{usernamepassword}".encode()).decode().replace("\n", "")
      )
      headers = {"Accept": "application/json", "Content-Type": "application/json"}
      request = urllib.request.Request(searchRepoURL, data, headers)
      request.add_header("Authorization", f"Basic {base64string}")
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))
      if response_code == 200:
        Logger.info("Repository created Successfully")
        # Get Policies
        repoData = json.loads(data)
        repoName = repoData["name"]
        typeOfPolicy = repoData["repositoryType"]
        # Get Policies by repo name
        policyList = self.get_policy_by_repo_name(
          name=repoName,
          component=typeOfPolicy,
          status="true",
          usernamepassword=usernamepassword,
        )
        if policyList is not None and (len(policyList)) > 0:
          policiesUpdateCount = 0
          for policy in policyList:
            updatedPolicyObj = self.get_policy_params(typeOfPolicy, policy, policy_user)
            policyResCode = self.update_ranger_policy(
              updatedPolicyObj["id"], json.dumps(updatedPolicyObj), usernamepassword
            )
            if policyResCode == 200:
              policiesUpdateCount = policiesUpdateCount + 1
            else:
              Logger.info("Policy Update failed")
              # Check for count of updated policies
          if len(policyList) == policiesUpdateCount:
            Logger.info(
              "Ranger Repository created successfully and policies updated successfully providing shpurdp-qa user all permissions"
            )
            return response
          else:
            return None
        else:
          Logger.info("Policies not found for the newly created Repository")
        return None
      else:
        Logger.info("Repository creation failed")
        return None
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(
          f"Error creating repository. Http status code - {e.code}. \n {e.read()}"
        )
      else:
        raise Fail(f"Error creating repository. Reason - {e.reason}.")
    except http.client.BadStatusLine:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  @safe_retry(
    times=75, sleep_time=8, backoff_factor=1, err_class=Fail, return_on_fail=None
  )
  def check_ranger_login_urllib2(self, url):
    """
    :param url: ranger admin host url
    :return Returns login check response
    """
    try:
      response = openurl(url, timeout=20)
      response_code = response.getcode()
      return response_code
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(
          f"Connection to Ranger Admin failed. Http status code - {e.code}. \n {e.read()}"
        )
      else:
        raise Fail(f"Connection to Ranger Admin failed. Reason - {e.reason}.")
    except http.client.BadStatusLine as e:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  @safe_retry(
    times=75, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None
  )
  def get_policy_by_repo_name(self, name, component, status, usernamepassword):
    """
    :param name: repository name
    :param component: component name for which policy needs to be searched
    :param status: true or false
    :param usernamepassword: user credentials using which policy needs to be searched
    :return Returns successful response else None
    """
    try:
      searchPolicyURL = (
        self.urlPolicies
        + "?repositoryName="
        + name
        + "&repositoryType="
        + component
        + "&isEnabled="
        + status
      )
      request = urllib.request.Request(searchPolicyURL)
      base64string = (
        base64.b64encode(usernamepassword.encode()).decode().replace("\n", "")
      )
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", f"Basic {base64string}")
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response["vXPolicies"]) > 0:
        return response["vXPolicies"]
      else:
        return None
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(
          f"Error getting policy from repository {name} for component {component}. Http status code - {e.code}. \n {e.read()}"
        )
      else:
        raise Fail(
          f"Error getting policy from repository {name} for component {component}. Reason - {e.reason}."
        )
    except http.client.BadStatusLine:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  @safe_retry(
    times=75, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None
  )
  def update_ranger_policy(self, policyId, data, usernamepassword):
    """
    :param policyId: policy id which needs to be updated
    :param data: policy data that needs to be updated
    :param usernamepassword: user credentials using which policy needs to be updated
    :return Returns successful response and response code else None
    """
    try:
      searchRepoURL = self.urlPolicies + "/" + str(policyId)
      base64string = (
        base64.b64encode(f"{usernamepassword}".encode()).decode().replace("\n", "")
      )
      headers = {"Accept": "application/json", "Content-Type": "application/json"}
      request = urllib.request.Request(searchRepoURL, data, headers)
      request.add_header("Authorization", f"Basic {base64string}")
      request.get_method = lambda: "PUT"
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))
      if response_code == 200:
        Logger.info("Policy updated Successfully")
        return response_code
      else:
        Logger.error("Update Policy failed")
        return None
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(f"Error updating policy. Http status code - {e.code}. \n {e.read()}")
      else:
        raise Fail(f"Error updating policy. Reason - {e.reason}.")
    except http.client.BadStatusLine:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  def get_policy_params(self, typeOfPolicy, policyObj, policy_user):
    """
    :param typeOfPolicy: component name for which policy has to be get
    :param policyObj: policy dict
    :param policy_user: policy user that needs to be updated
    :returns Returns updated policy dict
    """
    typeOfPolicy = typeOfPolicy.lower()
    if typeOfPolicy == "hdfs":
      policyObj["permMapList"] = [
        {"userList": [policy_user], "permList": ["read", "write", "execute", "admin"]}
      ]
    elif typeOfPolicy == "hive":
      policyObj["permMapList"] = [
        {
          "userList": [policy_user],
          "permList": [
            "select",
            "update",
            "create",
            "drop",
            "alter",
            "index",
            "lock",
            "all",
            "admin",
          ],
        }
      ]
    elif typeOfPolicy == "hbase":
      policyObj["permMapList"] = [
        {"userList": [policy_user], "permList": ["read", "write", "create", "admin"]}
      ]
    elif typeOfPolicy == "knox":
      policyObj["permMapList"] = [
        {"userList": [policy_user], "permList": ["allow", "admin"]}
      ]
    elif typeOfPolicy == "storm":
      policyObj["permMapList"] = [
        {
          "userList": [policy_user],
          "permList": [
            "submitTopology",
            "fileUpload",
            "getNimbusConf",
            "getClusterInfo",
            "fileDownload",
            "killTopology",
            "rebalance",
            "activate",
            "deactivate",
            "getTopologyConf",
            "getTopology",
            "getUserTopology",
            "getTopologyInfo",
            "uploadNewCredential",
            "admin",
          ],
        }
      ]
    return policyObj

  @safe_retry(
    times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None
  )
  def create_shpurdp_admin_user(
    self, shpurdp_admin_username, shpurdp_admin_password, usernamepassword
  ):
    """
    :param shpurdp_admin_username: username of user to be created
    :param shpurdp_admin_username: user password of user to be created
    :return Returns response code for successful user creation else None
    """
    flag_shpurdp_admin_present = False
    match = re.match("[a-zA-Z0-9_\S]+$", shpurdp_admin_password)
    if match is None:
      raise Fail("Invalid password given for Ranger Admin user for Shpurdp")
    try:
      url = self.urlUsers + "?name=" + str(shpurdp_admin_username)
      request = urllib.request.Request(url)
      base64string = (
        base64.b64encode(usernamepassword.encode()).decode().replace("\n", "")
      )
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", f"Basic {base64string}")
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response["vXUsers"]) >= 0:
        for vxuser in response["vXUsers"]:
          if vxuser["name"] == shpurdp_admin_username:
            flag_shpurdp_admin_present = True
            break
          else:
            flag_shpurdp_admin_present = False

        if flag_shpurdp_admin_present:
          Logger.info(shpurdp_admin_username + " user already exists.")
          return response_code
        else:
          Logger.info(
            shpurdp_admin_username
            + " user is not present, creating user using given configurations"
          )
          url = self.urlSecUsers
          admin_user = dict()
          admin_user["status"] = 1
          admin_user["userRoleList"] = ["ROLE_SYS_ADMIN"]
          admin_user["name"] = shpurdp_admin_username
          admin_user["password"] = shpurdp_admin_password
          admin_user["description"] = shpurdp_admin_username
          admin_user["firstName"] = shpurdp_admin_username
          data = json.dumps(admin_user)
          base64string = (
            base64.b64encode(f"{usernamepassword}".encode()).decode().replace("\n", "")
          )
          headers = {"Accept": "application/json", "Content-Type": "application/json"}
          request = urllib.request.Request(url, data.encode(), headers)
          request.add_header("Authorization", f"Basic {base64string}")
          result = openurl(request, timeout=20)
          response_code = result.getcode()
          response = json.loads(json.JSONEncoder().encode(result.read()))
          if response_code == 200 and response is not None:
            Logger.info("Shpurdp admin user creation successful.")
            return response_code
          else:
            Logger.info("Shpurdp admin user creation failed.")
            return None
      else:
        return None
    except urllib.error.URLError as e:
      if isinstance(e, urllib.error.HTTPError):
        raise Fail(
          f"Error creating shpurdp admin user. Http status code - {e.code}. \n {e.read()}"
        )
      else:
        raise Fail(f"Error creating shpurdp admin user. Reason - {e.reason}.")
    except http.client.BadStatusLine:
      raise Fail(
        "Ranger Admin service is not reachable, please restart the service and then try again"
      )
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")
