/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.controller.internal;

import java.util.Collections;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.HostRequest;
import org.apache.shpurdp.server.controller.HostResponse;
import org.apache.shpurdp.server.controller.ServiceComponentHostRequest;
import org.apache.shpurdp.server.controller.ServiceComponentHostResponse;
import org.apache.shpurdp.server.state.HostState;
import org.apache.shpurdp.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostStatusHelper {

  private static final Logger LOG =
    LoggerFactory.getLogger(HostStatusHelper.class);

  public static boolean isHostComponentLive(ShpurdpManagementController managementController,
                                             String clusterName, String hostName,
                                      String serviceName, String componentName) {
    if (clusterName == null) {
      return false;
    }

    ServiceComponentHostResponse componentHostResponse;

    try {
      ServiceComponentHostRequest componentRequest =
        new ServiceComponentHostRequest(clusterName, serviceName,
          componentName, hostName, null);

      Set<ServiceComponentHostResponse> hostComponents =
        managementController.getHostComponents(Collections.singleton(componentRequest));

      componentHostResponse = hostComponents.size() == 1 ? hostComponents.iterator().next() : null;
    } catch (ShpurdpException e) {
      LOG.debug("Error checking {} server host component state: ", componentName, e);
      return false;
    }

    //Cluster without SCH
    return componentHostResponse != null &&
      componentHostResponse.getLiveState().equals(State.STARTED.name());
  }

  public static boolean isHostLive(ShpurdpManagementController managementController, String clusterName, String hostName) {
    if (clusterName == null) {
      return false;
    }
    HostResponse hostResponse;

    try {
      HostRequest hostRequest = new HostRequest(hostName, clusterName);
      Set<HostResponse> hosts = HostResourceProvider.getHosts(managementController, hostRequest, null);

      hostResponse = hosts.size() == 1 ? hosts.iterator().next() : null;
    } catch (ShpurdpException e) {
      LOG.debug("Error while checking host live status: ", e);
      return false;
    }
    //Cluster without host
    return hostResponse != null &&
      !hostResponse.getHostState().equals(HostState.HEARTBEAT_LOST);
  }
}
