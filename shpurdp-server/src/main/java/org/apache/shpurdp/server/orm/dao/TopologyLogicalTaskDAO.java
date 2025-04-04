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
import org.apache.shpurdp.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.shpurdp.server.orm.helpers.SQLConstants;
import org.apache.shpurdp.server.orm.helpers.SQLOperations;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyLogicalTaskDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public TopologyLogicalTaskEntity findById(Long id) {
    return entityManagerProvider.get().find(TopologyLogicalTaskEntity.class, id);
  }

  @RequiresSession
  public Set<Long> findHostTaskIdsByPhysicalTaskIds(Set<Long> physicalTaskIds) {
    EntityManager entityManager = entityManagerProvider.get();
    final Set<Long> result = new HashSet<>();
    final TypedQuery<Long> topologyHostTaskQuery =
      entityManager.createNamedQuery("TopologyLogicalTaskEntity.findHostTaskIdsByPhysicalTaskIds", Long.class);

    SQLOperations.batch(physicalTaskIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      topologyHostTaskQuery.setParameter("physicalTaskIds", chunk);
      result.addAll(daoUtils.selectList(topologyHostTaskQuery));
      return 0;
    });
    return Sets.newHashSet(result);
  }

  @RequiresSession
  public List<TopologyLogicalTaskEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), TopologyLogicalTaskEntity.class);
  }

  @Transactional
  public void create(TopologyLogicalTaskEntity logicalTaskEntity) {
    entityManagerProvider.get().persist(logicalTaskEntity);
  }

  @Transactional
  public TopologyLogicalTaskEntity merge(TopologyLogicalTaskEntity logicalTaskEntity) {
    return entityManagerProvider.get().merge(logicalTaskEntity);
  }

  @Transactional
  public void remove(TopologyLogicalTaskEntity logicalTaskEntity) {
    entityManagerProvider.get().remove(logicalTaskEntity);
  }
}

