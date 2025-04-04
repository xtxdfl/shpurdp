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

package org.apache.shpurdp.server.security.ldap;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.H2DatabaseCleaner;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.orm.GuiceJpaInitializer;
import org.apache.shpurdp.server.security.ClientSecurityType;
import org.apache.shpurdp.server.security.authorization.AuthorizationTestModule;
import org.apache.shpurdp.server.security.authorization.Users;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Performs sync request to real LDAP server.
 */
@Ignore
public class LdapPerformanceTest {

  private static Injector injector;

  @Inject
  private ShpurdpLdapDataPopulator populator;

  @Inject
  private Users users;

  @Inject
  Configuration configuration;
  
  @Inject
  ShpurdpLdapConfiguration ldapConfiguration;

  final String SPRING_CONTEXT_LOCATION = "classpath:webapp/WEB-INF/spring-security.xml";

  @Before
  public void setUp() {
    injector = Guice.createInjector(new AuthorizationTestModule());

    injector.injectMembers(this);
    injector.getInstance(GuiceJpaInitializer.class);
    configuration.setClientSecurityType(ClientSecurityType.LDAP);
    
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.SERVER_HOST, "c6402.shpurdp.apache.org");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.SERVER_PORT, "389");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS, "posixAccount");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE, "uid");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS, "posixGroup");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE, "cn");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE, "memberUid");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.USER_SEARCH_BASE, "dc=apache,dc=org");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.ANONYMOUS_BIND, "false");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.BIND_DN, "uid=hdfs,ou=people,ou=dev,dc=apache,dc=org");
    ldapConfiguration.setValueFor(ShpurdpServerConfigurationKey.BIND_PASSWORD, "hdfs");
  }

  @After
  public void tearDown() throws ShpurdpException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testLdapSync() throws ShpurdpException, InterruptedException {
    long time = System.currentTimeMillis();
    Set<LdapGroupDto> groups = populator.getExternalLdapGroupInfo();
    Set<LdapUserDto> users = populator.getExternalLdapUserInfo();
    Set<String> userNames = new HashSet<>();
    for (LdapUserDto user : users) {
      userNames.add(user.getUserName());
    }
    Set<String> groupNames = new HashSet<>();
    for (LdapGroupDto group : groups) {
      groupNames.add(group.getGroupName());
    }
    System.out.println("Data fetch: " + (System.currentTimeMillis() - time));
    time = System.currentTimeMillis();
    LdapBatchDto batchDto = new LdapBatchDto();
    populator.synchronizeLdapUsers(userNames, batchDto, false);
    populator.synchronizeLdapGroups(groupNames, batchDto, false);
    this.users.processLdapSync(batchDto);
    System.out.println("Initial sync: " + (System.currentTimeMillis() - time));
    time = System.currentTimeMillis();
    batchDto = new LdapBatchDto();
    populator.synchronizeLdapUsers(userNames, batchDto, false);
    populator.synchronizeLdapGroups(groupNames, batchDto, false);
    this.users.processLdapSync(batchDto);
    System.out.println("Subsequent sync: " + (System.currentTimeMillis() - time));
  }
}
