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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * Checks that all hosts have particular repository version. Hosts that are in
 * maintenance mode will be skipped and will not report a warning. Even if they
 * do not have the repo version, they will not be included in the upgrade
 * orchstration, so no warning is required.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.REPOSITORY_VERSION,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HostsRepositoryVersionCheck extends ClusterCheck {

  static final UpgradeCheckDescription HOSTS_REPOSITORY_VERSION = new UpgradeCheckDescription("HOSTS_REPOSITORY_VERSION",
      UpgradeCheckType.HOST,
      "All hosts should have target version installed",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following hosts must have version {{version}} installed: {{fails}}.").build());


  /**
   * Constructor.
   */
  public HostsRepositoryVersionCheck() {
    super(HOSTS_REPOSITORY_VERSION);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws ShpurdpException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);

    for (Host host : clusterHosts.values()) {
      // hosts in MM will produce a warning if they do not have the repo version
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      if (maintenanceState != MaintenanceState.OFF) {
        continue;
      }

      RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
      StackId repositoryStackId = new StackId(repositoryVersion.getStackId());

      // get the host version entity for this host and repository
      final HostVersionEntity hostVersion = hostVersionDaoProvider.get().findByClusterStackVersionAndHost(
          clusterName, repositoryStackId, repositoryVersion.getVersion(), host.getHostName());

      // the repo needs to either be installed or not required
      Set<RepositoryVersionState> okStates = EnumSet.of(RepositoryVersionState.INSTALLED,
          RepositoryVersionState.NOT_REQUIRED);

      if (hostVersion == null || !okStates.contains(hostVersion.getState())) {
        result.getFailedOn().add(host.getHostName());

        result.getFailedDetail().add(
            new HostDetail(host.getHostId(), host.getHostName()));
      }
    }

    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}