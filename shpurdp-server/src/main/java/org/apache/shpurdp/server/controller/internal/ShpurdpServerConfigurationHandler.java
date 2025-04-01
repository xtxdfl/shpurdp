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

package org.apache.shpurdp.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.RootServiceComponentConfiguration;
import org.apache.shpurdp.server.controller.spi.SystemException;
import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * ShpurdpServerConfigurationHandler handles Shpurdp server specific configuration properties.
 */
@Singleton
public class ShpurdpServerConfigurationHandler extends RootServiceComponentConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ShpurdpServerConfigurationHandler.class);

  private final ShpurdpConfigurationDAO shpurdpConfigurationDAO;
  private final ShpurdpEventPublisher publisher;

  @Inject
  ShpurdpServerConfigurationHandler(ShpurdpConfigurationDAO shpurdpConfigurationDAO, ShpurdpEventPublisher publisher) {
    this.shpurdpConfigurationDAO = shpurdpConfigurationDAO;
    this.publisher = publisher;
  }

  @Override
  public Map<String, RootServiceComponentConfiguration> getComponentConfigurations(String categoryName) {
    Map<String, RootServiceComponentConfiguration> configurations = null;

    List<ShpurdpConfigurationEntity> entities = (categoryName == null)
        ? shpurdpConfigurationDAO.findAll()
        : shpurdpConfigurationDAO.findByCategory(categoryName);

    if (entities != null) {
      configurations = new HashMap<>();
      for (ShpurdpConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        RootServiceComponentConfiguration configuration = configurations.get(category);
        if (configuration == null) {
          configuration = new RootServiceComponentConfiguration();
          configurations.put(category, configuration);
        }

        configuration.addProperty(entity.getPropertyName(), entity.getPropertyValue());
        if (categoryName != null) {
          configuration.addPropertyType(entity.getPropertyName(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(categoryName, entity.getPropertyName()));
        }
      }
    }

    return configurations;
  }

  @Override
  public void removeComponentConfiguration(String categoryName) {
    if (null == categoryName) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting Shpurdp configuration with id: {}", categoryName);
      if (shpurdpConfigurationDAO.removeByCategory(categoryName) > 0) {
        publisher.publish(new ShpurdpConfigurationChangedEvent(categoryName));
      }
    }
  }

  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws ShpurdpException {
    validateProperties(categoryName, properties);
    final boolean toBePublished = properties.isEmpty() ? false : shpurdpConfigurationDAO.reconcileCategory(categoryName, properties, removePropertiesIfNotSpecified);

    if (toBePublished) {
      // notify subscribers about the configuration changes
      publisher.publish(new ShpurdpConfigurationChangedEvent(categoryName));
    }
  }

  private void validateProperties(String categoryName, Map<String, String> properties) {
    for (String key : properties.keySet()) {
      if (ShpurdpServerConfigurationUtils.getConfigurationKey(categoryName, key) == null) {
        throw new IllegalArgumentException(String.format("Invalid Shpurdp server configuration key: %s:%s", categoryName, key));
      }
    }
  }

  @Override
  public OperationResult performOperation(String categoryName, Map<String, String> properties,
                                          boolean mergeExistingProperties, String operation,
                                          Map<String, Object> operationParameters) throws SystemException {
    throw new SystemException(String.format("The requested operation is not supported for this category: %s", categoryName));
  }

  public Map<String, Map<String, String>> getConfigurations() {
    Map<String, Map<String, String>> configurations = new HashMap<>();
    List<ShpurdpConfigurationEntity> entities = shpurdpConfigurationDAO.findAll();

    if (entities != null) {
      for (ShpurdpConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> configuration = configurations.computeIfAbsent(category, k -> new HashMap<>());
        configuration.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return configurations;
  }

  /**
   * Get the properties associated with a configuration category
   *
   * @param categoryName the name of the requested category
   * @return a map of property names to values; or null if the data does not exist
   */
  public Map<String, String> getConfigurationProperties(String categoryName) {
    Map<String, String> properties = null;

    List<ShpurdpConfigurationEntity> entities = shpurdpConfigurationDAO.findByCategory(categoryName);
    if (entities != null) {
      properties = new HashMap<>();
      for (ShpurdpConfigurationEntity entity : entities) {
        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }
    }

    return properties;
  }

  protected Set<String> getEnabledServices(String categoryName, String manageServicesConfigurationPropertyName, String enabledServicesPropertyName) {
    final Map<String, String> configurationProperties = getConfigurationProperties(categoryName);
    final boolean manageConfigurations = StringUtils.isNotBlank(manageServicesConfigurationPropertyName)
        && "true".equalsIgnoreCase(configurationProperties.get(manageServicesConfigurationPropertyName));
    final String enabledServices = (manageConfigurations) ? configurationProperties.get(enabledServicesPropertyName) : null;

    if (StringUtils.isEmpty(enabledServices)) {
      return Collections.emptySet();
    } else {
      return Arrays.stream(enabledServices.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
    }
  }
}
