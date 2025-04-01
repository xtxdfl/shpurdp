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

package org.apache.shpurdp.server.ldap.service.ads;

import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapException;
import org.apache.shpurdp.server.ldap.service.LdapConfigurationService;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Mock(type = MockType.STRICT)
  private LdapConnectionTemplate ldapConnectionTemplateMock;


  @TestSubject
  private LdapConfigurationService ldapConfigurationService = new DefaultLdapConfigurationService();

  @Before
  public void before() {
    resetAll();
  }

  @Test
  public void testShouldConnectionCheckSucceedWhenConnectionCallbackSucceeds() throws Exception {
    // GIVEN
    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(Maps.newHashMap());

    // the cllback returns TRUE
    EasyMock.expect(ldapConnectionTemplateMock.execute(EasyMock.anyObject(ConnectionCallback.class))).andReturn(Boolean.TRUE);
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    replayAll();
    // WHEN
    ldapConfigurationService.checkConnection(shpurdpLdapConfiguration);

    // THEN
    // no exceptions are thrown

  }

  @Test(expected = ShpurdpLdapException.class)
  public void testShouldConnectionCheckFailWhenConnectionCallbackFails() throws Exception {

    // GIVEN
    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(Maps.newHashMap());

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateMock.execute(EasyMock.anyObject(ConnectionCallback.class))).andReturn(Boolean.FALSE);
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    replayAll();
    // WHEN
    ldapConfigurationService.checkConnection(shpurdpLdapConfiguration);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldUserAttributeConfigurationCheckSucceedWhenUserDnIsFound() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS.key(), "person");
    configMap.put(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE.key(), "uid");

    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);
    // users found with dn
    EasyMock.expect(ldapConnectionTemplateMock.searchFirst(EasyMock.anyObject(Dn.class), EasyMock.anyString(), EasyMock.anyObject(SearchScope.class),
      EasyMock.anyObject(EntryMapper.class))).andReturn("dn");

    replayAll();
    // WHEN
    String userDn = ldapConfigurationService.checkUserAttributes("testUser", "testPassword", shpurdpLdapConfiguration);

    // THEN
    Assert.assertEquals("The found userDn is not the expected one", userDn, "dn");

  }

  @Test(expected = ShpurdpLdapException.class)
  public void testShouldUserAttributeConfigurationCheckFailWhenNoUsersFound() throws Exception {
    // GIVEN
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(ShpurdpServerConfigurationKey.USER_OBJECT_CLASS.key(), "posixAccount");
    configMap.put(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE.key(), "dn");

    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    // no users found, the returned dn is null
    EasyMock.expect(ldapConnectionTemplateMock.searchFirst(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class),
      EasyMock.anyObject(EntryMapper.class))).andReturn(null);

    replayAll();
    // WHEN
    String userDn = ldapConfigurationService.checkUserAttributes("testUser", "testPassword",
      shpurdpLdapConfiguration);

    // THEN
    Assert.assertEquals("The found userDn is not the expected one", userDn, "dn");

  }


  @Test
  public void testShouldGroupAttributeConfigurationCheckSucceedWhenGroupForUserDnIsFound() throws Exception {
    // GIVEN

    Map<String, String> configMap = groupConfigObjectMap();

    SearchRequest sr = new SearchRequestImpl();

    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(sr);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(EntryMapper.class)))
      .andReturn(Lists.newArrayList("userGroup"));

    replayAll();
    // WHEN
    Set<String> userGroups = ldapConfigurationService.checkGroupAttributes("userDn", shpurdpLdapConfiguration);

    // THEN
    Assert.assertNotNull("No groups found", userGroups);

  }


  @Test(expected = ShpurdpLdapException.class)
  public void testShouldGroupAttributeConfigurationCheckFailWhenNoGroupsForUserDnFound() throws Exception {
    // GIVEN

    Map<String, String> configMap = groupConfigObjectMap();

    SearchRequest sr = new SearchRequestImpl();

    ShpurdpLdapConfiguration shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(sr);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(EntryMapper.class)))
      .andReturn(Lists.newArrayList());

    replayAll();
    // WHEN
    Set<String> userGroups = ldapConfigurationService.checkGroupAttributes("userDn", shpurdpLdapConfiguration);

    // THEN
    Assert.assertNotNull("No groups found", userGroups);

  }

  private Map<String, String> groupConfigObjectMap() {
    Map<String, String> configMap = Maps.newHashMap();
    configMap.put(ShpurdpServerConfigurationKey.GROUP_OBJECT_CLASS.key(), "groupOfNames");
    configMap.put(ShpurdpServerConfigurationKey.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    configMap.put(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE.key(), "uid");
    configMap.put(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key(), "member");
    return configMap;
  }


}