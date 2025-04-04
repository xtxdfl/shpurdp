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

package org.apache.shpurdp.server.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.security.authorization.LdapServerProperties;
import org.junit.Before;
import org.junit.Test;

public class ShpurdpLdapConfigurationTest {

  private ShpurdpLdapConfiguration configuration;

  @Before
  public void setup() {
    configuration = new ShpurdpLdapConfiguration();
  }

  @Test
  public void testLdapUserSearchFilterDefault() throws Exception {
    assertEquals("(&(uid={0})(objectClass=person))", configuration.getLdapServerProperties().getUserSearchFilter(false));
  }

  @Test
  public void testLdapUserSearchFilter() throws Exception {
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE, "test_uid");
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_SEARCH_FILTER, "{usernameAttribute}={0}");
    assertEquals("test_uid={0}", configuration.getLdapServerProperties().getUserSearchFilter(false));
  }

  @Test
  public void testAlternateLdapUserSearchFilterDefault() throws Exception {
    assertEquals("(&(userPrincipalName={0})(objectClass=person))", configuration.getLdapServerProperties().getUserSearchFilter(true));
  }

  @Test
  public void testAlternatLdapUserSearchFilter() throws Exception {
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE, "test_uid");
    configuration.setValueFor(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER, "{usernameAttribute}={5}");
    assertEquals("test_uid={5}", configuration.getLdapServerProperties().getUserSearchFilter(true));
  }

  @Test
  public void testAlternateUserSearchEnabledIsSetToFalseByDefault() throws Exception {
    assertFalse(configuration.isLdapAlternateUserSearchEnabled());
  }

  @Test
  public void testAlternateUserSearchEnabledTrue() throws Exception {
    configuration.setValueFor(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_ENABLED, "true");
    assertTrue(configuration.isLdapAlternateUserSearchEnabled());
  }

  @Test
  public void testAlternateUserSearchEnabledFalse() throws Exception {
    configuration.setValueFor(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_ENABLED, "false");
    assertFalse(configuration.isLdapAlternateUserSearchEnabled());
  }

  @Test
  public void testGetLdapServerProperties() throws Exception {
    final String managerPw = "shpurdpTest";

    configuration.setValueFor(ShpurdpServerConfigurationKey.SERVER_HOST, "host");
    configuration.setValueFor(ShpurdpServerConfigurationKey.SERVER_PORT, "1");
    configuration.setValueFor(ShpurdpServerConfigurationKey.SECONDARY_SERVER_HOST, "secHost");
    configuration.setValueFor(ShpurdpServerConfigurationKey.SECONDARY_SERVER_PORT, "2");
    configuration.setValueFor(ShpurdpServerConfigurationKey.USE_SSL, "true");
    configuration.setValueFor(ShpurdpServerConfigurationKey.ANONYMOUS_BIND, "true");
    configuration.setValueFor(ShpurdpServerConfigurationKey.BIND_DN, "5");
    configuration.setValueFor(ShpurdpServerConfigurationKey.BIND_PASSWORD, managerPw);
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_SEARCH_BASE, "7");
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE, "8");
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_BASE, "9");
    configuration.setValueFor(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS, "10");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_BASE, "11");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS, "12");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE, "13");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE, "14");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_MAPPING_RULES, "15");
    configuration.setValueFor(ShpurdpServerConfigurationKey.GROUP_SEARCH_FILTER, "16");

    final LdapServerProperties ldapProperties = configuration.getLdapServerProperties();

    assertEquals("host:1", ldapProperties.getPrimaryUrl());
    assertEquals("secHost:2", ldapProperties.getSecondaryUrl());
    assertTrue(ldapProperties.isUseSsl());
    assertTrue(ldapProperties.isAnonymousBind());
    assertEquals("5", ldapProperties.getManagerDn());
    assertEquals(managerPw, ldapProperties.getManagerPassword());
    assertEquals("7", ldapProperties.getBaseDN());
    assertEquals("8", ldapProperties.getUsernameAttribute());
    assertEquals("9", ldapProperties.getUserBase());
    assertEquals("10", ldapProperties.getUserObjectClass());
    assertEquals("11", ldapProperties.getGroupBase());
    assertEquals("12", ldapProperties.getGroupObjectClass());
    assertEquals("13", ldapProperties.getGroupMembershipAttr());
    assertEquals("14", ldapProperties.getGroupNamingAttr());
    assertEquals("15", ldapProperties.getAdminGroupMappingRules());
    assertEquals("16", ldapProperties.getGroupSearchFilter());
  }

}
