/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.shpurdp.server.controller.internal;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.LDAP_ENABLED;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.SERVER_HOST;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.SSO_MANAGE_SERVICES;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.RootServiceComponentConfiguration;
import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import junit.framework.Assert;

@RunWith(EasyMockRunner.class)
public class ShpurdpServerConfigurationHandlerTest extends EasyMockSupport {

  @Test
  public void getComponentConfigurations() {
    List<ShpurdpConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "SHPURDP,SERVICE1"));

    List<ShpurdpConfigurationEntity> ldapEntities = new ArrayList<>();
    ldapEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    ldapEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    List<ShpurdpConfigurationEntity> tproxyEntities = new ArrayList<>();
    tproxyEntities.add(createEntity(TPROXY_CONFIGURATION.getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key(), "true"));
    tproxyEntities.add(createEntity(TPROXY_CONFIGURATION.getCategoryName(), "shpurdp.tproxy.proxyuser.knox.hosts", "host1"));

    List<ShpurdpConfigurationEntity> allEntities = new ArrayList<>();
    allEntities.addAll(ssoEntities);
    allEntities.addAll(ldapEntities);
    allEntities.addAll(tproxyEntities);

    ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
    expect(shpurdpConfigurationDAO.findAll()).andReturn(allEntities).once();
    expect(shpurdpConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(ssoEntities).once();
    expect(shpurdpConfigurationDAO.findByCategory(LDAP_CONFIGURATION.getCategoryName())).andReturn(ldapEntities).once();
    expect(shpurdpConfigurationDAO.findByCategory(TPROXY_CONFIGURATION.getCategoryName())).andReturn(tproxyEntities).once();
    expect(shpurdpConfigurationDAO.findByCategory("invalid category")).andReturn(null).once();

    ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);

    ShpurdpServerConfigurationHandler handler = new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher);

    replayAll();

    Map<String, RootServiceComponentConfiguration> allConfigurations = handler.getComponentConfigurations(null);
    Assert.assertEquals(3, allConfigurations.size());
    Assert.assertTrue(allConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(TPROXY_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> ssoConfigurations = handler.getComponentConfigurations(SSO_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, ssoConfigurations.size());
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> ldapConfigurations = handler.getComponentConfigurations(LDAP_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, ldapConfigurations.size());
    Assert.assertTrue(ldapConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> tproxyConfigurations = handler.getComponentConfigurations(TPROXY_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, tproxyConfigurations.size());
    Assert.assertTrue(tproxyConfigurations.containsKey(TPROXY_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> invalidConfigurations = handler.getComponentConfigurations("invalid category");
    Assert.assertNull(invalidConfigurations);

    verifyAll();
  }

  @Test
  public void removeComponentConfiguration() {
    ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
    expect(shpurdpConfigurationDAO.removeByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(1).once();
    expect(shpurdpConfigurationDAO.removeByCategory("invalid category")).andReturn(0).once();

    ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);
    publisher.publish(anyObject(ShpurdpConfigurationChangedEvent.class));
    expectLastCall().once();

    ShpurdpServerConfigurationHandler handler = new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher);

    replayAll();

    handler.removeComponentConfiguration(SSO_CONFIGURATION.getCategoryName());
    handler.removeComponentConfiguration("invalid category");

    verifyAll();
  }

  @Test
  public void updateComponentCategory() throws ShpurdpException {
    Map<String, String> properties = new HashMap<>();
    properties.put(SSO_ENABLED_SERVICES.key(), "SERVICE1");
    properties.put(SSO_MANAGE_SERVICES.key(), "true");

    ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
    expect(shpurdpConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), properties, true))
        .andReturn(true).once();
    expect(shpurdpConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), properties, false))
        .andReturn(true).once();

    ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);
    publisher.publish(anyObject(ShpurdpConfigurationChangedEvent.class));
    expectLastCall().times(2);

    ShpurdpServerConfigurationHandler handler = new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher);

    replayAll();

    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), properties, false);

    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), properties, true);

    try {
      handler.updateComponentCategory("invalid category", properties, true);
      Assert.fail("Expecting IllegalArgumentException to be thrown");
    } catch (IllegalArgumentException e) {
      // This is expected
    }

    verifyAll();
  }

  @Test
  public void getConfigurations() {
    List<ShpurdpConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "SHPURDP,SERVICE1"));

    List<ShpurdpConfigurationEntity> allEntities = new ArrayList<>(ssoEntities);
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
    expect(shpurdpConfigurationDAO.findAll()).andReturn(allEntities).once();

    ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);

    ShpurdpServerConfigurationHandler handler = new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher);

    replayAll();

    Map<String, Map<String, String>> allConfigurations = handler.getConfigurations();
    Assert.assertEquals(2, allConfigurations.size());
    Assert.assertTrue(allConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));

    verifyAll();
  }

  @Test
  public void getConfigurationProperties() {
    List<ShpurdpConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "SHPURDP,SERVICE1"));

    List<ShpurdpConfigurationEntity> allEntities = new ArrayList<>(ssoEntities);
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    ShpurdpConfigurationDAO shpurdpConfigurationDAO = createMock(ShpurdpConfigurationDAO.class);
    expect(shpurdpConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(ssoEntities).once();
    expect(shpurdpConfigurationDAO.findByCategory("invalid category")).andReturn(null).once();

    ShpurdpEventPublisher publisher = createMock(ShpurdpEventPublisher.class);

    ShpurdpServerConfigurationHandler handler = new ShpurdpServerConfigurationHandler(shpurdpConfigurationDAO, publisher);

    replayAll();

    Map<String, String> ssoConfigurations = handler.getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    Assert.assertEquals(2, ssoConfigurations.size());
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_ENABLED_SERVICES.key()));
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_MANAGE_SERVICES.key()));

    Map<String, String> invalidConfigurations = handler.getConfigurationProperties("invalid category");
    Assert.assertNull(invalidConfigurations);

    verifyAll();
  }


  private ShpurdpConfigurationEntity createEntity(String categoryName, String key, String value) {
    ShpurdpConfigurationEntity entity = new ShpurdpConfigurationEntity();
    entity.setCategoryName(categoryName);
    entity.setPropertyName(key);
    entity.setPropertyValue(value);
    return entity;
  }
}