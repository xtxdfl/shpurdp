/**
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
package org.apache.shpurdp.server.orm.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.shpurdp.server.orm.RequiresSession;
import org.apache.shpurdp.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.shpurdp.server.orm.helpers.SQLConstants;
import org.apache.shpurdp.server.orm.helpers.SQLOperations;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyLogicalRequestDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public TopologyLogicalRequestEntity findById(Long id) {
    return entityManagerProvider.get().find(TopologyLogicalRequestEntity.class, id);
  }

  @RequiresSession
  public List<TopologyLogicalRequestEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), TopologyLogicalRequestEntity.class);
  }

  @Transactional
  public void create(TopologyLogicalRequestEntity requestEntity) {
    entityManagerProvider.get().persist(requestEntity);
  }

  @Transactional
  public TopologyLogicalRequestEntity merge(TopologyLogicalRequestEntity requestEntity) {
    return entityManagerProvider.get().merge(requestEntity);
  }

  @Transactional
  public void remove(TopologyLogicalRequestEntity requestEntity) {
    entityManagerProvider.get().remove(requestEntity);
  }

  @RequiresSession
  public Set<Long> findRequestIdsByIds(Set<Long> ids) {
    EntityManager entityManager = entityManagerProvider.get();
    final Set<Long> result = new HashSet<>();
    final TypedQuery<Long> topologyLogicalRequestQuery =
      entityManager.createNamedQuery("TopologyLogicalRequestEntity.findRequestIds", Long.class);

    SQLOperations.batch(ids, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      topologyLogicalRequestQuery.setParameter("ids", chunk);
      result.addAll(daoUtils.selectList(topologyLogicalRequestQuery));
      return 0;
    });
    return Sets.newHashSet(result);
  }
}
