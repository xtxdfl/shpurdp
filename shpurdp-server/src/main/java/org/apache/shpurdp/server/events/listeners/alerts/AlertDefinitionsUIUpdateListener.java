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

import static org.apache.shpurdp.server.events.AlertDefinitionEventType.DELETE;
import static org.apache.shpurdp.server.events.AlertDefinitionEventType.UPDATE;

import java.util.Collections;
import java.util.Map;

import javax.inject.Provider;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.agent.stomp.AlertDefinitionsHolder;
import org.apache.shpurdp.server.agent.stomp.dto.AlertCluster;
import org.apache.shpurdp.server.events.AlertDefinitionChangedEvent;
import org.apache.shpurdp.server.events.AlertDefinitionDeleteEvent;
import org.apache.shpurdp.server.events.AlertDefinitionEventType;
import org.apache.shpurdp.server.events.AlertDefinitionRegistrationEvent;
import org.apache.shpurdp.server.events.AlertDefinitionsUIUpdateEvent;
import org.apache.shpurdp.server.events.ServiceComponentInstalledEvent;
import org.apache.shpurdp.server.events.ServiceComponentUninstalledEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.events.publishers.STOMPUpdatePublisher;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.alert.AlertDefinition;
import org.apache.shpurdp.server.state.alert.AlertDefinitionHash;
import org.apache.shpurdp.server.state.alert.AlertHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class AlertDefinitionsUIUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(AlertDefinitionsUIUpdateListener.class);

  @Inject
  private Provider<AlertDefinitionHash> helper;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AlertDefinitionsHolder alertDefinitionsHolder;

  @Inject
  private AlertHelper alertHelper;

  @Inject
  public AlertDefinitionsUIUpdateListener(ShpurdpEventPublisher shpurdpEventPublisher) {
    shpurdpEventPublisher.register(this);
  }

  public final static String SHPURDP_STALE_ALERT_NAME = "shpurdp_server_stale_alerts";

  @Subscribe
  public void onAlertDefinitionRegistered(AlertDefinitionRegistrationEvent event) throws ShpurdpException {
    handleSingleDefinitionChange(UPDATE, event.getDefinition());
  }

  @Subscribe
  public void onAlertDefinitionChanged(AlertDefinitionChangedEvent event) throws ShpurdpException {
    handleSingleDefinitionChange(UPDATE, event.getDefinition());
  }

  @Subscribe
  public void onAlertDefinitionDeleted(AlertDefinitionDeleteEvent event) throws ShpurdpException {
    handleSingleDefinitionChange(DELETE, event.getDefinition());
  }

  @Subscribe
  public void onServiceComponentInstalled(ServiceComponentInstalledEvent event) throws ShpurdpException {
    String hostName = event.getHostName();
    String serviceName = event.getServiceName();
    String componentName = event.getComponentName();

    Map<Long, AlertDefinition> definitions = helper.get().findByServiceComponent(event.getClusterId(), serviceName, componentName);

    if (event.isMasterComponent()) {
      try {
        Cluster cluster = clusters.get().getClusterById(event.getClusterId());
        if (cluster.getService(serviceName).getServiceComponents().get(componentName).getServiceComponentHosts().containsKey(hostName)) {
          definitions.putAll(helper.get().findByServiceMaster(event.getClusterId(), serviceName));
        }
      } catch (ShpurdpException e) {
        String msg = String.format("Failed to get alert definitions for master component %s/%s", serviceName, componentName);
        LOG.warn(msg, e);
      }
    }
    if (!definitions.isEmpty()) {
      alertDefinitionsHolder.provideAlertDefinitionAgentUpdateEvent(UPDATE, event.getClusterId(), definitions, hostName);
      Map<Long, AlertCluster> map = Collections.singletonMap(event.getClusterId(), new AlertCluster(definitions, hostName));
      STOMPUpdatePublisher.publish(new AlertDefinitionsUIUpdateEvent(UPDATE, map));
    }
  }

  @Subscribe
  public void onServiceComponentUninstalled(ServiceComponentUninstalledEvent event) throws ShpurdpException {
    String hostName = event.getHostName();
    Map<Long, AlertDefinition> definitions = helper.get().findByServiceComponent(event.getClusterId(), event.getServiceName(), event.getComponentName());
    if (event.isMasterComponent()) {
      definitions.putAll(helper.get().findByServiceMaster(event.getClusterId(), event.getServiceName()));
    }
    if (!definitions.isEmpty()) {
      alertDefinitionsHolder.provideAlertDefinitionAgentUpdateEvent(DELETE, event.getClusterId(), definitions, hostName);
      Map<Long, AlertCluster> map = Collections.singletonMap(event.getClusterId(), new AlertCluster(definitions, hostName));
      STOMPUpdatePublisher.publish(new AlertDefinitionsUIUpdateEvent(DELETE, map));
    }
  }

  private void handleSingleDefinitionChange(AlertDefinitionEventType eventType, AlertDefinition alertDefinition) throws ShpurdpException {
    LOG.info("{} alert definition '{}'", eventType, alertDefinition);
    Cluster cluster = clusters.get().getCluster(alertDefinition.getClusterId());
    helper.get().invalidateHosts(alertDefinition); // do we need to invalidate, what's the purpose of this?
    for (String hostName : alertDefinition.matchingHosts(clusters.get())) {
      alertDefinitionsHolder.provideAlertDefinitionAgentUpdateEvent(eventType, alertDefinition.getClusterId(),
          Collections.singletonMap(alertDefinition.getDefinitionId(), alertDefinition), hostName);
    }
    if (alertDefinition.getName().equals(SHPURDP_STALE_ALERT_NAME)) {
      for (Host host : cluster.getHosts()) {
        alertDefinitionsHolder.provideStaleAlertDefinitionUpdateEvent(eventType, alertDefinition.getClusterId(),
            alertHelper.getWaitFactorMultiplier(alertDefinition), host.getHostName());
      }
    }
    Map<Long, AlertCluster> update = Collections.singletonMap(alertDefinition.getClusterId(), new AlertCluster(alertDefinition, null));
    AlertDefinitionsUIUpdateEvent event = new AlertDefinitionsUIUpdateEvent(eventType, update);
    STOMPUpdatePublisher.publish(event);
  }
}
