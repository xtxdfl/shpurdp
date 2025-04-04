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

import static org.mockito.ArgumentMatchers.nullable;

import java.util.HashMap;
import java.util.Map;

import org.apache.shpurdp.server.orm.dao.HostVersionDAO;
import org.apache.shpurdp.server.orm.dao.RepositoryVersionDAO;
import org.apache.shpurdp.server.orm.entities.HostVersionEntity;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.orm.entities.StackEntity;
import org.apache.shpurdp.server.state.CheckHelper;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.MaintenanceState;
import org.apache.shpurdp.server.state.RepositoryVersionState;
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
 * Unit tests for HostsRepositoryVersionCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HostsRepositoryVersionCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final HostVersionDAO hostVersionDAO = Mockito.mock(HostVersionDAO.class);
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);

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

  @Before
  public void setup() throws Exception {
    m_services.clear();

    StackId stackId = new StackId("HDP", "1.0");
    String version = "1.0.0.0-1234";
    
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(stackId.toString());
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn(version);
    
    Mockito.when(m_repositoryVersionEntity.getStackId()).thenReturn(stackId);

    m_checkHelper.m_clusters = clusters;
  }

  @Test
  public void testPerform() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    hostsRepositoryVersionCheck.checkHelperProvider = new Provider<CheckHelper>() {
      @Override
      public CheckHelper get() {
        return m_checkHelper;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);
    
    

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    UpgradeCheckResult check = hostsRepositoryVersionCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.0.6");

    final RepositoryVersionEntity repositoryVersion = new RepositoryVersionEntity();
    repositoryVersion.setStack(stackEntity);
    
    

    final HostVersionEntity hostVersion = new HostVersionEntity();
    hostVersion.setState(RepositoryVersionState.INSTALLED);
    Mockito.when(
        hostVersionDAO.findByClusterStackVersionAndHost(Mockito.anyString(),
            Mockito.any(StackId.class), Mockito.anyString(),
            nullable(String.class))).thenReturn(hostVersion);

    check = hostsRepositoryVersionCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformWithVersion() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);

    Mockito.when(host1.getHostName()).thenReturn("host1");
    Mockito.when(host2.getHostName()).thenReturn("host2");
    Mockito.when(host3.getHostName()).thenReturn("host3");

    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);

    HostVersionEntity hve = new HostVersionEntity();
    hve.setRepositoryVersion(m_repositoryVersionEntity);
    hve.setState(RepositoryVersionState.INSTALLED);

    for (String hostName : hosts.keySet()) {
      Mockito.when(hostVersionDAO.findByClusterStackVersionAndHost("cluster",
          m_repositoryVersionEntity.getStackId(), m_repositoryVersion.getVersion(), hostName)).thenReturn(
              hve);
    }

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    UpgradeCheckResult check = hostsRepositoryVersionCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformWithVersionNotRequired() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);

    Mockito.when(host1.getHostName()).thenReturn("host1");
    Mockito.when(host2.getHostName()).thenReturn("host2");
    Mockito.when(host3.getHostName()).thenReturn("host3");

    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);

    HostVersionEntity hve = new HostVersionEntity();
    hve.setRepositoryVersion(m_repositoryVersionEntity);
    hve.setState(RepositoryVersionState.NOT_REQUIRED);

    for (String hostName : hosts.keySet()) {
      Mockito.when(hostVersionDAO.findByClusterStackVersionAndHost("cluster",
          m_repositoryVersionEntity.getStackId(), m_repositoryVersion.getVersion(), hostName)).thenReturn(
              hve);
    }

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    UpgradeCheckResult check = hostsRepositoryVersionCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }

}
