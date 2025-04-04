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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.DuplicateResourceException;
import org.apache.shpurdp.server.StaticallyInject;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.predicate.EqualsPredicate;
import org.apache.shpurdp.server.controller.spi.NoSuchParentResourceException;
import org.apache.shpurdp.server.controller.spi.NoSuchResourceException;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.RequestStatus;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.shpurdp.server.controller.spi.SystemException;
import org.apache.shpurdp.server.controller.spi.UnsupportedPropertyException;
import org.apache.shpurdp.server.orm.dao.RemoteShpurdpClusterDAO;
import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterEntity;
import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterServiceEntity;
import org.apache.shpurdp.server.security.authorization.RoleAuthorization;
import org.apache.shpurdp.server.view.RemoteShpurdpClusterRegistry;
import org.apache.shpurdp.view.MaskException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 *  Resource Provider for Remote Cluster
 */
@StaticallyInject
public class RemoteClusterResourceProvider extends AbstractAuthorizedResourceProvider {

  /**
   * Remote Cluster property id constants.
   */
  public static final String CLUSTER_NAME_PROPERTY_ID = "ClusterInfo/name";
  public static final String CLUSTER_ID_PROPERTY_ID = "ClusterInfo/cluster_id";
  public static final String CLUSTER_URL_PROPERTY_ID  = "ClusterInfo/url";
  public static final String USERNAME_PROPERTY_ID = "ClusterInfo/username";
  public static final String PASSWORD_PROPERTY_ID = "ClusterInfo/password";
  public static final String SERVICES_PROPERTY_ID = "ClusterInfo/services";

  /**
   * The logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(RemoteClusterResourceProvider.class);

  /**
   * The key property ids for a Remote Cluster resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.RemoteCluster, CLUSTER_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Remote Cluster resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      CLUSTER_NAME_PROPERTY_ID,
      CLUSTER_ID_PROPERTY_ID,
      CLUSTER_URL_PROPERTY_ID,
      USERNAME_PROPERTY_ID,
      PASSWORD_PROPERTY_ID,
      SERVICES_PROPERTY_ID);

  @Inject
  private static RemoteShpurdpClusterDAO remoteShpurdpClusterDAO;

  @Inject
  private static Configuration configuration;

  @Inject
  private static RemoteShpurdpClusterRegistry remoteShpurdpClusterRegistry;

  /**
   * Create a  new resource provider.
   */
  protected RemoteClusterResourceProvider() {
    super(Resource.Type.RemoteCluster, propertyIds, keyPropertyIds);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.SHPURDP_ADD_DELETE_CLUSTERS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties));
    }
    notifyCreate(Resource.Type.RemoteCluster, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources    = new HashSet<>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    if (propertyMaps.isEmpty()) {
      propertyMaps.add(Collections.emptyMap());
    }

    for (Map<String, Object> propertyMap : propertyMaps) {

      String clusterName = (String) propertyMap.get(CLUSTER_NAME_PROPERTY_ID);

      if(!Strings.isNullOrEmpty(clusterName)){
        RemoteShpurdpClusterEntity cluster = remoteShpurdpClusterDAO.findByName(clusterName);
        if(cluster == null) {
          throw new NoSuchResourceException(String.format("Cluster with name %s cannot be found",clusterName) );
        }
        resources.add(toResource(requestedIds, cluster));
      }else {
        for (RemoteShpurdpClusterEntity cluster : remoteShpurdpClusterDAO.findAll()){
          Resource resource = toResource(requestedIds, cluster);
          resources.add(resource);
        }
      }
    }
    return resources;
  }

  protected Resource toResource(Set<String> requestedIds, RemoteShpurdpClusterEntity cluster) {
    Resource   resource   = new ResourceImpl(Resource.Type.RemoteCluster);
    setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, cluster.getName(), requestedIds);
    setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, cluster.getId(), requestedIds);
    setResourceProperty(resource, CLUSTER_URL_PROPERTY_ID, cluster.getUrl(), requestedIds);
    setResourceProperty(resource, USERNAME_PROPERTY_ID, cluster.getUsername(), requestedIds);
    ArrayList<String> services = new ArrayList<>();
    for (RemoteShpurdpClusterServiceEntity remoteClusterServiceEntity : cluster.getServices()) {
      services.add(remoteClusterServiceEntity.getServiceName());
    }
    setResourceProperty(resource, SERVICES_PROPERTY_ID,services, requestedIds);
    return resource;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        modifyResources(getUpdateCommand(propertyMap));
      }
    }
    notifyUpdate(Resource.Type.RemoteCluster, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    modifyResources(getDeleteCommand(predicate));
    notifyDelete(Resource.Type.ViewInstance, predicate);
    return getRequestStatus(null);

  }

  /**
   * Get the command to create the RemoteShpurdpCluster
   * @param properties
   * @return A command to create the RemoteShpurdpCluster
   */
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws ShpurdpException {
        String name = (String)properties.get(CLUSTER_NAME_PROPERTY_ID);

        if(StringUtils.isEmpty(name)){
          throw new IllegalArgumentException("Cluster Name cannot ne null or Empty");
        }

        if(remoteShpurdpClusterDAO.findByName(name) != null){
          throw new DuplicateResourceException(String.format("Remote cluster with name %s already exists",name));
        }

        saveOrUpdateRemoteShpurdpClusterEntity(properties,false);

        return null;
      }
    };
  }

  /**
   * Get the command to update the RemoteShpurdpCluster
   * @param properties
   * @return A command to update the RemoteShpurdpCluster
   */
  private Command<Void> getUpdateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws ShpurdpException {
        String name = (String)properties.get(CLUSTER_NAME_PROPERTY_ID);

        if (StringUtils.isEmpty(name)) {
          throw new IllegalArgumentException("Cluster Name cannot be null or Empty");
        }

        String id = (String)properties.get(CLUSTER_ID_PROPERTY_ID);

        if (StringUtils.isEmpty(id)) {
          throw new IllegalArgumentException("Cluster Id cannot be null or Empty");
        }

        saveOrUpdateRemoteShpurdpClusterEntity(properties,true);
        return null;

      }
    };
  }

  /**
   *  Save or update Remote Shpurdp Cluster Entity to database
   *
   * @param properties
   * @param update
   * @throws ShpurdpException
   */
  private void saveOrUpdateRemoteShpurdpClusterEntity(Map<String, Object> properties,boolean update) throws ShpurdpException {
    String name = (String)properties.get(CLUSTER_NAME_PROPERTY_ID);
    String url = (String)properties.get(CLUSTER_URL_PROPERTY_ID);
    String username = (String)properties.get(USERNAME_PROPERTY_ID);
    String password = (String)properties.get(PASSWORD_PROPERTY_ID);

    if (StringUtils.isEmpty(url) && StringUtils.isEmpty(username)) {
      throw new IllegalArgumentException("Url or username cannot be null");
    }

    RemoteShpurdpClusterEntity entity ;

    if (update) {
      Long id = Long.valueOf((String) properties.get(CLUSTER_ID_PROPERTY_ID));
      entity = remoteShpurdpClusterDAO.findById(id);
      if (entity == null) {
        throw new IllegalArgumentException(String.format("Cannot find cluster with Id : \"%s\"", id));
      }
    } else {

      entity = remoteShpurdpClusterDAO.findByName(name);
      if (entity != null) {
        throw new DuplicateResourceException(String.format("Cluster with name : \"%s\" already exists", name));
      }
    }

    // Check Password not null for create
    //Check username matches the entity username if password not present
    if(StringUtils.isBlank(password) && !update){
      throw new IllegalArgumentException("Password cannot be null");
    }else if(StringUtils.isBlank(password) && update && !username.equals(entity.getUsername())){
      throw new IllegalArgumentException("Failed to update. Username does not match.");
    }

    if (entity == null) {
      entity = new RemoteShpurdpClusterEntity();
    }

    entity.setName(name);
    entity.setUrl(url);
    try {
      if (password != null) {
        entity.setUsername(username);
        entity.setPassword(password);
      }
    } catch (MaskException e) {
      throw new IllegalArgumentException("Failed to create new Remote Cluster " + name + ". Illegal Password");
    }

    try {
      remoteShpurdpClusterRegistry.saveOrUpdate(entity,update);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to create new Remote Cluster " + name +". " + e.getMessage(),e);
    }
  }

  /**
   * Get the command to delete the Cluster
   * @param predicate
   * @return The delete command
   */
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws ShpurdpException {
        Comparable deletedCluster = ((EqualsPredicate) predicate).getValue();
        String toDelete = deletedCluster.toString();
        RemoteShpurdpClusterEntity clusterEntity = remoteShpurdpClusterDAO.findByName(toDelete);
        if(clusterEntity == null){
          throw new IllegalArgumentException("The Cluster "+ toDelete +" does not exist");
        }

        remoteShpurdpClusterRegistry.delete(clusterEntity);
        return null;
      }
    };
  }
}
