/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.configuration;

import static org.apache.shpurdp.server.configuration.ConfigurationPropertyType.PASSWORD;
import static org.apache.shpurdp.server.configuration.ConfigurationPropertyType.PLAINTEXT;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants representing supported LDAP related property names
 */
public enum ShpurdpServerConfigurationKey {

  /* ********************************************************
   * LDAP Configuration Keys
   * ******************************************************** */
  SHPURDP_MANAGES_LDAP_CONFIGURATION(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.manage_services", PLAINTEXT, "false", "A Boolean value indicating whether Shpurdp is to manage the LDAP configuration for services or not.", false),
  LDAP_ENABLED_SERVICES(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.enabled_services", PLAINTEXT, null, "A comma-delimited list of services that are expected to be configured for LDAP.  A \"*\" indicates all services.", false),

  LDAP_ENABLED(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.authentication.enabled", PLAINTEXT, "false", "An internal property used for unit testing and development purposes.", false),
  SERVER_HOST(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.server.host", PLAINTEXT, "localhost", "The LDAP URL host used for connecting to an LDAP server when authenticating users.", false),
  SERVER_PORT(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.server.port", PLAINTEXT, "33389", "The LDAP URL port used for connecting to an LDAP server when authenticating users.", false),
  SECONDARY_SERVER_HOST(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.secondary.server.host", PLAINTEXT, null, "A second LDAP URL host to use as a backup when authenticating users.", false),
  SECONDARY_SERVER_PORT(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.secondary.server.port", PLAINTEXT, null, "A second LDAP URL port to use as a backup when authenticating users.", false),
  USE_SSL(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.use_ssl", PLAINTEXT, "false", "Determines whether to use LDAP over SSL (LDAPS).", false),

  TRUST_STORE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.trust_store", PLAINTEXT, "", "", false), //TODO
  TRUST_STORE_TYPE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.trust_store.type", PLAINTEXT, null, "The type of truststore used by the 'javax.net.ssl.trustStoreType' property.", false),
  TRUST_STORE_PATH(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.trust_store.path", PLAINTEXT, null, "The location of the truststore to use when setting the 'javax.net.ssl.trustStore' property.", false),
  TRUST_STORE_PASSWORD(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.trust_store.password", PASSWORD, null, "The password to use when setting the 'javax.net.ssl.trustStorePassword' property", false),
  ANONYMOUS_BIND(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.anonymous_bind", PLAINTEXT, "true", "Determines whether LDAP requests can connect anonymously or if a managed user is required to connect.", false),

  BIND_DN(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.bind_dn", PLAINTEXT, null, "The DN of the manager account to use when binding to LDAP if anonymous binding is disabled.", false),
  BIND_PASSWORD(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.connectivity.bind_password", PASSWORD, null, "The password for the manager account used to bind to LDAP if anonymous binding is disabled.", false),

  ATTR_DETECTION(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.detection", PLAINTEXT, "", "", false), //TODO

  DN_ATTRIBUTE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.dn_attr", PLAINTEXT, "dn", "The attribute used for determining what the distinguished name property is.", false),

  USER_OBJECT_CLASS(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.user.object_class", PLAINTEXT, "person", "The class to which user objects in LDAP belong.", false),
  USER_NAME_ATTRIBUTE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.user.name_attr", PLAINTEXT, "uid", "The attribute used for determining the user name, such as 'uid'.", false),
  USER_GROUP_MEMBER_ATTRIBUTE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.user.group_member_attr", PLAINTEXT, "memberof", "The LDAP attribute which identifies user group membership.", false),
  USER_SEARCH_BASE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.user.search_base", PLAINTEXT, "dc=shpurdp,dc=apache,dc=org", "The base DN to use when filtering LDAP users and groups. This is only used when LDAP authentication is enabled.", false),
  USER_BASE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.search_user_base", PLAINTEXT, "ou=people,dc=shpurdp,dc=apache,dc=org", "The filter used when searching for users in LDAP.", false),

  GROUP_OBJECT_CLASS(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.group.object_class", PLAINTEXT, "posixGroup", "Specifies the LDAP object class value that defines groups in the directory service.", false),
  GROUP_NAME_ATTRIBUTE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.group.name_attr", PLAINTEXT, "cn", "The attribute used to determine the group name in LDAP.", false),
  GROUP_MEMBER_ATTRIBUTE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.group.member_attr", PLAINTEXT, "member", "The LDAP attribute which identifies group membership.", false),
  GROUP_SEARCH_BASE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.group.search_base", PLAINTEXT, "dc=shpurdp,dc=apache,dc=org", "The base DN to use when filtering LDAP users and groups. This is only used when LDAP authentication is enabled.", false),
  GROUP_BASE(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.attributes.group.search_group_base", PLAINTEXT, "ou=groups,dc=shpurdp,dc=apache,dc=org", "The filter used when searching for groups in LDAP.", false),

  USER_SEARCH_FILTER(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.user_search_filter", PLAINTEXT, "(&({usernameAttribute}={0})(objectClass={userObjectClass}))", "A filter used to lookup a user in LDAP based on the Shpurdp user name.", false),
  USER_MEMBER_REPLACE_PATTERN(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.user_member_replace_pattern", PLAINTEXT, "", "Regex pattern to use when replacing the user member attribute ID value with a placeholder. This is used in cases where a UID of an LDAP member is not a full CN or unique ID (e.g.: 'member: <SID=123>;<GID=123>;cn=myCn,dc=org,dc=apache')", false),
  USER_MEMBER_FILTER(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.user_member_filter", PLAINTEXT, "", "Filter to use for syncing user members of a group from LDAP (by default it is not used). For example: (&(objectclass=posixaccount)(uid={member}))", false),

  ALTERNATE_USER_SEARCH_ENABLED(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.alternate_user_search_enabled", PLAINTEXT, "false", "Determines whether a secondary (alternate) LDAP user search filer is used if the primary filter fails to find a user.", false),
  ALTERNATE_USER_SEARCH_FILTER(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.alternate_user_search_filter", PLAINTEXT, "(&(userPrincipalName={0})(objectClass={userObjectClass}))", "An alternate LDAP user search filter which can be used if 'authentication.ldap.alternateUserSearchEnabled' is enabled and the primary filter fails to find a user.", false),

  GROUP_SEARCH_FILTER(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.group_search_filter", PLAINTEXT, "", "The DN to use when searching for LDAP groups.", false),
  GROUP_MEMBER_REPLACE_PATTERN(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.group_member_replace_pattern", PLAINTEXT, "", "Regex pattern to use when replacing the group member attribute ID value with a placeholder. This is used in cases where a UID of an LDAP member is not a full CN or unique ID (e.g.: 'member: <SID=123>;<GID=123>;cn=myCn,dc=org,dc=apache')", false),
  GROUP_MEMBER_FILTER(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.group_member_filter", PLAINTEXT, "", "Filter to use for syncing group members of a group from LDAP. (by default it is not used). For example: (&(objectclass=posixgroup)(cn={member}))", false),
  GROUP_MAPPING_RULES(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.group_mapping_rules", PLAINTEXT, "Shpurdp Administrators", "A comma-separate list of groups which would give a user administrative access to Shpurdp when syncing from LDAP. This is only used when 'authorization.ldap.groupSearchFilter' is blank. For instance: Hadoop Admins, Hadoop Admins.*, DC Admins, .*Hadoop Operators", false),

  FORCE_LOWERCASE_USERNAMES(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.force_lowercase_usernames", PLAINTEXT, "", "Declares whether to force the ldap user name to be lowercase or leave as-is.\nThis is useful when local user names are expected to be lowercase but the LDAP user names are not.", false),
  REFERRAL_HANDLING(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.referrals", PLAINTEXT, "follow", "Determines whether to follow LDAP referrals to other URLs when the LDAP controller doesn't have the requested object.", false),
  PAGINATION_ENABLED(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.pagination_enabled", PLAINTEXT, "true", "Determines whether results from LDAP are paginated when requested.", false),
  COLLISION_BEHAVIOR(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.collision_behavior", PLAINTEXT, "convert", "Determines how to handle username collision while updating from LDAP.", false),
  DISABLE_ENDPOINT_IDENTIFICATION(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "shpurdp.ldap.advanced.disable_endpoint_identification", PLAINTEXT, "false", "Determines whether to disable endpoint identification (hostname verification) during SSL handshake while updating from LDAP.", false),

  /* ********************************************************
   * SSO Configuration Keys
   * ******************************************************** */
  SSO_MANAGE_SERVICES(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.manage_services", PLAINTEXT, "false", "A Boolean value indicating whether Shpurdp is to manage the SSO configuration for services or not.", false),
  SSO_ENABLED_SERVICES(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.enabled_services", PLAINTEXT, null, "A comma-delimited list of services that are expected to be configured for SSO.  A \"*\" indicates all services.", false),

  SSO_PROVIDER_URL(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.provider.url", PLAINTEXT, null, "The URL for SSO provider to use in the absence of a JWT token when handling a JWT request.", false),
  SSO_PROVIDER_CERTIFICATE(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.provider.certificate", PLAINTEXT, null, "The x509 certificate containing the public key to use when verifying the authenticity of a JWT token from the SSO provider.", false),
  SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.provider.originalUrlParamName", PLAINTEXT, "originalUrl", "The original URL to use when constructing the URL for SSO provider.", false),

  SSO_JWT_AUDIENCES(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.jwt.audiences", PLAINTEXT, null, "A list of the JWT audiences expected. Leaving this blank will allow for any audience.", false),
  SSO_JWT_COOKIE_NAME(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.jwt.cookieName", PLAINTEXT, "hadoop-jwt", "The name of the cookie which will be used to extract the JWT token from the request.", false),

  SSO_AUTHENTICATION_ENABLED(ShpurdpServerConfigurationCategory.SSO_CONFIGURATION, "shpurdp.sso.authentication.enabled", PLAINTEXT, "false", "Determines whether to use JWT authentication when logging into Shpurdp.", false),

  /* ********************************************************
   * Trusted Proxy Configuration Keys
   * ******************************************************** */
  TPROXY_AUTHENTICATION_ENABLED(ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION, "shpurdp.tproxy.authentication.enabled", PLAINTEXT, "false", "Determines whether to allow a proxy user to specifiy a proxied user when logging into Shpurdp.", false),
  TPROXY_ALLOWED_HOSTS(ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION, "shpurdp\\.tproxy\\.proxyuser\\..+\\.hosts", PLAINTEXT, "", "List of hosts from which trusted-proxy user can connect.", true),
  TPROXY_ALLOWED_USERS(ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION, "shpurdp\\.tproxy\\.proxyuser\\..+\\.users", PLAINTEXT, "", "List of users which the trusted-proxy user can proxy for.", true),
  TPROXY_ALLOWED_GROUPS(ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION, "shpurdp\\.tproxy\\.proxyuser\\..+\\.groups", PLAINTEXT, "", "List of groups which the trusted-proxy user can proxy user for.", true);

  private static final Logger LOG = LoggerFactory.getLogger(ShpurdpServerConfigurationKey.class);

  private final ShpurdpServerConfigurationCategory configurationCategory;
  private final String propertyName;
  private final ConfigurationPropertyType configurationPropertyType;
  private final String defaultValue;
  private final String description;
  private final boolean regex;

  ShpurdpServerConfigurationKey(ShpurdpServerConfigurationCategory configurationCategory, String propName, ConfigurationPropertyType configurationPropertyType, String defaultValue, String description, boolean regex) {
    this.configurationCategory = configurationCategory;
    this.propertyName = propName;
    this.configurationPropertyType = configurationPropertyType;
    this.defaultValue = defaultValue;
    this.description = description;
    this.regex = regex;
  }

  public ShpurdpServerConfigurationCategory getConfigurationCategory() {
    return configurationCategory;
  }

  public String key() {
    return this.propertyName;
  }

  public ConfigurationPropertyType getConfigurationPropertyType() {
    return configurationPropertyType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getDescription() {
    return description;
  }

  public boolean isRegex() {
    return regex;
  }

  public static ShpurdpServerConfigurationKey translate(ShpurdpServerConfigurationCategory category, String keyName) {
    if (category != null && StringUtils.isNotEmpty(keyName)) {
      for (ShpurdpServerConfigurationKey key : values()) {
        if (key.configurationCategory.equals(category)) {
          if ((key.regex && keyName.matches(key.propertyName)) || key.propertyName.equals(keyName)) {
            return key;
          }
        }
      }
    }

    String categoryName = (category == null) ? "null" : category.getCategoryName();
    LOG.warn("Invalid Shpurdp server configuration key: {}:{}", categoryName, keyName);
    return null;
  }
  
  public static Set<String> findPasswordConfigurations() {
      return Stream.of(ShpurdpServerConfigurationKey.values()).filter(k -> PASSWORD == k.getConfigurationPropertyType()).map(f -> f.propertyName).collect(Collectors.toSet());
  }
}
