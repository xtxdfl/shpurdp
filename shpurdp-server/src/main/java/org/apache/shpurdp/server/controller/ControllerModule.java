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

package org.apache.shpurdp.server.controller;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_ONLY;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_OR_EXTEND;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_BOTH_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION_MODE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_AND_CREATE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.NON_JTA_DATASOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.THROW_EXCEPTIONS;

import java.beans.PropertyVetoException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpService;
import org.apache.shpurdp.server.EagerSingleton;
import org.apache.shpurdp.server.StaticallyInject;
import org.apache.shpurdp.server.actionmanager.ActionDBAccessor;
import org.apache.shpurdp.server.actionmanager.ActionDBAccessorImpl;
import org.apache.shpurdp.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.shpurdp.server.actionmanager.HostRoleCommandFactory;
import org.apache.shpurdp.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.shpurdp.server.actionmanager.RequestFactory;
import org.apache.shpurdp.server.actionmanager.StageFactory;
import org.apache.shpurdp.server.actionmanager.StageFactoryImpl;
import org.apache.shpurdp.server.checks.DatabaseConsistencyCheckHelper;
import org.apache.shpurdp.server.checks.UpgradeCheckRegistry;
import org.apache.shpurdp.server.checks.UpgradeCheckRegistryProvider;
import org.apache.shpurdp.server.cleanup.ClasspathScannerUtils;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfiguration;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.configuration.Configuration.ConnectionPoolType;
import org.apache.shpurdp.server.configuration.Configuration.DatabaseType;
import org.apache.shpurdp.server.controller.internal.AlertTargetResourceProvider;
import org.apache.shpurdp.server.controller.internal.AuthResourceProvider;
import org.apache.shpurdp.server.controller.internal.ClusterStackVersionResourceProvider;
import org.apache.shpurdp.server.controller.internal.ComponentResourceProvider;
import org.apache.shpurdp.server.controller.internal.CredentialResourceProvider;
import org.apache.shpurdp.server.controller.internal.HostComponentResourceProvider;
import org.apache.shpurdp.server.controller.internal.HostKerberosIdentityResourceProvider;
import org.apache.shpurdp.server.controller.internal.HostResourceProvider;
import org.apache.shpurdp.server.controller.internal.KerberosDescriptorResourceProvider;
import org.apache.shpurdp.server.controller.internal.MemberResourceProvider;
import org.apache.shpurdp.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.shpurdp.server.controller.internal.RootServiceComponentConfigurationResourceProvider;
import org.apache.shpurdp.server.controller.internal.ServiceResourceProvider;
import org.apache.shpurdp.server.controller.internal.UpgradeResourceProvider;
import org.apache.shpurdp.server.controller.internal.UserAuthenticationSourceResourceProvider;
import org.apache.shpurdp.server.controller.internal.UserResourceProvider;
import org.apache.shpurdp.server.controller.internal.ViewInstanceResourceProvider;
import org.apache.shpurdp.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.shpurdp.server.controller.logging.LoggingRequestHelperFactoryImpl;
import org.apache.shpurdp.server.controller.metrics.MetricPropertyProviderFactory;
import org.apache.shpurdp.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
import org.apache.shpurdp.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;
import org.apache.shpurdp.server.controller.utilities.KerberosChecker;
import org.apache.shpurdp.server.events.AgentConfigsUpdateEvent;
import org.apache.shpurdp.server.events.ShpurdpEvent;
import org.apache.shpurdp.server.hooks.ShpurdpEventFactory;
import org.apache.shpurdp.server.hooks.HookContext;
import org.apache.shpurdp.server.hooks.HookContextFactory;
import org.apache.shpurdp.server.hooks.HookService;
import org.apache.shpurdp.server.hooks.users.PostUserCreationHookContext;
import org.apache.shpurdp.server.hooks.users.UserCreatedEvent;
import org.apache.shpurdp.server.hooks.users.UserHookService;
import org.apache.shpurdp.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.shpurdp.server.metadata.RoleCommandOrderProvider;
import org.apache.shpurdp.server.metrics.system.MetricsService;
import org.apache.shpurdp.server.metrics.system.impl.MetricsServiceImpl;
import org.apache.shpurdp.server.mpack.MpackManagerFactory;
import org.apache.shpurdp.server.notifications.DispatchFactory;
import org.apache.shpurdp.server.notifications.NotificationDispatcher;
import org.apache.shpurdp.server.notifications.dispatchers.AlertScriptDispatcher;
import org.apache.shpurdp.server.notifications.dispatchers.ShpurdpSNMPDispatcher;
import org.apache.shpurdp.server.notifications.dispatchers.SNMPDispatcher;
import org.apache.shpurdp.server.orm.DBAccessor;
import org.apache.shpurdp.server.orm.DBAccessorImpl;
import org.apache.shpurdp.server.orm.PersistenceType;
import org.apache.shpurdp.server.orm.dao.HostRoleCommandDAO;
import org.apache.shpurdp.server.scheduler.ExecutionScheduler;
import org.apache.shpurdp.server.scheduler.ExecutionSchedulerImpl;
import org.apache.shpurdp.server.security.SecurityHelper;
import org.apache.shpurdp.server.security.SecurityHelperImpl;
import org.apache.shpurdp.server.security.authorization.AuthorizationHelper;
import org.apache.shpurdp.server.security.authorization.internal.InternalAuthenticationInterceptor;
import org.apache.shpurdp.server.security.authorization.internal.RunWithInternalSecurityContext;
import org.apache.shpurdp.server.security.encryption.AESEncryptionService;
import org.apache.shpurdp.server.security.encryption.AgentConfigUpdateEncryptor;
import org.apache.shpurdp.server.security.encryption.ShpurdpServerConfigurationEncryptor;
import org.apache.shpurdp.server.security.encryption.ConfigPropertiesEncryptor;
import org.apache.shpurdp.server.security.encryption.CredentialStoreService;
import org.apache.shpurdp.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.shpurdp.server.security.encryption.EncryptionService;
import org.apache.shpurdp.server.security.encryption.Encryptor;
import org.apache.shpurdp.server.serveraction.kerberos.KerberosOperationHandlerFactory;
import org.apache.shpurdp.server.serveraction.users.CollectionPersisterService;
import org.apache.shpurdp.server.serveraction.users.CollectionPersisterServiceFactory;
import org.apache.shpurdp.server.serveraction.users.CsvFilePersisterService;
import org.apache.shpurdp.server.stack.StackManagerFactory;
import org.apache.shpurdp.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.shpurdp.server.stageplanner.RoleGraphFactory;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Config;
import org.apache.shpurdp.server.state.ConfigFactory;
import org.apache.shpurdp.server.state.ConfigImpl;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.apache.shpurdp.server.state.ServiceComponentFactory;
import org.apache.shpurdp.server.state.ServiceComponentHost;
import org.apache.shpurdp.server.state.ServiceComponentHostFactory;
import org.apache.shpurdp.server.state.ServiceComponentImpl;
import org.apache.shpurdp.server.state.ServiceFactory;
import org.apache.shpurdp.server.state.ServiceImpl;
import org.apache.shpurdp.server.state.cluster.ClusterFactory;
import org.apache.shpurdp.server.state.cluster.ClusterImpl;
import org.apache.shpurdp.server.state.cluster.ClustersImpl;
import org.apache.shpurdp.server.state.configgroup.ConfigGroup;
import org.apache.shpurdp.server.state.configgroup.ConfigGroupFactory;
import org.apache.shpurdp.server.state.configgroup.ConfigGroupImpl;
import org.apache.shpurdp.server.state.host.HostFactory;
import org.apache.shpurdp.server.state.host.HostImpl;
import org.apache.shpurdp.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.shpurdp.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.shpurdp.server.state.scheduler.RequestExecution;
import org.apache.shpurdp.server.state.scheduler.RequestExecutionFactory;
import org.apache.shpurdp.server.state.scheduler.RequestExecutionImpl;
import org.apache.shpurdp.server.state.stack.OsFamily;
import org.apache.shpurdp.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.shpurdp.server.topology.BlueprintFactory;
import org.apache.shpurdp.server.topology.PersistedState;
import org.apache.shpurdp.server.topology.PersistedStateImpl;
import org.apache.shpurdp.server.topology.SecurityConfigurationFactory;
import org.apache.shpurdp.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.shpurdp.server.utils.PasswordUtils;
import org.apache.shpurdp.server.utils.ThreadPools;
import org.apache.shpurdp.server.view.ViewInstanceHandlerList;
import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.ServiceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.jpa.ShpurdpJpaPersistModule;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Used for injection purposes.
 */
public class ControllerModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(ControllerModule.class);
  private static final String SHPURDP_PACKAGE = "org.apache.shpurdp.server";

  private final Configuration configuration;
  private final OsFamily os_family;
  private final HostsMap hostsMap;
  private final ThreadPools threadPools;
  private boolean dbInitNeeded;
  private final Gson prettyGson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();


  // ----- Constructors ------------------------------------------------------

  public ControllerModule() throws Exception {
    configuration = new Configuration();
    hostsMap = new HostsMap(configuration);
    os_family = new OsFamily(configuration);
    threadPools = new ThreadPools(configuration);
  }

  public ControllerModule(Properties properties) throws Exception {
    configuration = new Configuration(properties);
    hostsMap = new HostsMap(configuration);
    os_family = new OsFamily(configuration);
    threadPools = new ThreadPools((configuration));
  }


  // ----- ControllerModule --------------------------------------------------

  /**
   * Get the common persistence related configuration properties.
   *
   * @return the configuration properties
   */
  public static Properties getPersistenceProperties(Configuration configuration) {
    Properties properties = new Properties();

    // log what database type has been calculated
    DatabaseType databaseType = configuration.getDatabaseType();
    LOG.info("Detected {} as the database type from the JDBC URL", databaseType);

    switch (configuration.getPersistenceType()) {
      case IN_MEMORY:
        properties.setProperty(JDBC_URL, Configuration.JDBC_IN_MEMORY_URL);
        properties.setProperty(JDBC_DRIVER, Configuration.JDBC_IN_MEMORY_DRIVER);
        properties.setProperty(JDBC_USER, Configuration.JDBC_IN_MEMORY_USER);
        properties.setProperty(JDBC_PASSWORD, Configuration.JDBC_IN_MEMORY_PASSWORD);
        properties.setProperty(DDL_GENERATION, CREATE_ONLY);
        properties.setProperty(THROW_EXCEPTIONS, "true");
        break;
      case REMOTE:
        properties.setProperty(JDBC_URL, configuration.getDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, configuration.getDatabaseDriver());
        break;
      case LOCAL:
        properties.setProperty(JDBC_URL, configuration.getLocalDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, Configuration.SERVER_JDBC_DRIVER.getDefaultValue());
        break;
    }

    //allow to override values above
    // custom jdbc driver properties
    Properties customDatabaseDriverProperties = configuration.getDatabaseCustomProperties();
    properties.putAll(customDatabaseDriverProperties);

    // custom persistence properties
    Properties customPersistenceProperties = configuration.getPersistenceCustomProperties();
    properties.putAll(customPersistenceProperties);

    // determine the type of pool to use
    boolean isConnectionPoolingExternal = false;
    ConnectionPoolType connectionPoolType = configuration.getConnectionPoolType();
    if (connectionPoolType == ConnectionPoolType.C3P0) {
      isConnectionPoolingExternal = true;
    }

    // force the use of c3p0 with MySQL
    if (databaseType == DatabaseType.MYSQL) {
      isConnectionPoolingExternal = true;
    }

    // use c3p0
    if (isConnectionPoolingExternal) {
      LOG.info("Using c3p0 {} as the EclipsLink DataSource",
          ComboPooledDataSource.class.getSimpleName());

      // Oracle requires a different validity query
      String testQuery = "SELECT 1";
      if (databaseType == DatabaseType.ORACLE) {
        testQuery = "SELECT 1 FROM DUAL";
      }

      ComboPooledDataSource dataSource = new ComboPooledDataSource();

      // attempt to load the driver; if this fails, warn and move on
      try {
        dataSource.setDriverClass(configuration.getDatabaseDriver());
      } catch (PropertyVetoException pve) {
        LOG.warn("Unable to initialize c3p0", pve);
        return properties;
      }

      // basic configuration stuff
      dataSource.setJdbcUrl(configuration.getDatabaseUrl());
      dataSource.setUser(configuration.getDatabaseUser());
      dataSource.setPassword(configuration.getDatabasePassword());

      // pooling
      dataSource.setMinPoolSize(configuration.getConnectionPoolMinimumSize());
      dataSource.setInitialPoolSize(configuration.getConnectionPoolMinimumSize());
      dataSource.setMaxPoolSize(configuration.getConnectionPoolMaximumSize());
      dataSource.setAcquireIncrement(configuration.getConnectionPoolAcquisitionSize());
      dataSource.setAcquireRetryAttempts(configuration.getConnectionPoolAcquisitionRetryAttempts());
      dataSource.setAcquireRetryDelay(configuration.getConnectionPoolAcquisitionRetryDelay());

      // validity
      dataSource.setMaxConnectionAge(configuration.getConnectionPoolMaximumAge());
      dataSource.setMaxIdleTime(configuration.getConnectionPoolMaximumIdle());
      dataSource.setMaxIdleTimeExcessConnections(configuration.getConnectionPoolMaximumExcessIdle());
      dataSource.setPreferredTestQuery(testQuery);
      dataSource.setIdleConnectionTestPeriod(configuration.getConnectionPoolIdleTestInternval());

      properties.put(NON_JTA_DATASOURCE, dataSource);
    }

    return properties;
  }


  // ----- AbstractModule ----------------------------------------------------

  @Override
  protected void configure() {
    installFactories();

    final SessionHandler sessionHandler = new SessionHandler();
    bind(SessionHandler.class).toInstance(sessionHandler);

    bind(KerberosOperationHandlerFactory.class);
    bind(KerberosDescriptorFactory.class);
    bind(KerberosServiceDescriptorFactory.class);
    bind(KerberosHelper.class).to(KerberosHelperImpl.class);

    bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
    bind(EncryptionService.class).to(AESEncryptionService.class);
    //to support different Encryptor implementation we have to annotate them by their name and use them as @Named injects
    if (configuration.shouldEncryptSensitiveData()) {
      bind(new TypeLiteral<Encryptor<Config>>() {}).annotatedWith(Names.named("ConfigPropertiesEncryptor")).to(ConfigPropertiesEncryptor.class);
      bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).to(AgentConfigUpdateEncryptor.class);
      bind(new TypeLiteral<Encryptor<ShpurdpServerConfiguration>>() {}).annotatedWith(Names.named("ShpurdpServerConfigurationEncryptor")).to(ShpurdpServerConfigurationEncryptor.class);
    } else {
      bind(new TypeLiteral<Encryptor<Config>>() {}).annotatedWith(Names.named("ConfigPropertiesEncryptor")).toInstance(Encryptor.NONE);
      bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
      bind(new TypeLiteral<Encryptor<ShpurdpServerConfiguration>>() {}).annotatedWith(Names.named("ShpurdpServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
    }

    bind(Configuration.class).toInstance(configuration);
    bind(OsFamily.class).toInstance(os_family);
    bind(ThreadPools.class).toInstance(threadPools);
    bind(HostsMap.class).toInstance(hostsMap);
    bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
    bind(DelegatingFilterProxy.class).toInstance(new DelegatingFilterProxy() {
      {
        setTargetBeanName("springSecurityFilterChain");
      }
    });

    bind(Gson.class).annotatedWith(Names.named("prettyGson")).toInstance(prettyGson);

    install(buildJpaPersistModule());

    bind(Gson.class).in(Scopes.SINGLETON);
    bind(SecureRandom.class).in(Scopes.SINGLETON);

    bind(Clusters.class).to(ClustersImpl.class);
    bind(ShpurdpCustomCommandExecutionHelper.class);
    bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(
        configuration.getExecutionSchedulerWait());

    // This time is added to summary timeout time of all tasks in stage
    // So it's an "additional time", given to stage to finish execution before
    // it is considered as timed out
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);
    bindConstant().annotatedWith(Names.named("alertServiceCorePoolSize")).to(configuration.getAlertServiceCorePoolSize());

    bindConstant().annotatedWith(Names.named("dbInitNeeded")).to(dbInitNeeded);
    bindConstant().annotatedWith(Names.named("statusCheckInterval")).to(5000L);

    //ExecutionCommands cache size

    bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).
        to(configuration.getExecutionCommandsCacheSize());


    // Host role commands status summary max cache enable/disable
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).
        to(configuration.getHostRoleCommandStatusSummaryCacheEnabled());

    // Host role commands status summary max cache size
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).
        to(configuration.getHostRoleCommandStatusSummaryCacheSize());
    // Host role command status summary cache expiry duration in minutes
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).
        to(configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration());

    bind(ShpurdpManagementController.class).to(
        ShpurdpManagementControllerImpl.class);
    bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
    bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
    bind(DBAccessor.class).to(DBAccessorImpl.class);
    bind(ViewInstanceHandlerList.class).to(ShpurdpHandlerList.class);
    bind(TimelineMetricCacheProvider.class);
    bind(TimelineMetricCacheEntryFactory.class);
    bind(SecurityConfigurationFactory.class).in(Scopes.SINGLETON);

    bind(PersistedState.class).to(PersistedStateImpl.class);

    // factory to create LoggingRequestHelper instances for LogSearch integration
    bind(LoggingRequestHelperFactory.class).to(LoggingRequestHelperFactoryImpl.class);

    bind(MetricsService.class).to(MetricsServiceImpl.class).in(Scopes.SINGLETON);

    requestStaticInjection(DatabaseConsistencyCheckHelper.class);
    requestStaticInjection(KerberosChecker.class);
    requestStaticInjection(AuthorizationHelper.class);
    requestStaticInjection(PasswordUtils.class);

    bindByAnnotation(null);
    bindNotificationDispatchers(null);
    
    bind(UpgradeCheckRegistry.class).toProvider(UpgradeCheckRegistryProvider.class).in(Singleton.class);
    bind(HookService.class).to(UserHookService.class);

    InternalAuthenticationInterceptor shpurdpAuthenticationInterceptor = new InternalAuthenticationInterceptor();
    requestInjection(shpurdpAuthenticationInterceptor);
    bindInterceptor(any(), annotatedWith(RunWithInternalSecurityContext.class), shpurdpAuthenticationInterceptor);
  }

  // ----- helper methods ----------------------------------------------------

  private PersistModule buildJpaPersistModule() {
    PersistenceType persistenceType = configuration.getPersistenceType();
    ShpurdpJpaPersistModule jpaPersistModule = new ShpurdpJpaPersistModule(Configuration.JDBC_UNIT_NAME);

    Properties persistenceProperties = ControllerModule.getPersistenceProperties(configuration);

    if (!persistenceType.equals(PersistenceType.IN_MEMORY)) {
      persistenceProperties.setProperty(JDBC_USER, configuration.getDatabaseUser());
      persistenceProperties.setProperty(JDBC_PASSWORD, configuration.getDatabasePassword());

      switch (configuration.getJPATableGenerationStrategy()) {
        case CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_ONLY);
          dbInitNeeded = true;
          break;
        case DROP_AND_CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, DROP_AND_CREATE);
          dbInitNeeded = true;
          break;
        case CREATE_OR_EXTEND:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_OR_EXTEND);
          break;
        default:
          break;
      }

      persistenceProperties.setProperty(DDL_GENERATION_MODE, DDL_BOTH_GENERATION);
      persistenceProperties.setProperty(CREATE_JDBC_DDL_FILE, "DDL-create.jdbc");
      persistenceProperties.setProperty(DROP_JDBC_DDL_FILE, "DDL-drop.jdbc");
    }

    jpaPersistModule.properties(persistenceProperties);
    return jpaPersistModule;
  }

  /**
   * Bind classes to their Factories, which can be built on-the-fly.
   * Often, will also have to edit AgentResourceTest.java
   */
  private void installFactories() {
    install(new FactoryModuleBuilder().implement(
        Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
    install(new FactoryModuleBuilder().implement(
        Host.class, HostImpl.class).build(HostFactory.class));
    install(new FactoryModuleBuilder().implement(
        Service.class, ServiceImpl.class).build(ServiceFactory.class));

    install(new FactoryModuleBuilder()
        .implement(ResourceProvider.class, Names.named("host"), HostResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("hostComponent"), HostComponentResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("service"), ServiceResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("component"), ComponentResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("member"), MemberResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("repositoryVersion"), RepositoryVersionResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("hostKerberosIdentity"), HostKerberosIdentityResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("user"), UserResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("userAuthenticationSource"), UserAuthenticationSourceResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("credential"), CredentialResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("kerberosDescriptor"), KerberosDescriptorResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("upgrade"), UpgradeResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("clusterStackVersion"), ClusterStackVersionResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("alertTarget"), AlertTargetResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("viewInstance"), ViewInstanceResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("rootServiceHostComponentConfiguration"), RootServiceComponentConfigurationResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("auth"), AuthResourceProvider.class)
        .build(ResourceProviderFactory.class));

    install(new FactoryModuleBuilder().implement(
      ServiceComponent.class, ServiceComponentImpl.class).build(
      ServiceComponentFactory.class));
    install(new FactoryModuleBuilder().implement(
        ServiceComponentHost.class, ServiceComponentHostImpl.class).build(
        ServiceComponentHostFactory.class));
    install(new FactoryModuleBuilder().implement(
        Config.class, ConfigImpl.class).build(ConfigFactory.class));
    install(new FactoryModuleBuilder().implement(
        ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
    install(new FactoryModuleBuilder().implement(RequestExecution.class,
        RequestExecutionImpl.class).build(RequestExecutionFactory.class));

    bind(StageFactory.class).to(StageFactoryImpl.class);
    bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);

    install(new FactoryModuleBuilder().build(RoleGraphFactory.class));

    install(new FactoryModuleBuilder().build(RequestFactory.class));
    install(new FactoryModuleBuilder().build(StackManagerFactory.class));
    install(new FactoryModuleBuilder().build(ExecutionCommandWrapperFactory.class));
    install(new FactoryModuleBuilder().build(MetricPropertyProviderFactory.class));
    install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
    install(new FactoryModuleBuilder().build(MpackManagerFactory.class));
    install(new FactoryModuleBuilder().build(org.apache.shpurdp.server.topology.addservice.RequestValidatorFactory.class));

    bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
    bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
    bind(org.apache.shpurdp.server.topology.StackFactory.class).to(org.apache.shpurdp.server.topology.DefaultStackFactory.class);
    bind(BlueprintFactory.class);

    install(new FactoryModuleBuilder().implement(ShpurdpEvent.class, Names.named("userCreated"), UserCreatedEvent.class).build(ShpurdpEventFactory.class));
    install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class).build(HookContextFactory.class));
    install(new FactoryModuleBuilder().implement(CollectionPersisterService.class, CsvFilePersisterService.class).build(CollectionPersisterServiceFactory.class));

    install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

  }

  /**
   * Initializes specially-marked interfaces that require injection.
   * <p/>
   * An example of where this is needed is with a singleton that is headless; in
   * other words, it doesn't have any injections but still needs to be part of
   * the Guice framework.
   * <p/>
   * A second example of where this is needed is when classes require static
   * members that are available via injection.
   * <p/>
   * Although Spring has a nicer API for performing this search, it's dreadfully
   * slow on annotation processing. This class will use
   * {@link ClasspathScannerUtils} which in turn uses {@link ClassPath} to
   * perform the scan.
   * <p/>
   * If {@code matchedClasses} is null this will scan
   * {@code org.apache.shpurdp.server} (currently) for any {@link EagerSingleton}
   * or {@link StaticallyInject} or {@link ShpurdpService} instances.
   *
   * @param matchedClasses
   *          the set of previously found classes, or {@code null} to invoke
   *          scanning.
   * @return the set of classes that was found.
   */
  @SuppressWarnings("unchecked")
  protected Set<Class<?>> bindByAnnotation(Set<Class<?>> matchedClasses) {
    // only search if necessary
    if (null == matchedClasses) {
      List<Class<?>> classes = new ArrayList<>();
      classes.add(EagerSingleton.class);
      classes.add(StaticallyInject.class);
      classes.add(ShpurdpService.class);

      LOG.info("Searching package {} for annotations matching {}", SHPURDP_PACKAGE, classes);

      matchedClasses = ClasspathScannerUtils.findOnClassPath(SHPURDP_PACKAGE, new ArrayList<>(), classes);

      if (null == matchedClasses || matchedClasses.size() == 0) {
        LOG.warn("No instances of {} found to register", classes);
        return matchedClasses;
      }
    }

    Set<com.google.common.util.concurrent.Service> services = new HashSet<>();

    for (Class<?> clazz : matchedClasses) {
      if (null != clazz.getAnnotation(EagerSingleton.class)) {
        bind(clazz).asEagerSingleton();
        LOG.debug("Eagerly binding singleton {}", clazz);
      }

      if (null != clazz.getAnnotation(StaticallyInject.class)) {
        requestStaticInjection(clazz);
        LOG.debug("Statically injecting {} ", clazz);
      }

      // Shpurdp services are registered with Guava
      if (null != clazz.getAnnotation(ShpurdpService.class)) {
        // safety check to ensure it's actually a Guava service
        if (!com.google.common.util.concurrent.Service.class.isAssignableFrom(clazz)) {
          String message = MessageFormat.format(
              "Unable to register service {0} because it is not a Service which can be scheduled",
              clazz);

          LOG.error(message);
          throw new RuntimeException(message);
        }

        // instantiate the service, register as singleton via toInstance()
        com.google.common.util.concurrent.Service service = null;
        try {
          service = (com.google.common.util.concurrent.Service) clazz.newInstance();
          bind((Class<com.google.common.util.concurrent.Service>) clazz).toInstance(service);
          services.add(service);
          LOG.info("Registering service {} ", clazz);
        } catch (Exception exception) {
          LOG.error("Unable to register {} as a service", clazz, exception);
          throw new RuntimeException(exception);
        }
      }
    }

    ServiceManager manager = new ServiceManager(services);
    bind(ServiceManager.class).toInstance(manager);

    return matchedClasses;
  }

  /**
   * Searches for all instances of {@link NotificationDispatcher} on the
   * classpath and registers each as a singleton with the
   * {@link DispatchFactory}.
   */
  @SuppressWarnings("unchecked")
  protected Set<BeanDefinition> bindNotificationDispatchers(Set<BeanDefinition> beanDefinitions) {

    // make the factory a singleton
    DispatchFactory dispatchFactory = DispatchFactory.getInstance();
    bind(DispatchFactory.class).toInstance(dispatchFactory);

    if (null == beanDefinitions || beanDefinitions.isEmpty()) {
      String packageName = AlertScriptDispatcher.class.getPackage().getName();
      LOG.info("Searching package {} for dispatchers matching {}", packageName,
          NotificationDispatcher.class);

      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);

      // match all implementations of the dispatcher interface
      AssignableTypeFilter filter = new AssignableTypeFilter(
          NotificationDispatcher.class);

      scanner.addIncludeFilter(filter);

      beanDefinitions = scanner.findCandidateComponents(packageName);
    }

    // no dispatchers is a problem
    if (null == beanDefinitions || beanDefinitions.size() == 0) {
      LOG.error("No instances of {} found to register", NotificationDispatcher.class);
      return null;
    }

    // for every discovered dispatcher, singleton-ize them and register with
    // the dispatch factory
    for (BeanDefinition beanDefinition : beanDefinitions) {
      String className = beanDefinition.getBeanClassName();
      if (className != null) {
        Class<?> clazz = ClassUtils.resolveClassName(className,
                ClassUtils.getDefaultClassLoader());
        try {
          NotificationDispatcher dispatcher;
          if (clazz.equals(ShpurdpSNMPDispatcher.class)) {
            dispatcher = (NotificationDispatcher) clazz.getConstructor(Integer.class)
                    .newInstance(configuration.getShpurdpSNMPUdpBindPort());
          } else if (clazz.equals(SNMPDispatcher.class)) {
            dispatcher = (NotificationDispatcher) clazz.getConstructor(Integer.class)
                    .newInstance(configuration.getSNMPUdpBindPort());
          } else {
            dispatcher = (NotificationDispatcher) clazz.newInstance();
          }
          dispatchFactory.register(dispatcher.getType(), dispatcher);
          bind((Class<NotificationDispatcher>) clazz).toInstance(dispatcher);
          LOG.info("Binding and registering notification dispatcher {}", clazz);
        } catch (Exception exception) {
          LOG.error("Unable to bind and register notification dispatcher {}",
                  clazz, exception);
        }
      } else {
        LOG.error("Binding and registering notification dispatcher is not possible for" +
            " beanDefinition: {} in the absence of className", beanDefinition);
      }
    }

    return beanDefinitions;
  }
}
