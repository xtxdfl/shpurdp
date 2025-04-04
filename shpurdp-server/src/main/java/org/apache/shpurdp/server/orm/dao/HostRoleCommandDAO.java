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

package org.apache.shpurdp.server.orm.dao;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.shpurdp.annotations.TransactionalLock;
import org.apache.shpurdp.annotations.TransactionalLock.LockArea;
import org.apache.shpurdp.annotations.TransactionalLock.LockType;
import org.apache.shpurdp.server.Role;
import org.apache.shpurdp.server.RoleCommand;
import org.apache.shpurdp.server.actionmanager.HostRoleCommand;
import org.apache.shpurdp.server.actionmanager.HostRoleCommandFactory;
import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.agent.AgentCommand.AgentCommandType;
import org.apache.shpurdp.server.api.query.JpaPredicateVisitor;
import org.apache.shpurdp.server.api.query.JpaSortBuilder;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.spi.PageRequest;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.SortRequest;
import org.apache.shpurdp.server.controller.utilities.PredicateHelper;
import org.apache.shpurdp.server.events.TaskCreateEvent;
import org.apache.shpurdp.server.events.TaskUpdateEvent;
import org.apache.shpurdp.server.events.publishers.TaskEventPublisher;
import org.apache.shpurdp.server.orm.RequiresSession;
import org.apache.shpurdp.server.orm.TransactionalLocks;
import org.apache.shpurdp.server.orm.entities.HostEntity;
import org.apache.shpurdp.server.orm.entities.HostRoleCommandEntity;
import org.apache.shpurdp.server.orm.entities.HostRoleCommandEntity_;
import org.apache.shpurdp.server.orm.entities.StageEntity;
import org.apache.shpurdp.server.orm.helpers.SQLConstants;
import org.apache.shpurdp.server.orm.helpers.SQLOperations;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;

@Singleton
public class HostRoleCommandDAO {

  private static final Logger LOG = LoggerFactory.getLogger(HostRoleCommandDAO.class);

  private static final String SUMMARY_DTO = String.format(
    "SELECT NEW %s(" +
      "MAX(hrc.stage.skippable), " +
      "MIN(hrc.startTime), " +
      "MAX(hrc.endTime), " +
      "hrc.stageId, " +
      "SUM(CASE WHEN hrc.status = :aborted THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :completed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :failed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding_failed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding_timedout THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :in_progress THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :pending THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :queued THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :timedout THEN 1 ELSE 0 END)," +
      "SUM(CASE WHEN hrc.status = :skipped_failed THEN 1 ELSE 0 END)" +
      ") FROM HostRoleCommandEntity hrc " +
      " GROUP BY hrc.requestId, hrc.stageId HAVING hrc.requestId = :requestId",
      HostRoleCommandStatusSummaryDTO.class.getName());

  /**
   * SQL template to get requests that have at least one task in any of the
   * specified statuses.
   */
  private static final String REQUESTS_BY_TASK_STATUS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :taskStatuses ORDER BY task.requestId {0}";

  /**
   * SQL template to get all requests which have had all of their tasks
   * COMPLETED
   */
  private static final String COMPLETED_REQUESTS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE task.requestId NOT IN (SELECT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :notCompletedStatuses) ORDER BY task.requestId {0}";

  /**
   * A cache that holds {@link HostRoleCommandStatusSummaryDTO} grouped by stage
   * id for requests by request id. The JPQL computing the host role command
   * status summary for a request is rather expensive thus this cache helps
   * reducing the load on the database.
   * <p/>
   * Methods which interact with this cache, including invalidation and
   * population, should use the {@link TransactionalLock} annotation along with
   * the {@link LockArea#HRC_STATUS_CACHE}. This will prevent stale data from
   * being read during a transaction which has updated a
   * {@link HostRoleCommandEntity}'s {@link HostRoleStatus} but has not
   * committed yet.
   * <p/>
   * This cache cannot be a {@link LoadingCache} since there is an inherent
   * problem with concurrency of reloads. Namely, if the entry has been read
   * during a load, but not yet put into the cache and another invalidation is
   * registered. The old value would eventually make it into the cache and the
   * last invalidation would not invalidate anything since the cache was empty
   * at the time.
   */
  private final Cache<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> hrcStatusSummaryCache;

  /**
   * Specifies whether caching for {@link HostRoleCommandStatusSummaryDTO} grouped by stage id for requests
   * is enabled.
   */
  private final boolean hostRoleCommandStatusSummaryCacheEnabled;


  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @Inject
  private Configuration configuration;


  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private TaskEventPublisher taskEventPublisher;

  /**
   * Used to ensure that methods which rely on the completion of
   * {@link Transactional} can detect when they are able to run.
   *
   * @see TransactionalLock
   */
  @Inject
  private final TransactionalLocks transactionLocks = null;

  public final static String HRC_STATUS_SUMMARY_CACHE_SIZE =  "hostRoleCommandStatusSummaryCacheSize";
  public final static String HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES = "hostRoleCommandStatusCacheExpiryDurationMins";
  public final static String HRC_STATUS_SUMMARY_CACHE_ENABLED =  "hostRoleCommandStatusSummaryCacheEnabled";

  /**
   * Invalidates the host role command status summary cache entry that corresponds to the given request.
   * @param requestId the key of the cache entry to be invalidated.
   */
  protected void invalidateHostRoleCommandStatusSummaryCache(Long requestId) {
    if (!hostRoleCommandStatusSummaryCacheEnabled ) {
      return;
    }

    LOG.debug("Invalidating host role command status summary cache for request {} !", requestId);
    hrcStatusSummaryCache.invalidate(requestId);
  }

  /**
   * Invalidates the host role command status summary cache entry that
   * corresponds to each request.
   *
   * @param requestIds
   *          the requests to invalidate
   */
  protected void invalidateHostRoleCommandStatusSummaryCache(Set<Long> requestIds) {
    for (Long requestId : requestIds) {
      if (null != requestId) {
        invalidateHostRoleCommandStatusSummaryCache(requestId);
      }
    }
  }

  /**
   * Invalidates those entries in host role command status cache which are
   * dependent on the passed
   * {@link org.apache.shpurdp.server.orm.entities.HostRoleCommandEntity} entity.
   *
   * @param hostRoleCommandEntity
   */
  protected void invalidateHostRoleCommandStatusSummaryCache(
      HostRoleCommandEntity hostRoleCommandEntity) {
    if ( !hostRoleCommandStatusSummaryCacheEnabled ) {
      return;
    }

    if (hostRoleCommandEntity != null) {
      Long requestId = hostRoleCommandEntity.getRequestId();
      if (requestId == null) {
        StageEntity stageEntity = hostRoleCommandEntity.getStage();
        if (stageEntity != null) {
          requestId = stageEntity.getRequestId();
        }
      }

      if (requestId != null) {
        invalidateHostRoleCommandStatusSummaryCache(requestId.longValue());
      }
    }
  }

  /**
   * Loads the counts of tasks for a request and groups them by stage id.
   * This allows for very efficient loading when there are a huge number of stages
   * and tasks to iterate (for example, during a Stack Upgrade).
   * @param requestId the request id
   * @return the map of stage-to-summary objects
   */
  @RequiresSession
  private Map<Long, HostRoleCommandStatusSummaryDTO> loadAggregateCounts(Long requestId) {
    Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<>();

    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManager.createQuery(SUMMARY_DTO,
        HostRoleCommandStatusSummaryDTO.class);

    query.setParameter("requestId", requestId);
    query.setParameter("aborted", HostRoleStatus.ABORTED);
    query.setParameter("completed", HostRoleStatus.COMPLETED);
    query.setParameter("failed", HostRoleStatus.FAILED);
    query.setParameter("holding", HostRoleStatus.HOLDING);
    query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
    query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
    query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
    query.setParameter("pending", HostRoleStatus.PENDING);
    query.setParameter("queued", HostRoleStatus.QUEUED);
    query.setParameter("timedout", HostRoleStatus.TIMEDOUT);
    query.setParameter("skipped_failed", HostRoleStatus.SKIPPED_FAILED);

    for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
      map.put(dto.getStageId(), dto);
    }

    return map;
  }

  @Inject
  public HostRoleCommandDAO(
      @Named(HRC_STATUS_SUMMARY_CACHE_ENABLED) boolean hostRoleCommandStatusSummaryCacheEnabled,
      @Named(HRC_STATUS_SUMMARY_CACHE_SIZE) long hostRoleCommandStatusSummaryCacheLimit,
      @Named(HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES) long hostRoleCommandStatusSummaryCacheExpiryDurationMins) {
    this.hostRoleCommandStatusSummaryCacheEnabled = hostRoleCommandStatusSummaryCacheEnabled;

    LOG.info("Host role command status summary cache {} !", hostRoleCommandStatusSummaryCacheEnabled ? "enabled" : "disabled");

    hrcStatusSummaryCache = CacheBuilder.newBuilder()
      .maximumSize(hostRoleCommandStatusSummaryCacheLimit)
      .expireAfterWrite(hostRoleCommandStatusSummaryCacheExpiryDurationMins, TimeUnit.MINUTES)
      .build();
  }

  @RequiresSession
  public HostRoleCommandEntity findByPK(long taskId) {
    return entityManagerProvider.get().find(HostRoleCommandEntity.class, taskId);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByPKs(Collection<Long> taskIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
      "SELECT task FROM HostRoleCommandEntity task WHERE task.taskId IN ?1 " +
        "ORDER BY task.taskId",
      HostRoleCommandEntity.class);

    List<HostRoleCommandEntity> result = new ArrayList<>();
    SQLOperations.batch(taskIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      result.addAll(daoUtils.selectList(query, chunk));
      return 0;
    });

    return Lists.newArrayList(result);
  }

  /**
   * Retrieves minimal host role command columns which are required to calculate stare state.
   * @param taskIds collection of host role commands to process.
   * @return minimized host role command entities.
   */
  @RequiresSession
  public List<HostRoleCommandEntity> findStatusRolesByPKs(Collection<Long> taskIds) {
    TypedQuery<Object[]> query = entityManagerProvider.get().createQuery(
      "SELECT task.taskId, task.status, task.role FROM HostRoleCommandEntity task WHERE task.taskId IN ?1 " +
        "ORDER BY task.taskId",
        Object[].class);

    List<HostRoleCommandEntity> result = new ArrayList<>();
    SQLOperations.batch(taskIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      List<Object[]> queryResult = daoUtils.selectList(query, chunk);
      result.addAll(queryResult.stream().map(
          o -> {
            HostRoleCommandEntity e = new HostRoleCommandEntity();
            e.setTaskId((Long) o[0]);
            e.setStatus(HostRoleStatus.valueOf(o[1].toString()));
            e.setRole(Role.valueOf(o[2].toString()));
            return e;
          }).collect(Collectors.toList()));

      return 0;
    });

    return Lists.newArrayList(result);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByHostId(Long hostId) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByHostId",
        HostRoleCommandEntity.class);

    query.setParameter("hostId", hostId);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequestIds(Collection<Long> requestIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class);

    List<HostRoleCommandEntity> result = new ArrayList<>();
    SQLOperations.batch(requestIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      result.addAll(daoUtils.selectList(query, chunk));
      return 0;
    });

    return Lists.newArrayList(result);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequestIdAndStatuses(Long requestId, Collection<HostRoleStatus> statuses) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByRequestIdAndStatuses", HostRoleCommandEntity.class);
    query.setParameter("requestId", requestId);
    query.setParameter("statuses", statuses);
    List<HostRoleCommandEntity> results = query.getResultList();
    return results;
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequestIds(Collection<Long> requestIds) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", Long.class);

    List<Long> result = new ArrayList<>();
    SQLOperations.batch(requestIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      result.addAll(daoUtils.selectList(query, chunk));
      return 0;
    });

    return Lists.newArrayList(result);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    if (CollectionUtils.isEmpty(requestIds) || CollectionUtils.isEmpty(taskIds)) {
      return Collections.<HostRoleCommandEntity>emptyList();
    }

    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class
    );

    return runQueryForVastRequestsAndTaskIds(query, requestIds, taskIds);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    if (CollectionUtils.isEmpty(requestIds) || CollectionUtils.isEmpty(taskIds)) {
      return Collections.<Long>emptyList();
    }

    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", Long.class
    );

    return runQueryForVastRequestsAndTaskIds(query, requestIds, taskIds);
  }

  private <T> List<T> runQueryForVastRequestsAndTaskIds(TypedQuery<T> query, Collection<Long> requestIds, Collection<Long> taskIds) {
    final int batchSize = SQLConstants.IN_ARGUMENT_MAX_SIZE;
    final List<T> result = new ArrayList<>();
    SQLOperations.batch(taskIds, batchSize, (taskChunk, currentTaskBatch, totalTaskBatches, totalTaskSize) -> {
      SQLOperations.batch(requestIds, batchSize, (requestChunk, currentRequestBatch, totalRequestBatches, totalRequestSize) -> {
        result.addAll(daoUtils.selectList(query, requestChunk, taskChunk));
        return 0;
      });
      return 0;
    });

    return Lists.newArrayList(result);
  }

  @RequiresSession
  public List<Long> findTaskIdsByHostRoleAndStatus(String hostname, String role, HostRoleStatus status) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.hostEntity.hostName=?1 AND task.role=?2 AND task.status=?3 " +
            "ORDER BY task.taskId", Long.class
    );

    return daoUtils.selectList(query, hostname, role, status);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRoleAndStatus(String role, HostRoleStatus status) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.role=?1 AND task.status=?2 " +
            "ORDER BY task.taskId", Long.class);

    return daoUtils.selectList(query, role, status);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findSortedCommandsByRequestIdAndCustomCommandName(Long requestId, String customCommandName) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.requestId=?1 AND hostRoleCommand.customCommandName=?2 " +
        "ORDER BY hostRoleCommand.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, requestId, customCommandName);
  }


  @RequiresSession
  public List<HostRoleCommandEntity> findSortedCommandsByStageAndHost(StageEntity stageEntity, HostEntity hostEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 AND hostRoleCommand.hostEntity.hostName=?2 " +
        "ORDER BY hostRoleCommand.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, stageEntity, hostEntity.getHostName());
  }

  @RequiresSession
  public Map<String, List<HostRoleCommandEntity>> findSortedCommandsByStage(StageEntity stageEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 " +
        "ORDER BY hostRoleCommand.hostEntity.hostName, hostRoleCommand.taskId", HostRoleCommandEntity.class);
    List<HostRoleCommandEntity> commandEntities = daoUtils.selectList(query, stageEntity);

    Map<String, List<HostRoleCommandEntity>> hostCommands = new HashMap<>();

    for (HostRoleCommandEntity commandEntity : commandEntities) {
      if (!hostCommands.containsKey(commandEntity.getHostName())) {
        hostCommands.put(commandEntity.getHostName(), new ArrayList<>());
      }

      hostCommands.get(commandEntity.getHostName()).add(commandEntity);
    }

    return hostCommands;
  }

  @RequiresSession
  public List<Long> findTaskIdsByStage(long requestId, long stageId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand.taskId " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage.requestId=?1 " +
        "AND hostRoleCommand.stage.stageId=?2 "+
        "ORDER BY hostRoleCommand.taskId", Long.class);

    return daoUtils.selectList(query, requestId, stageId);
  }

  @RequiresSession
  public Map<Long, Integer> getHostIdToCountOfCommandsWithStatus(Collection<HostRoleStatus> statuses) {
    Map<Long, Integer> hostIdToCount = new HashMap<>();

    String queryName = "SELECT command.hostId FROM HostRoleCommandEntity command WHERE command.status IN :statuses";
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(queryName, Long.class);
    query.setParameter("statuses", statuses);
    List<Long> results = query.getResultList();

    for (Long hostId : results) {
      if (hostIdToCount.containsKey(hostId)) {
        hostIdToCount.put(hostId, hostIdToCount.get(hostId) + 1);
      } else {
        hostIdToCount.put(hostId, 1);
      }
    }

    return hostIdToCount;
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByHostRole(String hostName, long requestId, long stageId, String role) {

    String queryName = (null == hostName) ? "HostRoleCommandEntity.findByHostRoleNullHost" :
        "HostRoleCommandEntity.findByHostRole";

    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        queryName, HostRoleCommandEntity.class);

    if (null != hostName) {
      query.setParameter("hostName", hostName);
    }
    query.setParameter("requestId", requestId);
    query.setParameter("stageId", stageId);
    query.setParameter("role", role);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequest(long requestId) {
    return findByRequest(requestId, false);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequest(long requestId, boolean refreshHint) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
      "HostRoleCommandEntity.findByRequestId",
      HostRoleCommandEntity.class);
    if (refreshHint) {
      query.setHint(QueryHints.REFRESH, HintValues.TRUE);
    }
    query.setParameter("requestId", requestId);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequest(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT command.taskId " +
      "FROM HostRoleCommandEntity command " +
      "WHERE command.requestId=?1 ORDER BY command.taskId", Long.class);
    return daoUtils.selectList(query, requestId);
  }

  /**
   * Gets the commands in a particular status.
   *
   * @param statuses
   *          the statuses to include (not {@code null}).
   * @return the commands in the given set of statuses.
   */
  @RequiresSession
  public List<HostRoleCommandEntity> findByStatus(
      Collection<HostRoleStatus> statuses) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByCommandStatuses",
        HostRoleCommandEntity.class);

    query.setParameter("statuses", statuses);
    return daoUtils.selectList(query);
  }

  /**
   * Gets the number of commands in a particular status.
   *
   * @param statuses
   *          the statuses to include (not {@code null}).
   * @return the count of commands in the given set of statuses.
   */
  @RequiresSession
  public Number getCountByStatus(Collection<HostRoleStatus> statuses) {
    TypedQuery<Number> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findCountByCommandStatuses", Number.class);

    query.setParameter("statuses", statuses);
    return daoUtils.selectSingle(query);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostRoleCommandEntity.class);
  }

  /**
   * Finds all the {@link HostRoleCommandEntity}s for the given request that are
   * between the specified stage IDs and have the specified status.
   *
   * @param requestId
   *          the request ID
   * @param status
   *          the command status to query for (not {@code null}).
   * @param minStageId
   *          the lowest stage ID to requests tasks for.
   * @param maxStageId
   *          the highest stage ID to request tasks for.
   * @return the tasks that satisfy the specified parameters.
   */
  @RequiresSession
  public List<HostRoleCommandEntity> findByStatusBetweenStages(long requestId,
      HostRoleStatus status, long minStageId, long maxStageId) {

    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByStatusBetweenStages", HostRoleCommandEntity.class);

    query.setParameter("requestId", requestId);
    query.setParameter("status", status);
    query.setParameter("minStageId", minStageId);
    query.setParameter("maxStageId", maxStageId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets requests that have tasks in any of the specified statuses.
   *
   * @param statuses
   * @param maxResults
   * @param ascOrder
   * @return
   */
  @RequiresSession
  public List<Long> getRequestsByTaskStatus(
      Collection<HostRoleStatus> statuses, int maxResults, boolean ascOrder) {
    String sortOrder = "ASC";
    if (!ascOrder) {
      sortOrder = "DESC";
    }

    String sql = MessageFormat.format(REQUESTS_BY_TASK_STATUS_SQL, sortOrder);
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setParameter("taskStatuses", statuses);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<Long> getCompletedRequests(int maxResults, boolean ascOrder) {
    String sortOrder = "ASC";
    if (!ascOrder) {
      sortOrder = "DESC";
    }

    String sql = MessageFormat.format(COMPLETED_REQUESTS_SQL, sortOrder);
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setParameter("notCompletedStatuses",
        HostRoleStatus.NOT_COMPLETED_STATUSES);

    return daoUtils.selectList(query);
  }

  /**
   * NB: You cannot rely on return value if batch write is enabled
   */
  @Transactional
  public int updateStatusByRequestId(long requestId, HostRoleStatus target, Collection<HostRoleStatus> sources) {
    TypedQuery<HostRoleCommandEntity> selectQuery = entityManagerProvider.get().createQuery("SELECT command " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.requestId=?1 AND command.status IN ?2", HostRoleCommandEntity.class);

    List<HostRoleCommandEntity> commandEntities = daoUtils.selectList(selectQuery, requestId, sources);

    for (HostRoleCommandEntity entity : commandEntities) {
      entity.setStatus(target);
      merge(entity);
    }

    return commandEntities.size();
  }

  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public void create(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(entity);

    invalidateHostRoleCommandStatusSummaryCache(entity);
  }

  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public HostRoleCommandEntity merge(HostRoleCommandEntity entity) {
    entity = mergeWithoutPublishEvent(entity);
    publishTaskUpdateEvent(Collections.singletonList(hostRoleCommandFactory.createExisting(entity)));
    return entity;
  }

  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public HostRoleCommandEntity mergeWithoutPublishEvent(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entity = entityManager.merge(entity);
    invalidateHostRoleCommandStatusSummaryCache(entity);
    return entity;
  }

  @Transactional
  public void removeByHostId(Long hostId) {
    Collection<HostRoleCommandEntity> commands = findByHostId(hostId);
    for (HostRoleCommandEntity cmd : commands) {
      remove(cmd);
    }
  }

  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public List<HostRoleCommandEntity> mergeAll(Collection<HostRoleCommandEntity> entities) {
    Set<Long> requestsToInvalidate = new LinkedHashSet<>();
    List<HostRoleCommandEntity> managedList = new ArrayList<>(entities.size());
    for (HostRoleCommandEntity entity : entities) {
      EntityManager entityManager = entityManagerProvider.get();
      entity = entityManager.merge(entity);
      managedList.add(entity);

      Long requestId = entity.getRequestId();
      if (requestId == null) {
        StageEntity stageEntity = entity.getStage();
        if (stageEntity != null) {
          requestId = stageEntity.getRequestId();
        }
      }

      requestsToInvalidate.add(requestId);
    }

    invalidateHostRoleCommandStatusSummaryCache(requestsToInvalidate);
    publishTaskUpdateEvent(getHostRoleCommands(entities));
    return managedList;
  }

  /**
   *
   * @param entities
   */
  public List<HostRoleCommand> getHostRoleCommands(Collection<HostRoleCommandEntity> entities) {
    Function<HostRoleCommandEntity, HostRoleCommand> transform = new Function<HostRoleCommandEntity, HostRoleCommand> () {
      @Override
      public HostRoleCommand apply(HostRoleCommandEntity entity) {
        return hostRoleCommandFactory.createExisting(entity);
      }
    };
    return FluentIterable.from(entities)
        .transform(transform)
        .toList();

  }

  /**
   *
   * @param hostRoleCommands
   */
  public void publishTaskUpdateEvent(List<HostRoleCommand> hostRoleCommands) {
    if (!hostRoleCommands.isEmpty()) {
      TaskUpdateEvent taskUpdateEvent = new TaskUpdateEvent(hostRoleCommands);
      taskEventPublisher.publish(taskUpdateEvent);
    }
  }

  /**
   *
   * @param hostRoleCommands
   */
  public void publishTaskCreateEvent(List<HostRoleCommand> hostRoleCommands) {
    if (!hostRoleCommands.isEmpty()) {
      TaskCreateEvent taskCreateEvent = new TaskCreateEvent(hostRoleCommands);
      taskEventPublisher.publish(taskCreateEvent);
    }
  }



  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public void remove(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.remove(entity);
    invalidateHostRoleCommandStatusSummaryCache(entity);
  }

  @Transactional
  public void removeByPK(int taskId) {
    remove(findByPK(taskId));
  }


  /**
   * Finds the counts of tasks for a request and groups them by stage id. If
   * caching is enabled, this will first consult the cache. Cache misses will
   * then defer to loading the data from the database and then caching the
   * result.
   *
   * @param requestId
   *          the request id
   * @return the map of stage-to-summary objects
   */
  @RequiresSession
  public Map<Long, HostRoleCommandStatusSummaryDTO> findAggregateCounts(Long requestId) {
    if (!hostRoleCommandStatusSummaryCacheEnabled) {
      return loadAggregateCounts(requestId);
    }

    Map<Long, HostRoleCommandStatusSummaryDTO> map = hrcStatusSummaryCache.getIfPresent(requestId);
    if (null != map) {
      return map;
    }

    // ensure that we wait for any running transactions working on this cache to
    // complete
    ReadWriteLock lock = transactionLocks.getLock(LockArea.HRC_STATUS_CACHE);
    lock.readLock().lock();

    try {
      map = loadAggregateCounts(requestId);
      hrcStatusSummaryCache.put(requestId, map);

      return map;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * During Rolling and Express Upgrade, want to bubble up the error of the most recent failure, i.e., greatest
   * task id, assuming that there are no other completed tasks after it.
   * @param requestId upgrade request id
   * @return Most recent task failure during stack upgrade, or null if one doesn't exist.
   */
  @RequiresSession
  public HostRoleCommandEntity findMostRecentFailure(Long requestId) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findTasksByStatusesOrderByIdDesc", HostRoleCommandEntity.class);

    query.setParameter("requestId", requestId);
    query.setParameter("statuses", HostRoleStatus.STACK_UPGRADE_FAILED_STATUSES);
    List<HostRoleCommandEntity> results = query.getResultList();

    if (!results.isEmpty()) {
      HostRoleCommandEntity candidate = results.get(0);

      // Ensure that there are no other completed tasks in a future stage to avoid returning an old error.
      // During Express Upgrade, we can run multiple commands in the same stage, so it's possible to have
      // COMPLETED tasks in the failed task's stage.
      // During Rolling Upgrade, we run exactly one command per stage.
      TypedQuery<Number> numberAlreadyRanTasksInFutureStage = entityManagerProvider.get().createNamedQuery(
          "HostRoleCommandEntity.findNumTasksAlreadyRanInStage", Number.class);

      numberAlreadyRanTasksInFutureStage.setParameter("requestId", requestId);
      numberAlreadyRanTasksInFutureStage.setParameter("taskId", candidate.getTaskId());
      numberAlreadyRanTasksInFutureStage.setParameter("stageId", candidate.getStageId());
      numberAlreadyRanTasksInFutureStage.setParameter("statuses", HostRoleStatus.SCHEDULED_STATES);

      Number result = daoUtils.selectSingle(numberAlreadyRanTasksInFutureStage);
      if (result.longValue() == 0L) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Updates the {@link HostRoleCommandEntity#isFailureAutoSkipped()} flag for
   * all commands for the given request.
   * <p/>
   * This will update each entity to ensure that the cache is maintained in a
   * correct state. A batch update doesn't always reflect in JPA-managed
   * entities.
   * <p/>
   * Stages which do not support automatically skipped commands will be updated
   * with a value of {@code false}.
   *
   * @param requestId
   *          the request ID of the commands to update
   * @param skipOnFailure
   *          {@code true} to automatically skip failures, {@code false}
   *          otherwise.
   * @param skipOnServiceCheckFailure
   *          {@code true} to skip service check failures
   *
   * @see StageEntity#isAutoSkipOnFailureSupported()
   */
  @Transactional
  public void updateAutomaticSkipOnFailure(long requestId,
      boolean skipOnFailure, boolean skipOnServiceCheckFailure) {

    List<HostRoleCommandEntity> tasks = findByRequest(requestId);
    for (HostRoleCommandEntity task : tasks) {
      // if the stage does not support automatically skipping its commands, then
      // do nothing
      StageEntity stage = task.getStage();

      boolean isStageSkippable = stage.isSkippable();
      boolean isAutoSkipSupportedOnStage = stage.isAutoSkipOnFailureSupported();

      // if the stage is not skippable or it does not support auto skip
      if (!isStageSkippable || !isAutoSkipSupportedOnStage) {
        task.setAutoSkipOnFailure(false);
      } else {
        if (task.getRoleCommand() == RoleCommand.SERVICE_CHECK) {
          task.setAutoSkipOnFailure(skipOnServiceCheckFailure);
        } else {
          task.setAutoSkipOnFailure(skipOnFailure);
        }
      }

      // save changes
      merge(task);
    }
  }

  /**
   * Finds all {@link HostRoleCommandEntity} that match the provided predicate.
   * This method will make JPA do the heavy lifting of providing a slice of the
   * result set.
   *
   * @param request
   * @return
   */
  @RequiresSession
  public List<HostRoleCommandEntity> findAll(Request request, Predicate predicate) {
    EntityManager entityManager = entityManagerProvider.get();

    // convert the Shpurdp predicate into a JPA predicate
    HostRoleCommandPredicateVisitor visitor = new HostRoleCommandPredicateVisitor();
    PredicateHelper.visit(predicate, visitor);

    CriteriaQuery<HostRoleCommandEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    SortRequest sortRequest = request.getSortRequest();
    if (null != sortRequest) {
      JpaSortBuilder<HostRoleCommandEntity> sortBuilder = new JpaSortBuilder<>();
      List<Order> sortOrders = sortBuilder.buildSortOrders(sortRequest, visitor);
      query.orderBy(sortOrders);
    }

    TypedQuery<HostRoleCommandEntity> typedQuery = entityManager.createQuery(query);

    // pagination
    PageRequest pagination = request.getPageRequest();
    if (null != pagination) {
      typedQuery.setFirstResult(pagination.getOffset());
      typedQuery.setMaxResults(pagination.getPageSize());
    }

    return daoUtils.selectList(typedQuery);
  }

  /**
   * Gets a lists of hosts with commands in progress given a range of requests.
   * The range of requests should include all requests with at least 1 stage in
   * progress.
   *
   * @return the list of hosts with commands in progress.
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  @RequiresSession
  public List<String> getHostsWithPendingTasks(long iLowestRequestIdInProgress,
      long iHighestRequestIdInProgress) {
    TypedQuery<String> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findHostsByCommandStatus", String.class);

    query.setParameter("iLowestRequestIdInProgress", iLowestRequestIdInProgress);
    query.setParameter("iHighestRequestIdInProgress", iHighestRequestIdInProgress);
    query.setParameter("statuses", HostRoleStatus.IN_PROGRESS_STATUSES);
    return daoUtils.selectList(query);
  }

  /**
   * Gets a lists of hosts with commands in progress which occurr before the
   * specified request ID. This will only return commands which are not
   * {@link AgentCommandType#BACKGROUND_EXECUTION_COMMAND} as thsee commands do
   * not block future requests.
   *
   * @param lowerRequestIdInclusive
   *          the lowest request ID to consider (inclusive) when getting any
   *          blocking hosts.
   * @param requestId
   *          the request ID to calculate any blocking hosts for (essentially,
   *          the upper limit exclusive)
   * @return the list of hosts from older running requests which will block
   *         those same hosts in the specified request ID.
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  @RequiresSession
  public List<String> getBlockingHostsForRequest(long lowerRequestIdInclusive,
      long requestId) {
    TypedQuery<String> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.getBlockingHostsForRequest", String.class);

    query.setParameter("lowerRequestIdInclusive", lowerRequestIdInclusive);
    query.setParameter("upperRequestIdExclusive", requestId);
    query.setParameter("statuses", HostRoleStatus.IN_PROGRESS_STATUSES);
    return daoUtils.selectList(query);
  }

  /**
   * Gets the most recently run service check grouped by the command's role
   * (which is the only way to identify the service it was for!?)
   *
   * @param clusterId
   *          the ID of the cluster to get the service checks for.
   */
  @RequiresSession
  public List<LastServiceCheckDTO> getLatestServiceChecksByRole(long clusterId) {
    TypedQuery<LastServiceCheckDTO> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findLatestServiceChecksByRole", LastServiceCheckDTO.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("roleCommand", RoleCommand.SERVICE_CHECK);

    return daoUtils.selectList(query);
  }

  /**
   * The {@link HostRoleCommandPredicateVisitor} is used to convert an Shpurdp
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class HostRoleCommandPredicateVisitor
      extends JpaPredicateVisitor<HostRoleCommandEntity> {

    /**
     * Constructor.
     *
     */
    public HostRoleCommandPredicateVisitor() {
      super(entityManagerProvider.get(), HostRoleCommandEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<HostRoleCommandEntity> getEntityClass() {
      return HostRoleCommandEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(String propertyId) {
      return HostRoleCommandEntity_.getPredicateMapping().get(propertyId);
    }
  }

  public Set<Long> findTaskIdsByRequestStageIds(List<RequestDAO.StageEntityPK> requestStageIds) {
    EntityManager entityManager = entityManagerProvider.get();
    List<Long> taskIds = new ArrayList<>();
    for (RequestDAO.StageEntityPK requestIds : requestStageIds) {
      TypedQuery<Long> hostRoleCommandQuery =
              entityManager.createNamedQuery("HostRoleCommandEntity.findTaskIdsByRequestStageIds", Long.class);

      hostRoleCommandQuery.setParameter("requestId", requestIds.getRequestId());
      hostRoleCommandQuery.setParameter("stageId", requestIds.getStageId());

      taskIds.addAll(daoUtils.selectList(hostRoleCommandQuery));
    }

    return Sets.newHashSet(taskIds);
  }

  /**
   * A simple DTO for storing the most recent service check time for a given
   * {@link Role}.
   */
  public static class LastServiceCheckDTO {

    /**
     * The role.
     */
    public final String role;

    /**
     * The time that the service check ended.
     */
    public final long endTime;

    /**
     * Constructor.
     *
     * @param role
     * @param endTime
     */
    public LastServiceCheckDTO(String role, long endTime) {
      this.role = role;
      this.endTime = endTime;
    }
  }
}
