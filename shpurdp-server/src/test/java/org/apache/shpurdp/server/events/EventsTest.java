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
package org.apache.shpurdp.server.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.H2DatabaseCleaner;
import org.apache.shpurdp.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.shpurdp.server.events.ShpurdpEvent.ShpurdpEventType;
import org.apache.shpurdp.server.orm.GuiceJpaInitializer;
import org.apache.shpurdp.server.orm.InMemoryDefaultTestModule;
import org.apache.shpurdp.server.orm.OrmTestHelper;
import org.apache.shpurdp.server.orm.dao.AlertDefinitionDAO;
import org.apache.shpurdp.server.orm.dao.AlertDispatchDAO;
import org.apache.shpurdp.server.orm.entities.AlertDefinitionEntity;
import org.apache.shpurdp.server.orm.entities.AlertGroupEntity;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.HostState;
import org.apache.shpurdp.server.state.MaintenanceState;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.apache.shpurdp.server.state.ServiceComponentFactory;
import org.apache.shpurdp.server.state.ServiceComponentHost;
import org.apache.shpurdp.server.state.ServiceComponentHostFactory;
import org.apache.shpurdp.server.state.ServiceFactory;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.State;
import org.apache.shpurdp.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests that {@link EventsTest} instances are fired correctly and
 * that alert data is bootstrapped into the database.
 */
public class EventsTest {

  private static final String HOSTNAME = "c6401.shpurdp.apache.org";

  private Clusters m_clusters;
  private Cluster m_cluster;
  private String m_clusterName;
  private Injector m_injector;
  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;
  private MockEventListener m_listener;
  private OrmTestHelper m_helper;
  private AlertDefinitionDAO m_definitionDao;
  private AlertDispatchDAO m_alertDispatchDao;

  private final String STACK_VERSION = "2.0.6";
  private final String REPO_VERSION = "2.0.6-1234";
  private RepositoryVersionEntity m_repositoryVersion;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);

    m_helper = m_injector.getInstance(OrmTestHelper.class);

    // register mock listener
    EventBus synchronizedBus = EventBusSynchronizer.synchronizeShpurdpEventPublisher(m_injector);
    m_listener = m_injector.getInstance(MockEventListener.class);
    synchronizedBus.register(m_listener);

    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_alertDispatchDao = m_injector.getInstance(AlertDispatchDAO.class);

    m_clusterName = "foo";
    StackId stackId = new StackId("HDP", STACK_VERSION);
    m_helper.createStack(stackId);

    m_clusters.addCluster(m_clusterName, stackId);
    m_clusters.addHost(HOSTNAME);

    Host host = m_clusters.getHost(HOSTNAME);
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);

    m_cluster = m_clusters.getCluster(m_clusterName);
    Assert.assertNotNull(m_cluster);

    m_cluster.setDesiredStackVersion(stackId);
    m_repositoryVersion = m_helper.getOrCreateRepositoryVersion(stackId, REPO_VERSION);

    m_clusters.mapHostToCluster(HOSTNAME, m_clusterName);
    m_clusters.updateHostMappings(host);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
    m_injector = null;
  }

  /**
   * Tests that {@link ServiceInstalledEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceInstalledEvent() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceInstalledEvent.class;
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    installHdfsService();
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
  }

  /**
   * Tests that {@link ServiceRemovedEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceRemovedEvent() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceRemovedEvent.class;
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    installHdfsService();
    m_cluster.deleteAllServices();
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
  }

  /**
   * Tests that {@link ServiceRemovedEvent}s are fired correctly and alerts and
   * the default alert group are removed.
   *
   * @throws Exception
   */
  @Test
  public void testServiceRemovedEventForAlerts() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceRemovedEvent.class;
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    installHdfsService();

    // get the default group for HDFS
    AlertGroupEntity group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    // verify the default group is there
    Assert.assertNotNull(group);
    Assert.assertTrue(group.isDefault());

    // check that there are alert definitions
    Assert.assertTrue(m_definitionDao.findAll(m_cluster.getClusterId()).size() > 0);

    // get all definitions for HDFS
    List<AlertDefinitionEntity> hdfsDefinitions = m_definitionDao.findByService(
        m_cluster.getClusterId(), "HDFS");

    // make sure there are at least 1
    Assert.assertTrue(hdfsDefinitions.size() > 0);

    AlertDefinitionEntity definition = hdfsDefinitions.get(0);

    // delete HDFS
    m_cluster.getService("HDFS").delete(new DeleteHostComponentStatusMetaData());

    // verify the event was received
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));

    // verify that the definitions were removed
    hdfsDefinitions = m_definitionDao.findByService(m_cluster.getClusterId(), "HDFS");

    Assert.assertEquals(0, hdfsDefinitions.size());

    // verify that the default group was removed
    group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    Assert.assertNull(group);
  }

  /**
   * Tests that {@link ServiceRemovedEvent}s are fired correctly and the default alert group
   * is removed even though alerts were already removed at the time the event is fired.
   *
   * @throws Exception
   */
  @Test
  public void testServiceRemovedEventForDefaultAlertGroup() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceRemovedEvent.class;
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    installHdfsService();

    // get the default group for HDFS
    AlertGroupEntity group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    // verify the default group is there
    Assert.assertNotNull(group);
    Assert.assertTrue(group.isDefault());

    // get all definitions for HDFS
    List<AlertDefinitionEntity> hdfsDefinitions = m_definitionDao.findByService(
        m_cluster.getClusterId(), "HDFS");

    // delete the definitions
    for (AlertDefinitionEntity definition : hdfsDefinitions) {
      m_definitionDao.remove(definition);
    }

    // verify that the definitions were removed
    hdfsDefinitions = m_definitionDao.findByService(m_cluster.getClusterId(), "HDFS");

    Assert.assertEquals(0, hdfsDefinitions.size());

    // delete HDFS
    m_cluster.getService("HDFS").delete(new DeleteHostComponentStatusMetaData());

    // verify the event was received
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));

    // verify that the default group was removed
    group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    Assert.assertNull(group);
  }

  /**
   * Tests that {@link ServiceRemovedEvent}s are fired correctly and alerts are removed
   * even though the default alert group was already removed at the time the event is fired .
   *
   * @throws Exception
   */
  @Test
  public void testServiceRemovedEventForAlertDefinitions() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceRemovedEvent.class;
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    installHdfsService();

    // get the default group for HDFS
    AlertGroupEntity group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    // verify the default group is there
    Assert.assertNotNull(group);
    Assert.assertTrue(group.isDefault());

    // check that there are alert definitions
    Assert.assertTrue(m_definitionDao.findAll(m_cluster.getClusterId()).size() > 0);

    // get all definitions for HDFS
    List<AlertDefinitionEntity> hdfsDefinitions = m_definitionDao.findByService(
        m_cluster.getClusterId(), "HDFS");

    // make sure there are at least 1
    Assert.assertTrue(hdfsDefinitions.size() > 0);

    // delete the default alert group
    m_alertDispatchDao.remove(group);

    // verify that the default group was removed
    group = m_alertDispatchDao.findGroupByName(m_cluster.getClusterId(), "HDFS");

    Assert.assertNull(group);

    // delete HDFS
    m_cluster.getService("HDFS").delete(new DeleteHostComponentStatusMetaData());

    // verify the event was received
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));

    // verify that the definitions were removed
    hdfsDefinitions = m_definitionDao.findByService(m_cluster.getClusterId(), "HDFS");

    Assert.assertEquals(0, hdfsDefinitions.size());
  }

  /**
   * Tests that {@link ServiceComponentUninstalledEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceComponentUninstalledEvent() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ServiceComponentUninstalledEvent.class;
    installHdfsService();

    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    m_cluster.getServiceComponentHosts(HOSTNAME).get(0).delete(new DeleteHostComponentStatusMetaData());

    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
  }

  /**
   * Tests that {@link MaintenanceModeEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testMaintenanceModeEvents() throws Exception {
    installHdfsService();
    Service service = m_cluster.getService("HDFS");
    Class<? extends ShpurdpEvent> eventClass = MaintenanceModeEvent.class;

    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
    Assert.assertEquals(1, m_listener.getShpurdpEventReceivedCount(eventClass));

    m_listener.reset();
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));

    List<ServiceComponentHost> componentHosts = m_cluster.getServiceComponentHosts(HOSTNAME);
    ServiceComponentHost componentHost = componentHosts.get(0);
    componentHost.setMaintenanceState(MaintenanceState.OFF);

    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
    Assert.assertEquals(1, m_listener.getShpurdpEventReceivedCount(eventClass));

    m_listener.reset();
    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));

    Host host = m_clusters.getHost(HOSTNAME);
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.ON);
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.OFF);

    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
    Assert.assertEquals(2, m_listener.getShpurdpEventReceivedCount(eventClass));
  }

  /**
   * Tests that {@link ServiceComponentUninstalledEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testClusterRenameEvent() throws Exception {
    Class<? extends ShpurdpEvent> eventClass = ClusterEvent.class;
    installHdfsService();

    Assert.assertFalse(m_listener.isShpurdpEventReceived(eventClass));
    m_cluster.setClusterName(UUID.randomUUID().toString());

    Assert.assertTrue(m_listener.isShpurdpEventReceived(eventClass));
    List<ShpurdpEvent> shpurdpEvents = m_listener.getShpurdpEventInstances(eventClass);
    Assert.assertEquals(1, shpurdpEvents.size());
    Assert.assertEquals(ShpurdpEventType.CLUSTER_RENAME, shpurdpEvents.get(0).getType());
  }

  private void installHdfsService() throws Exception {
    String serviceName = "HDFS";
    Service service = m_serviceFactory.createNew(m_cluster, serviceName, m_repositoryVersion);
    service = m_cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ServiceComponent component = m_componentFactory.createNew(service, "DATANODE");
    service.addServiceComponent(component);
    component.setDesiredState(State.INSTALLED);

    ServiceComponentHost sch = m_schFactory.createNew(component, HOSTNAME);

    component.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
  }
}
