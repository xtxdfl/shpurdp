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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.shpurdp.server.events.AlertDefinitionDeleteEvent;
import org.apache.shpurdp.server.events.AlertGroupsUpdateEvent;
import org.apache.shpurdp.server.events.UpdateEventType;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.events.publishers.STOMPUpdatePublisher;
import org.apache.shpurdp.server.orm.dao.AlertDispatchDAO;
import org.apache.shpurdp.server.orm.entities.AlertDefinitionEntity;
import org.apache.shpurdp.server.orm.entities.AlertGroupEntity;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@EagerSingleton
public class AlertGroupsUpdateListener {

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AlertDispatchDAO alertDispatchDAO;

  @Inject
  public AlertGroupsUpdateListener(ShpurdpEventPublisher shpurdpEventPublisher) {
    shpurdpEventPublisher.register(this);
  }

  @Subscribe
  public void onAlertDefinitionDeleted(AlertDefinitionDeleteEvent event) {
    List<AlertGroupUpdate> alertGroupUpdates = new ArrayList<>();
    for (AlertGroupEntity alertGroupEntity : alertDispatchDAO.findAllGroups(event.getClusterId())) {
      boolean eventAffectsGroup = alertGroupEntity.getAlertDefinitions().stream()
        .map(AlertDefinitionEntity::getDefinitionId)
        .anyMatch(each -> Objects.equals(each, event.getDefinition().getDefinitionId()));
      if (eventAffectsGroup) {
        AlertGroupUpdate alertGroupUpdate = new AlertGroupUpdate(alertGroupEntity);
        alertGroupUpdate.getTargets().remove(event.getDefinition().getDefinitionId());
        alertGroupUpdates.add(alertGroupUpdate);
      }
    }
    STOMPUpdatePublisher.publish(new AlertGroupsUpdateEvent(alertGroupUpdates, UpdateEventType.UPDATE));
  }
}
