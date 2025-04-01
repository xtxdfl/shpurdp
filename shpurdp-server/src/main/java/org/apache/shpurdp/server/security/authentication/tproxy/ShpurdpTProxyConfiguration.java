/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shpurdp.server.security.authentication.tproxy;

import java.util.Map;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfiguration;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.collect.ImmutableMap;

/**
 * ShpurdpTProxyConfiguration is a {@link ShpurdpServerConfiguration} implementation that contains
 * configuration data for the Shpurdp server tproxy-configuration configuration properties.
 */
public class ShpurdpTProxyConfiguration extends ShpurdpServerConfiguration {

  private static final String TEMPLATE_PROXY_USER_ALLOWED_HOSTS = "shpurdp.tproxy.proxyuser.%s.hosts";
  private static final String TEMPLATE_PROXY_USER_ALLOWED_USERS = "shpurdp.tproxy.proxyuser.%s.users";
  private static final String TEMPLATE_PROXY_USER_ALLOWED_GROUPS = "shpurdp.tproxy.proxyuser.%s.groups";

  /**
   * Constructor
   * <p>
   * Copies the given configuration propery map into an {@link ImmutableMap} and pulls out propery
   * values upon request.
   *
   * @param configurationMap a map of property names to values
   */
  ShpurdpTProxyConfiguration(Map<String, String> configurationMap) {
    super(configurationMap);
  }
  
  @Override
  protected ShpurdpServerConfigurationCategory getCategory() {
    return ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION;
  }

  /**
   * Determines of trusted proxy support is enabled based on the configuration data.
   *
   * @return <code>true</code> if trusted proxy support is enabled; <code>false</code> otherwise
   * @see ShpurdpServerConfigurationKey#TPROXY_AUTHENTICATION_ENABLED
   */
  public boolean isEnabled() {
    return Boolean.valueOf(getValue(ShpurdpServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED, configurationMap));
  }

  public String getAllowedHosts(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_HOSTS, proxyUser),
        configurationMap,
        ShpurdpServerConfigurationKey.TPROXY_ALLOWED_HOSTS.getDefaultValue());
  }

  public String getAllowedUsers(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_USERS, proxyUser),
        configurationMap,
        ShpurdpServerConfigurationKey.TPROXY_ALLOWED_USERS.getDefaultValue());
  }

  public String getAllowedGroups(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_GROUPS, proxyUser),
        configurationMap,
        ShpurdpServerConfigurationKey.TPROXY_ALLOWED_GROUPS.getDefaultValue());
  }

  @Override
  public boolean equals(Object o) {
    return new EqualsBuilder()
        .append(configurationMap, ((ShpurdpTProxyConfiguration) o).configurationMap)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(configurationMap)
        .toHashCode();
  }
}