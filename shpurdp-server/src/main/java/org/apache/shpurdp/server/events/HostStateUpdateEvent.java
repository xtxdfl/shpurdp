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

package org.apache.shpurdp.server.events;

import org.apache.shpurdp.server.state.HostState;

public class HostStateUpdateEvent extends ShpurdpEvent {

  private String hostName;
  private HostState hostState;

  public HostStateUpdateEvent(String hostName, HostState hostState) {
    super(ShpurdpEventType.HOST_STATE_CHANGE);
    this.hostName = hostName;
    this.hostState = hostState;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public HostState getHostState() {
    return hostState;
  }

  public void setHostState(HostState hostState) {
    this.hostState = hostState;
  }
}
