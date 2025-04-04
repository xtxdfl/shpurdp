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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.events.JpaInitializedEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.apache.shpurdp.server.security.encryption.Encryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.jpa.ShpurdpJpaPersistService;

/**
 * ShpurdpServerConfigurationProvider is an abstract class to be extended by Shpurdp server configuration
 * provider classes.
 * <p>
 * This implementation registers for the following Shpurdp server events to help load the relevant properties
 * when available and/or updated:
 *
 * <ul>
 * <li>ShpurdpConfigurationChangedEvent</li>
 * <li>JpaInitializedEvent</li>
 * </ul>
 */
public abstract class ShpurdpServerConfigurationProvider<T extends ShpurdpServerConfiguration> implements Provider<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ShpurdpServerConfigurationProvider.class);

  @Inject
  private Provider<ShpurdpConfigurationDAO> shpurdpConfigurationDAOProvider;

  @Inject @Named("ShpurdpServerConfigurationEncryptor")
  private Encryptor<ShpurdpServerConfiguration> encryptor;

  private final AtomicBoolean jpaStarted = new AtomicBoolean(false);

  private final ShpurdpServerConfigurationCategory configurationCategory;

  private T instance = null;

  protected ShpurdpServerConfigurationProvider(ShpurdpServerConfigurationCategory configurationCategory, ShpurdpEventPublisher publisher, ShpurdpJpaPersistService persistService) {
    this.configurationCategory = configurationCategory;

    if (publisher != null) {
      publisher.register(this);
      LOGGER.info("Registered {} in event publisher", this.getClass().getName());
    }

    if (persistService != null) {
      jpaStarted.set(persistService.isStarted());
    }
  }

  /**
   * Upon receiving this event, if the expected configuration category has been updated, the data is
   * reloaded.
   *
   * @param event the received {@link ShpurdpConfigurationChangedEvent}
   */
  @Subscribe
  public void shpurdpConfigurationChanged(ShpurdpConfigurationChangedEvent event) {
    if (configurationCategory.getCategoryName().equalsIgnoreCase(event.getCategoryName())) {
      LOGGER.info("Shpurdp configuration changed event received: {}", event);
      instance = loadInstance();
      
    }
  }

  /**
   * Upon receiving this event, the JPA infrastructure has completed initialization and the relevant
   * data may be retrieved.
   *
   * @param event the received {@link JpaInitializedEvent}
   */
  @Subscribe
  public void jpaInitialized(JpaInitializedEvent event) {
    LOGGER.info("JPA initialized event received: {}", event);
    jpaStarted.set(true);
    instance = loadInstance();
  }

  @Override
  public T get() {
    LOGGER.debug("Getting {} configuration...", configurationCategory.getCategoryName());
    if (instance == null) {
      instance = loadInstance();
    }
    return instance;
  }

  /**
   * Attempts to load the relevant configuration data.
   * <p>
   * If the JPA infrastructure has not been initialized, return the instance data that has already
   * been set up or (if null) requests an empty instance.
   *
   * @return the loaded instance data
   */
  private T loadInstance() {
    if (jpaStarted.get()) {
      LOGGER.info("Loading {} configuration data", configurationCategory.getCategoryName());
      T instance = loadInstance(shpurdpConfigurationDAOProvider.get().findByCategory(configurationCategory.getCategoryName()));
      encryptor.decryptSensitiveData(instance);
      return instance;
    } else {
      LOGGER.info("Cannot load {} configuration data since JPA is not initialized", configurationCategory.getCategoryName());
      if (instance == null) {
        return loadInstance(Collections.emptyList());
      } else {
        return instance;
      }
    }
  }

  /**
   * Converts a collection of {@link ShpurdpConfigurationEntity}s into a map of property names to values.
   *
   * @param configurationEntities the collection of {@link ShpurdpConfigurationEntity}s to process
   * @return a map of property names to values; or an empty map if no entities are available to process
   */
  protected Map<String, String> toProperties(Collection<ShpurdpConfigurationEntity> configurationEntities) {
    Map<String, String> map = new HashMap<>();

    if (configurationEntities != null) {
      for (ShpurdpConfigurationEntity entity : configurationEntities) {
        map.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return map;
  }

  /**
   * Returns a instance of the data configuration container implementation filled with data from the
   * supplied collection of {@link ShpurdpConfigurationEntity}s
   *
   * @param configurationEntities a collection of {@link ShpurdpConfigurationEntity}s
   * @return an instance of the data configuration container implementation
   */
  protected abstract T loadInstance(Collection<ShpurdpConfigurationEntity> configurationEntities);
}
