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

package org.apache.shpurdp.server.controller.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.actionmanager.ActionDBAccessor;
import org.apache.shpurdp.server.actionmanager.ActionManager;
import org.apache.shpurdp.server.actionmanager.HostRoleCommandFactory;
import org.apache.shpurdp.server.actionmanager.RequestFactory;
import org.apache.shpurdp.server.actionmanager.StageFactory;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.audit.AuditLogger;
import org.apache.shpurdp.server.checks.UpgradeCheckRegistry;
import org.apache.shpurdp.server.checks.UpgradeCheckRegistryProvider;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfiguration;
import org.apache.shpurdp.server.controller.AbstractRootServiceResponseFactory;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.KerberosHelper;
import org.apache.shpurdp.server.controller.KerberosHelperImpl;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;
import org.apache.shpurdp.server.controller.utilities.PredicateBuilder;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.shpurdp.server.events.AgentConfigsUpdateEvent;
import org.apache.shpurdp.server.hooks.HookContextFactory;
import org.apache.shpurdp.server.hooks.HookService;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapConfigurationProvider;
import org.apache.shpurdp.server.metadata.RoleCommandOrderProvider;
import org.apache.shpurdp.server.mpack.MpackManagerFactory;
import org.apache.shpurdp.server.orm.dao.AlertDefinitionDAO;
import org.apache.shpurdp.server.orm.dao.AlertsDAO;
import org.apache.shpurdp.server.orm.dao.HostRoleCommandDAO;
import org.apache.shpurdp.server.orm.dao.RepositoryVersionDAO;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.sample.checks.SampleServiceCheck;
import org.apache.shpurdp.server.scheduler.ExecutionScheduler;
import org.apache.shpurdp.server.security.encryption.CredentialStoreService;
import org.apache.shpurdp.server.security.encryption.Encryptor;
import org.apache.shpurdp.server.serveraction.kerberos.KerberosConfigDataFileWriterFactory;
import org.apache.shpurdp.server.serveraction.kerberos.KerberosIdentityDataFileWriterFactory;
import org.apache.shpurdp.server.stack.StackManagerFactory;
import org.apache.shpurdp.server.stack.upgrade.Direction;
import org.apache.shpurdp.server.stack.upgrade.UpgradePack;
import org.apache.shpurdp.server.stack.upgrade.UpgradePack.PrerequisiteCheckConfig;
import org.apache.shpurdp.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.shpurdp.server.stageplanner.RoleGraphFactory;
import org.apache.shpurdp.server.state.CheckHelper;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Config;
import org.apache.shpurdp.server.state.ConfigFactory;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.DesiredConfig;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponentFactory;
import org.apache.shpurdp.server.state.ServiceComponentHost;
import org.apache.shpurdp.server.state.ServiceComponentHostFactory;
import org.apache.shpurdp.server.state.ServiceInfo;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.StackInfo;
import org.apache.shpurdp.server.state.configgroup.ConfigGroupFactory;
import org.apache.shpurdp.server.state.scheduler.RequestExecutionFactory;
import org.apache.shpurdp.server.state.stack.OsFamily;
import org.apache.shpurdp.server.topology.PersistedState;
import org.apache.shpurdp.server.topology.TopologyManager;
import org.apache.shpurdp.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.shpurdp.spi.ClusterInformation;
import org.apache.shpurdp.spi.RepositoryType;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckStatus;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckType;
import org.apache.shpurdp.spi.upgrade.UpgradeType;
import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * PreUpgradeCheckResourceProvider tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UpgradeCheckRegistry.class })
public class PreUpgradeCheckResourceProviderTest extends EasyMockSupport {

  private static final String TEST_SERVICE_CHECK_CLASS_NAME = "org.apache.shpurdp.server.sample.checks.SampleServiceCheck";

  private static final String CLUSTER_NAME = "Cluster100";

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testGetResources() throws Exception{
    Injector injector = createInjector();
    ShpurdpManagementController managementController = injector.getInstance(ShpurdpManagementController.class);

    Clusters clusters = injector.getInstance(Clusters.class);
    UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);

    RepositoryVersionDAO repoDao = injector.getInstance(RepositoryVersionDAO.class);
    RepositoryVersionEntity repo = createNiceMock(RepositoryVersionEntity.class);
    UpgradePack upgradePack = createNiceMock(UpgradePack.class);
    PrerequisiteCheckConfig config = createNiceMock(PrerequisiteCheckConfig.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    ClusterInformation clusterInformation = createNiceMock(ClusterInformation.class);

    expect(service.getDesiredRepositoryVersion()).andReturn(repo).atLeastOnce();

    StackId currentStackId = createNiceMock(StackId.class);
    StackId targetStackId = createNiceMock(StackId.class);
    ShpurdpMetaInfo shpurdpMetaInfo = injector.getInstance(ShpurdpMetaInfo.class);
    shpurdpMetaInfo.init();
    expectLastCall().anyTimes();

    Config actualConfig = createNiceMock(Config.class);
    DesiredConfig desiredConfig = createNiceMock(DesiredConfig.class);
    Map<String, DesiredConfig> configMap = Maps.newHashMap();
    configMap.put("config-type", desiredConfig);

    expect(desiredConfig.getTag()).andReturn("config-tag-1").atLeastOnce();
    expect(cluster.getDesiredConfigs()).andReturn(configMap).atLeastOnce();
    expect(cluster.getConfig("config-type", "config-tag-1")).andReturn(actualConfig).atLeastOnce();
    expect(cluster.buildClusterInformation()).andReturn(clusterInformation).anyTimes();

    expect(clusterInformation.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();

    Map<String, Service> allServiceMap = new HashMap<>();
    allServiceMap.put("Service100", service);

    Map<String, ServiceInfo> allServiceInfoMap = new HashMap<>();
    allServiceInfoMap.put("Service100", serviceInfo);

    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    expect(serviceComponentHost.getServiceName()).andReturn("Service100").atLeastOnce();
    expect(serviceComponentHost.getServiceComponentName()).andReturn("Component100").atLeastOnce();
    expect(serviceComponentHost.getHostName()).andReturn("c6401.shpurdp.apache.org").atLeastOnce();
    List<ServiceComponentHost> serviceComponentHosts = Lists.newArrayList();
    serviceComponentHosts.add(serviceComponentHost);
    expect(cluster.getServiceComponentHosts()).andReturn(serviceComponentHosts).atLeastOnce();

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getShpurdpMetaInfo()).andReturn(shpurdpMetaInfo).anyTimes();

    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn(CLUSTER_NAME).atLeastOnce();
    expect(cluster.getServices()).andReturn(allServiceMap).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(currentStackId).anyTimes();

    expect(currentStackId.getStackName()).andReturn("Stack100").anyTimes();
    expect(currentStackId.getStackVersion()).andReturn("1.0").anyTimes();
    expect(targetStackId.getStackId()).andReturn("Stack100-1.1").anyTimes();
    expect(targetStackId.getStackName()).andReturn("Stack100").anyTimes();
    expect(targetStackId.getStackVersion()).andReturn("1.1").anyTimes();

    expect(repoDao.findByPK(1L)).andReturn(repo).anyTimes();
    expect(repo.getStackId()).andReturn(targetStackId).atLeastOnce();
    expect(repo.getId()).andReturn(1L).atLeastOnce();
    expect(repo.getType()).andReturn(RepositoryType.STANDARD).atLeastOnce();
    expect(repo.getVersion()).andReturn("1.1.0.0").atLeastOnce();
    expect(upgradeHelper.suggestUpgradePack(CLUSTER_NAME, currentStackId, targetStackId, Direction.UPGRADE, UpgradeType.NON_ROLLING, "upgrade_pack11")).andReturn(upgradePack);

    List<String> prerequisiteChecks = new LinkedList<>();
    prerequisiteChecks.add(TEST_SERVICE_CHECK_CLASS_NAME);
    expect(upgradePack.getPrerequisiteCheckConfig()).andReturn(config);
    expect(upgradePack.getPrerequisiteChecks()).andReturn(prerequisiteChecks).anyTimes();
    expect(upgradePack.getTarget()).andReturn("1.1.*.*").anyTimes();
    expect(upgradePack.getOwnerStackId()).andReturn(targetStackId).atLeastOnce();
    expect(upgradePack.getType()).andReturn(UpgradeType.ROLLING).atLeastOnce();

    expect(shpurdpMetaInfo.getServices("Stack100", "1.0")).andReturn(allServiceInfoMap).anyTimes();
//    String checks = ClassLoader.getSystemClassLoader().getResource("checks").getPath();
    expect(serviceInfo.getChecksFolder()).andReturn(new File("checks"));


    class TestClassLoader extends URLClassLoader {
      public TestClassLoader() {
        super(new URL[0], TestClassLoader.class.getClassLoader());
      }

      @Override
      public URL[] getURLs() {
        try {
          return new URL[] { new URL("file://foo") };
        } catch (MalformedURLException e) {
          return new URL[0];
        }
      }
    }
    TestClassLoader testClassLoader = new TestClassLoader();



    StackInfo stackInfo = createNiceMock(StackInfo.class);

    expect(shpurdpMetaInfo.getStack(targetStackId)).andReturn(stackInfo).anyTimes();
//    String tname = targetStackId.getStackName();
//    String tv = targetStackId.getStackVersion();
    expect(shpurdpMetaInfo.getStack("Stack100", "1.1") ).andReturn(stackInfo).atLeastOnce();
    expect(stackInfo.getLibraryClassLoader()).andReturn(testClassLoader).anyTimes();
    expect(stackInfo.getLibraryInstance(EasyMock.anyObject(), EasyMock.eq(TEST_SERVICE_CHECK_CLASS_NAME)))
      .andReturn(new SampleServiceCheck()).atLeastOnce();

    // mock out plugin check loading
    Reflections reflectionsMock = createNiceMock(Reflections.class);

    PowerMockito.whenNew(Reflections.class).withParameterTypes(
        Configuration.class).withArguments(Matchers.any(ConfigurationBuilder.class)).thenReturn(
            reflectionsMock);

    PowerMock.replay(Reflections.class);

    // replay
    replayAll();

    ResourceProvider provider = getPreUpgradeCheckResourceProvider(managementController, injector);
    // create the request
    Request request = PropertyHelper.getReadRequest(new HashSet<>());
    PredicateBuilder builder = new PredicateBuilder();
    Predicate predicate = builder.property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).equals(CLUSTER_NAME).and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID).equals("upgrade_pack11").and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).equals(UpgradeType.NON_ROLLING).and()
        .property(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID).equals("1").toPredicate();

    Set<Resource> resources = Collections.emptySet();
    resources = provider.getResources(request, predicate);

    // make sure all of the checks ran and were returned in the response; some
    // of the checks are stripped out b/c they don't define any required upgrade
    // types. The value being asserted here is a combination of built-in checks
    // which are required for the upgrade type as well as any provided checks
    // discovered in the stack
    Assert.assertEquals(20, resources.size());

    // find the service check provided by the library classloader and verify it ran
    Resource customUpgradeCheck = null;
    for (Resource resource : resources) {
      String id = (String) resource.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_ID_PROPERTY_ID);
      if (StringUtils.equals(id, "SAMPLE_SERVICE_CHECK")) {
        customUpgradeCheck = resource;
        break;
      }
    }

    assertNotNull(customUpgradeCheck);

    String description = (String) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CHECK_PROPERTY_ID);
    Assert.assertEquals("Sample service check description.", description);
    UpgradeCheckStatus status = (UpgradeCheckStatus) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_STATUS_PROPERTY_ID);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, status);
    String reason = (String) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_REASON_PROPERTY_ID);
    Assert.assertEquals("Sample service check always fails.", reason);
    UpgradeCheckType checkType = (UpgradeCheckType) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID);
    Assert.assertEquals(UpgradeCheckType.HOST, checkType);
    String clusterName = (String) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID);
    Assert.assertEquals(CLUSTER_NAME, clusterName);
    UpgradeType upgradeType = (UpgradeType) customUpgradeCheck.getPropertyValue(PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID);
    Assert.assertEquals(UpgradeType.NON_ROLLING, upgradeType);

  }

  /**
   * This factory method creates PreUpgradeCheckResourceProvider using the mock managementController
   */
  public PreUpgradeCheckResourceProvider getPreUpgradeCheckResourceProvider(ShpurdpManagementController managementController, Injector injector) throws  ShpurdpException {
    //UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);
    //injector.injectMembers(upgradeHelper);
    PreUpgradeCheckResourceProvider provider = new PreUpgradeCheckResourceProvider(managementController);
    return provider;
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {

      @Override
      @SuppressWarnings("unchecked")
      protected void configure() {
        Clusters clusters = createNiceMock(Clusters.class);
        Provider<Clusters> clusterProvider = () -> clusters;

        UpgradeHelper upgradeHelper = createNiceMock(UpgradeHelper.class);
        Provider<UpgradeHelper> upgradeHelperProvider = () -> upgradeHelper;

        CheckHelper checkHelper = new CheckHelper();

        bind(CheckHelper.class).toInstance(checkHelper);
        bind(Clusters.class).toProvider(clusterProvider);
        bind(UpgradeCheckRegistry.class).toProvider(UpgradeCheckRegistryProvider.class);
        bind(UpgradeHelper.class).toProvider(upgradeHelperProvider);
        bind(KerberosHelper.class).to(KerberosHelperImpl.class);
        bind(KerberosIdentityDataFileWriterFactory.class).toInstance(createNiceMock(KerberosIdentityDataFileWriterFactory.class));
        bind(KerberosConfigDataFileWriterFactory.class).toInstance(createNiceMock(KerberosConfigDataFileWriterFactory.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(ShpurdpManagementController.class).toInstance(createNiceMock(ShpurdpManagementController.class));
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessor.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
        bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
        bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
        bind(RoleCommandOrderProvider.class).toInstance(createNiceMock(RoleCommandOrderProvider.class));
        bind(RoleGraphFactory.class).toInstance(createNiceMock(RoleGraphFactory.class));
        bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
        bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(HostRoleCommandFactory.class).toInstance(createNiceMock(HostRoleCommandFactory.class));
        bind(HookContextFactory.class).toInstance(createNiceMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createNiceMock(HookService.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
        bind(ConfigureClusterTaskFactory.class).toInstance(createNiceMock(ConfigureClusterTaskFactory.class));
        bind(TopologyManager.class).toInstance(createNiceMock(TopologyManager.class));
        bind(ShpurdpMetaInfo.class).toInstance(createNiceMock(ShpurdpMetaInfo.class));
        bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));
        bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
        bind(RepositoryVersionDAO.class).toInstance(createNiceMock(RepositoryVersionDAO.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        Provider<EntityManager> entityManagerProvider = createNiceMock(Provider.class);
        bind(EntityManager.class).toProvider(entityManagerProvider);
        bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
        bind(new TypeLiteral<Encryptor<ShpurdpServerConfiguration>>() {}).annotatedWith(Names.named("ShpurdpServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
        bind(ShpurdpLdapConfigurationProvider.class).toInstance(createMock(ShpurdpLdapConfigurationProvider.class));

        requestStaticInjection(PreUpgradeCheckResourceProvider.class);
      }
    });
  }

}
