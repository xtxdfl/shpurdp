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
package org.apache.shpurdp.server.checks;

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.shpurdp.annotations.UpgradeCheckInfo;
import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.orm.entities.HostVersionEntity;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.MaintenanceState;
import org.apache.shpurdp.server.state.RepositoryVersionState;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.spi.RepositoryVersion;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckDescription;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckGroup;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckRequest;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckResult;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckStatus;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckType;
import org.apache.shpurdp.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * Checks if Install Packages needs to be re-run
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.DEFAULT,
    order = 3.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class InstallPackagesCheck extends ClusterCheck {

  static final UpgradeCheckDescription INSTALL_PACKAGES_CHECK = new UpgradeCheckDescription("INSTALL_PACKAGES_CHECK",
      UpgradeCheckType.CLUSTER,
      "Install packages must be re-run",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "Re-run Install Packages before starting upgrade").build());

  /**
   * Constructor.
   */
  public InstallPackagesCheck() {
    super(INSTALL_PACKAGES_CHECK);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws ShpurdpException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();

    final StackId targetStackId = new StackId(repositoryVersion.getStackId());

    final Set<HostDetail> failedHosts = new TreeSet<>();

    for (Host host : cluster.getHosts()) {
      if (host.getMaintenanceState(cluster.getClusterId()) != MaintenanceState.ON) {
        for (HostVersionEntity hve : hostVersionDaoProvider.get().findByHost(host.getHostName())) {
          if (StringUtils.equals(hve.getRepositoryVersion().getVersion(), repositoryVersion.getVersion())
              && hve.getState() == RepositoryVersionState.INSTALL_FAILED) {

            failedHosts.add(new HostDetail(host.getHostId(), host.getHostName()));
          }
        }
      }
    }

    if (!failedHosts.isEmpty()) {
      String message = MessageFormat.format("Hosts in cluster [{0},{1},{2},{3}] are in INSTALL_FAILED state because " +
              "Install Packages had failed. Please re-run Install Packages, if necessary place following hosts " +
              "in Maintenance mode: {4}", cluster.getClusterName(), targetStackId.getStackName(),
          targetStackId.getStackVersion(), repositoryVersion.getVersion(),
          StringUtils.join(failedHosts, ", "));

      LinkedHashSet<String> failedHostNames = failedHosts.stream().map(
          failedHost -> failedHost.hostName).collect(
              Collectors.toCollection(LinkedHashSet::new));

      result.setFailedOn(failedHostNames);
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(message);
      result.getFailedDetail().addAll(failedHosts);

      return result;
    }

    result.setStatus(UpgradeCheckStatus.PASS);
    return result;
  }
}
