/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shpurdp.server.security.authorization;

import static java.lang.Boolean.parseBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ShpurdpLdapBindAuthenticatorTest extends EasyMockSupport {
  
  private Injector injector;
  private ShpurdpLdapConfiguration ldapConfiguration;
  
  @Before
  public void init() throws Exception {
    injector = createInjector();
    ldapConfiguration = injector.getInstance(ShpurdpLdapConfiguration.class);
  }

  @Test
  public void testAuthenticateWithoutLogin() throws Exception {
    testAuthenticate("username", "username", false);
  }

  @Test
  public void testAuthenticateWithNullLDAPUsername() throws Exception {
    testAuthenticate("username", null, false);
  }

  @Test
  public void testAuthenticateWithLoginAliasDefault() throws Exception {
    testAuthenticate("username", "ldapUsername", false);
  }

  @Test
  public void testAuthenticateWithLoginAliasForceToLower() throws Exception {
    testAuthenticate("username", "ldapUsername", true);
  }

  @Test
  public void testAuthenticateBadPassword() throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", "ldapUsername");
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    LdapName basePath = LdapUtils.newLdapName(basePathString);

    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapName())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andThrow(new org.springframework.ldap.AuthenticationException(null))
        .once();

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(anyString())).andReturn(searchedUserContext).once();

    setupDatabaseConfigurationExpectations(false, false);

    replayAll();


    ShpurdpLdapBindAuthenticator bindAuthenticator = new ShpurdpLdapBindAuthenticator(ldapCtxSource, ldapConfiguration);
    bindAuthenticator.setUserSearch(userSearch);

    try {
      bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken("username", "password"));
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException");
    } catch (org.springframework.security.authentication.BadCredentialsException e) {
      // expected
    } catch (Throwable t) {
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException\nEncountered thrown exception " + t.getClass().getName());
    }

    verifyAll();
  }

  private void testAuthenticate(String shpurdpUsername, String ldapUsername, boolean forceUsernameToLower) throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", ldapUsername);
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    LdapName basePath = LdapUtils.newLdapName(basePathString);

    @SuppressWarnings("unchecked")
    NamingEnumeration<SearchResult> adminGroups = createMock(NamingEnumeration.class);
    expect(adminGroups.hasMore())
        .andReturn(false)
        .atLeastOnce();
    adminGroups.close();
    expectLastCall().atLeastOnce();

    DirContextOperations boundUserContext = createMock(DirContextOperations.class);
    System.out.println(ldapUserDNString);
    expect(boundUserContext.search(eq("ou=groups"), eq("(&(member=" + ldapUserDNString + ")(objectclass=group)(|(cn=Shpurdp Administrators)))"), anyObject(SearchControls.class)))
        .andReturn(adminGroups)
        .atLeastOnce();
    boundUserContext.close();
    expectLastCall().atLeastOnce();
    
    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapName())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andReturn(boundUserContext)
        .once();
    expect(ldapCtxSource.getReadOnlyContext())
        .andReturn(boundUserContext)
        .once();

    Attributes searchedAttributes = new BasicAttributes("uid", ldapUsername);

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();
    expect(searchedUserContext.getAttributes())
        .andReturn(searchedAttributes)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(shpurdpUsername)).andReturn(searchedUserContext).once();

    ServletRequestAttributes servletRequestAttributes = createMock(ServletRequestAttributes.class);

    if (!StringUtils.isEmpty(ldapUsername) && !shpurdpUsername.equals(ldapUsername)) {
      servletRequestAttributes.setAttribute(eq(shpurdpUsername), eq(forceUsernameToLower ? ldapUsername.toLowerCase() : ldapUsername), eq(RequestAttributes.SCOPE_SESSION));
      expectLastCall().once();
      servletRequestAttributes.setAttribute(eq(forceUsernameToLower ? ldapUsername.toLowerCase() : ldapUsername),eq(shpurdpUsername), eq(RequestAttributes.SCOPE_SESSION));
      expectLastCall().once();
    }

    setupDatabaseConfigurationExpectations(true, forceUsernameToLower);

    replayAll();

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
//    servletRequestAttributes.setAttribute(shpurdpUsername,ldapUsername, RequestAttributes.SCOPE_SESSION);
//    expectLastCall().anyTimes();

    ShpurdpLdapBindAuthenticator bindAuthenticator = new ShpurdpLdapBindAuthenticator(ldapCtxSource, ldapConfiguration);
    bindAuthenticator.setUserSearch(userSearch);
    DirContextOperations user = bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken(shpurdpUsername, "password"));

    verifyAll();

    String ldapUserNameAttribute = ldapConfiguration.getLdapServerProperties().getUsernameAttribute();
    assertEquals(ldapUsername, user.getStringAttribute(ldapUserNameAttribute));
  }
  
  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(ShpurdpLdapConfiguration.class).toInstance(createNiceMock(ShpurdpLdapConfiguration.class));
      }
    });
  }
  
  private void setupDatabaseConfigurationExpectations(boolean expectedDatabaseConfigCall, boolean forceUsernameToLowerCase) {
    final LdapServerProperties ldapServerProperties = getDefaultLdapServerProperties(forceUsernameToLowerCase);
    ldapServerProperties.setGroupObjectClass("group");
    if (expectedDatabaseConfigCall) {
      expect(ldapConfiguration.getLdapServerProperties()).andReturn(ldapServerProperties).anyTimes();
    }
  }
  
  private static LdapServerProperties getDefaultLdapServerProperties(boolean forceUsernameToLowerCase) {
    final LdapServerProperties ldapServerProperties = new LdapServerProperties();
    ldapServerProperties.setPrimaryUrl(ShpurdpServerConfigurationKey.SERVER_HOST.getDefaultValue() + ":" + ShpurdpServerConfigurationKey.SERVER_PORT.getDefaultValue());
    ldapServerProperties.setSecondaryUrl(ShpurdpServerConfigurationKey.SECONDARY_SERVER_HOST.getDefaultValue() + ":" + ShpurdpServerConfigurationKey.SECONDARY_SERVER_PORT.getDefaultValue());
    ldapServerProperties.setUseSsl(parseBoolean(ShpurdpServerConfigurationKey.USE_SSL.getDefaultValue()));
    ldapServerProperties.setAnonymousBind(parseBoolean(ShpurdpServerConfigurationKey.ANONYMOUS_BIND.getDefaultValue()));
    ldapServerProperties.setManagerDn(ShpurdpServerConfigurationKey.BIND_DN.getDefaultValue());
    ldapServerProperties.setManagerPassword(ShpurdpServerConfigurationKey.BIND_PASSWORD.getDefaultValue());
    ldapServerProperties.setBaseDN(ShpurdpServerConfigurationKey.USER_SEARCH_BASE.getDefaultValue());
    ldapServerProperties.setUsernameAttribute(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setForceUsernameToLowercase(forceUsernameToLowerCase);
    ldapServerProperties.setUserBase(ShpurdpServerConfigurationKey.USER_BASE.getDefaultValue());
    ldapServerProperties.setUserObjectClass(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS.getDefaultValue());
    ldapServerProperties.setDnAttribute(ShpurdpServerConfigurationKey.DN_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setGroupBase(ShpurdpServerConfigurationKey.GROUP_BASE.getDefaultValue());
    ldapServerProperties.setGroupObjectClass(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS.getDefaultValue());
    ldapServerProperties.setGroupMembershipAttr(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setGroupNamingAttr(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setAdminGroupMappingRules(ShpurdpServerConfigurationKey.GROUP_MAPPING_RULES.getDefaultValue());
    ldapServerProperties.setAdminGroupMappingMemberAttr("");
    ldapServerProperties.setUserSearchFilter(ShpurdpServerConfigurationKey.USER_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setAlternateUserSearchFilter(ShpurdpServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setGroupSearchFilter(ShpurdpServerConfigurationKey.GROUP_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setReferralMethod(ShpurdpServerConfigurationKey.REFERRAL_HANDLING.getDefaultValue());
    ldapServerProperties.setSyncUserMemberReplacePattern(ShpurdpServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN.getDefaultValue());
    ldapServerProperties.setSyncGroupMemberReplacePattern(ShpurdpServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN.getDefaultValue());
    ldapServerProperties.setSyncUserMemberFilter(ShpurdpServerConfigurationKey.USER_MEMBER_FILTER.getDefaultValue());
    ldapServerProperties.setSyncGroupMemberFilter(ShpurdpServerConfigurationKey.GROUP_MEMBER_FILTER.getDefaultValue());
    ldapServerProperties.setPaginationEnabled(parseBoolean(ShpurdpServerConfigurationKey.PAGINATION_ENABLED.getDefaultValue()));
    return ldapServerProperties;
  }
}
