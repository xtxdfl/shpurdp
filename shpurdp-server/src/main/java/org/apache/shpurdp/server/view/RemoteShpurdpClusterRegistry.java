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

package org.apache.shpurdp.server.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.ClusterNotFoundException;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.orm.dao.RemoteShpurdpClusterDAO;
import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterEntity;
import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterServiceEntity;
import org.apache.shpurdp.view.ShpurdpHttpException;

/**
 * Registry for Remote Shpurdp Cluster
 */
@Singleton
public class RemoteShpurdpClusterRegistry {

  private ConcurrentHashMap<Long,RemoteShpurdpCluster> clusterMap = new ConcurrentHashMap<>();

  @Inject
  private RemoteShpurdpClusterDAO remoteShpurdpClusterDAO;

  @Inject
  private Configuration configuration;

  public RemoteShpurdpCluster get(Long clusterId) throws MalformedURLException, ClusterNotFoundException {
    RemoteShpurdpCluster remoteShpurdpCluster = clusterMap.get(clusterId);
    if (remoteShpurdpCluster == null) {
      RemoteShpurdpCluster cluster = getCluster(clusterId);
      RemoteShpurdpCluster oldCluster = clusterMap.putIfAbsent(clusterId, cluster);
      if (oldCluster == null) remoteShpurdpCluster = cluster;
      else remoteShpurdpCluster = oldCluster;
    }
    return remoteShpurdpCluster;
  }


  private RemoteShpurdpCluster getCluster(Long clusterId) throws MalformedURLException, ClusterNotFoundException {
    RemoteShpurdpClusterEntity remoteShpurdpClusterEntity = remoteShpurdpClusterDAO.findById(clusterId);
    if (remoteShpurdpClusterEntity == null) {
      throw new ClusterNotFoundException(clusterId);
    }
    RemoteShpurdpCluster remoteShpurdpCluster = new RemoteShpurdpCluster(remoteShpurdpClusterEntity, configuration);
    return remoteShpurdpCluster;
  }

  /**
   * Update the remote cluster properties
   *
   * @param entity
   */
  public void update(RemoteShpurdpClusterEntity entity) {
    remoteShpurdpClusterDAO.update(entity);
    clusterMap.remove(entity.getId());
  }

  /**
   * Remove the cluster entity from registry and database
   *
   * @param entity
   */
  public void delete(RemoteShpurdpClusterEntity entity) {
    remoteShpurdpClusterDAO.delete(entity);
    clusterMap.remove(entity.getId());
  }

  /**
   * Save Remote Cluster Entity after setting services.
   *
   * @param entity
   * @param update
   * @throws IOException
   * @throws ShpurdpHttpException
   */
  public void saveOrUpdate(RemoteShpurdpClusterEntity entity, boolean update) throws IOException, ShpurdpHttpException {

    RemoteShpurdpCluster cluster = new RemoteShpurdpCluster(entity, configuration);
    Set<String> services = cluster.getServices();

    if (!cluster.isShpurdpOrClusterAdmin()) {
      throw new ShpurdpException("User must be Shpurdp or Cluster Adminstrator.");
    }

    Collection<RemoteShpurdpClusterServiceEntity> serviceEntities = new ArrayList<>();

    for (String service : services) {
      RemoteShpurdpClusterServiceEntity serviceEntity = new RemoteShpurdpClusterServiceEntity();
      serviceEntity.setServiceName(service);
      serviceEntity.setCluster(entity);
      serviceEntities.add(serviceEntity);
    }

    entity.setServices(serviceEntities);

    if (update) {
      update(entity);
    } else {
      remoteShpurdpClusterDAO.save(entity);
    }
  }

}
