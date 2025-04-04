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

import static org.apache.shpurdp.server.checks.ShpurdpMetricsHadoopSinkVersionCompatibilityCheck.MIN_HADOOP_SINK_VERSION_PROPERTY_NAME;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.ShpurdpServer;
import org.apache.shpurdp.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.RequestStatus;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.shpurdp.server.orm.dao.HostRoleCommandDAO;
import org.apache.shpurdp.server.orm.dao.RepositoryVersionDAO;
import org.apache.shpurdp.server.orm.dao.RequestDAO;
import org.apache.shpurdp.server.orm.entities.HostRoleCommandEntity;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.orm.entities.RequestEntity;
import org.apache.shpurdp.server.state.CheckHelper;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.repository.ClusterVersionSummary;
import org.apache.shpurdp.server.state.repository.VersionDefinitionXml;
import org.apache.shpurdp.spi.ClusterInformation;
import org.apache.shpurdp.spi.RepositoryType;
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
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Provider;

@RunWith(PowerMockRunner.class)
@PrepareForTest ({ShpurdpServer.class, AbstractControllerResourceProvider.class, PropertyHelper.class})
public class ShpurdpMetricsHadoopSinkVersionCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);
  private final ShpurdpMetricsHadoopSinkVersionCompatibilityCheck m_check = new ShpurdpMetricsHadoopSinkVersionCompatibilityCheck();
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(
    RepositoryVersionDAO.class);

  private ClusterVersionSummary m_clusterVersionSummary;

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
    m_repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);
    m_clusterVersionSummary = Mockito.mock(ClusterVersionSummary.class);
    m_vdfXml = Mockito.mock(VersionDefinitionXml.class);
    MockitoAnnotations.initMocks(this);

    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    m_check.config = config;

    StackId stackId = new StackId("HDP", "3.0");
    String version = "3.0.0.0-1234";

    Mockito.when(m_repositoryVersion.getId()).thenReturn(1L);
    Mockito.when(m_repositoryVersion.getRepositoryType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(stackId.toString());
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn(version);


    when(m_repositoryVersionEntity.getVersion()).thenReturn(version);
    when(m_repositoryVersionEntity.getStackId()).thenReturn(stackId);

    m_services.clear();

    when(m_repositoryVersionEntity.getType()).thenReturn(RepositoryType.STANDARD);
    when(m_repositoryVersionEntity.getRepositoryXml()).thenReturn(m_vdfXml);
    when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class), Mockito.any(ShpurdpMetaInfo.class))).thenReturn(m_clusterVersionSummary);
    when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());

    m_checkHelper.m_clusters = m_clusters;
    Mockito.when(m_checkHelper.m_repositoryVersionDAO.findByPK(Mockito.anyLong())).thenReturn(m_repositoryVersionEntity);

    m_check.checkHelperProvider = new Provider<CheckHelper>() {
      @Override
      public CheckHelper get() {
        return m_checkHelper;
      }
    };
  }

  /**
   * Tests that the check is applicable when hive is installed.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicable() throws Exception {
    assertTrue(m_check.getApplicableServices().contains("HDFS"));
    assertTrue(m_check.getApplicableServices().contains("SHPURDP_METRICS"));
  }

  /**
   * Tests that the warning is correctly tripped when there are not enough
   * metastores.
   *
   * @throws Exception
   */
  @Test(timeout = 60000)
  public void testPerform() throws Exception {

    ShpurdpManagementController shpurdpManagementControllerMock = Mockito.mock(ShpurdpManagementController.class);
    PowerMockito.mockStatic(ShpurdpServer.class);
    when(ShpurdpServer.getController()).thenReturn(shpurdpManagementControllerMock);

    ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
    PowerMockito.mockStatic(AbstractControllerResourceProvider.class);
    when(AbstractControllerResourceProvider.getResourceProvider(Mockito.eq(Resource.Type.Request), any(ShpurdpManagementController.class))).thenReturn(resourceProviderMock);

    PowerMockito.mockStatic(PropertyHelper.class);
    Request requestMock = mock(Request.class);
    when(PropertyHelper.getCreateRequest(any(), any())).thenReturn(requestMock);
    when(PropertyHelper.getPropertyId("Requests", "id")).thenReturn("requestIdProp");

    RequestStatus requestStatusMock = mock(RequestStatus.class);
    Resource responseResourceMock = mock(Resource.class);
    when(resourceProviderMock.createResources(requestMock)).thenReturn(requestStatusMock);
    when(requestStatusMock.getRequestResource()).thenReturn(responseResourceMock);
    when(responseResourceMock.getPropertyValue(anyString())).thenReturn(100l);

    Clusters clustersMock = mock(Clusters.class);
    when(shpurdpManagementControllerMock.getClusters()).thenReturn(clustersMock);
    Cluster clusterMock = mock(Cluster.class);
    when(clustersMock.getCluster("c1")).thenReturn(clusterMock);
    when(clusterMock.getHosts(eq("SHPURDP_METRICS"), eq("METRICS_MONITOR"))).thenReturn(Collections.singleton("h1"));

    RequestDAO requestDAOMock = mock(RequestDAO.class);
    RequestEntity requestEntityMock  = mock(RequestEntity.class);
    when(requestDAOMock.findByPks(Collections.singleton(100l), true)).thenReturn(Collections.singletonList(requestEntityMock));
    when(requestEntityMock.getStatus()).thenReturn(HostRoleStatus.IN_PROGRESS).thenReturn(HostRoleStatus.COMPLETED);

    Field requestDaoField = m_check.getClass().getDeclaredField("requestDAO");
    requestDaoField.setAccessible(true);
    requestDaoField.set(m_check, requestDAOMock);

    Map<String, String> checkProperties = new HashMap<>();
    checkProperties.put(MIN_HADOOP_SINK_VERSION_PROPERTY_NAME, "2.7.0.0");

    ClusterInformation clusterInformation = new ClusterInformation("c1", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, checkProperties, null);

    UpgradeCheckResult check = m_check.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }

  @Test(timeout = 60000)
  public void testPerformFail() throws Exception{
    ShpurdpManagementController shpurdpManagementControllerMock = Mockito.mock(ShpurdpManagementController.class);
    PowerMockito.mockStatic(ShpurdpServer.class);
    when(ShpurdpServer.getController()).thenReturn(shpurdpManagementControllerMock);

    ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
    PowerMockito.mockStatic(AbstractControllerResourceProvider.class);
    when(AbstractControllerResourceProvider.getResourceProvider(Mockito.eq(Resource.Type.Request), any(ShpurdpManagementController.class))).thenReturn(resourceProviderMock);

    PowerMockito.mockStatic(PropertyHelper.class);
    Request requestMock = mock(Request.class);
    when(PropertyHelper.getCreateRequest(any(), any())).thenReturn(requestMock);
    when(PropertyHelper.getPropertyId("Requests", "id")).thenReturn("requestIdProp");

    RequestStatus requestStatusMock = mock(RequestStatus.class);
    Resource responseResourceMock = mock(Resource.class);
    when(resourceProviderMock.createResources(requestMock)).thenReturn(requestStatusMock);
    when(requestStatusMock.getRequestResource()).thenReturn(responseResourceMock);
    when(responseResourceMock.getPropertyValue(anyString())).thenReturn(101l);

    Clusters clustersMock = mock(Clusters.class);
    when(shpurdpManagementControllerMock.getClusters()).thenReturn(clustersMock);
    Cluster clusterMock = mock(Cluster.class);
    when(clustersMock.getCluster("c1")).thenReturn(clusterMock);
    when(clusterMock.getHosts(eq("SHPURDP_METRICS"), eq("METRICS_MONITOR"))).thenReturn(Collections.singleton("h1_fail"));

    RequestDAO requestDAOMock = mock(RequestDAO.class);
    RequestEntity requestEntityMock  = mock(RequestEntity.class);
    when(requestDAOMock.findByPks(Collections.singleton(101l), true)).thenReturn(Collections.singletonList(requestEntityMock));
    when(requestEntityMock.getStatus()).thenReturn(HostRoleStatus.IN_PROGRESS).thenReturn(HostRoleStatus.FAILED);

    Field requestDaoField = m_check.getClass().getDeclaredField("requestDAO");
    requestDaoField.setAccessible(true);
    requestDaoField.set(m_check, requestDAOMock);


    when(requestEntityMock.getRequestId()).thenReturn(101l);
    HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
    HostRoleCommandEntity hrcEntityMock  = mock(HostRoleCommandEntity.class);
    when(hostRoleCommandDAOMock.findByRequest(101l, true)).thenReturn(Collections.singletonList(hrcEntityMock));
    when(hrcEntityMock.getStatus()).thenReturn(HostRoleStatus.FAILED);
    when(hrcEntityMock.getHostName()).thenReturn("h1_fail");

    Field hrcDaoField = m_check.getClass().getDeclaredField("hostRoleCommandDAO");
    hrcDaoField.setAccessible(true);
    hrcDaoField.set(m_check, hostRoleCommandDAOMock);

    Map<String, String> checkProperties = new HashMap<>();
    checkProperties.put(MIN_HADOOP_SINK_VERSION_PROPERTY_NAME, "2.7.0.0");

    ClusterInformation clusterInformation = new ClusterInformation("c1", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, checkProperties, null);

    UpgradeCheckResult check = m_check.perform(request);

    Assert.assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());
    Assert.assertTrue(check.getFailReason().contains("upgrade 'shpurdp-metrics-hadoop-sink'"));
    Assert.assertEquals(check.getFailedOn().size(), 1);
    Assert.assertTrue(check.getFailedOn().iterator().next().contains("h1_fail"));
  }
}
