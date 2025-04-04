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

import os
import re
from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger
from resource_management.core.resources.system import File, Directory, Execute, Link
from resource_management.core.source import DownloadSource, InlineTemplate, Template
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.modify_properties_file import (
  ModifyPropertiesFile,
)
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.generate_logfeeder_input_config import (
  generate_logfeeder_input_config,
)
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.core.utils import PasswordString
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions import solr_cloud_util
from shpurdp_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
from resource_management.core.exceptions import ExecutionFailed

# This file contains functions used for setup/configure of Ranger Admin and Ranger Usersync.
# The design is to mimic what is done by the setup.sh script bundled by Ranger component currently.


def ranger(name=None, upgrade_type=None):
  """
  parameter name: name of ranger service component
  """
  if name == "ranger_admin":
    setup_ranger_admin(upgrade_type=upgrade_type)

  if name == "ranger_usersync":
    setup_usersync(upgrade_type=upgrade_type)

  if name == "ranger_tagsync":
    setup_tagsync(upgrade_type=upgrade_type)


def setup_ranger_admin(upgrade_type=None):
  import params

  if upgrade_type is None:
    upgrade_type = Script.get_upgrade_type(default("/commandParams/upgrade_type", ""))

  ranger_home = params.ranger_home
  ranger_conf = params.ranger_conf

  Directory(
    ranger_conf, owner=params.unix_user, group=params.unix_group, create_parents=True
  )

  copy_jdbc_connector(ranger_home)

  File(
    format("/usr/lib/shpurdp-agent/{check_db_connection_jar_name}"),
    content=DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
    mode=0o644,
  )

  generate_logfeeder_input_config(
    "ranger", Template("input.config-ranger.json.j2", extra_imports=[default])
  )

  cp = format("{check_db_connection_jar}")
  if params.db_flavor.lower() == "sqla":
    cp = cp + os.pathsep + format("{ranger_home}/ews/lib/sajdbc4.jar")
  else:
    cp = cp + os.pathsep + format("{driver_curl_target}")
  cp = cp + os.pathsep + format("{ranger_home}/ews/lib/*")

  db_connection_check_command = format(
    "{shpurdp_java_home}/bin/java -cp {cp} org.apache.shpurdp.server.DBConnectionVerification '{ranger_jdbc_connection_url}' {ranger_db_user} {ranger_db_password!p} {ranger_jdbc_driver}"
  )
  env_dict = {}
  if params.db_flavor.lower() == "sqla":
    env_dict = {"LD_LIBRARY_PATH": params.ld_lib_path}

  Execute(
    db_connection_check_command,
    path="/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin",
    tries=5,
    try_sleep=10,
    environment=env_dict,
  )

  Execute(
    (
      "ln",
      "-sf",
      format("{ranger_home}/ews/webapp/WEB-INF/classes/conf"),
      format("{ranger_home}/conf"),
    ),
    not_if=format("ls {ranger_home}/conf"),
    only_if=format("ls {ranger_home}/ews/webapp/WEB-INF/classes/conf"),
    sudo=True,
  )

  if upgrade_type is not None:
    src_file = format(
      "{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/ranger-admin-default-site.xml"
    )
    dst_file = format("{ranger_home}/conf/ranger-admin-default-site.xml")
    Execute(("cp", "-f", src_file, dst_file), sudo=True)

    src_file = format(
      "{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/security-applicationContext.xml"
    )
    dst_file = format("{ranger_home}/conf/security-applicationContext.xml")

    Execute(("cp", "-f", src_file, dst_file), sudo=True)

  Directory(
    format("{ranger_home}/"),
    owner=params.unix_user,
    group=params.unix_group,
    recursive_ownership=True,
  )

  Directory(
    params.ranger_pid_dir,
    mode=0o755,
    owner=params.unix_user,
    group=params.user_group,
    cd_access="a",
    create_parents=True,
  )

  Directory(
    params.admin_log_dir,
    owner=params.unix_user,
    group=params.unix_group,
    create_parents=True,
    cd_access="a",
    mode=0o755,
  )

  if os.path.isfile(params.ranger_admin_default_file):
    File(
      params.ranger_admin_default_file, owner=params.unix_user, group=params.unix_group
    )
  else:
    Logger.warning(
      f"Required file {params.ranger_admin_default_file} does not exist, copying the file to {ranger_conf} path"
    )
    src_file = format(
      "{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/ranger-admin-default-site.xml"
    )
    dst_file = format("{ranger_home}/conf/ranger-admin-default-site.xml")
    Execute(("cp", "-f", src_file, dst_file), sudo=True)
    File(
      params.ranger_admin_default_file, owner=params.unix_user, group=params.unix_group
    )

  if os.path.isfile(params.security_app_context_file):
    File(
      params.security_app_context_file, owner=params.unix_user, group=params.unix_group
    )
  else:
    Logger.warning(
      f"Required file {params.security_app_context_file} does not exist, copying the file to {ranger_conf} path"
    )
    src_file = format(
      "{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/security-applicationContext.xml"
    )
    dst_file = format("{ranger_home}/conf/security-applicationContext.xml")
    Execute(("cp", "-f", src_file, dst_file), sudo=True)
    File(
      params.security_app_context_file, owner=params.unix_user, group=params.unix_group
    )

  if (
    default("/configurations/ranger-admin-site/ranger.authentication.method", "")
    == "PAM"
  ):
    d = "/etc/pam.d"
    if os.path.isdir(d):
      if os.path.isfile(os.path.join(d, "ranger-admin")):
        Logger.info("ranger-admin PAM file already exists.")
      else:
        File(
          format("{d}/ranger-admin"),
          content=Template("ranger_admin_pam.j2"),
          owner=params.unix_user,
          group=params.unix_group,
          mode=0o644,
        )
      if os.path.isfile(os.path.join(d, "ranger-remote")):
        Logger.info("ranger-remote PAM file already exists.")
      else:
        File(
          format("{d}/ranger-remote"),
          content=Template("ranger_remote_pam.j2"),
          owner=params.unix_user,
          group=params.unix_group,
          mode=0o644,
        )
    else:
      Logger.error(
        "Unable to use PAM authentication, /etc/pam.d/ directory does not exist."
      )

  Execute(
    (
      "ln",
      "-sf",
      format("{ranger_home}/ews/ranger-admin-services.sh"),
      "/usr/bin/ranger-admin",
    ),
    not_if=format("ls /usr/bin/ranger-admin"),
    only_if=format("ls {ranger_home}/ews/ranger-admin-services.sh"),
    sudo=True,
  )

  # remove plain-text password from xml configs

  ranger_admin_site_copy = {}
  ranger_admin_site_copy.update(params.config["configurations"]["ranger-admin-site"])
  for prop in params.ranger_admin_password_properties:
    if prop in ranger_admin_site_copy:
      ranger_admin_site_copy[prop] = "_"
  if "ranger.ha.spnego.kerberos.keytab" in ranger_admin_site_copy:
    ranger_admin_site_copy["ranger.spnego.kerberos.keytab"] = ranger_admin_site_copy[
      "ranger.ha.spnego.kerberos.keytab"
    ]

  XmlConfig(
    "ranger-admin-site.xml",
    conf_dir=ranger_conf,
    configurations=ranger_admin_site_copy,
    configuration_attributes=params.config["configurationAttributes"][
      "ranger-admin-site"
    ],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  Directory(
    os.path.join(ranger_conf, "ranger_jaas"),
    mode=0o700,
    owner=params.unix_user,
    group=params.unix_group,
  )

  File(
    format("{params.ranger_conf}/logback.xml"),
    content=InlineTemplate(params.admin_logback_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  do_keystore_setup(upgrade_type=upgrade_type)

  create_core_site_xml(ranger_conf)

  if params.stack_supports_ranger_kerberos:
    if params.is_hbase_ha_enabled and params.ranger_hbase_plugin_enabled:
      XmlConfig(
        "hbase-site.xml",
        conf_dir=ranger_conf,
        configurations=params.config["configurations"]["hbase-site"],
        configuration_attributes=params.config["configurationAttributes"]["hbase-site"],
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o644,
      )

    if params.is_namenode_ha_enabled and params.ranger_hdfs_plugin_enabled:
      XmlConfig(
        "hdfs-site.xml",
        conf_dir=ranger_conf,
        configurations=params.config["configurations"]["hdfs-site"],
        configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o644,
      )

  File(
    format("{ranger_conf}/ranger-admin-env.sh"),
    content=InlineTemplate(params.ranger_env_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o755,
  )


def setup_ranger_db(stack_version=None):
  import params

  ranger_home = params.ranger_home

  if stack_version is not None:
    ranger_home = format("{stack_root}/{stack_version}/ranger-admin")

  copy_jdbc_connector(ranger_home)

  ModifyPropertiesFile(
    format("{ranger_home}/install.properties"),
    properties={"audit_store": params.ranger_audit_source_type},
    owner=params.unix_user,
  )

  ModifyPropertiesFile(
    format("{ranger_home}/install.properties"),
    properties={"ranger_admin_max_heap_size": params.ranger_admin_max_heap_size},
    owner=params.unix_user,
  )

  env_dict = {"RANGER_ADMIN_HOME": ranger_home, "JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {
      "RANGER_ADMIN_HOME": ranger_home,
      "JAVA_HOME": params.java_home,
      "LD_LIBRARY_PATH": params.ld_lib_path,
    }

  # User wants us to setup the DB user and DB?
  if params.create_db_dbuser:
    Logger.info("Setting up Ranger DB and DB User")
    dba_setup = format("shpurdp-python-wrap {ranger_home}/dba_script.py -q")
    Execute(
      dba_setup,
      environment=env_dict,
      logoutput=True,
      user=params.unix_user,
    )
  else:
    Logger.info("Separate DBA property not set. Assuming Ranger DB and DB User exists!")

  db_setup = format("shpurdp-python-wrap {ranger_home}/db_setup.py")
  Execute(
    db_setup,
    environment=env_dict,
    logoutput=True,
    user=params.unix_user,
  )


def setup_java_patch(stack_version=None):
  import params

  ranger_home = params.ranger_home
  if stack_version is not None:
    ranger_home = format("{stack_root}/{stack_version}/ranger-admin")

  env_dict = {"RANGER_ADMIN_HOME": ranger_home, "JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {
      "RANGER_ADMIN_HOME": ranger_home,
      "JAVA_HOME": params.java_home,
      "LD_LIBRARY_PATH": params.ld_lib_path,
    }

  setup_java_patch = format("shpurdp-python-wrap {ranger_home}/db_setup.py -javapatch")
  Execute(
    setup_java_patch,
    environment=env_dict,
    logoutput=True,
    user=params.unix_user,
  )


def do_keystore_setup(upgrade_type=None):
  import params

  ranger_home = params.ranger_home
  cred_lib_path = params.cred_lib_path

  ranger_credential_helper(
    cred_lib_path,
    params.ranger_jpa_jdbc_credential_alias,
    params.ranger_shpurdp_db_password,
    params.ranger_credential_provider_path,
  )

  if params.ranger_auth_method.upper() == "LDAP":
    ranger_credential_helper(
      params.cred_lib_path,
      params.ranger_ldap_password_alias,
      params.ranger_ldap_bind_auth_password,
      params.ranger_credential_provider_path,
    )

  if params.ranger_auth_method.upper() == "ACTIVE_DIRECTORY":
    ranger_credential_helper(
      params.cred_lib_path,
      params.ranger_ad_password_alias,
      params.ranger_ad_bind_auth_password,
      params.ranger_credential_provider_path,
    )

  if params.stack_supports_secure_ssl_password:
    ranger_credential_helper(
      params.cred_lib_path,
      params.ranger_truststore_alias,
      params.truststore_password,
      params.ranger_credential_provider_path,
    )

    if params.https_enabled and not params.http_enabled:
      ranger_credential_helper(
        params.cred_lib_path,
        params.ranger_https_keystore_alias,
        params.https_keystore_password,
        params.ranger_credential_provider_path,
      )

  File(
    params.ranger_credential_provider_path,
    owner=params.unix_user,
    group=params.unix_group,
    only_if=format("test -e {ranger_credential_provider_path}"),
    mode=0o640,
  )

  update_dot_jceks_crc_ownership(
    credential_provider_path=params.ranger_credential_provider_path,
    user=params.unix_user,
    group=params.unix_group,
  )


def password_validation(password):
  import params

  if password.strip() == "":
    raise Fail(
      "Blank password is not allowed for Bind user. Please enter valid password."
    )
  if re.search("[\\\`'\"]", password):
    raise Fail(
      "LDAP/AD bind password contains one of the unsupported special characters like \" ' \ `"
    )
  else:
    Logger.info("password validated")


def copy_jdbc_connector(ranger_home):
  import params

  if params.jdbc_jar_name is None and params.driver_curl_source.endswith("/None"):
    error_message = format(
      "{db_flavor} jdbc driver cannot be downloaded from {jdk_location}\nPlease run 'shpurdp-server setup --jdbc-db={db_flavor} --jdbc-driver={{path_to_jdbc}}' on shpurdp-server host."
    )
    raise Fail(error_message)

  if params.driver_curl_source and not params.driver_curl_source.endswith("/None"):
    if params.previous_jdbc_jar and os.path.isfile(params.previous_jdbc_jar):
      File(params.previous_jdbc_jar, action="delete")

  File(
    params.downloaded_custom_connector,
    content=DownloadSource(params.driver_curl_source),
    mode=0o644,
  )

  driver_curl_target = format("{ranger_home}/ews/lib/{jdbc_jar_name}")

  if params.db_flavor.lower() == "sqla":
    Execute(
      ("tar", "-xvf", params.downloaded_custom_connector, "-C", params.tmp_dir),
      sudo=True,
    )

    Execute(
      (
        "cp",
        "--remove-destination",
        params.jar_path_in_archive,
        os.path.join(ranger_home, "ews", "lib"),
      ),
      path=["/bin", "/usr/bin/"],
      sudo=True,
    )

    File(os.path.join(ranger_home, "ews", "lib", "sajdbc4.jar"), mode=0o644)

    Directory(params.jdbc_libs_dir, cd_access="a", create_parents=True)

    Execute(
      as_sudo(
        ["yes", "|", "cp", params.libs_path_in_archive, params.jdbc_libs_dir],
        auto_escape=False,
      ),
      path=["/bin", "/usr/bin/"],
    )
  else:
    Execute(
      (
        "cp",
        "--remove-destination",
        params.downloaded_custom_connector,
        os.path.join(ranger_home, "ews", "lib"),
      ),
      path=["/bin", "/usr/bin/"],
      sudo=True,
    )

    File(os.path.join(ranger_home, "ews", "lib", params.jdbc_jar_name), mode=0o644)

  ModifyPropertiesFile(
    format("{ranger_home}/install.properties"),
    properties=params.config["configurations"]["admin-properties"],
    owner=params.unix_user,
  )

  if params.db_flavor.lower() == "sqla":
    ModifyPropertiesFile(
      format("{ranger_home}/install.properties"),
      properties={"SQL_CONNECTOR_JAR": format("{ranger_home}/ews/lib/sajdbc4.jar")},
      owner=params.unix_user,
    )
  else:
    ModifyPropertiesFile(
      format("{ranger_home}/install.properties"),
      properties={"SQL_CONNECTOR_JAR": format("{driver_curl_target}")},
      owner=params.unix_user,
    )


def setup_usersync(upgrade_type=None):
  import params

  usersync_home = params.usersync_home
  ranger_home = params.ranger_home
  ranger_ugsync_conf = params.ranger_ugsync_conf

  if (
    not is_empty(params.ranger_usersync_ldap_ldapbindpassword)
    and params.ug_sync_source
    == "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder"
  ):
    password_validation(params.ranger_usersync_ldap_ldapbindpassword)

  Directory(
    params.ranger_pid_dir,
    mode=0o755,
    owner=params.unix_user,
    group=params.user_group,
    cd_access="a",
    create_parents=True,
  )

  Directory(
    params.usersync_log_dir,
    owner=params.unix_user,
    group=params.unix_group,
    cd_access="a",
    create_parents=True,
    mode=0o755,
    recursive_ownership=True,
  )

  Directory(format("{ranger_ugsync_conf}/"), owner=params.unix_user)

  generate_logfeeder_input_config(
    "ranger", Template("input.config-ranger.json.j2", extra_imports=[default])
  )

  if upgrade_type is not None:
    src_file = format("{usersync_home}/conf.dist/ranger-ugsync-default.xml")
    dst_file = format("{usersync_home}/conf/ranger-ugsync-default.xml")
    Execute(("cp", "-f", src_file, dst_file), sudo=True)

  File(
    format("{params.ranger_ugsync_conf}/logback.xml"),
    content=InlineTemplate(params.usersync_logback_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  # remove plain-text password from xml configs
  ranger_ugsync_site_copy = {}
  ranger_ugsync_site_copy.update(params.config["configurations"]["ranger-ugsync-site"])
  for prop in params.ranger_usersync_password_properties:
    if prop in ranger_ugsync_site_copy:
      ranger_ugsync_site_copy[prop] = "_"

  XmlConfig(
    "ranger-ugsync-site.xml",
    conf_dir=ranger_ugsync_conf,
    configurations=ranger_ugsync_site_copy,
    configuration_attributes=params.config["configurationAttributes"][
      "ranger-ugsync-site"
    ],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  if os.path.isfile(params.ranger_ugsync_default_file):
    File(
      params.ranger_ugsync_default_file, owner=params.unix_user, group=params.unix_group
    )

  if os.path.isfile(params.cred_validator_file):
    File(params.cred_validator_file, group=params.unix_group, mode=0o750)

  if os.path.isfile(params.pam_cred_validator_file):
    File(params.pam_cred_validator_file, group=params.unix_group, mode=0o750)

  ranger_credential_helper(
    params.ugsync_cred_lib,
    "usersync.ssl.key.password",
    params.ranger_usersync_keystore_password,
    params.ugsync_jceks_path,
  )

  if (
    not is_empty(params.ranger_usersync_ldap_ldapbindpassword)
    and params.ug_sync_source
    == "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder"
  ):
    ranger_credential_helper(
      params.ugsync_cred_lib,
      "ranger.usersync.ldap.bindalias",
      params.ranger_usersync_ldap_ldapbindpassword,
      params.ugsync_jceks_path,
    )

  ranger_credential_helper(
    params.ugsync_cred_lib,
    "usersync.ssl.truststore.password",
    params.ranger_usersync_truststore_password,
    params.ugsync_jceks_path,
  )

  File(
    params.ugsync_jceks_path,
    owner=params.unix_user,
    group=params.unix_group,
    only_if=format("test -e {ugsync_jceks_path}"),
    mode=0o640,
  )

  update_dot_jceks_crc_ownership(
    credential_provider_path=params.ugsync_jceks_path,
    user=params.unix_user,
    group=params.unix_group,
  )

  File(
    params.usersync_services_file,
    mode=0o755,
  )

  if not os.path.isfile(params.ranger_usersync_keystore_file):
    cmd = format(
      "{java_home}/bin/keytool -genkeypair -keyalg RSA -alias selfsigned -keystore '{ranger_usersync_keystore_file}' -keypass {ranger_usersync_keystore_password!p} -storepass {ranger_usersync_keystore_password!p} -validity 3600 -keysize 2048 -dname '{default_dn_name}'"
    )

    Execute(cmd, logoutput=True, user=params.unix_user)

    File(
      params.ranger_usersync_keystore_file,
      owner=params.unix_user,
      group=params.user_group,
      only_if=format("test -e {ranger_usersync_keystore_file}"),
      mode=0o640,
    )

  create_core_site_xml(ranger_ugsync_conf)

  File(
    format("{ranger_ugsync_conf}/ranger-usersync-env.sh"),
    content=InlineTemplate(params.ranger_env_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o755,
  )


def setup_tagsync(upgrade_type=None):
  import params

  ranger_tagsync_home = params.ranger_tagsync_home
  ranger_home = params.ranger_home
  ranger_tagsync_conf = params.ranger_tagsync_conf

  Directory(
    format("{ranger_tagsync_conf}"),
    owner=params.unix_user,
    group=params.unix_group,
    create_parents=True,
  )

  Directory(
    params.ranger_pid_dir,
    mode=0o755,
    create_parents=True,
    owner=params.unix_user,
    group=params.user_group,
    cd_access="a",
  )

  Directory(
    params.tagsync_log_dir,
    create_parents=True,
    owner=params.unix_user,
    group=params.unix_group,
    cd_access="a",
    mode=0o755,
  )

  XmlConfig(
    "ranger-tagsync-site.xml",
    conf_dir=ranger_tagsync_conf,
    configurations=params.config["configurations"]["ranger-tagsync-site"],
    configuration_attributes=params.config["configurationAttributes"][
      "ranger-tagsync-site"
    ],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  if params.stack_supports_ranger_tagsync_ssl_xml_support:
    Logger.info("Stack supports tagsync-ssl configurations, performing the same.")
    setup_tagsync_ssl_configs()
  else:
    Logger.info("Stack doesnt support tagsync-ssl configurations, skipping the same.")

  PropertiesFile(
    format("{ranger_tagsync_conf}/atlas-application.properties"),
    properties=params.tagsync_application_properties,
    mode=0o755,
    owner=params.unix_user,
    group=params.unix_group,
  )

  File(
    format("{params.ranger_tagsync_conf}/logback.xml"),
    content=InlineTemplate(params.tagsync_logback_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  File(
    params.tagsync_services_file,
    mode=0o755,
  )

  create_core_site_xml(ranger_tagsync_conf)

  File(
    format("{ranger_tagsync_conf}/ranger-tagsync-env.sh"),
    content=InlineTemplate(params.ranger_env_content),
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o755,
  )


def ranger_credential_helper(lib_path, alias_key, alias_value, file_path):
  import params

  java_bin = format("{java_home}/bin/java")
  file_path = format("jceks://file{file_path}")
  cmd = (
    java_bin,
    "-cp",
    lib_path,
    "org.apache.ranger.credentialapi.buildks",
    "create",
    alias_key,
    "-value",
    PasswordString(alias_value),
    "-provider",
    file_path,
  )
  Execute(cmd, environment={"JAVA_HOME": params.java_home}, logoutput=True, sudo=True)


def create_core_site_xml(conf_dir):
  import params

  if params.stack_supports_ranger_kerberos:
    if params.has_namenode:
      # if there is the viewFS mount table content, create separate xml config and include in in the core-site
      # else just create core-site
      if params.mount_table_content:
        XmlConfig(
          "core-site.xml",
          conf_dir=conf_dir,
          configurations=params.config["configurations"]["core-site"],
          configuration_attributes=params.config["configurationAttributes"][
            "core-site"
          ],
          owner=params.unix_user,
          group=params.unix_group,
          mode=0o644,
          xml_include_file=os.path.join(conf_dir, params.xml_inclusion_file_name),
        )

        File(
          os.path.join(conf_dir, params.xml_inclusion_file_name),
          owner=params.unix_user,
          group=params.unix_group,
          content=params.mount_table_content,
          mode=0o644,
        )
      else:
        XmlConfig(
          "core-site.xml",
          conf_dir=conf_dir,
          configurations=params.config["configurations"]["core-site"],
          configuration_attributes=params.config["configurationAttributes"][
            "core-site"
          ],
          owner=params.unix_user,
          group=params.unix_group,
          mode=0o644,
        )
    else:
      Logger.warning("HDFS service not installed. Creating core-site.xml file.")
      XmlConfig(
        "core-site.xml",
        conf_dir=conf_dir,
        configurations=params.core_site_property,
        configuration_attributes={},
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o644,
      )


def setup_ranger_audit_solr():
  import params

  if params.security_enabled and params.stack_supports_ranger_kerberos:
    if params.solr_jaas_file is not None:
      File(
        format("{solr_jaas_file}"),
        content=Template("ranger_solr_jaas_conf.j2"),
        owner=params.unix_user,
      )
  try:
    check_znode()

    if params.stack_supports_ranger_solr_configs:
      Logger.info("Solr configrations supported,creating solr-configurations.")
      File(
        format("{ranger_solr_conf}/solrconfig.xml"),
        content=InlineTemplate(params.ranger_solr_config_content),
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o644,
      )

      solr_cloud_util.upload_configuration_to_zk(
        zookeeper_quorum=params.zookeeper_quorum,
        solr_znode=params.solr_znode,
        config_set=params.ranger_solr_config_set,
        config_set_dir=params.ranger_solr_conf,
        tmp_dir=params.tmp_dir,
        java64_home=params.java_home,
        solrconfig_content=InlineTemplate(params.ranger_solr_config_content),
        jaas_file=params.solr_jaas_file,
        retry=30,
        interval=5,
      )

    else:
      Logger.info("Solr configrations not supported, skipping solr-configurations.")
      solr_cloud_util.upload_configuration_to_zk(
        zookeeper_quorum=params.zookeeper_quorum,
        solr_znode=params.solr_znode,
        config_set=params.ranger_solr_config_set,
        config_set_dir=params.ranger_solr_conf,
        tmp_dir=params.tmp_dir,
        java64_home=params.java_home,
        jaas_file=params.solr_jaas_file,
        retry=30,
        interval=5,
      )

    if (
      params.security_enabled
      and params.has_infra_solr
      and not params.is_external_solrCloud_enabled
      and params.stack_supports_ranger_kerberos
    ):
      solr_cloud_util.add_solr_roles(
        params.config,
        roles=[
          params.infra_solr_role_ranger_admin,
          params.infra_solr_role_ranger_audit,
          params.infra_solr_role_dev,
        ],
        new_service_principals=[params.ranger_admin_jaas_principal],
      )
      service_default_principals_map = [
        ("hdfs", "nn"),
        ("hbase", "hbase"),
        ("hive", "hive"),
        ("kafka", "kafka"),
        ("kms", "rangerkms"),
        ("knox", "knox"),
        ("nifi", "nifi"),
        ("storm", "storm"),
        ("yanr", "yarn"),
      ]
      service_principals = get_ranger_plugin_principals(service_default_principals_map)
      solr_cloud_util.add_solr_roles(
        params.config,
        roles=[params.infra_solr_role_ranger_audit, params.infra_solr_role_dev],
        new_service_principals=service_principals,
      )

    solr_cloud_util.create_collection(
      zookeeper_quorum=params.zookeeper_quorum,
      solr_znode=params.solr_znode,
      collection=params.ranger_solr_collection_name,
      config_set=params.ranger_solr_config_set,
      java64_home=params.java_home,
      shards=params.ranger_solr_shards,
      replication_factor=int(params.replication_factor),
      jaas_file=params.solr_jaas_file,
    )

    if (
      params.security_enabled
      and params.has_infra_solr
      and not params.is_external_solrCloud_enabled
      and params.stack_supports_ranger_kerberos
    ):
      secure_znode(
        format("{solr_znode}/configs/{ranger_solr_config_set}"), params.solr_jaas_file
      )
      secure_znode(
        format("{solr_znode}/collections/{ranger_solr_collection_name}"),
        params.solr_jaas_file,
      )
  except ExecutionFailed as execution_exception:
    Logger.error(
      f"Error when configuring Solr for Ranger, Kindly check Solr/Zookeeper services to be up and running:\n {execution_exception}"
    )


def setup_ranger_admin_passwd_change(username, user_password, user_default_password):
  import params

  env_dict = {"RANGER_ADMIN_HOME": params.ranger_home, "JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {
      "RANGER_ADMIN_HOME": params.ranger_home,
      "JAVA_HOME": params.java_home,
      "LD_LIBRARY_PATH": params.ld_lib_path,
    }

  cmd = format(
    "shpurdp-python-wrap {ranger_home}/db_setup.py -changepassword {username} {user_default_password!p} {user_password!p}"
  )
  Execute(
    cmd,
    environment=env_dict,
    user=params.unix_user,
    tries=3,
    try_sleep=5,
    logoutput=True,
  )


def setup_ranger_all_admin_password_change(
  admin_username,
  default_admin_password,
  admin_password,
  rangerusersync_username,
  default_rangerusersync_user_password,
  rangerusersync_user_password,
  rangertagsync_username,
  default_rangertagsync_user_password,
  rangertagsync_user_password,
  keyadmin_username,
  default_keyadmin_user_password,
  keyadmin_user_password,
):
  import params

  env_dict = {"RANGER_ADMIN_HOME": params.ranger_home, "JAVA_HOME": params.java_home}
  if params.db_flavor.lower() == "sqla":
    env_dict = {
      "RANGER_ADMIN_HOME": params.ranger_home,
      "JAVA_HOME": params.java_home,
      "LD_LIBRARY_PATH": params.ld_lib_path,
    }

  password_change_cmd = format(
    "shpurdp-python-wrap {ranger_home}/db_setup.py -changepassword "
    " -pair {admin_username} {default_admin_password!p} {admin_password!p} "
    " -pair {rangerusersync_username} {default_rangerusersync_user_password!p} {rangerusersync_user_password!p} "
    " -pair {rangertagsync_username} {default_rangertagsync_user_password!p} {rangertagsync_user_password!p} "
    " -pair {keyadmin_username} {default_keyadmin_user_password!p} {keyadmin_user_password!p} "
  )
  Execute(
    password_change_cmd,
    environment=env_dict,
    user=params.unix_user,
    tries=3,
    try_sleep=5,
    logoutput=True,
  )


@retry(times=10, sleep_time=5, err_class=Fail)
def check_znode():
  import params

  solr_cloud_util.check_znode(
    zookeeper_quorum=params.zookeeper_quorum,
    solr_znode=params.solr_znode,
    java64_home=params.java_home,
  )


def secure_znode(znode, jaasFile):
  import params

  solr_cloud_util.secure_znode(
    config=params.config,
    zookeeper_quorum=params.zookeeper_quorum,
    solr_znode=znode,
    jaas_file=jaasFile,
    java64_home=params.java_home,
    sasl_users=[params.ranger_admin_jaas_principal],
  )


def get_ranger_plugin_principals(services_defaults_tuple_list):
  """
  Get ranger plugin user principals from service-default value maps using ranger-*-audit configurations
  """
  import params

  user_principals = []
  if len(services_defaults_tuple_list) < 1:
    raise Exception("Services - defaults map parameter is missing.")

  for service, default_value in services_defaults_tuple_list:
    user_principal = default(
      format(
        "configurations/ranger-{service}-audit/xasecure.audit.jaas.Client.option.principal"
      ),
      default_value,
    )
    user_principals.append(user_principal)
  return user_principals


def setup_tagsync_ssl_configs():
  import params

  Directory(params.security_store_path, cd_access="a", create_parents=True)

  Directory(
    params.tagsync_etc_path,
    cd_access="a",
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o775,
    create_parents=True,
  )

  # remove plain-text password from xml configs
  ranger_tagsync_policymgr_ssl_copy = {}
  ranger_tagsync_policymgr_ssl_copy.update(
    params.config["configurations"]["ranger-tagsync-policymgr-ssl"]
  )
  for prop in params.ranger_tagsync_password_properties:
    if prop in ranger_tagsync_policymgr_ssl_copy:
      ranger_tagsync_policymgr_ssl_copy[prop] = "_"

  XmlConfig(
    "ranger-policymgr-ssl.xml",
    conf_dir=params.ranger_tagsync_conf,
    configurations=ranger_tagsync_policymgr_ssl_copy,
    configuration_attributes=params.config["configurationAttributes"][
      "ranger-tagsync-policymgr-ssl"
    ],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  ranger_credential_helper(
    params.tagsync_cred_lib,
    "sslKeyStore",
    params.ranger_tagsync_keystore_password,
    params.ranger_tagsync_credential_file,
  )
  ranger_credential_helper(
    params.tagsync_cred_lib,
    "sslTrustStore",
    params.ranger_tagsync_truststore_password,
    params.ranger_tagsync_credential_file,
  )

  File(
    params.ranger_tagsync_credential_file,
    owner=params.unix_user,
    group=params.unix_group,
    only_if=format("test -e {ranger_tagsync_credential_file}"),
    mode=0o640,
  )

  update_dot_jceks_crc_ownership(
    credential_provider_path=params.ranger_tagsync_credential_file,
    user=params.unix_user,
    group=params.unix_group,
  )

  # remove plain-text password from xml configs
  atlas_tagsync_ssl_copy = {}
  atlas_tagsync_ssl_copy.update(params.config["configurations"]["atlas-tagsync-ssl"])
  for prop in params.ranger_tagsync_password_properties:
    if prop in atlas_tagsync_ssl_copy:
      atlas_tagsync_ssl_copy[prop] = "_"

  XmlConfig(
    "atlas-tagsync-ssl.xml",
    conf_dir=params.ranger_tagsync_conf,
    configurations=atlas_tagsync_ssl_copy,
    configuration_attributes=params.config["configurationAttributes"][
      "atlas-tagsync-ssl"
    ],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0o644,
  )

  ranger_credential_helper(
    params.tagsync_cred_lib,
    "sslKeyStore",
    params.atlas_tagsync_keystore_password,
    params.atlas_tagsync_credential_file,
  )
  ranger_credential_helper(
    params.tagsync_cred_lib,
    "sslTrustStore",
    params.atlas_tagsync_truststore_password,
    params.atlas_tagsync_credential_file,
  )

  File(
    params.atlas_tagsync_credential_file,
    owner=params.unix_user,
    group=params.unix_group,
    only_if=format("test -e {atlas_tagsync_credential_file}"),
    mode=0o640,
  )

  update_dot_jceks_crc_ownership(
    credential_provider_path=params.atlas_tagsync_credential_file,
    user=params.unix_user,
    group=params.unix_group,
  )

  Logger.info("Configuring tagsync-ssl configurations done successfully.")


def update_password_configs():
  import params

  password_configs = {"db_root_password": "_", "db_password": "_"}

  if params.stack_supports_ranger_audit_db:
    password_configs["audit_db_password"] = "_"

  ModifyPropertiesFile(
    format("{ranger_home}/install.properties"),
    properties=password_configs,
    owner=params.unix_user,
  )


def validate_user_password(password_property=None):
  import params

  validation = []

  if password_property is None:
    ranger_password_properties = [
      "admin_password",
      "ranger_admin_password",
      "rangerusersync_user_password",
      "rangertagsync_user_password",
      "keyadmin_user_password",
    ]
  else:
    ranger_password_properties = [password_property]

  for index in range(len(ranger_password_properties)):
    password = params.config["configurations"]["ranger-env"][
      ranger_password_properties[index]
    ]
    if not bool(re.search(r"^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$", password)) or bool(
      re.search("[\\\`\"']", password)
    ):
      validation.append(ranger_password_properties[index])

  if len(validation) > 0:
    raise Fail(
      "Password validation failed for : "
      + ", ".join(validation)
      + ". Password should be minimum 8 characters with minimum one alphabet and one numeric. Unsupported special characters are \" ' \ `"
    )


def update_dot_jceks_crc_ownership(credential_provider_path, user, group):
  dot_jceks_crc_file_path = os.path.join(
    os.path.dirname(credential_provider_path),
    "." + os.path.basename(credential_provider_path) + ".crc",
  )

  File(
    dot_jceks_crc_file_path,
    owner=user,
    group=group,
    only_if=format("test -e {dot_jceks_crc_file_path}"),
    mode=0o640,
  )
