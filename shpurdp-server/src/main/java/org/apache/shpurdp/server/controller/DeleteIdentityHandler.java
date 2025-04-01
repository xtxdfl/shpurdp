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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.apache.shpurdp.server.controller.KerberosHelperImpl.BASE_LOG_DIR;
import static org.apache.shpurdp.server.controller.KerberosHelperImpl.REMOVE_KEYTAB;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.Role;
import org.apache.shpurdp.server.RoleCommand;
import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.actionmanager.Stage;
import org.apache.shpurdp.server.actionmanager.StageFactory;
import org.apache.shpurdp.server.agent.CommandReport;
import org.apache.shpurdp.server.controller.internal.RequestResourceFilter;
import org.apache.shpurdp.server.serveraction.ServerAction;
import org.apache.shpurdp.server.serveraction.kerberos.AbstractPrepareKerberosServerAction;
import org.apache.shpurdp.server.serveraction.kerberos.Component;
import org.apache.shpurdp.server.serveraction.kerberos.DestroyPrincipalsServerAction;
import org.apache.shpurdp.server.serveraction.kerberos.KDCType;
import org.apache.shpurdp.server.serveraction.kerberos.KerberosOperationHandler;
import org.apache.shpurdp.server.serveraction.kerberos.KerberosServerAction;
import org.apache.shpurdp.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Config;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.kerberos.KerberosDescriptor;
import org.apache.shpurdp.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.shpurdp.server.utils.StageUtils;
import org.apache.commons.collections4.CollectionUtils;

import com.google.gson.reflect.TypeToken;


/**
 * I delete kerberos identities (principals and keytabs) of a given component.
 */
class DeleteIdentityHandler {
  public static final String COMPONENT_FILTER = "component_filter";

  private final ShpurdpCustomCommandExecutionHelper customCommandExecutionHelper;
  private final Integer taskTimeout;
  private final StageFactory stageFactory;
  private final ShpurdpManagementController shpurdpManagementController;

  public DeleteIdentityHandler(ShpurdpCustomCommandExecutionHelper customCommandExecutionHelper, Integer taskTimeout, StageFactory stageFactory, ShpurdpManagementController shpurdpManagementController) {
    this.customCommandExecutionHelper = customCommandExecutionHelper;
    this.taskTimeout = taskTimeout;
    this.stageFactory = stageFactory;
    this.shpurdpManagementController = shpurdpManagementController;
  }

  /**
   * Creates and adds stages to the given stage container for deleting kerberos identities.
   * The service component that belongs to the identity doesn't need to be installed.
   */
  public void addDeleteIdentityStages(Cluster cluster, OrderedRequestStageContainer stageContainer, CommandParams commandParameters, boolean manageIdentities)
    throws ShpurdpException
  {
    ServiceComponentHostServerActionEvent event = new ServiceComponentHostServerActionEvent(RootComponent.SHPURDP_SERVER.name(), StageUtils.getHostName(), System.currentTimeMillis());
    String hostParamsJson = StageUtils.getGson().toJson(customCommandExecutionHelper.createDefaultHostParams(cluster, cluster.getDesiredStackVersion()));
    if (manageIdentities) {
      addPrepareDeleteIdentity(cluster, hostParamsJson, event, commandParameters, stageContainer);
      addDeleteKeytab(cluster, commandParameters.getAffectedHostNames(), hostParamsJson, commandParameters, stageContainer);
      addDestroyPrincipals(cluster, hostParamsJson, event, commandParameters, stageContainer);
    }
    addFinalize(cluster, hostParamsJson, event, stageContainer, commandParameters);
  }

  private void addPrepareDeleteIdentity(Cluster cluster,
                                        String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                        CommandParams commandParameters,
                                        OrderedRequestStageContainer stageContainer)
    throws ShpurdpException
  {
    Stage stage = createServerActionStage(stageContainer.getLastStageId(),
      cluster,
      stageContainer.getId(),
      "Prepare delete identities",
      "{}",
      hostParamsJson,
      PrepareDeleteIdentityServerAction.class,
      event,
      commandParameters.asMap(),
      "Prepare delete identities",
      taskTimeout);
    stageContainer.addStage(stage);
  }

  private void addDestroyPrincipals(Cluster cluster,
                                    String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                    CommandParams commandParameters,
                                    OrderedRequestStageContainer stageContainer)
    throws ShpurdpException
  {
    Stage stage = createServerActionStage(stageContainer.getLastStageId(),
      cluster,
      stageContainer.getId(),
      "Destroy Principals",
      "{}",
      hostParamsJson,
      DestroyPrincipalsServerAction.class,
      event,
      commandParameters.asMap(),
      "Destroy Principals",
      Math.max(ServerAction.DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS, taskTimeout));
    stageContainer.addStage(stage);
  }

  private void addDeleteKeytab(Cluster cluster,
                               Set<String> hostFilter,
                               String hostParamsJson,
                               CommandParams commandParameters,
                               OrderedRequestStageContainer stageContainer)
      throws ShpurdpException {

    // Filter out any hosts that have been removed
    Set<String> hostNames = (CollectionUtils.isEmpty(hostFilter))
        ? null
        : hostFilter.stream()
        .filter(hostname -> shpurdpManagementController.getClusters().hostExists(hostname))
        .collect(toSet());

    if(CollectionUtils.isNotEmpty(hostNames)) {
      Stage stage = createNewStage(stageContainer.getLastStageId(),
          cluster,
          stageContainer.getId(),
          "Delete Keytabs",
          commandParameters.asJson(),
          hostParamsJson);

      Map<String, String> requestParams = new HashMap<>();
      List<RequestResourceFilter> requestResourceFilters = new ArrayList<>();
      RequestResourceFilter reqResFilter = new RequestResourceFilter("KERBEROS", "KERBEROS_CLIENT", new ArrayList<>(hostNames));
      requestResourceFilters.add(reqResFilter);

      ActionExecutionContext actionExecContext = new ActionExecutionContext(
          cluster.getClusterName(),
          REMOVE_KEYTAB,
          requestResourceFilters,
          requestParams);
      customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, requestParams, null);
      stageContainer.addStage(stage);
    }
  }

  private void addFinalize(Cluster cluster,
                           String hostParamsJson, ServiceComponentHostServerActionEvent event,
                           OrderedRequestStageContainer requestStageContainer,
                           CommandParams commandParameters)
    throws ShpurdpException
  {
    Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
      cluster,
      requestStageContainer.getId(),
      "Finalize Operations",
      "{}",
      hostParamsJson,
      DeleteDataDirAction.class,
      event,
      commandParameters.asMap(),
      "Finalize Operations", 300);
    requestStageContainer.addStage(stage);
  }


  public static class CommandParams {
    private final List<Component> components;
    private final Set<String> identities;
    private final String authName;
    private final File dataDirectory;
    private final String defaultRealm;
    private final KDCType kdcType;

    public CommandParams(List<Component> components, Set<String> identities, String authName, File dataDirectory, String defaultRealm, KDCType kdcType) {
      this.components = components;
      this.identities = identities;
      this.authName = authName;
      this.dataDirectory = dataDirectory;
      this.defaultRealm = defaultRealm;
      this.kdcType = kdcType;
    }

    public Map<String, String> asMap() {
      Map<String, String> commandParameters = new HashMap<>();
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, authName);
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, defaultRealm);
      commandParameters.put(KerberosServerAction.KDC_TYPE, kdcType.name());
      commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identities));
      commandParameters.put(COMPONENT_FILTER, StageUtils.getGson().toJson(components));
      commandParameters.put(KerberosServerAction.SERVICE_COMPONENT_FILTER, StageUtils.getGson().toJson(toServiceComponentFilter(components)));
      commandParameters.put(KerberosServerAction.HOST_FILTER, StageUtils.getGson().toJson(toHostFilter(components)));
      commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      return commandParameters;
    }

    public Set<String> getAffectedHostNames() {
      return components.stream().map(Component::getHostName).collect(toSet());
    }

    public String asJson() {
      return StageUtils.getGson().toJson(asMap());
    }

    /**
     * Convert a collection of {@link Component}s to a service/component filter.
     *
     * @param components the collection of relevant {@link Component}s
     * @return a map of service names to component names to include in an operation
     */
    private Map<String, ? extends Collection<String>> toServiceComponentFilter(List<Component> components) {
      if (components == null) {
        return null;
      }

      return components.stream().collect(groupingBy(Component::getServiceName, mapping(Component::getServiceComponentName, toSet())));
    }

    /**
     * Convert a collection of {@link Component}s to a host filter.
     *
     * @param components the collection of relevant {@link Component}s
     * @return a set of hostnames of hosts to include in an operation
     */
    private Set<String> toHostFilter(List<Component> components) {
      if (components == null) {
        return null;
      }

      return components.stream().map(Component::getHostName).collect(toSet());
    }
  }

  public static class PrepareDeleteIdentityServerAction extends AbstractPrepareKerberosServerAction {
    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws ShpurdpException, InterruptedException {
      KerberosDescriptor kerberosDescriptor = getKerberosDescriptor();
      processServiceComponents(
        getCluster(),
        kerberosDescriptor,
        componentFilter(),
        getIdentityFilter(),
        dataDirectory(),
        calculateConfig(kerberosDescriptor, serviceNames()),
        new HashMap<>(),
        false,
        new HashMap<>());
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }

    private Set<String> serviceNames() {
      return componentFilter().stream().map(Component::getServiceName).collect(toSet());
    }

    private List<Component> componentFilter() {
      Type jsonType = new TypeToken<List<Component>>() {}.getType();
      return StageUtils.getGson().fromJson(getCommandParameterValue(COMPONENT_FILTER), jsonType);
    }

    /**
     * Cleaning identities is asynchronous, it can happen that the service and its configuration is already deleted at this point.
     * We're extending the actual config with the properties of the latest deleted configuration of the service.
     * The service configuration is needed because principal names may contain placeholder variables which are replaced based on the service configuration.
     */
    private Map<String, Map<String, String>> calculateConfig(KerberosDescriptor kerberosDescriptor, Set<String> serviceNames) throws ShpurdpException {
      Map<String, Map<String, String>> actualConfig = getKerberosHelper().calculateConfigurations(getCluster(), null, kerberosDescriptor, false, false, null);
      extendWithDeletedConfigOfService(actualConfig, serviceNames);
      return actualConfig;
    }

    private void extendWithDeletedConfigOfService(Map<String, Map<String, String>> configToBeExtended, Set<String> serviceNames) throws ShpurdpException {
      Set<String> deletedConfigTypes = serviceNames.stream()
        .flatMap(serviceName -> configTypesOfService(serviceName).stream())
        .collect(toSet());
      for (Config deletedConfig : getCluster().getLatestConfigsWithTypes(deletedConfigTypes)) {
        configToBeExtended.put(deletedConfig.getType(), deletedConfig.getProperties());
      }
    }

    private Set<String> configTypesOfService(String serviceName) {
      try {
        StackId stackId = getCluster().getCurrentStackVersion();
        StackServiceRequest stackServiceRequest = new StackServiceRequest(stackId.getStackName(), stackId.getStackVersion(), serviceName);
        return ShpurdpServer.getController().getStackServices(singleton(stackServiceRequest)).stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not find stack service " + serviceName))
          .getConfigTypes()
          .keySet();
      } catch (ShpurdpException e) {
        throw new RuntimeException(e);
      }
    }

    private String dataDirectory() {
      return getCommandParameterValue(getCommandParameters(), DATA_DIRECTORY);
    }

    private KerberosDescriptor getKerberosDescriptor() throws ShpurdpException {
      return getKerberosHelper().getKerberosDescriptor(getCluster(), false);
    }

    @Override
    protected boolean pruneServiceFilter() {
      // Do not prune off services that have been previously removed.
      return false;
    }
  }

  private Stage createNewStage(long id, Cluster cluster, long requestId, String requestContext, String commandParams, String hostParams) {
    Stage stage = stageFactory.createNew(requestId,
      BASE_LOG_DIR + File.pathSeparator + requestId,
      cluster.getClusterName(),
      cluster.getClusterId(),
      requestContext,
      commandParams,
      hostParams);
    stage.setStageId(id);
    return stage;
  }

  private Stage createServerActionStage(long id, Cluster cluster, long requestId,
                                       String requestContext,
                                       String commandParams, String hostParams,
                                       Class<? extends ServerAction> actionClass,
                                       ServiceComponentHostServerActionEvent event,
                                       Map<String, String> commandParameters, String commandDetail,
                                       Integer timeout) throws ShpurdpException {

    Stage stage = createNewStage(id, cluster, requestId, requestContext,  commandParams, hostParams);
    stage.addServerActionCommand(actionClass.getName(), null, Role.SHPURDP_SERVER_ACTION,
      RoleCommand.EXECUTE, cluster.getClusterName(), event, commandParameters, commandDetail,
      shpurdpManagementController.findConfigurationTagsWithOverrides(cluster, null, null), timeout,
      false, false);

    return stage;
  }

  private static class DeleteDataDirAction extends KerberosServerAction {

    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws ShpurdpException, InterruptedException {
      deleteDataDirectory(getCommandParameterValue(DATA_DIRECTORY));
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }

    @Override
    protected boolean pruneServiceFilter() {
      // Do not prune off services that have been previously removed.
      return false;
    }

    @Override
    protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, boolean includedInFilter, Map<String, Object> requestSharedDataContext) throws ShpurdpException {
      return null;
    }
  }
}
