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
package org.apache.shpurdp.server.events.listeners.alerts;

import java.util.Collection;
import java.util.Collections;

import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.agent.AlertDefinitionCommand;
import org.apache.shpurdp.server.agent.HeartBeatResponse;
import org.apache.shpurdp.server.events.AlertHashInvalidationEvent;
import org.apache.shpurdp.server.events.ShpurdpEvent.ShpurdpEventType;
import org.apache.shpurdp.server.events.ClusterEvent;
import org.apache.shpurdp.server.events.ServiceComponentUninstalledEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.state.alert.AlertDefinitionHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertHashInvalidationListener} is used to respond to
 * {@link AlertHashInvalidationEvent} and {@link ClusterEvent} instances and
 * ensure that the {@link AlertDefinitionCommand}s are enqueued for the
 * {@link HeartBeatResponse}.
 * <p/>
 * <ul>
 * <li>{@link ClusterEvent} - invalidates all alerts across the cluster</li>
 * <li>{@link AlertHashInvalidationEvent} - invalidates a specific alert across
 * affected hosts</li>
 * </ul>
 */
@Singleton
@EagerSingleton
public class AlertHashInvalidationListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertHashInvalidationListener.class);

  /**
   * Invalidates hosts so that they can receive updated alert definition
   * commands.
   */
  @Inject
  private Provider<AlertDefinitionHash> m_alertDefinitionHash;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertHashInvalidationListener(ShpurdpEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles {@link AlertHashInvalidationEvent} by performing the following
   * tasks:
   * <ul>
   * <li>Enqueuing {@link AlertDefinitionCommand}</li>
   * </ul>
   *
   * @param event
   *          the event being handled.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onShpurdpEvent(AlertHashInvalidationEvent event) {
    LOG.debug("Received event {}", event);

    Collection<String> hosts = event.getHosts();
    long clusterId = event.getClusterId();

    // no hosts, nothing to do
    if (null == hosts || hosts.isEmpty()) {
      return;
    }

    AlertDefinitionHash hash = m_alertDefinitionHash.get();
    hash.enqueueAgentCommands(clusterId, hosts);
  }

  /**
   * Handles {@link AlertHashInvalidationEvent} by performing the following
   * tasks:
   * <ul>
   * <li>Alert has invalidation</li>
   * <li>Enqueuing {@link AlertDefinitionCommand}</li>
   * </ul>
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(ServiceComponentUninstalledEvent event) {
    LOG.debug("Received event {}", event);

    long clusterId = event.getClusterId();
    String hostName = event.getHostName();

    if (null == hostName) {
      return;
    }

    // invalidate hash and enqueue commands
    m_alertDefinitionHash.get().invalidate(hostName);
    m_alertDefinitionHash.get().enqueueAgentCommands(clusterId,
        Collections.singletonList(hostName));
  }

  /**
   * Handles {@link ClusterEvent} by performing the following tasks:
   * <ul>
   * <li>Invalidates all alerts across all hosts. This is because agents use the
   * cluster name as an identifier and if the cluster is renamed, then they need
   * to be rescheduled.</li>
   * </ul>
   *
   * @param event
   *          the event being handled.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onShpurdpEvent(ClusterEvent event) {
    LOG.debug("Received event {}", event);

    if( event.getType() != ShpurdpEventType.CLUSTER_RENAME ) {
      return;
    }


    AlertDefinitionHash hash = m_alertDefinitionHash.get();
    hash.invalidateAll();

    long clusterId = event.getClusterId();
    hash.enqueueAgentCommands(clusterId);
  }
}
