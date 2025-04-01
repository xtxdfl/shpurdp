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

import javax.inject.Inject;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.state.Clusters;

import com.google.inject.Injector;

/**
 * Updates script-based alert definitions with paths from the stack.
 */
public class UpdateAlertScriptPaths extends AbstractFinalUpgradeCatalog {

  @Inject
  public UpdateAlertScriptPaths(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDMLUpdates() throws ShpurdpException, SQLException {
    ShpurdpManagementController shpurdpManagementController = injector.getInstance(ShpurdpManagementController.class);
    ShpurdpMetaInfo shpurdpMetaInfo = injector.getInstance(ShpurdpMetaInfo.class);
    Clusters clusters = shpurdpManagementController.getClusters();
    shpurdpMetaInfo.reconcileAlertDefinitions(clusters, true);
  }
}
