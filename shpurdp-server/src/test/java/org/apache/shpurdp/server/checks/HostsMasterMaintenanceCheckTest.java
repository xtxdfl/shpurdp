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

import java.util.HashMap;
import java.util.Map;

import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.orm.dao.RepositoryVersionDAO;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.shpurdp.server.stack.upgrade.UpgradePack;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.repository.ClusterVersionSummary;
import org.apache.shpurdp.server.state.repository.VersionDefinitionXml;
import org.apache.shpurdp.spi.ClusterInformation;
import org.apache.shpurdp.spi.RepositoryVersion;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckRequest;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckResult;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckStatus;
import org.apache.shpurdp.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Unit tests for HostsMasterMaintenanceCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HostsMasterMaintenanceCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);
  private final RepositoryVersionHelper repositoryVersionHelper = Mockito.mock(RepositoryVersionHelper.class);
  private final ShpurdpMetaInfo shpurdpMetaInfo = Mockito.mock(ShpurdpMetaInfo.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersion m_repositoryVersion;

  @Mock
  private RepositoryVersionEntity m_repositoryVersionEntity;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_services.clear();
  }

  @Test
  public void testPerform() throws Exception {
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("1.0.0.0-1234");

    final String upgradePackName = "upgrade_pack";
    final HostsMasterMaintenanceCheck hostsMasterMaintenanceCheck = new HostsMasterMaintenanceCheck();
    hostsMasterMaintenanceCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsMasterMaintenanceCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsMasterMaintenanceCheck.repositoryVersionHelper = new Provider<RepositoryVersionHelper>() {
      @Override
      public RepositoryVersionHelper get() {
        return repositoryVersionHelper;
      }
    };
    hostsMasterMaintenanceCheck.shpurdpMetaInfo = new Provider<ShpurdpMetaInfo>() {
      @Override
      public ShpurdpMetaInfo get() {
        return shpurdpMetaInfo;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(repositoryVersionHelper.getUpgradePackageName(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (UpgradeType) Mockito.anyObject())).thenReturn(null);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest checkRequest = new UpgradeCheckRequest(clusterInformation,
        UpgradeType.ROLLING, m_repositoryVersion, null, null);

    UpgradeCheckResult result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());

    Mockito.when(repositoryVersionHelper.getUpgradePackageName(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (UpgradeType) Mockito.anyObject())).thenReturn(upgradePackName);
    Mockito.when(shpurdpMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(new HashMap<>());

    result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());

    final Map<String, UpgradePack> upgradePacks = new HashMap<>();
    final UpgradePack upgradePack = Mockito.mock(UpgradePack.class);
    Mockito.when(upgradePack.getName()).thenReturn(upgradePackName);
    upgradePacks.put(upgradePack.getName(), upgradePack);
    Mockito.when(shpurdpMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(upgradePacks);
    Mockito.when(upgradePack.getTasks()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(clusters.getHostsForCluster(Mockito.anyString())).thenReturn(new HashMap<>());

    result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }
}
