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

import java.util.Collections;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.ObjectNotFoundException;
import org.apache.shpurdp.server.StaticallyInject;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.controller.ServiceComponentHostRequest;
import org.apache.shpurdp.server.controller.ServiceComponentHostResponse;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.ComponentInfo;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculator of HIVE service state.
 */
@StaticallyInject
public final class HiveServiceCalculatedState extends DefaultServiceCalculatedState
  implements ServiceCalculatedState {

  private static final Logger LOG = LoggerFactory.getLogger(HiveServiceCalculatedState.class);

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

        int activeHiveMetastoreComponentCount = 0;
        State nonStartedState = null;
        boolean embeddedMysqlComponentExists = false;
        boolean hiveServerComponentStarted = false;
        boolean webHcatComponentStarted = false;
        boolean mysqlComponentStarted = false;

        for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
          try {
            ComponentInfo componentInfo = shpurdpMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), hostComponentResponse.getServiceName(),
              hostComponentResponse.getComponentName());

            if (componentInfo.isMaster()) {
              State state = getHostComponentState(hostComponentResponse);

              String componentName = hostComponentResponse.getComponentName();
              if (componentName.equals("MYSQL_SERVER")) {
                embeddedMysqlComponentExists = true;
              }

              switch (state) {
                case STARTED:
                case DISABLED:
                  if (componentName.equals("HIVE_METASTORE")) {
                    ++activeHiveMetastoreComponentCount;
                  } else if (componentName.equals("HIVE_SERVER")) {
                    hiveServerComponentStarted = true;
                  } else if (componentName.equals("MYSQL_SERVER")) {
                    mysqlComponentStarted = true;
                  } else if (componentName.equals("WEBHCAT_SERVER")) {
                    webHcatComponentStarted = true;
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

        if (nonStartedState == null ||
          (hiveServerComponentStarted && webHcatComponentStarted && activeHiveMetastoreComponentCount > 0 &&
            (!embeddedMysqlComponentExists || mysqlComponentStarted))) {
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
