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
package org.apache.shpurdp.server.events;

/**
 * The {@link HostEvent} represents all events in Shpurdp that occur directly on
 * a host. This excludes events on the host's services and components.
 */
public abstract class HostEvent extends ShpurdpEvent {

  /**
   * The host's name.
   */
  protected final String m_hostName;

  /**
   * Constructor.
   *
   * @param eventType
   */
  public HostEvent(ShpurdpEventType eventType, String hostName) {
    super(eventType);
    m_hostName = hostName;
  }

  /**
   * Gets the host's name that the event belongs to.
   *
   * @return the hostName
   */
  public String getHostName() {
    return m_hostName;
  }
}
