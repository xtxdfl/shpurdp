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

package org.apache.shpurdp.server.ldap.service;

import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.easymock.Capture;
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Unit test suite for the LdapFacade operations.
 */
public class ShpurdpLdapFacadeTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  public LdapConfigurationService ldapConfigurationServiceMock;

  @Mock(type = MockType.STRICT)
  public LdapAttributeDetectionService ldapAttributeDetectionServiceMock;

  @TestSubject
  private LdapFacade ldapFacade = new ShpurdpLdapFacade();

  private ShpurdpLdapConfiguration shpurdpLdapConfiguration;


  private Capture<ShpurdpLdapConfiguration> shpurdpLdapConfigurationCapture;

  @Before
  public void before() {
    shpurdpLdapConfiguration = new ShpurdpLdapConfiguration(Maps.newHashMap());
    shpurdpLdapConfigurationCapture = Capture.newInstance();


    resetAll();
  }

  /**
   * Tests whether the facade method call delegates to the proper service call.
   * The thest is success if the same instance is passed to the service.
   *
   * @throws Exception
   */
  @Test
  public void testShouldConfigurationCheckDelegateToTheRightServiceCall() throws Exception {
    // GIVEN
    // the mocks are set up
    ldapConfigurationServiceMock.checkConnection(EasyMock.capture(shpurdpLdapConfigurationCapture));
    replayAll();
    // WHEN
    // the facade method is called
    ldapFacade.checkConnection(shpurdpLdapConfiguration);

    // THEN
    // the captured configuration instance is the same the facade method got called with
    Assert.assertEquals("The configuration instance souldn't change before passing it to the service",
        shpurdpLdapConfiguration, shpurdpLdapConfigurationCapture.getValue());
  }

  @Test(expected = ShpurdpLdapException.class)
  public void testShouldConfigurationCheckFailureResultInShpurdpLdapException() throws Exception {
    // GIVEN
    ldapConfigurationServiceMock.checkConnection(EasyMock.anyObject(ShpurdpLdapConfiguration.class));
    EasyMock.expectLastCall().andThrow(new ShpurdpLdapException("Testing ..."));
    replayAll();

    // WHEN
    ldapFacade.checkConnection(shpurdpLdapConfiguration);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldLdapAttributesCheckDelegateToTheRightServiceCalls() throws Exception {
    // GIVEN

    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(ShpurdpLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(ShpurdpLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");


    Capture<String> testUserCapture = Capture.newInstance();
    Capture<String> testPasswordCapture = Capture.newInstance();
    Capture<String> userDnCapture = Capture.newInstance();

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.capture(testUserCapture), EasyMock.capture(testPasswordCapture),
        EasyMock.capture(shpurdpLdapConfigurationCapture))).andReturn("userDn");

    EasyMock.expect(ldapConfigurationServiceMock.checkGroupAttributes(EasyMock.capture(userDnCapture),
        EasyMock.capture(shpurdpLdapConfigurationCapture))).andReturn(Sets.newHashSet("userGroup"));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, shpurdpLdapConfiguration);

    // THEN
    Assert.assertEquals("testUser", testUserCapture.getValue());
    Assert.assertEquals("testPassword", testPasswordCapture.getValue());
    Assert.assertEquals("userDn", userDnCapture.getValue());

    Assert.assertTrue(testUserGroups.contains("userGroup"));

  }

  @Test(expected = ShpurdpLdapException.class)
  public void testShouldAttributeCheckFailuresResultInShpurdpLdapException() throws Exception {
    // GIVEN
    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(ShpurdpLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(ShpurdpLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.anyString(), EasyMock.anyString(),
        EasyMock.anyObject(ShpurdpLdapConfiguration.class))).andThrow(new ShpurdpLdapException("Testing ..."));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, shpurdpLdapConfiguration);
    // THEN
    // Exception is thrown
  }

  @Test
  public void testShouldLdapAttributeDetectionDelegateToTheRightServiceCalls() throws Exception {

    // configuration map with user attributes detected
    Map<String, String> userConfigMap = Maps.newHashMap();
    userConfigMap.put(ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE.key(), "uid");
    ShpurdpLdapConfiguration userAttrDecoratedConfig = new ShpurdpLdapConfiguration(userConfigMap);

    // configuration map with user+group attributes detected
    Map<String, String> groupConfigMap = Maps.newHashMap(userConfigMap);
    groupConfigMap.put(ShpurdpServerConfigurationKey.GROUP_NAME_ATTRIBUTE.key(), "dn");
    ShpurdpLdapConfiguration groupAttrDecoratedConfig = new ShpurdpLdapConfiguration(groupConfigMap);

    Capture<ShpurdpLdapConfiguration> userAttrDetectionConfigCapture = Capture.newInstance();
    Capture<ShpurdpLdapConfiguration> groupAttrDetectionConfigCapture = Capture.newInstance();

    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.capture(userAttrDetectionConfigCapture)))
        .andReturn(userAttrDecoratedConfig);

    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapGroupAttributes(EasyMock.capture(groupAttrDetectionConfigCapture)))
        .andReturn(groupAttrDecoratedConfig);

    replayAll();

    // WHEN
    ShpurdpLdapConfiguration detected = ldapFacade.detectAttributes(shpurdpLdapConfiguration);

    // THEN
    Assert.assertEquals("User attribute detection called with the wrong configuration", shpurdpLdapConfiguration,
        userAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Group attribute detection called with the wrong configuration", userAttrDecoratedConfig,
        groupAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Attribute detection returned an invalid configuration", groupAttrDecoratedConfig, detected);

  }

  @Test(expected = ShpurdpLdapException.class)
  public void testShouldAttributeDetectionFailuresResultInShpurdpLdapException() throws Exception {
    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.anyObject(ShpurdpLdapConfiguration.class)))
        .andThrow(new ShpurdpLdapException("Testing ..."));

    replayAll();

    // WHEN
    ldapFacade.detectAttributes(shpurdpLdapConfiguration);

    // THEN
    // Exception is thrown
  }
}