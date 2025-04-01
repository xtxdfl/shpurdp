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

package org.apache.shpurdp.server.configuration;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.events.JpaInitializedEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.apache.shpurdp.server.security.encryption.Encryptor;
import org.apache.shpurdp.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.ShpurdpJpaPersistService;

public class ShpurdpServerConfigurationProviderTest extends EasyMockSupport {

  private static final ShpurdpServerConfigurationCategory TEST_CONFIGURATION = TPROXY_CONFIGURATION;

  @Test
  public void testGetAndLoadDataForVariousEvents() {
    Injector injector = getInjector();

    ShpurdpServerConfiguration emptyTestConfiguration = createMock(ShpurdpServerConfiguration.class);

    ShpurdpServerConfiguration filledTestConfiguration1 = createMock(ShpurdpServerConfiguration.class);

    ShpurdpServerConfiguration filledTestConfiguration2 = createMock(ShpurdpServerConfiguration.class);

    ShpurdpEventPublisher publisher = injector.getInstance(ShpurdpEventPublisher.class);
    ShpurdpJpaPersistService persistService = injector.getInstance(ShpurdpJpaPersistService.class);

    ShpurdpServerConfigurationProvider provider = createMockBuilder(ShpurdpServerConfigurationProvider.class)
        .addMockedMethod("loadInstance", Collection.class)
        .withConstructor(TEST_CONFIGURATION, publisher, persistService)
        .createMock();

    expect(provider.loadInstance(Collections.emptyList())).andReturn(emptyTestConfiguration).once();
    expect(provider.loadInstance(null)).andReturn(filledTestConfiguration1).once();
    expect(provider.loadInstance(null)).andReturn(filledTestConfiguration2).once();

    replayAll();

    injector.injectMembers(provider);

    ShpurdpServerConfiguration configuration = provider.get();
    Assert.assertSame(emptyTestConfiguration, configuration);

    // Push a configuration change event...
    provider.shpurdpConfigurationChanged(new ShpurdpConfigurationChangedEvent(TEST_CONFIGURATION.getCategoryName()));

    ShpurdpServerConfiguration configuration2 = provider.get();
    // This should return the same instance as before since loadInstance should not have done anything
    Assert.assertSame(configuration, configuration2);

    // Push an initializing JPA event...
    provider.jpaInitialized(new JpaInitializedEvent());

    ShpurdpServerConfiguration configuration3 = provider.get();
    Assert.assertSame(filledTestConfiguration1, configuration3);

    // Push a configuration change event...
    provider.shpurdpConfigurationChanged(new ShpurdpConfigurationChangedEvent(TEST_CONFIGURATION.getCategoryName()));

    ShpurdpServerConfiguration configuration4 = provider.get();
    // This should return a different instance since loadInstance should have done some work
    Assert.assertNotSame(configuration3, configuration4);

    verifyAll();
  }

  @Test
  public void testToProperties() {
    Injector injector = getInjector();

    ShpurdpEventPublisher publisher = injector.getInstance(ShpurdpEventPublisher.class);
    ShpurdpJpaPersistService persistService = injector.getInstance(ShpurdpJpaPersistService.class);

    ShpurdpServerConfigurationProvider provider = createMockBuilder(ShpurdpServerConfigurationProvider.class)
        .withConstructor(TEST_CONFIGURATION, publisher, persistService)
        .createMock();

    replayAll();

    Map actualProperties;

    actualProperties = provider.toProperties(null);
    Assert.assertNotNull(actualProperties);
    Assert.assertEquals(Collections.emptyMap(), actualProperties);

    actualProperties = provider.toProperties(Collections.emptyList());
    Assert.assertNotNull(actualProperties);
    Assert.assertEquals(Collections.emptyMap(), actualProperties);

    Map<String, String> expectedProperties = new HashMap<>();
    expectedProperties.put("one", "1");
    expectedProperties.put("two", "2");
    expectedProperties.put("three", "3");

    actualProperties = provider.toProperties(createShpurdpConfigurationEntities(expectedProperties));
    Assert.assertNotNull(actualProperties);
    Assert.assertNotSame(expectedProperties, actualProperties);
    Assert.assertEquals(expectedProperties, actualProperties);

    verifyAll();
  }

  private Collection<ShpurdpConfigurationEntity> createShpurdpConfigurationEntities(Map<String, String> properties) {
    List<ShpurdpConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      ShpurdpConfigurationEntity entity = new ShpurdpConfigurationEntity();
      entity.setCategoryName("some-category");
      entity.setPropertyName(entry.getKey());
      entity.setPropertyValue(entry.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private Injector getInjector() {
    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        ShpurdpJpaPersistService persistService = createMockBuilder(ShpurdpJpaPersistService.class)
            .withConstructor("test", Collections.emptyMap())
            .createMock();

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(ShpurdpJpaPersistService.class).toInstance(persistService);
        bind(ShpurdpConfigurationDAO.class).toInstance(createNiceMock(ShpurdpConfigurationDAO.class));
        bind(new TypeLiteral<Encryptor<ShpurdpServerConfiguration>>() {}).annotatedWith(Names.named("ShpurdpServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      }
    });
  }
}