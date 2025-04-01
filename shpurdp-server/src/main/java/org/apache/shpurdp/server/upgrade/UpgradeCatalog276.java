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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * The {@link UpgradeCatalog276} upgrades Shpurdp from 2.7.5 to 2.7.6.
 */
public class UpgradeCatalog276 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog276.class);

  @Inject
  public UpgradeCatalog276(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.5";
  }

  @Override
  public String getTargetVersion() {
    return "2.7.6";
  }

  /**
   * Perform database schema transformation. Can work only before persist service start
   *
   * @throws ShpurdpException
   * @throws SQLException
   */
  @Override
  protected void executeDDLUpdates() throws ShpurdpException, SQLException {
    // no actions needed
  }

  /**
   * Perform data insertion before running normal upgrade of data, requires started persist service
   *
   * @throws ShpurdpException
   * @throws SQLException
   */
  @Override
  protected void executePreDMLUpdates() throws ShpurdpException, SQLException {
    // no actions needed
  }

  /**
   * Performs normal data upgrade
   *
   * @throws ShpurdpException
   * @throws SQLException
   */
  @Override
  protected void executeDMLUpdates() throws ShpurdpException, SQLException {
    LOG.debug("UpgradeCatalog276 executing DML Updates.");
    fixNativeLibrariesPathsForMR2AndTez();
  }

  protected void fixNativeLibrariesPathsForMR2AndTez() throws ShpurdpException {
    ShpurdpManagementController shpurdpManagementController = injector.getInstance(ShpurdpManagementController.class);
    Clusters clusters = shpurdpManagementController.getClusters();
    if (clusters != null) {
      String oldNativePath = "/usr/hdp/${hdp.version}/hadoop/lib/native:" +
          "/usr/hdp/${hdp.version}/hadoop/lib/native/Linux-{{architecture}}-64";
      String fixedNativePath = "/usr/hdp/current/hadoop-client/lib/native:" +
          "/usr/hdp/current/hadoop-client/lib/native/Linux-{{architecture}}-64";

      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("TEZ")) {
            updateConfigGroupNativePaths("tez-site",
                new HashSet<>(Arrays.asList("tez.am.launch.env", "tez.task.launch.env")),
                oldNativePath, fixedNativePath, cluster);
          }
          if (installedServices.contains("MAPREDUCE2")) {
            updateConfigGroupNativePaths("mapred-site",
                new HashSet<>(Arrays.asList("mapreduce.admin.user.env")),
                oldNativePath, fixedNativePath, cluster);
          }
        }
      }
    }
  }

  private void updateConfigGroupNativePaths(String configGroupName, Set<String> configsToChange,
                                            String replaceFrom, String replaceTo, Cluster cluster) throws ShpurdpException {
    Config targetConfig = cluster.getDesiredConfigByType(configGroupName);

    if (targetConfig != null) {
      Map<String, String> newProperty = new HashMap<>();

      for (String configName : configsToChange) {
        String configValue = targetConfig.getProperties().get(configName);
        if (configValue != null && configValue.contains(replaceFrom)) {
          String newConfigValue = configValue.replace(replaceFrom, replaceTo);
          LOG.info("Native path will be updated for '{}' property of '{}' config type, from '{}' to '{}'",
              configName, configGroupName, configValue, newConfigValue);
          newProperty.put(configName, newConfigValue);
        }
      }

      if (!newProperty.isEmpty()) {
        updateConfigurationPropertiesForCluster(cluster, configGroupName, newProperty, true, false);
      }
    }
  }
}