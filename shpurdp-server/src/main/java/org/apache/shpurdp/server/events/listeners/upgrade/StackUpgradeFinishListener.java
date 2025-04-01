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
package org.apache.shpurdp.server.events.listeners.upgrade;


import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.events.StackUpgradeFinishEvent;
import org.apache.shpurdp.server.events.publishers.VersionEventPublisher;
import org.apache.shpurdp.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.shpurdp.server.metadata.RoleCommandOrderProvider;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link StackUpgradeFinishListener} class handles  updating component info
 * after stack upgrade or downgrade finish
 */
@Singleton
@EagerSingleton
public class StackUpgradeFinishListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackUpgradeFinishListener.class);

  @Inject
  Provider<ShpurdpMetaInfo> shpurdpMetaInfo;

  @Inject
  Provider<RoleCommandOrderProvider> roleCommandOrderProvider;

  /**
   * Constructor.
   *
   * @param eventPublisher  the publisher
   */
  @Inject
  public StackUpgradeFinishListener(VersionEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onShpurdpEvent(StackUpgradeFinishEvent event) {
    LOG.debug("Received event {}", event);

    Cluster cluster = event.getCluster();

    for (Service service : cluster.getServices().values()) {
      try {
        //update service info due to new stack
        service.updateServiceInfo();
        //update component info due to new stack
        for (ServiceComponent sc : service.getServiceComponents().values()) {
          sc.updateComponentInfo();
        }
      } catch (ShpurdpException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Caught ShpurdpException when update component info", e);
          }
        }
      }

      // Clear the RoleCommandOrder cache on upgrade
      if (roleCommandOrderProvider.get() instanceof CachedRoleCommandOrderProvider) {
        LOG.info("Clearing RCO cache");
        CachedRoleCommandOrderProvider cachedRcoProvider = (CachedRoleCommandOrderProvider) roleCommandOrderProvider.get();
        cachedRcoProvider.clearRoleCommandOrderCache();
      }

      try {
        shpurdpMetaInfo.get().reconcileAlertDefinitions(cluster, true);
      } catch (ShpurdpException e){
        LOG.error("Caught ShpurdpException when update alert definitions", e);
      }

    }

  }
