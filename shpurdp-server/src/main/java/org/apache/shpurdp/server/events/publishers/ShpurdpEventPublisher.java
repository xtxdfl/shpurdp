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
package org.apache.shpurdp.server.events.publishers;

import java.util.concurrent.Executors;

import org.apache.shpurdp.server.events.ShpurdpEvent;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * The {@link ShpurdpEventPublisher} is used to publish instances of
 * {@link ShpurdpEvent} to any {@link Subscribe} methods interested. It uses a
 * single-threaded {@link AsyncEventBus}.
 */
@Singleton
public class ShpurdpEventPublisher {

  /**
   * A single threaded event bus for processing Shpurdp events in serial.
   */
  private final EventBus m_eventBus;

  /**
   * Constructor.
   */
  public ShpurdpEventPublisher() {
    m_eventBus = new AsyncEventBus("shpurdp-event-bus",
        Executors.newSingleThreadExecutor());
  }

  /**
   * Publishes the specified event to all registered listeners that
   * {@link Subscribe} to any of the {@link ShpurdpEvent} instances.
   *
   * @param event
   */
  public void publish(ShpurdpEvent event) {
    m_eventBus.post(event);
  }

  /**
   * Register a listener to receive events. The listener should use the
   * {@link Subscribe} annotation.
   *
   * @param object
   *          the listener to receive events.
   */
  public void register(Object object) {
    m_eventBus.register(object);
  }
}
