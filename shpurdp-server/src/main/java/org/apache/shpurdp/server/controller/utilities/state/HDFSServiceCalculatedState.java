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

package org.apache.shpurdp.server.controller.utilities.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.ObjectNotFoundException;
import org.apache.shpurdp.server.StaticallyInject;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.controller.ServiceComponentHostRequest;
import org.apache.shpurdp.server.controller.ServiceComponentHostResponse;
import org.apache.shpurdp.server.stack.NameService;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.ComponentInfo;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculator of HDFS service state.
 */
@StaticallyInject
public final class HDFSServiceCalculatedState extends DefaultServiceCalculatedState
  implements ServiceCalculatedState {

  private static final Logger LOG = LoggerFactory.getLogger(HDFSServiceCalculatedState.class);

  @Override
  public State getState(String clusterName, String serviceName) {
    try {
      Cluster cluster = getCluster(clusterName);
      if (cluster != null && managementControllerProvider != null) {
        ShpurdpMetaInfo shpurdpMetaInfo = managementControllerProvider.get().getShpurdpMetaInfo();
        Service service = cluster.getService(serviceName);
        StackId stackId = service.getDesiredStackId();

        ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
          serviceName, null, null, null);

        Set<ServiceComponentHostResponse> hostComponentResponses =
          managementControllerProvider.get().getHostComponents(Collections.singleton(request), true);

        Set<String> startedOrDisabledNNHosts = new HashSet<>();

        int     nameNodeCount       = 0;
        int     nameNodeStartedOrDisabledCount = 0;
        boolean hasSecondary        = false;
        boolean hasJournal          = false;
        State   nonStartedState     = null;

        for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
          try {
            ComponentInfo componentInfo = shpurdpMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), hostComponentResponse.getServiceName(),
              hostComponentResponse.getComponentName());

            if (componentInfo.isMaster()) {
              String componentName = hostComponentResponse.getComponentName();
              boolean isNameNode = false;

              switch (componentName) {
                case "NAMENODE":
                  ++nameNodeCount;
                  isNameNode = true;
                  break;
                case "SECONDARY_NAMENODE":
                  hasSecondary = true;
                  break;
                case "JOURNALNODE":
                  hasJournal = true;
                  break;
              }

              State state = getHostComponentState(hostComponentResponse);

              switch (state) {
                case STARTED:
                case DISABLED:
                  if (isNameNode) {
                    ++nameNodeStartedOrDisabledCount;
                    startedOrDisabledNNHosts.add(hostComponentResponse.getHostname());
                  }
                  break;
                default:
                  nonStartedState = state;
              }
            }
          } catch (ObjectNotFoundException e) {
            // component doesn't exist, nothing to do
          }
        }

        boolean multipleNameServices = nameNodeCount > 2;
        int nameServiceWithStartedOrDisabledNNCount = 0;
        List<NameService> nameServices = new ArrayList<>();

        // count name services that has at least 1 namenode in started or disabled state
        if (multipleNameServices) {
          ConfigHelper configHelper = managementControllerProvider.get().getConfigHelper();
          nameServices = NameService.fromConfig(configHelper, cluster);

          for (NameService nameService : nameServices) {
            boolean hasStartedOrDisabledNN = false;
            for (NameService.NameNode nameNode : nameService.getNameNodes()) {
              if (startedOrDisabledNNHosts.contains(nameNode.getHost())) {
                hasStartedOrDisabledNN = true;
                break;
              }
            }
            if (hasStartedOrDisabledNN) {
              nameServiceWithStartedOrDisabledNNCount++;
            }
          }
        }

        // all started OR at least one active namenode for single namespace AND at least one namenode for each namespace for multiple namespaces
        if (nonStartedState == null ||  // all started
            ((nameNodeCount > 0 && !hasSecondary || hasJournal) &&
                nameNodeStartedOrDisabledCount > 0 &&
                (!multipleNameServices || nameServiceWithStartedOrDisabledNNCount == nameServices.size()))) {
          return State.STARTED;
        }
        return nonStartedState;
      }
    } catch (ShpurdpException e) {
      LOG.error("Can't determine service state.", e);
    }
    return State.UNKNOWN;
  }
}
