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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */
package org.apache.shpurdp.server.controller.internal;

import static org.apache.shpurdp.server.controller.internal.ClusterPrivilegeResourceProvider.CLUSTER_NAME;
import static org.apache.shpurdp.server.controller.internal.ViewPrivilegeResourceProvider.INSTANCE_NAME;
import static org.apache.shpurdp.server.controller.internal.ViewPrivilegeResourceProvider.VERSION;
import static org.apache.shpurdp.server.controller.internal.ViewPrivilegeResourceProvider.VIEW_NAME;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.shpurdp.server.orm.dao.ClusterDAO;
import org.apache.shpurdp.server.orm.entities.ClusterEntity;
import org.apache.shpurdp.server.orm.entities.GroupEntity;
import org.apache.shpurdp.server.orm.entities.PermissionEntity;
import org.apache.shpurdp.server.orm.entities.PrivilegeEntity;
import org.apache.shpurdp.server.orm.entities.ResourceEntity;
import org.apache.shpurdp.server.orm.entities.ResourceTypeEntity;
import org.apache.shpurdp.server.orm.entities.UserEntity;
import org.apache.shpurdp.server.orm.entities.ViewEntity;
import org.apache.shpurdp.server.orm.entities.ViewInstanceEntity;
import org.apache.shpurdp.server.security.authorization.ResourceType;
import org.apache.shpurdp.server.security.authorization.RoleAuthorization;
import org.apache.shpurdp.server.view.ViewRegistry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for Shpurdp privileges.
 */
public class ShpurdpPrivilegeResourceProvider extends PrivilegeResourceProvider<Object> {

  public static final String TYPE = PrivilegeResourceProvider.PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PrivilegeResourceProvider.TYPE_PROPERTY_ID;

  /**
   * Data access object used to obtain privilege entities.
   */
  protected static ClusterDAO clusterDAO;

  /**
   * The property ids for an Shpurdp privilege resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      PRIVILEGE_ID,
      PERMISSION_NAME,
      PERMISSION_LABEL,
      PRINCIPAL_NAME,
      PRINCIPAL_TYPE,
      VIEW_NAME,
      VERSION,
      INSTANCE_NAME,
      CLUSTER_NAME,
      TYPE);

  /**
   * The key property ids for a privilege resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.ShpurdpPrivilege, PRIVILEGE_ID)
      .build();


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an ShpurdpPrivilegeResourceProvider.
   */
  public ShpurdpPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.ShpurdpPrivilege);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.SHPURDP_ASSIGN_ROLES);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  // ----- ShpurdpPrivilegeResourceProvider ---------------------------------

  /**
   * Static initialization.
   *
   * @param clusterDao  the cluster data access object
   */
  public static void init(ClusterDAO clusterDao) {
    clusterDAO  = clusterDao;
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, Object> getResourceEntities(Map<String, Object> properties) {
    Map<Long, Object> resourceEntities = new LinkedHashMap<>();

    resourceEntities.put(ResourceEntity.SHPURDP_RESOURCE_ID, null);
    // add cluster entities
    List<ClusterEntity> clusterEntities = clusterDAO.findAll();

    if (clusterEntities != null) {
      for (ClusterEntity clusterEntity : clusterEntities) {
        resourceEntities.put(clusterEntity.getResource().getId(), clusterEntity);
      }
    }
    //add view entities
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    for (ViewEntity viewEntity : viewRegistry.getDefinitions()) {
      if (viewEntity.isDeployed()) {
        for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()) {
          resourceEntities.put(viewInstanceEntity.getResource().getId(), viewInstanceEntity);
        }
      }
    }
    return resourceEntities;
  }

  @Override
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, PermissionEntity> roleEntities,
                                Map<Long, Object> resourceEntities,
                                Set<String> requestedIds) {
    Resource resource = super.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, requestedIds);
    if (resource != null) {
      ResourceEntity resourceEntity = privilegeEntity.getResource();
      ResourceTypeEntity type = resourceEntity.getResourceType();
      String typeName = type.getName();
      ResourceType resourceType = ResourceType.translate(typeName);

      if(resourceType != null) {
        switch (resourceType) {
          case SHPURDP:
            // there is nothing special to add for this case
            break;
          case CLUSTER:
            ClusterEntity clusterEntity = (ClusterEntity) resourceEntities.get(resourceEntity.getId());
            setResourceProperty(resource, CLUSTER_NAME, clusterEntity.getClusterName(), requestedIds);
            break;
          case VIEW:
            ViewInstanceEntity viewInstanceEntity = (ViewInstanceEntity) resourceEntities.get(resourceEntity.getId());
            ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

            setResourceProperty(resource, VIEW_NAME, viewEntity.getCommonName(), requestedIds);
            setResourceProperty(resource, VERSION, viewEntity.getVersion(), requestedIds);
            setResourceProperty(resource, INSTANCE_NAME, viewInstanceEntity.getName(), requestedIds);
            break;
        }

        setResourceProperty(resource, TYPE, resourceType.name(), requestedIds);
      }
    }

    return resource;
  }
  @Override
  public Long getResourceEntityId(Predicate predicate) {
    return ResourceEntity.SHPURDP_RESOURCE_ID;
  }
}
