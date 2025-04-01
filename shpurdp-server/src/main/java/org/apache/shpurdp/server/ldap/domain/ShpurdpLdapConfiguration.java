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

package org.apache.shpurdp.server.ldap.domain;

import static java.lang.Boolean.parseBoolean;

import java.util.HashMap;
import java.util.Map;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfiguration;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.configuration.LdapUsernameCollisionHandlingBehavior;
import org.apache.shpurdp.server.security.authorization.LdapServerProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class is a representation of all the LDAP related configuration properties.
 * <p>
 * <strong>IMPORTANT: </strong>in case you declare any new LDAP related property
 * please do it in the Python class
 * <code>stacks.shpurdp_configuration.ShpurdpLDAPConfiguration</code> too.
 */
public class ShpurdpLdapConfiguration extends ShpurdpServerConfiguration {

  public ShpurdpLdapConfiguration() {
    this(null);
  }

  public ShpurdpLdapConfiguration(Map<String, String> configurationMap) {
    super(configurationMap);
  }
  
  @Override
  protected ShpurdpServerConfigurationCategory getCategory() {
    return ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION;
  }

  public boolean isShpurdpManagesLdapConfiguration() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.SHPURDP_MANAGES_LDAP_CONFIGURATION));
  }

  public String getLdapEnabledServices() {
    return configValue(ShpurdpServerConfigurationKey.LDAP_ENABLED_SERVICES);
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.LDAP_ENABLED));
  }

  public String serverHost() {
    return configValue(ShpurdpServerConfigurationKey.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.parseInt(configValue(ShpurdpServerConfigurationKey.SERVER_PORT));
  }

  public String serverUrl() {
    return serverHost() + ":" + serverPort();
  }

  public String secondaryServerHost() {
    return configValue(ShpurdpServerConfigurationKey.SECONDARY_SERVER_HOST);
  }

  public int secondaryServerPort() {
    final String secondaryServerPort = configValue(ShpurdpServerConfigurationKey.SECONDARY_SERVER_PORT);
    return secondaryServerPort == null ? 0 : Integer.parseInt(secondaryServerPort);
  }

  public String secondaryServerUrl() {
    return secondaryServerHost() + ":" + secondaryServerPort();
  }

  public boolean useSSL() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.USE_SSL));
  }

  public String trustStore() {
    return configValue(ShpurdpServerConfigurationKey.TRUST_STORE);
  }

  public String trustStoreType() {
    return configValue(ShpurdpServerConfigurationKey.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return configValue(ShpurdpServerConfigurationKey.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return configValue(ShpurdpServerConfigurationKey.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return configValue(ShpurdpServerConfigurationKey.BIND_DN);
  }

  public String bindPassword() {
    return configValue(ShpurdpServerConfigurationKey.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return configValue(ShpurdpServerConfigurationKey.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return configValue(ShpurdpServerConfigurationKey.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return configValue(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return configValue(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return configValue(ShpurdpServerConfigurationKey.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_SEARCH_BASE);
  }

  public String groupMappingRules() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_MAPPING_RULES);
  }

  public String userSearchFilter() {
    return configValue(ShpurdpServerConfigurationKey.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return configValue(ShpurdpServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return configValue(ShpurdpServerConfigurationKey.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return configValue(ShpurdpServerConfigurationKey.REFERRAL_HANDLING);
  }

  public boolean disableEndpointIdentification() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.DISABLE_ENDPOINT_IDENTIFICATION));
  }

  @Override
  public Map<String, String> toMap() {
    return new HashMap<>(configurationMap);
  }

  @Override
  public String toString() {
    return configurationMap.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ShpurdpLdapConfiguration that = (ShpurdpLdapConfiguration) o;

    return new EqualsBuilder().append(configurationMap, that.configurationMap).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(configurationMap).toHashCode();
  }

  public boolean isLdapAlternateUserSearchEnabled() {
    return Boolean.valueOf(configValue(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_ENABLED));
  }

  public LdapServerProperties getLdapServerProperties() {
    final LdapServerProperties ldapServerProperties = new LdapServerProperties();

    ldapServerProperties.setPrimaryUrl(serverUrl());
    if (StringUtils.isNotBlank(secondaryServerHost())) {
      ldapServerProperties.setSecondaryUrl(secondaryServerUrl());
    }
    ldapServerProperties.setUseSsl(parseBoolean(configValue(ShpurdpServerConfigurationKey.USE_SSL)));
    ldapServerProperties.setAnonymousBind(parseBoolean(configValue(ShpurdpServerConfigurationKey.ANONYMOUS_BIND)));
    ldapServerProperties.setManagerDn(configValue(ShpurdpServerConfigurationKey.BIND_DN));
    ldapServerProperties.setManagerPassword(configValue(ShpurdpServerConfigurationKey.BIND_PASSWORD));
    ldapServerProperties.setBaseDN(configValue(ShpurdpServerConfigurationKey.USER_SEARCH_BASE));
    ldapServerProperties.setUsernameAttribute(configValue(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE));
    ldapServerProperties.setForceUsernameToLowercase(parseBoolean(configValue(ShpurdpServerConfigurationKey.FORCE_LOWERCASE_USERNAMES)));
    ldapServerProperties.setUserBase(configValue(ShpurdpServerConfigurationKey.USER_BASE));
    ldapServerProperties.setUserObjectClass(configValue(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS));
    ldapServerProperties.setDnAttribute(configValue(ShpurdpServerConfigurationKey.DN_ATTRIBUTE));
    ldapServerProperties.setGroupBase(configValue(ShpurdpServerConfigurationKey.GROUP_BASE));
    ldapServerProperties.setGroupObjectClass(configValue(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS));
    ldapServerProperties.setGroupMembershipAttr(configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE));
    ldapServerProperties.setGroupNamingAttr(configValue(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE));
    ldapServerProperties.setAdminGroupMappingRules(configValue(ShpurdpServerConfigurationKey.GROUP_MAPPING_RULES));
    ldapServerProperties.setAdminGroupMappingMemberAttr("");
    ldapServerProperties.setUserSearchFilter(configValue(ShpurdpServerConfigurationKey.USER_SEARCH_FILTER));
    ldapServerProperties.setAlternateUserSearchFilter(configValue(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER));
    ldapServerProperties.setGroupSearchFilter(configValue(ShpurdpServerConfigurationKey.GROUP_SEARCH_FILTER));
    ldapServerProperties.setReferralMethod(configValue(ShpurdpServerConfigurationKey.REFERRAL_HANDLING));
    ldapServerProperties.setSyncUserMemberReplacePattern(configValue(ShpurdpServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncGroupMemberReplacePattern(configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncUserMemberFilter(configValue(ShpurdpServerConfigurationKey.USER_MEMBER_FILTER));
    ldapServerProperties.setSyncGroupMemberFilter(configValue(ShpurdpServerConfigurationKey.GROUP_MEMBER_FILTER));
    ldapServerProperties.setPaginationEnabled(parseBoolean(configValue(ShpurdpServerConfigurationKey.PAGINATION_ENABLED)));
    ldapServerProperties.setDisableEndpointIdentification(disableEndpointIdentification());

    if (hasAnyValueWithKey(ShpurdpServerConfigurationKey.GROUP_BASE, ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS, ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE,
        ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE, ShpurdpServerConfigurationKey.GROUP_MAPPING_RULES, ShpurdpServerConfigurationKey.GROUP_SEARCH_FILTER)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  private boolean hasAnyValueWithKey(ShpurdpServerConfigurationKey... shpurdpServerConfigurationKey) {
    for (ShpurdpServerConfigurationKey key : shpurdpServerConfigurationKey) {
      if (configurationMap.containsKey(key.key())) {
        return true;
      }
    }
    return false;
  }

  public LdapUsernameCollisionHandlingBehavior syncCollisionHandlingBehavior() {
    if ("skip".equalsIgnoreCase(configValue(ShpurdpServerConfigurationKey.COLLISION_BEHAVIOR))) {
      return LdapUsernameCollisionHandlingBehavior.SKIP;
    }
    return LdapUsernameCollisionHandlingBehavior.CONVERT;
  }

  private String configValue(ShpurdpServerConfigurationKey shpurdpManagesLdapConfiguration) {
    return getValue(shpurdpManagesLdapConfiguration, configurationMap);
  }

}
