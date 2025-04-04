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
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import (
  get_stack_feature_version,
)
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.get_bare_principal import (
  get_bare_principal,
)
from resource_management.libraries.functions.get_kinit_path import get_kinit_path
from resource_management.core.exceptions import Fail

# a map of the Shpurdp role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  "RANGER_ADMIN": "ranger-admin",
  "RANGER_USERSYNC": "ranger-usersync",
  "RANGER_TAGSYNC": "ranger-tagsync",
}

component_directory = Script.get_component_from_role(
  SERVER_ROLE_DIRECTORY_MAP, "RANGER_ADMIN"
)

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()

stack_name = default("/clusterLevelParams/stack_name", None)
version = default("/commandParams/version", None)

stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted = format_stack_version(stack_version_unformatted)

upgrade_marker_file = format("{tmp_dir}/rangeradmin_ru.inprogress")

xml_configurations_supported = config["configurations"]["ranger-env"][
  "xml_configurations_supported"
]

create_db_dbuser = config["configurations"]["ranger-env"]["create_db_dbuser"]

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_rolling_upgrade = check_stack_feature(
  StackFeature.ROLLING_UPGRADE, version_for_stack_feature_checks
)
stack_supports_config_versioning = check_stack_feature(
  StackFeature.CONFIG_VERSIONING, version_for_stack_feature_checks
)
stack_supports_usersync_non_root = check_stack_feature(
  StackFeature.RANGER_USERSYNC_NON_ROOT, version_for_stack_feature_checks
)
stack_supports_ranger_tagsync = check_stack_feature(
  StackFeature.RANGER_TAGSYNC_COMPONENT, version_for_stack_feature_checks
)
stack_supports_ranger_audit_db = check_stack_feature(
  StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_kerberos = check_stack_feature(
  StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks
)
stack_supports_usersync_passwd = check_stack_feature(
  StackFeature.RANGER_USERSYNC_PASSWORD_JCEKS, version_for_stack_feature_checks
)
stack_supports_infra_client = check_stack_feature(
  StackFeature.RANGER_INSTALL_INFRA_CLIENT, version_for_stack_feature_checks
)
stack_supports_pid = check_stack_feature(
  StackFeature.RANGER_PID_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_admin_password_change = check_stack_feature(
  StackFeature.RANGER_ADMIN_PASSWD_CHANGE, version_for_stack_feature_checks
)
stack_supports_ranger_setup_db_on_start = check_stack_feature(
  StackFeature.RANGER_SETUP_DB_ON_START, version_for_stack_feature_checks
)
stack_supports_ranger_tagsync_ssl_xml_support = check_stack_feature(
  StackFeature.RANGER_TAGSYNC_SSL_XML_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_solr_configs = check_stack_feature(
  StackFeature.RANGER_SOLR_CONFIG_SUPPORT, version_for_stack_feature_checks
)
stack_supports_secure_ssl_password = check_stack_feature(
  StackFeature.SECURE_RANGER_SSL_PASSWORD, version_for_stack_feature_checks
)
stack_supports_multiple_env_sh_files = check_stack_feature(
  StackFeature.MULTIPLE_ENV_SH_FILES_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_all_admin_change_default_password = check_stack_feature(
  StackFeature.RANGER_ALL_ADMIN_CHANGE_DEFAULT_PASSWORD,
  version_for_stack_feature_checks,
)
stack_supports_ranger_zone_feature = check_stack_feature(
  StackFeature.RANGER_SUPPORT_SECURITY_ZONE_FEATURE, version_for_stack_feature_checks
)

upgrade_direction = default("/commandParams/upgrade_direction", None)

ranger_home = format("{stack_root}/current/ranger-admin")
ranger_conf = format("{ranger_home}/conf")
ranger_admin_services_file = format("{ranger_home}/ews/ranger-admin-services.sh")


usersync_home = format("{stack_root}/current/ranger-usersync")
ranger_ugsync_conf = format("{usersync_home}/conf")
usersync_services_file = format("{usersync_home}/ranger-usersync-services.sh")

ranger_tagsync_home = format("{stack_root}/current/ranger-tagsync")
ranger_tagsync_conf = format("{ranger_tagsync_home}/conf")
tagsync_services_file = format("{ranger_tagsync_home}/ranger-tagsync-services.sh")

security_store_path = "/etc/security/serverKeys"
tagsync_etc_path = "/etc/ranger/tagsync"
ranger_tagsync_credential_file = os.path.join(tagsync_etc_path, "rangercred.jceks")
atlas_tagsync_credential_file = os.path.join(tagsync_etc_path, "atlascred.jceks")
ranger_tagsync_keystore_password = config["configurations"][
  "ranger-tagsync-policymgr-ssl"
]["xasecure.policymgr.clientssl.keystore.password"]
ranger_tagsync_truststore_password = config["configurations"][
  "ranger-tagsync-policymgr-ssl"
]["xasecure.policymgr.clientssl.truststore.password"]
atlas_tagsync_keystore_password = config["configurations"]["atlas-tagsync-ssl"][
  "xasecure.policymgr.clientssl.keystore.password"
]
atlas_tagsync_truststore_password = config["configurations"]["atlas-tagsync-ssl"][
  "xasecure.policymgr.clientssl.truststore.password"
]

if upgrade_direction == Direction.DOWNGRADE and not check_stack_feature(
  StackFeature.CONFIG_VERSIONING, version_for_stack_feature_checks
):
  stack_supports_rolling_upgrade = True
  stack_supports_config_versioning = False

if upgrade_direction == Direction.DOWNGRADE and not check_stack_feature(
  StackFeature.RANGER_USERSYNC_NON_ROOT, version_for_stack_feature_checks
):
  stack_supports_usersync_non_root = False

ranger_stop = format("{ranger_admin_services_file} stop")
ranger_start = format("{ranger_admin_services_file} start")

usersync_stop = format("{usersync_services_file} stop")
usersync_start = format("{usersync_services_file} start")

java_home = config["shpurdpLevelParams"]["java_home"]
shpurdp_java_home = config["shpurdpLevelParams"]["shpurdp_java_home"]
shpurdp_java_exec = format("{shpurdp_java_home}/bin/java")
unix_user = config["configurations"]["ranger-env"]["ranger_user"]
unix_group = config["configurations"]["ranger-env"]["ranger_group"]
ranger_pid_dir = default("/configurations/ranger-env/ranger_pid_dir", "/var/run/ranger")
usersync_log_dir = default(
  "/configurations/ranger-ugsync-site/ranger.usersync.logdir",
  "/var/log/ranger/usersync",
)
admin_log_dir = default(
  "/configurations/ranger-admin-site/ranger.logs.base.dir", "/var/log/ranger/admin"
)
ranger_admin_default_file = format("{ranger_conf}/ranger-admin-default-site.xml")
security_app_context_file = format("{ranger_conf}/security-applicationContext.xml")
ranger_ugsync_default_file = format("{ranger_ugsync_conf}/ranger-ugsync-default.xml")

cred_validator_file = format("{usersync_home}/native/credValidator.uexe")
pam_cred_validator_file = format("{usersync_home}/native/pamCredValidator.uexe")

db_flavor = (config["configurations"]["admin-properties"]["DB_FLAVOR"]).lower()
usersync_exturl = config["configurations"]["admin-properties"]["policymgr_external_url"]
if usersync_exturl.endswith("/"):
  usersync_exturl = usersync_exturl.rstrip("/")
ranger_host = config["clusterHostInfo"]["ranger_admin_hosts"][0]
ugsync_host = "localhost"
usersync_host_info = config["clusterHostInfo"]["ranger_usersync_hosts"]
if not is_empty(usersync_host_info) and len(usersync_host_info) > 0:
  ugsync_host = config["clusterHostInfo"]["ranger_usersync_hosts"][0]
ranger_external_url = config["configurations"]["admin-properties"][
  "policymgr_external_url"
]
if ranger_external_url.endswith("/"):
  ranger_external_url = ranger_external_url.rstrip("/")
ranger_db_name = config["configurations"]["admin-properties"]["db_name"]
ranger_auditdb_name = default(
  "/configurations/admin-properties/audit_db_name", "ranger_audits"
)

sql_command_invoker = config["configurations"]["admin-properties"][
  "SQL_COMMAND_INVOKER"
]
db_root_user = config["configurations"]["admin-properties"]["db_root_user"]
db_root_password = str(config["configurations"]["admin-properties"]["db_root_password"])
db_host = config["configurations"]["admin-properties"]["db_host"]
ranger_db_user = config["configurations"]["admin-properties"]["db_user"]
ranger_audit_db_user = default(
  "/configurations/admin-properties/audit_db_user", "rangerlogger"
)
ranger_db_password = str(config["configurations"]["admin-properties"]["db_password"])

# ranger-env properties
oracle_home = default("/configurations/ranger-env/oracle_home", "-")

# For curl command in ranger to get db connector
jdk_location = config["shpurdpLevelParams"]["jdk_location"]
java_share_dir = "/usr/share/java"
jdbc_jar_name = None
previous_jdbc_jar_name = None
if db_flavor.lower() == "mysql":
  jdbc_jar_name = default("/shpurdpLevelParams/custom_mysql_jdbc_name", None)
  previous_jdbc_jar_name = default(
    "/shpurdpLevelParams/previous_custom_mysql_jdbc_name", None
  )
  audit_jdbc_url = (
    format("jdbc:mysql://{db_host}/{ranger_auditdb_name}")
    if stack_supports_ranger_audit_db
    else None
  )
  jdbc_dialect = "org.eclipse.persistence.platform.database.MySQLPlatform"
elif db_flavor.lower() == "oracle":
  jdbc_jar_name = default("/shpurdpLevelParams/custom_oracle_jdbc_name", None)
  previous_jdbc_jar_name = default(
    "/shpurdpLevelParams/previous_custom_oracle_jdbc_name", None
  )
  jdbc_dialect = "org.eclipse.persistence.platform.database.OraclePlatform"
  colon_count = db_host.count(":")
  if colon_count == 2 or colon_count == 0:
    audit_jdbc_url = (
      format("jdbc:oracle:thin:@{db_host}") if stack_supports_ranger_audit_db else None
    )
  else:
    audit_jdbc_url = (
      format("jdbc:oracle:thin:@//{db_host}")
      if stack_supports_ranger_audit_db
      else None
    )
elif db_flavor.lower() == "postgres":
  jdbc_jar_name = default("/shpurdpLevelParams/custom_postgres_jdbc_name", None)
  previous_jdbc_jar_name = default(
    "/shpurdpLevelParams/previous_custom_postgres_jdbc_name", None
  )
  audit_jdbc_url = (
    format("jdbc:postgresql://{db_host}/{ranger_auditdb_name}")
    if stack_supports_ranger_audit_db
    else None
  )
  jdbc_dialect = "org.eclipse.persistence.platform.database.PostgreSQLPlatform"
elif db_flavor.lower() == "mssql":
  jdbc_jar_name = default("/shpurdpLevelParams/custom_mssql_jdbc_name", None)
  previous_jdbc_jar_name = default(
    "/shpurdpLevelParams/previous_custom_mssql_jdbc_name", None
  )
  audit_jdbc_url = (
    format("jdbc:sqlserver://{db_host};databaseName={ranger_auditdb_name}")
    if stack_supports_ranger_audit_db
    else None
  )
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLServerPlatform"
elif db_flavor.lower() == "sqla":
  jdbc_jar_name = default("/shpurdpLevelParams/custom_sqlanywhere_jdbc_name", None)
  previous_jdbc_jar_name = default(
    "/shpurdpLevelParams/previous_custom_sqlanywhere_jdbc_name", None
  )
  audit_jdbc_url = (
    format("jdbc:sqlanywhere:database={ranger_auditdb_name};host={db_host}")
    if stack_supports_ranger_audit_db
    else None
  )
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLAnywherePlatform"
else:
  raise Fail(format("'{db_flavor}' db flavor not supported."))

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")
previous_jdbc_jar = format("{java_share_dir}/{previous_jdbc_jar_name}")
if stack_supports_config_versioning:
  driver_curl_target = format("{ranger_home}/ews/lib/{jdbc_jar_name}")
  previous_jdbc_jar = format("{ranger_home}/ews/lib/{previous_jdbc_jar_name}")

if db_flavor.lower() == "sqla":
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  jar_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/sajdbc4.jar")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  jdbc_libs_dir = format("{ranger_home}/native/lib64")
  ld_lib_path = format("{jdbc_libs_dir}")

# for db connection
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/shpurdp-agent/{check_db_connection_jar_name}")
ranger_jdbc_connection_url = config["configurations"]["ranger-admin-site"][
  "ranger.jpa.jdbc.url"
]
ranger_jdbc_driver = config["configurations"]["ranger-admin-site"][
  "ranger.jpa.jdbc.driver"
]

ranger_credential_provider_path = config["configurations"]["ranger-admin-site"][
  "ranger.credential.provider.path"
]
ranger_jpa_jdbc_credential_alias = config["configurations"]["ranger-admin-site"][
  "ranger.jpa.jdbc.credential.alias"
]
ranger_shpurdp_db_password = str(
  config["configurations"]["admin-properties"]["db_password"]
)

ranger_jpa_audit_jdbc_credential_alias = default(
  "/configurations/ranger-admin-site/ranger.jpa.audit.jdbc.credential.alias",
  "rangeraudit",
)
ranger_shpurdp_audit_db_password = ""
if (
  not is_empty(config["configurations"]["admin-properties"]["audit_db_password"])
  and stack_supports_ranger_audit_db
):
  ranger_shpurdp_audit_db_password = str(
    config["configurations"]["admin-properties"]["audit_db_password"]
  )

ugsync_jceks_path = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.credstore.filename"
]
ugsync_cred_lib = os.path.join(usersync_home, "lib", "*")
cred_lib_path = os.path.join(ranger_home, "cred", "lib", "*")
cred_setup_prefix = (
  format("{ranger_home}/ranger_credential_helper.py"),
  "-l",
  cred_lib_path,
)
ranger_audit_source_type = config["configurations"]["ranger-admin-site"][
  "ranger.audit.source.type"
]

ranger_usersync_keystore_password = str(
  config["configurations"]["ranger-ugsync-site"]["ranger.usersync.keystore.password"]
)
ranger_usersync_ldap_ldapbindpassword = str(
  config["configurations"]["ranger-ugsync-site"][
    "ranger.usersync.ldap.ldapbindpassword"
  ]
)
ranger_usersync_truststore_password = str(
  config["configurations"]["ranger-ugsync-site"]["ranger.usersync.truststore.password"]
)
ranger_usersync_keystore_file = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.keystore.file"
]
default_dn_name = "cn=unixauthservice,ou=authenticator,o=mycompany,c=US"

ranger_admin_hosts = config["clusterHostInfo"]["ranger_admin_hosts"]
is_ranger_ha_enabled = True if len(ranger_admin_hosts) > 1 else False
ranger_ug_ldap_url = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.ldap.url"
]
ranger_ug_ldap_bind_dn = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.ldap.binddn"
]
ranger_ug_ldap_user_searchfilter = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.ldap.user.searchfilter"
]
ranger_ug_ldap_group_searchbase = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.group.searchbase"
]
ranger_ug_ldap_group_searchfilter = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.group.searchfilter"
]
ug_sync_source = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.source.impl.class"
]
current_host = config["agentLevelParams"]["hostname"]
if current_host in ranger_admin_hosts:
  ranger_host = current_host

# ranger-tagsync
ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
has_ranger_tagsync = len(ranger_tagsync_hosts) > 0

tagsync_log_dir = default(
  "/configurations/ranger-tagsync-site/ranger.tagsync.logdir", "/var/log/ranger/tagsync"
)
tagsync_jceks_path = config["configurations"]["ranger-tagsync-site"][
  "ranger.tagsync.keystore.filename"
]
atlas_tagsync_jceks_path = config["configurations"]["ranger-tagsync-site"][
  "ranger.tagsync.source.atlasrest.keystore.filename"
]
tagsync_application_properties = (
  dict(config["configurations"]["tagsync-application-properties"])
  if has_ranger_tagsync
  else None
)
tagsync_pid_file = format("{ranger_pid_dir}/tagsync.pid")
tagsync_cred_lib = os.path.join(ranger_tagsync_home, "lib", "*")


# ranger logback.xml
admin_logback_content = config["configurations"]["admin-logback"]["content"]
usersync_logback_content = config["configurations"]["usersync-logback"]["content"]
tagsync_logback_content = config["configurations"]["tagsync-logback"]["content"]

# ranger kerberos
security_enabled = config["configurations"]["cluster-env"]["security_enabled"]
namenode_hosts = default("/clusterHostInfo/namenode_hosts", [])
has_namenode = len(namenode_hosts) > 0

ugsync_policymgr_alias = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.policymgr.alias"
]
ugsync_policymgr_keystore = config["configurations"]["ranger-ugsync-site"][
  "ranger.usersync.policymgr.keystore"
]

# ranger solr
audit_solr_enabled = default(
  "/configurations/ranger-env/xasecure.audit.destination.solr", False
)
ranger_solr_config_set = config["configurations"]["ranger-env"][
  "ranger_solr_config_set"
]
ranger_solr_collection_name = config["configurations"]["ranger-env"][
  "ranger_solr_collection_name"
]
ranger_solr_shards = config["configurations"]["ranger-env"]["ranger_solr_shards"]
replication_factor = config["configurations"]["ranger-env"][
  "ranger_solr_replication_factor"
]
ranger_solr_conf = format("{ranger_home}/contrib/solr_for_audit_setup/conf")
infra_solr_hosts = default("/clusterHostInfo/infra_solr_hosts", [])
has_infra_solr = len(infra_solr_hosts) > 0
is_solrCloud_enabled = default("/configurations/ranger-env/is_solrCloud_enabled", False)
is_external_solrCloud_enabled = default(
  "/configurations/ranger-env/is_external_solrCloud_enabled", False
)
solr_znode = "/ranger_audits"
if stack_supports_infra_client and is_solrCloud_enabled:
  solr_znode = default(
    "/configurations/ranger-admin-site/ranger.audit.solr.zookeepers", "NONE"
  )
  if solr_znode != "" and solr_znode.upper() != "NONE":
    solr_znode = solr_znode.split("/")
    if len(solr_znode) > 1 and len(solr_znode) == 2:
      solr_znode = solr_znode[1]
      solr_znode = format("/{solr_znode}")
  if has_infra_solr and not is_external_solrCloud_enabled:
    solr_znode = config["configurations"]["infra-solr-env"]["infra_solr_znode"]
solr_user = unix_user
if has_infra_solr and not is_external_solrCloud_enabled:
  solr_user = default("/configurations/infra-solr-env/infra_solr_user", unix_user)
  infra_solr_role_ranger_admin = default(
    "configurations/infra-solr-security-json/infra_solr_role_ranger_admin",
    "ranger_user",
  )
  infra_solr_role_ranger_audit = default(
    "configurations/infra-solr-security-json/infra_solr_role_ranger_audit",
    "ranger_audit_user",
  )
  infra_solr_role_dev = default(
    "configurations/infra-solr-security-json/infra_solr_role_dev", "dev"
  )
custom_log4j = has_infra_solr and not is_external_solrCloud_enabled

ranger_audit_max_retention_days = config["configurations"]["ranger-solr-configuration"][
  "ranger_audit_max_retention_days"
]
ranger_audit_logs_merge_factor = config["configurations"]["ranger-solr-configuration"][
  "ranger_audit_logs_merge_factor"
]
ranger_solr_config_content = config["configurations"]["ranger-solr-configuration"][
  "content"
]

# get comma separated list of zookeeper hosts
zookeeper_port = default("/configurations/zoo.cfg/clientPort", None)
zookeeper_hosts = default("/clusterHostInfo/zookeeper_server_hosts", [])
index = 0
zookeeper_quorum = ""
for host in zookeeper_hosts:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(zookeeper_hosts):
    zookeeper_quorum += ","

# solr kerberised
solr_jaas_file = None
is_external_solrCloud_kerberos = default(
  "/configurations/ranger-env/is_external_solrCloud_kerberos", False
)

if security_enabled:
  if has_ranger_tagsync:
    ranger_tagsync_principal = config["configurations"]["ranger-tagsync-site"][
      "ranger.tagsync.kerberos.principal"
    ]
    if not is_empty(ranger_tagsync_principal) and ranger_tagsync_principal != "":
      tagsync_jaas_principal = ranger_tagsync_principal.replace(
        "_HOST", current_host.lower()
      )
    tagsync_keytab_path = config["configurations"]["ranger-tagsync-site"][
      "ranger.tagsync.kerberos.keytab"
    ]

  if stack_supports_ranger_kerberos:
    ranger_admin_keytab = config["configurations"]["ranger-admin-site"][
      "ranger.admin.kerberos.keytab"
    ]
    ranger_admin_principal = config["configurations"]["ranger-admin-site"][
      "ranger.admin.kerberos.principal"
    ]
    if not is_empty(ranger_admin_principal) and ranger_admin_principal != "":
      ranger_admin_jaas_principal = ranger_admin_principal.replace(
        "_HOST", ranger_host.lower()
      )
      if (
        stack_supports_infra_client
        and is_solrCloud_enabled
        and is_external_solrCloud_enabled
        and is_external_solrCloud_kerberos
      ):
        solr_jaas_file = format("{ranger_home}/conf/ranger_solr_jaas.conf")
        solr_kerberos_principal = ranger_admin_jaas_principal
        solr_kerberos_keytab = ranger_admin_keytab
      if (
        stack_supports_infra_client
        and is_solrCloud_enabled
        and not is_external_solrCloud_enabled
        and not is_external_solrCloud_kerberos
      ):
        solr_jaas_file = format("{ranger_home}/conf/ranger_solr_jaas.conf")
        solr_kerberos_principal = ranger_admin_jaas_principal
        solr_kerberos_keytab = ranger_admin_keytab

# logic to create core-site.xml if hdfs not installed
if stack_supports_ranger_kerberos and not has_namenode:
  core_site_property = {
    "hadoop.security.authentication": "kerberos" if security_enabled else "simple"
  }

  if security_enabled:
    realm = "EXAMPLE.COM"
    ranger_admin_bare_principal = "rangeradmin"
    ranger_usersync_bare_principal = "rangerusersync"
    ranger_tagsync_bare_principal = "rangertagsync"

    ranger_usersync_principal = config["configurations"]["ranger-ugsync-site"][
      "ranger.usersync.kerberos.principal"
    ]
    if not is_empty(ranger_admin_principal) and ranger_admin_principal != "":
      ranger_admin_bare_principal = get_bare_principal(ranger_admin_principal)
    if not is_empty(ranger_usersync_principal) and ranger_usersync_principal != "":
      ranger_usersync_bare_principal = get_bare_principal(ranger_usersync_principal)
    realm = config["configurations"]["kerberos-env"]["realm"]

    rule_dict = [
      {"principal": ranger_admin_bare_principal, "user": unix_user},
      {"principal": ranger_usersync_bare_principal, "user": "rangerusersync"},
    ]

    if has_ranger_tagsync:
      if not is_empty(ranger_tagsync_principal) and ranger_tagsync_principal != "":
        ranger_tagsync_bare_principal = get_bare_principal(ranger_tagsync_principal)
      rule_dict.append(
        {"principal": ranger_tagsync_bare_principal, "user": "rangertagsync"}
      )

    core_site_auth_to_local_property = ""
    for item in range(len(rule_dict)):
      rule_line = f"RULE:[2:$1@$0]({rule_dict[item]['principal']}@{realm})s/.*/{rule_dict[item]['user']}/\n"
      core_site_auth_to_local_property = rule_line + core_site_auth_to_local_property

    core_site_auth_to_local_property = core_site_auth_to_local_property + "DEFAULT"
    core_site_property["hadoop.security.auth_to_local"] = (
      core_site_auth_to_local_property
    )

upgrade_type = Script.get_upgrade_type(default("/commandParams/upgrade_type", ""))

# ranger service pid
user_group = config["configurations"]["cluster-env"]["user_group"]
ranger_admin_pid_file = format("{ranger_pid_dir}/rangeradmin.pid")
ranger_usersync_pid_file = format("{ranger_pid_dir}/usersync.pid")

# admin credential
admin_username = config["configurations"]["ranger-env"]["admin_username"]
admin_password = config["configurations"]["ranger-env"]["admin_password"]
default_admin_password = "admin"

ranger_is_solr_kerberised = "false"
if audit_solr_enabled and is_solrCloud_enabled:
  # Check internal solrCloud
  if security_enabled and not is_external_solrCloud_enabled:
    ranger_is_solr_kerberised = "true"
  # Check external solrCloud
  if is_external_solrCloud_enabled and is_external_solrCloud_kerberos:
    ranger_is_solr_kerberised = "true"

hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
is_hbase_ha_enabled = True if len(hbase_master_hosts) > 1 else False
is_namenode_ha_enabled = True if len(namenode_hosts) > 1 else False
ranger_hbase_plugin_enabled = False
ranger_hdfs_plugin_enabled = False


if is_hbase_ha_enabled:
  if not is_empty(
    config["configurations"]["ranger-hbase-plugin-properties"][
      "ranger-hbase-plugin-enabled"
    ]
  ):
    ranger_hbase_plugin_enabled = (
      config["configurations"]["ranger-hbase-plugin-properties"][
        "ranger-hbase-plugin-enabled"
      ].lower()
      == "yes"
    )
if is_namenode_ha_enabled:
  if not is_empty(
    config["configurations"]["ranger-hdfs-plugin-properties"][
      "ranger-hdfs-plugin-enabled"
    ]
  ):
    ranger_hdfs_plugin_enabled = (
      config["configurations"]["ranger-hdfs-plugin-properties"][
        "ranger-hdfs-plugin-enabled"
      ].lower()
      == "yes"
    )

ranger_admin_password_properties = [
  "ranger.jpa.jdbc.password",
  "ranger.jpa.audit.jdbc.password",
  "ranger.ldap.bind.password",
  "ranger.ldap.ad.bind.password",
]
ranger_usersync_password_properties = ["ranger.usersync.ldap.ldapbindpassword"]
ranger_tagsync_password_properties = [
  "xasecure.policymgr.clientssl.keystore.password",
  "xasecure.policymgr.clientssl.truststore.password",
]
if stack_supports_secure_ssl_password:
  ranger_admin_password_properties.extend(
    ["ranger.service.https.attrib.keystore.pass", "ranger.truststore.password"]
  )
  ranger_usersync_password_properties.extend(
    ["ranger.usersync.keystore.password", "ranger.usersync.truststore.password"]
  )

ranger_auth_method = config["configurations"]["ranger-admin-site"][
  "ranger.authentication.method"
]
ranger_ldap_password_alias = default(
  "/configurations/ranger-admin-site/ranger.ldap.binddn.credential.alias",
  "ranger.ldap.bind.password",
)
ranger_ad_password_alias = default(
  "/configurations/ranger-admin-site/ranger.ldap.ad.binddn.credential.alias",
  "ranger.ldap.ad.bind.password",
)
ranger_https_keystore_alias = default(
  "/configurations/ranger-admin-site/ranger.service.https.attrib.keystore.credential.alias",
  "keyStoreCredentialAlias",
)
ranger_truststore_alias = default(
  "/configurations/ranger-admin-site/ranger.truststore.alias", "trustStoreAlias"
)
https_enabled = config["configurations"]["ranger-admin-site"][
  "ranger.service.https.attrib.ssl.enabled"
]
http_enabled = config["configurations"]["ranger-admin-site"][
  "ranger.service.http.enabled"
]
https_keystore_password = config["configurations"]["ranger-admin-site"][
  "ranger.service.https.attrib.keystore.pass"
]
truststore_password = config["configurations"]["ranger-admin-site"][
  "ranger.truststore.password"
]

# need this to capture cluster name for ranger tagsync
cluster_name = config["clusterName"]
ranger_ldap_bind_auth_password = config["configurations"]["ranger-admin-site"][
  "ranger.ldap.bind.password"
]
ranger_ad_bind_auth_password = config["configurations"]["ranger-admin-site"][
  "ranger.ldap.ad.bind.password"
]

ranger_env_content = config["configurations"]["ranger-env"]["content"]
is_ranger_admin_host = "role" in config and config["role"] == "RANGER_ADMIN"
is_ranger_usersync_host = "role" in config and config["role"] == "RANGER_USERSYNC"
is_ranger_tagsync_host = "role" in config and config["role"] == "RANGER_TAGSYNC"

# zookeeper principal
zookeeper_principal = default(
  "/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper@EXAMPLE.COM"
)
zookeeper_principal_primary = get_bare_principal(zookeeper_principal)

# rangerusersync user credential
rangerusersync_username = "rangerusersync"
rangerusersync_user_password = config["configurations"]["ranger-env"][
  "rangerusersync_user_password"
]
default_rangerusersync_user_password = "rangerusersync"

# rangertagsync user credential
rangertagsync_username = "rangertagsync"
rangertagsync_user_password = config["configurations"]["ranger-env"][
  "rangertagsync_user_password"
]
default_rangertagsync_user_password = "rangertagsync"

# keyadmin user credential
keyadmin_username = "keyadmin"
keyadmin_user_password = config["configurations"]["ranger-env"][
  "keyadmin_user_password"
]
default_keyadmin_user_password = "keyadmin"

# atlas admin user password
atlas_admin_password = default(
  "/configurations/atlas-env/atlas.admin.password", "admin"
)

mount_table_content = None
if "viewfs-mount-table" in config["configurations"]:
  xml_inclusion_file_name = "viewfs-mount-table.xml"
  mount_table = config["configurations"]["viewfs-mount-table"]

  if "content" in mount_table and mount_table["content"].strip():
    mount_table_content = mount_table["content"]

# Ranger Services maximum heap size configurations
ranger_admin_max_heap_size = default(
  "/configurations/ranger-env/ranger_admin_max_heap_size", "1g"
)
ranger_usersync_max_heap_size = default(
  "/configurations/ranger-env/ranger_usersync_max_heap_size", "1g"
)
ranger_tagsync_max_heap_size = default(
  "/configurations/ranger-env/ranger_tagsync_max_heap_size", "1g"
)

# add zoneName field in ranger_audits collection when the current stack support security zone feature during upgrade
add_zoneName_field = {
  "add-field": {"name": "zoneName", "type": "key_lower_case", "multiValued": False}
}
infra_solr_ssl_enabled = default(
  "/configurations/infra-solr-env/infra_solr_ssl_enabled", False
)
infra_solr_protocol = "https" if infra_solr_ssl_enabled else "http"
infra_solr_port = default("/configurations/infra-solr-env/infra_solr_port", "8886")
if has_infra_solr:
  infra_solr_host = infra_solr_hosts[0]
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
