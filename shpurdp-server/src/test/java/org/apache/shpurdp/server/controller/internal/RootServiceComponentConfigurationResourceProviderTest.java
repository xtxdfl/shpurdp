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

package org.apache.shpurdp.server.controller.internal;

import static org.apache.shpurdp.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.shpurdp.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_COMPONENT_NAME_PROPERTY_ID;
import static org.apache.shpurdp.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.apache.shpurdp.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_SERVICE_NAME_PROPERTY_ID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.api.services.RootServiceComponentConfigurationService;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfiguration;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.RootComponent;
import org.apache.shpurdp.server.controller.RootService;
import org.apache.shpurdp.server.controller.predicate.AndPredicate;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;
import org.apache.shpurdp.server.controller.spi.SystemException;
import org.apache.shpurdp.server.controller.utilities.PredicateBuilder;
import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.ldap.service.LdapFacade;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.apache.shpurdp.server.security.TestAuthenticationFactory;
import org.apache.shpurdp.server.security.authorization.AuthorizationException;
import org.apache.shpurdp.server.security.encryption.ShpurdpServerConfigurationEncryptor;
import org.apache.shpurdp.server.security.encryption.Encryptor;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.stack.OsFamily;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, ShpurdpServerConfigurationHandler.class})
public class RootServiceComponentConfigurationResourceProviderTest extends EasyMockSupport {

  private static final String LDAP_CONFIG_CATEGORY = ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName();
  private static final String SSO_CONFIG_CATEGORY = ShpurdpServerConfigurationCategory.SSO_CONFIGURATION.getCategoryName();

  private Predicate predicate;
  private ResourceProvider resourceProvider;
  private RootServiceComponentConfigurationHandlerFactory factory;
  private Request request;
  private ShpurdpConfigurationDAO dao;
  private ShpurdpEventPublisher publisher;
  private ShpurdpServerLDAPConfigurationHandler shpurdpServerLDAPConfigurationHandler;
  private ShpurdpServerSSOConfigurationHandler shpurdpServerSSOConfigurationHandler;

  @Before
  public void init() {
    Injector injector = createInjector();
    resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
    predicate = createPredicate(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), LDAP_CONFIG_CATEGORY);
    request = createMock(Request.class);
    dao = injector.getInstance(ShpurdpConfigurationDAO.class);
    factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    publisher = injector.getInstance(ShpurdpEventPublisher.class);
    shpurdpServerLDAPConfigurationHandler = injector.getInstance(ShpurdpServerLDAPConfigurationHandler.class);
    shpurdpServerSSOConfigurationHandler = injector.getInstance(ShpurdpServerSSOConfigurationHandler.class);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testCreateResourcesWithDirective_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testCreateResources(Authentication authentication, String opDirective) throws Exception {
    Set<Map<String, Object>> propertySets = new HashSet<>();

    Map<String, String> properties = new HashMap<>();
    properties.put(ShpurdpServerConfigurationKey.LDAP_ENABLED.key(), "value1");
    properties.put(ShpurdpServerConfigurationKey.USER_BASE.key(), "value2");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));

    Map<String, String> properties2 = new HashMap<>();
    if (opDirective == null) {
      properties2.put(ShpurdpServerConfigurationKey.SSO_ENABLED_SERVICES.key(), "true");
      propertySets.add(toRequestProperties(SSO_CONFIG_CATEGORY, properties2));
    }

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();
    Capture<Map<String, String>> capturedProperties2 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(LDAP_CONFIG_CATEGORY), capture(capturedProperties1), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.reconcileCategory(eq(SSO_CONFIG_CATEGORY), capture(capturedProperties2), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.findByCategory(eq(SSO_CONFIG_CATEGORY)))
          .andReturn(Collections.emptyList())
          .once();


      publisher.publish(anyObject(ShpurdpConfigurationChangedEvent.class));
      expectLastCall().times(2);
    }

    expect(factory.getInstance(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(shpurdpServerLDAPConfigurationHandler)
        .once();
    if (opDirective == null) {
      expect(factory.getInstance(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), SSO_CONFIG_CATEGORY))
          .andReturn(shpurdpServerSSOConfigurationHandler)
          .once();
    }

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.createResources(request);
      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + LDAP_CONFIG_CATEGORY, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties, capturedProperties1);
      validateCapturedProperties(properties2, capturedProperties2);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
      Assert.assertFalse(capturedProperties2.hasCaptured());
    }
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    expect(dao.removeByCategory(LDAP_CONFIG_CATEGORY)).andReturn(1).once();

    publisher.publish(anyObject(ShpurdpConfigurationChangedEvent.class));
    expectLastCall().once();

    expect(factory.getInstance(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(shpurdpServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.deleteResources(request, predicate);

    verifyAll();
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    Map<String, String> properties = new HashMap<>();
    properties.put(ShpurdpServerConfigurationKey.ANONYMOUS_BIND.key(), "value1");
    properties.put(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key(), "value2");

    expect(dao.findByCategory(LDAP_CONFIG_CATEGORY)).andReturn(createEntities(LDAP_CONFIG_CATEGORY, properties)).once();

    expect(factory.getInstance(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(shpurdpServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> response = resourceProvider.getResources(request, predicate);

    verifyAll();

    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.size());

    Resource resource = response.iterator().next();
    Assert.assertEquals(Resource.Type.RootServiceComponentConfiguration, resource.getType());

    Map<String, Map<String, Object>> propertiesMap = resource.getPropertiesMap();
    Assert.assertEquals(3, propertiesMap.size());

    Assert.assertEquals(LDAP_CONFIG_CATEGORY, propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY).get("category"));

    Map<String, Object> retrievedProperties = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedProperties.size());

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
    }

    Map<String, Object> retrievedPropertyTypes = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedPropertyTypes.size());
  }

  @Test
  public void testUpdateResources_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testUpdateResourcesWithDirective_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testUpdateResources(Authentication authentication, String opDirective) throws Exception {
    Set<Map<String, Object>> propertySets = new HashSet<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(ShpurdpServerConfigurationKey.GROUP_BASE.key(), "value1");
    properties.put(ShpurdpServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key(), "value2");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(LDAP_CONFIG_CATEGORY), capture(capturedProperties1), eq(false)))
          .andReturn(true)
          .once();
      publisher.publish(anyObject(ShpurdpConfigurationChangedEvent.class));
      expectLastCall().times(1);
    }

    expect(factory.getInstance(RootService.SHPURDP.name(), RootComponent.SHPURDP_SERVER.name(), LDAP_CONFIG_CATEGORY))
        .andReturn(shpurdpServerLDAPConfigurationHandler)
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.updateResources(request, predicate);

      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + LDAP_CONFIG_CATEGORY, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties, capturedProperties1);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
    }
  }

  private Predicate createPredicate(String serviceName, String componentName, String categoryName) {
    Predicate predicateService = new PredicateBuilder()
        .property(CONFIGURATION_SERVICE_NAME_PROPERTY_ID)
        .equals(serviceName)
        .toPredicate();
    Predicate predicateComponent = new PredicateBuilder()
        .property(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID)
        .equals(componentName)
        .toPredicate();
    Predicate predicateCategory = new PredicateBuilder()
        .property(CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(categoryName)
        .toPredicate();
    return new AndPredicate(predicateService, predicateComponent, predicateCategory);
  }

  private List<ShpurdpConfigurationEntity> createEntities(String categoryName, Map<String, String> properties) {
    List<ShpurdpConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      ShpurdpConfigurationEntity entity = new ShpurdpConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private Map<String, Object> toRequestProperties(String categoryName1, Map<String, String> properties) {
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(CONFIGURATION_SERVICE_NAME_PROPERTY_ID, "SHPURDP");
    requestProperties.put(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, "SHPURDP_SERVER");
    requestProperties.put(CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      requestProperties.put(CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
    }
    return requestProperties;
  }

  private void validateCapturedProperties(Map<String, String> expectedProperties, Capture<Map<String, String>> capturedProperties) {
    Assert.assertTrue(capturedProperties.hasCaptured());

    Map<String, String> properties = capturedProperties.getValue();
    Assert.assertNotNull(properties);

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    properties = new TreeMap<>(properties);
    Assert.assertEquals(expectedProperties, properties);
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);
        ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
        Clusters clusters = createNiceMock(Clusters.class);
        ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
        ShpurdpManagementController managementController = createNiceMock(ShpurdpManagementController.class);
        StackAdvisorHelper stackAdvisorHelper = createNiceMock(StackAdvisorHelper.class);
        LdapFacade ldapFacade = createNiceMock(LdapFacade.class);
        Encryptor<ShpurdpServerConfiguration> encryptor = createNiceMock(ShpurdpServerConfigurationEncryptor.class);

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(ShpurdpConfigurationDAO.class).toInstance(shpurdpConfigurationDAO);
        bind(ShpurdpEventPublisher.class).toInstance(publisher);

        bind(ShpurdpServerConfigurationHandler.class).toInstance(new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher));
        bind(ShpurdpServerSSOConfigurationHandler.class).toInstance(new ShpurdpServerSSOConfigurationHandler(clusters, configHelper, managementController, stackAdvisorHelper, shpurdpConfigurationDAO, publisher));
        bind(ShpurdpServerLDAPConfigurationHandler.class).toInstance(new ShpurdpServerLDAPConfigurationHandler(clusters, configHelper, managementController,
            stackAdvisorHelper, shpurdpConfigurationDAO, publisher, ldapFacade, encryptor));
        bind(RootServiceComponentConfigurationHandlerFactory.class).toInstance(createMock(RootServiceComponentConfigurationHandlerFactory.class));
      }
    });
  }
}