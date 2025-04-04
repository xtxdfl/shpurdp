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
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.state.CheckHelper;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.StackId;
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
 * Unit tests for ServicesMaintenanceModeCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ServicesMaintenanceModeCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersion m_repositoryVersion;

  @Mock
  private RepositoryVersionEntity m_repositoryVersionEntity;

  private MockCheckHelper m_checkHelper = new MockCheckHelper();

  final Map<String, Service> m_services = new HashMap<>();

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_services.clear();

    StackId stackId = new StackId("HDP", "1.0");
    String version = "1.0.0.0-1234";

    Mockito.when(m_repositoryVersion.getId()).thenReturn(1L);
    Mockito.when(m_repositoryVersionEntity.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class), Mockito.any(ShpurdpMetaInfo.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());

    m_checkHelper.m_clusters = clusters;
    Mockito.when(m_checkHelper.m_repositoryVersionDAO.findByPK(Mockito.anyLong())).thenReturn(m_repositoryVersionEntity);
  }

  @Test
  public void testPerform() throws Exception {
    final ServicesMaintenanceModeCheck servicesMaintenanceModeCheck = new ServicesMaintenanceModeCheck();
    servicesMaintenanceModeCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    servicesMaintenanceModeCheck.shpurdpMetaInfo = new Provider<ShpurdpMetaInfo>() {
      @Override
      public ShpurdpMetaInfo get() {
        return Mockito.mock(ShpurdpMetaInfo.class);
      }
    };

    m_checkHelper.setMetaInfoProvider(servicesMaintenanceModeCheck.shpurdpMetaInfo);

    servicesMaintenanceModeCheck.checkHelperProvider = new Provider<CheckHelper>() {
    @Override
    public CheckHelper get() {
      return m_checkHelper;
    }
  };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Service service = Mockito.mock(Service.class);
    // We don't bother checking service desired state as it's performed by a separate check

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    UpgradeCheckResult check = servicesMaintenanceModeCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());

    check = servicesMaintenanceModeCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }
}
