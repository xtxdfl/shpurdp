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
package org.apache.shpurdp.server.events.publishers;

import java.util.ArrayList;
import java.util.List;

import org.apache.shpurdp.server.ShpurdpRuntimeException;
import org.apache.shpurdp.server.events.DefaultMessageEmitter;
import org.apache.shpurdp.server.events.STOMPEvent;
import org.apache.shpurdp.server.utils.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings({"UnstableApiUsage", "rawtypes"})
public class STOMPUpdatePublisher {
  private static final Logger LOG = LoggerFactory.getLogger(STOMPUpdatePublisher.class);

  private final EventBus agentEventBus;
  private final EventBus apiEventBus;

  private final List<BufferedUpdateEventPublisher> publishers = new ArrayList<>();


  public STOMPUpdatePublisher() {
    agentEventBus = new AsyncEventBus("agent-update-bus", ThreadPools.getSingleThreadedExecutor("stomp-agent-bus"));
    apiEventBus = new AsyncEventBus("api-update-bus", ThreadPools.getSingleThreadedExecutor("stomp-api-bus"));
  }

  public void registerPublisher(BufferedUpdateEventPublisher publisher) {
    if (publishers.contains(publisher)) {
      LOG.error("Publisher for type {} is already in use", publisher.getType());
    } else {
      publishers.add(publisher);
    }
  }

  public void publish(STOMPEvent event) {
    if (DefaultMessageEmitter.DEFAULT_AGENT_EVENT_TYPES.contains(event.getType())) {
      publishAgent(event);
    } else if (DefaultMessageEmitter.DEFAULT_API_EVENT_TYPES.contains(event.getType())) {
      publishAPI(event);
    } else {
      // TODO need better solution
      throw new ShpurdpRuntimeException("Event with type {" + event.getType() + "} can not be published.");
    }
  }

  @SuppressWarnings("unchecked")
  private void publishAPI(STOMPEvent event) {
    boolean published = false;
    for (BufferedUpdateEventPublisher publisher : publishers) {
      if (publisher.getType().equals(event.getType())) {
        publisher.publish(event, apiEventBus);
        published = true;
      }
    }
    if (!published) {
      apiEventBus.post(event);
    }
  }

  private void publishAgent(STOMPEvent event) {
    agentEventBus.post(event);
  }

  public void registerAgent(Object object) {
    agentEventBus.register(object);
  }

  public void registerAPI(Object object) {
    apiEventBus.register(object);
  }
}
