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

import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.events.RequestUpdateEvent;
import org.apache.shpurdp.server.events.UpgradeUpdateEvent;
import org.apache.shpurdp.server.events.publishers.STOMPUpdatePublisher;
import org.apache.shpurdp.server.orm.dao.HostRoleCommandDAO;
import org.apache.shpurdp.server.orm.dao.RequestDAO;
import org.apache.shpurdp.server.orm.dao.UpgradeDAO;
import org.apache.shpurdp.server.orm.entities.UpgradeEntity;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class UpgradeUpdateListener {

  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private UpgradeDAO upgradeDAO;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private RequestDAO requestDAO;

  @Inject
  public UpgradeUpdateListener(STOMPUpdatePublisher STOMPUpdatePublisher) {
    STOMPUpdatePublisher.registerAPI(this);

    this.STOMPUpdatePublisher = STOMPUpdatePublisher;
  }

  @Subscribe
  public void onRequestUpdate(RequestUpdateEvent requestUpdateEvent) {
    UpgradeEntity upgradeEntity = upgradeDAO.findUpgradeByRequestId(requestUpdateEvent.getRequestId());
    if (upgradeEntity != null) {
      STOMPUpdatePublisher.publish(UpgradeUpdateEvent.formUpdateEvent(hostRoleCommandDAO, requestDAO, upgradeEntity));
    }
  }
}
