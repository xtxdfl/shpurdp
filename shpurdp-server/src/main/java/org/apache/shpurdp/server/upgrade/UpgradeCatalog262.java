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
package org.apache.shpurdp.server.upgrade;

import java.sql.SQLException;
import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.orm.DBAccessor;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog262} upgrades Shpurdp from 2.6.1 to 2.6.2.
 */
public class UpgradeCatalog262 extends AbstractUpgradeCatalog {

  private static final String HOST_REQUEST_TABLE = "topology_host_request";
  private static final String STATUS_COLUMN = "status";
  private static final String STATUS_MESSAGE_COLUMN = "status_message";

  @Inject
  public UpgradeCatalog262(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.6.1";
  }

  @Override
  public String getTargetVersion() {
    return "2.6.2";
  }

  @Override
  protected void executeDDLUpdates() throws ShpurdpException, SQLException {
    addHostRequestStatusColumn();
  }

  private void addHostRequestStatusColumn() throws SQLException {
    dbAccessor.addColumn(HOST_REQUEST_TABLE, new DBAccessor.DBColumnInfo(STATUS_COLUMN, String.class, 255, null, true));
    dbAccessor.addColumn(HOST_REQUEST_TABLE, new DBAccessor.DBColumnInfo(STATUS_MESSAGE_COLUMN, String.class, 1024, null, true));
  }

  @Override
  protected void executePreDMLUpdates() throws ShpurdpException, SQLException {
    fixDesiredStack();
  }

  /**
   * if desired stack < current stack, set current stack as desired
   *
   * @throws ShpurdpException
   */
  private void fixDesiredStack() throws ShpurdpException {
    ShpurdpManagementController shpurdpManagementController = injector.getInstance(ShpurdpManagementController.class);
    Clusters clusters = shpurdpManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          StackId desiredStack = cluster.getDesiredStackVersion();
          StackId currentStack = cluster.getCurrentStackVersion();
          if (!desiredStack.equals(currentStack)) {
            cluster.setDesiredStackVersion(currentStack);
          }
        }
      }
    }
  }

  @Override
  protected void executeDMLUpdates() throws ShpurdpException, SQLException {
  }

}
