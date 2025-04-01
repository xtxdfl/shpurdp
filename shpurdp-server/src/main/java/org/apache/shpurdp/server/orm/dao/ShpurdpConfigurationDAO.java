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

package org.apache.shpurdp.server.orm.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;

import org.apache.shpurdp.server.orm.RequiresSession;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntityPK;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

/**
 * DAO dealing with shpurdp configuration related JPA operations.
 * Operations delegate to the JPA provider implementation of CRUD operations.
 */

@Singleton
public class ShpurdpConfigurationDAO extends CrudDAO<ShpurdpConfigurationEntity, ShpurdpConfigurationEntityPK> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShpurdpConfigurationDAO.class);

  @Inject
  public ShpurdpConfigurationDAO() {
    super(ShpurdpConfigurationEntity.class);
  }

  /**
   * Returns the Shpurdp configuration properties with the requested category name from the database.
   *
   * @param categoryName the configuration category name
   * @return the configuration entity
   */
  @RequiresSession
  public List<ShpurdpConfigurationEntity> findByCategory(String categoryName) {
    TypedQuery<ShpurdpConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
        "ShpurdpConfigurationEntity.findByCategory", ShpurdpConfigurationEntity.class);
    query.setParameter("categoryName", categoryName);
    return daoUtils.selectList(query);
  }

  /**
   * Removes the Shpurdp configuration properties with the requested category name from the database.
   *
   * @param categoryName the configuration category name
   * @return the number of items removed
   */
  @Transactional
  public int removeByCategory(String categoryName) {
    TypedQuery<ShpurdpConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
        "ShpurdpConfigurationEntity.deleteByCategory", ShpurdpConfigurationEntity.class);
    query.setParameter("categoryName", categoryName);
    return query.executeUpdate();
  }

  @Override
  @Transactional
  public void create(ShpurdpConfigurationEntity entity) {
    // make sure only one entry exists per configuration type...
    ShpurdpConfigurationEntity foundEntity = findByPK(new ShpurdpConfigurationEntityPK(entity.getCategoryName(), entity.getPropertyName()));
    if (foundEntity != null) {
      String message = String.format("Only one configuration entry can exist for the category %s and name %s", entity.getCategoryName(), entity.getPropertyName());
      LOGGER.error(message);
      throw new EntityExistsException(message);
    }

    super.create(entity);
  }

  @Override
  public ShpurdpConfigurationEntity merge(ShpurdpConfigurationEntity entity) {
    ShpurdpConfigurationEntity foundEntity = findByPK(new ShpurdpConfigurationEntityPK(entity.getCategoryName(), entity.getPropertyName()));
    if (foundEntity == null) {
      String message = String.format("The configuration entry for the category %s and name %s does not exist", entity.getCategoryName(), entity.getPropertyName());
      LOGGER.debug(message);
      throw new EntityNotFoundException(message);
    }

    ShpurdpConfigurationEntity updatedEntity = entity;

    if (!StringUtils.equals(foundEntity.getPropertyValue(), entity.getPropertyValue())) {
      // updating the existing entity
      updatedEntity = super.merge(entity);
      entityManagerProvider.get().flush();
    }

    return updatedEntity;
  }

  /**
   * Reconciles the properties associted with an Shpurdp confgiration category (for example, ldap-configuration)
   * using persisted properties and the supplied properties.
   * <p>
   * if <code>removeIfNotProvided</code> is <code>true</code>, only properties that exist in the new set of
   * properties will be persisted; others will be removed.
   * <p>
   * If <code>removeIfNotProvided</code> is <code>false</code>, then the new properties will be used
   * to update or append to the set of persisted properties.
   *
   * @param categoryName        the category name for the set of properties
   * @param properties          a map of name to value pairs
   * @param removeIfNotProvided <code>true</code> to explicitly set the set of properties for the category; <code>false</code> to upadate the set of properties for the category
   * @return <code>true</code> if changes were made; <code>false</code> if not changes were made.
   */
  @Transactional
  public boolean reconcileCategory(String categoryName, Map<String, String> properties, boolean removeIfNotProvided) {
    boolean changesDetected = false;
    List<ShpurdpConfigurationEntity> existingEntities = findByCategory(categoryName);
    Map<String, String> propertiesToProcess = new HashMap<>();

    if (properties != null) {
      propertiesToProcess.putAll(properties);
    }

    if (existingEntities != null) {
      for (ShpurdpConfigurationEntity entity : existingEntities) {
        String propertyName = entity.getPropertyName();

        if (propertiesToProcess.containsKey(propertyName)) {
          String newPropertyValue = propertiesToProcess.get(propertyName);
          if (!StringUtils.equals(newPropertyValue, entity.getPropertyValue())) {
            // Update the entry...
            entity.setPropertyValue(newPropertyValue);
            merge(entity);
            changesDetected = true;
          }
        } else if (removeIfNotProvided) {
          // Remove the entry since it is not in the new set of properties...
          remove(entity);
          changesDetected = true;
        }

        // If already processed, remove it so we know no to add it later...
        propertiesToProcess.remove(propertyName);
      }
    }

    // Add the new entries...
    if (!propertiesToProcess.isEmpty()) {
      for (Map.Entry<String, String> property : propertiesToProcess.entrySet()) {
        ShpurdpConfigurationEntity entity = new ShpurdpConfigurationEntity();
        entity.setCategoryName(categoryName);
        entity.setPropertyName(property.getKey());
        entity.setPropertyValue(property.getValue());
        create(entity);
      }

      changesDetected = true;
    }

    if (changesDetected) {
      entityManagerProvider.get().flush();
    }

    return changesDetected;
  }
}
