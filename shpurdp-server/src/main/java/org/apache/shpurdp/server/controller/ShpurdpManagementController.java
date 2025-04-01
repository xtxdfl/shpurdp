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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.RoleCommand;
import org.apache.shpurdp.server.actionmanager.ActionManager;
import org.apache.shpurdp.server.agent.ExecutionCommand;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.api.services.LoggingService;
import org.apache.shpurdp.server.controller.internal.DeleteStatusMetaData;
import org.apache.shpurdp.server.controller.internal.RequestStageContainer;
import org.apache.shpurdp.server.controller.logging.LoggingSearchPropertyProvider;
import org.apache.shpurdp.server.controller.metrics.MetricPropertyProviderFactory;
import org.apache.shpurdp.server.controller.metrics.MetricsCollectorHAManager;
import org.apache.shpurdp.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.shpurdp.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.shpurdp.server.events.ShpurdpEvent;
import org.apache.shpurdp.server.events.MetadataUpdateEvent;
import org.apache.shpurdp.server.events.TopologyUpdateEvent;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.metadata.RoleCommandOrder;
import org.apache.shpurdp.server.orm.entities.ExtensionLinkEntity;
import org.apache.shpurdp.server.orm.entities.MpackEntity;
import org.apache.shpurdp.server.orm.entities.StackEntity;
import org.apache.shpurdp.server.scheduler.ExecutionScheduleManager;
import org.apache.shpurdp.server.security.authorization.AuthorizationException;
import org.apache.shpurdp.server.security.encryption.CredentialStoreService;
import org.apache.shpurdp.server.security.ldap.LdapBatchDto;
import org.apache.shpurdp.server.security.ldap.LdapSyncDto;
import org.apache.shpurdp.server.stageplanner.RoleGraphFactory;
import org.apache.shpurdp.server.state.BlueprintProvisioningState;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.Config;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.DesiredConfig;
import org.apache.shpurdp.server.state.HostState;
import org.apache.shpurdp.server.state.MaintenanceState;
import org.apache.shpurdp.server.state.Module;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.apache.shpurdp.server.state.ServiceComponentFactory;
import org.apache.shpurdp.server.state.ServiceComponentHost;
import org.apache.shpurdp.server.state.ServiceInfo;
import org.apache.shpurdp.server.state.ServiceOsSpecific;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.State;
import org.apache.shpurdp.server.state.configgroup.ConfigGroupFactory;
import org.apache.shpurdp.server.state.quicklinksprofile.QuickLinkVisibilityController;
import org.apache.shpurdp.server.state.scheduler.RequestExecutionFactory;

/**
 * Management controller interface.
 */
public interface ShpurdpManagementController {

  /**
   * Get an Shpurdp endpoint URI for the given path.
   *
   * @param path  the path (e.g. /api/v1/users)
   *
   * @return the Shpurdp endpoint URI
   */
  String getShpurdpServerURI(String path);


  // ----- Create -----------------------------------------------------------

  /**
   * Create the cluster defined by the attributes in the given request object.
   *
   * @param request  the request object which defines the cluster to be created
   *
   * @throws ShpurdpException thrown if the cluster cannot be created
   * @throws AuthorizationException thrown if the authenticated user is not authorized to perform this operation
   */
  void createCluster(ClusterRequest request) throws ShpurdpException, AuthorizationException;

  /**
   * Create the host component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host component to be created
   *
   * @throws ShpurdpException thrown if the host component cannot be created
   */
  void createHostComponents(
      Set<ServiceComponentHostRequest> requests) throws ShpurdpException, AuthorizationException;

  /**
   * Create the host component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host component to be created
   *
   * @param isBlueprintProvisioned  means host components will be created during blueprint deploy
   *
   * @throws ShpurdpException thrown if the host component cannot be created
   */
  void createHostComponents(
      Set<ServiceComponentHostRequest> requests, boolean isBlueprintProvisioned) throws ShpurdpException, AuthorizationException;

  /**
   * Creates a configuration.
   *
   * @param request the request object which defines the configuration.
   * @param refreshCluster should the cluster entity be refreshed from DB
   * @throws ShpurdpException when the configuration cannot be created.
   * @throws AuthorizationException when user is not authorized to perform operation.
   */
  ConfigurationResponse createConfiguration(ConfigurationRequest request, boolean refreshCluster)
      throws ShpurdpException, AuthorizationException;

  /**
   * Creates a configuration.
   *
   * @param request the request object which defines the configuration.
   *
   * @throws ShpurdpException when the configuration cannot be created.
   */
  ConfigurationResponse createConfiguration(ConfigurationRequest request)
      throws ShpurdpException, AuthorizationException;

  /**
   * Create cluster config
   * TODO move this method to Cluster? doesn't seem to be on its place
   * @return config created
   */
  Config createConfig(Cluster cluster, StackId stackId, String type, Map<String, String> properties,
                      String versionTag, Map<String, Map<String, String>> propertiesAttributes, boolean refreshCluster);

  /**
   * Create cluster config
   * @return config created
   */
  Config createConfig(Cluster cluster, StackId stackId, String type, Map<String, String> properties,
                      String versionTag, Map<String, Map<String, String>> propertiesAttributes);

  /**
   * Creates groups.
   *
   * @param requests the request objects which define the groups.
   *
   * @throws ShpurdpException when the groups cannot be created.
   */
  void createGroups(Set<GroupRequest> requests) throws ShpurdpException;

  /**
   * Creates members of the group.
   *
   * @param requests the request objects which define the members.
   *
   * @throws ShpurdpException when the members cannot be created.
   */
  void createMembers(Set<MemberRequest> requests) throws ShpurdpException;

  /**
   * Register the mpack defined by the attributes in the given request object.
   *
   * @param request the request object which defines the mpack to be created
   * @throws ShpurdpException        thrown if the mpack cannot be created
   * @throws AuthorizationException thrown if the authenticated user is not authorized to perform this operation
   */
  MpackResponse registerMpack(MpackRequest request) throws IOException, AuthorizationException, ResourceAlreadyExistsException;


  // ----- Read -------------------------------------------------------------

  /**
   * Get the clusters identified by the given request objects.
   *
   * @param requests  the request objects which identify the clusters to be returned
   *
   * @return a set of cluster responses
   *
   * @throws ShpurdpException thrown if the resource cannot be read
   * @throws AuthorizationException thrown if the authenticated user is not authorized to perform this operation
   */
  Set<ClusterResponse> getClusters(Set<ClusterRequest> requests)
      throws ShpurdpException, AuthorizationException;

  /**
   * Get the host components identified by the given request objects.
   *
   * @param requests  the request objects which identify the host components
   * to be returned
   *
   * @return a set of host component responses
   *
   * @throws ShpurdpException thrown if the resource cannot be read
   */
  Set<ServiceComponentHostResponse> getHostComponents(
      Set<ServiceComponentHostRequest> requests) throws ShpurdpException;

  Set<ServiceComponentHostResponse> getHostComponents(
      Set<ServiceComponentHostRequest> requests, boolean statusOnly) throws ShpurdpException;

  /**
   * Gets the configurations identified by the given request objects.
   *
   * @param requests   the request objects
   *
   * @return  a set of configuration responses
   *
   * @throws ShpurdpException if the configurations could not be read
   */
  Set<ConfigurationResponse> getConfigurations(
      Set<ConfigurationRequest> requests) throws ShpurdpException;

  /**
   * Get service config version history
   * @param requests service config version requests
   * @return service config versions
   * @throws ShpurdpException
   */
  Set<ServiceConfigVersionResponse> getServiceConfigVersions(Set<ServiceConfigVersionRequest> requests)
      throws ShpurdpException;

  /**
   * Gets the user groups identified by the given request objects.
   *
   * @param requests the request objects
   *
   * @return a set of group responses
   *
   * @throws ShpurdpException if the groups could not be read
   */
  Set<GroupResponse> getGroups(Set<GroupRequest> requests)
      throws ShpurdpException;

  /**
   * Gets the group members identified by the given request objects.
   *
   * @param requests the request objects
   *
   * @return a set of member responses
   *
   * @throws ShpurdpException if the members could not be read
   */
  Set<MemberResponse> getMembers(Set<MemberRequest> requests)
      throws ShpurdpException;


  // ----- Update -----------------------------------------------------------

  /**
   * Update the cluster identified by the given request object with the
   * values carried by the given request object.
   *
   *
   * @param requests          request objects which define which cluster to
   *                          update and the values to set
   * @param requestProperties request specific properties independent of resource
   *
   * @return a track action response
   *
   * @throws ShpurdpException thrown if the resource cannot be updated
   * @throws AuthorizationException thrown if the authenticated user is not authorized to perform this operation
   */
  RequestStatusResponse updateClusters(Set<ClusterRequest> requests,
                                              Map<String, String> requestProperties)
      throws ShpurdpException, AuthorizationException;

  /**
   * Update the cluster identified by the given request object with the
   * values carried by the given request object.
   *
   *
   * @param requests          request objects which define which cluster to
   *                          update and the values to set
   * @param requestProperties request specific properties independent of resource
   *
   * @param fireAgentUpdates  should agent updates (configurations, metadata etc.) be fired inside
   *
   * @param refreshCluster  refreshes cluster entity after cluster configs update
   *
   * @return a track action response
   *
   * @throws ShpurdpException thrown if the resource cannot be updated
   * @throws AuthorizationException thrown if the authenticated user is not authorized to perform this operation
   */
  RequestStatusResponse updateClusters(Set<ClusterRequest> requests, Map<String, String> requestProperties,
                                       boolean fireAgentUpdates, boolean refreshCluster)
      throws ShpurdpException, AuthorizationException;

  /**
   * Updates the groups specified.
   *
   * @param requests the groups to modify
   *
   * @throws ShpurdpException if the resources cannot be updated
   */
  void updateGroups(Set<GroupRequest> requests) throws ShpurdpException;

  /**
   * Updates the members of the group specified.
   *
   * @param requests the members to be set for this group
   *
   * @throws ShpurdpException if the resources cannot be updated
   */
  void updateMembers(Set<MemberRequest> requests) throws ShpurdpException;


  // ----- Delete -----------------------------------------------------------

  /**
   * Delete the cluster identified by the given request object.
   *
   * @param request  the request object which identifies which cluster to delete
   *
   * @throws ShpurdpException thrown if the resource cannot be deleted
   */
  void deleteCluster(ClusterRequest request) throws ShpurdpException;

  /**
   * Delete the host component identified by the given request object.
   *
   * @param requests  the request object which identifies which host component to delete
   *
   * @return a track action response
   *
   * @throws ShpurdpException thrown if the resource cannot be deleted
   */
  DeleteStatusMetaData deleteHostComponents(
      Set<ServiceComponentHostRequest> requests) throws ShpurdpException, AuthorizationException;

  /**
   * Deletes the user groups specified.
   *
   * @param requests the groups to delete
   *
   * @throws ShpurdpException if the resources cannot be deleted
   */
  void deleteGroups(Set<GroupRequest> requests) throws ShpurdpException;

  /**
   * Deletes the group members specified.
   *
   * @param requests the members to delete
   *
   * @throws ShpurdpException if the resources cannot be deleted
   */
  void deleteMembers(Set<MemberRequest> requests) throws ShpurdpException;

  /**
   * Create the action defined by the attributes in the given request object.
   * Used only for custom commands/actions.
   *
   * @param actionRequest the request object which defines the action to be created
   * @param requestProperties the request properties
   *
   * @throws ShpurdpException thrown if the action cannot be created
   */
  RequestStatusResponse createAction(ExecuteActionRequest actionRequest, Map<String, String> requestProperties)
      throws ShpurdpException;

  /**
   * Get supported stacks.
   *
   * @param requests the stacks
   *
   * @return a set of stacks responses
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<StackResponse> getStacks(Set<StackRequest> requests) throws ShpurdpException;

  /**
   * Update stacks from the files at stackRoot.
   *
   * @return a track action response
   * @throws ShpurdpException if
   */
  RequestStatusResponse updateStacks() throws ShpurdpException;

  /**
   * Create a link between an extension and a stack
   *
   * @throws ShpurdpException if we fail to link the extension to the stack
   */
  void createExtensionLink(ExtensionLinkRequest request) throws ShpurdpException;

  /**
   * Update a link - switch the link's extension version while keeping the same stack version and extension name
   *
   * @throws ShpurdpException if we fail to link the extension to the stack
   */
  void updateExtensionLink(ExtensionLinkRequest request) throws ShpurdpException;

  /**
   * Update a link - switch the link's extension version while keeping the same stack version and extension name
   *
   * @throws ShpurdpException if we fail to link the extension to the stack
   */
  void updateExtensionLink(ExtensionLinkEntity oldLinkEntity, ExtensionLinkRequest newLinkRequest) throws ShpurdpException;

  /**
   * Delete a link between an extension and a stack
   *
   * @throws ShpurdpException if we fail to unlink the extension from the stack
   */
  void deleteExtensionLink(ExtensionLinkRequest request) throws ShpurdpException;

  /**
   * Get supported extensions.
   *
   * @param requests the extensions
   * @return a set of extensions responses
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<ExtensionResponse> getExtensions(Set<ExtensionRequest> requests) throws ShpurdpException;

  /**
   * Get supported extension versions.
   *
   * @param requests the extension versions
   * @return a set of extension versions responses
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<ExtensionVersionResponse> getExtensionVersions(Set<ExtensionVersionRequest> requests) throws ShpurdpException;

  /**
   * Get supported stacks versions.
   *
   * @param requests the stacks versions
   *
   * @return a set of stacks versions responses
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<StackVersionResponse> getStackVersions(Set<StackVersionRequest> requests) throws ShpurdpException;

  /**
   * Get repositories by stack name, version and operating system.
   *
   * @param requests the repositories
   *
   * @return a set of repositories
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<RepositoryResponse> getRepositories(Set<RepositoryRequest> requests) throws ShpurdpException;

  /**
   * Verifies repositories' base urls.
   *
   * @param requests the repositories
   *
   * @throws ShpurdpException if verification of any of urls fails
   */
  void verifyRepositories(Set<RepositoryRequest> requests) throws ShpurdpException;

  /**
   * Get repositories by stack name, version.
   *
   * @param requests the services
   *
   * @return a set of services
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<StackServiceResponse> getStackServices(Set<StackServiceRequest> requests) throws ShpurdpException;


  /**
   * Get configurations by stack name, version and service.
   *
   * @param requests the configurations
   *
   * @return a set of configurations
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<StackConfigurationResponse> getStackConfigurations(Set<StackConfigurationRequest> requests) throws ShpurdpException;


  /**
   * Get components by stack name, version and service.
   *
   * @param requests the components
   *
   * @return a set of components
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<StackServiceComponentResponse> getStackComponents(Set<StackServiceComponentRequest> requests) throws ShpurdpException;

  /**
   * Get operating systems by stack name, version.
   *
   * @param requests the operating systems
   *
   * @return a set of operating systems
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<OperatingSystemResponse> getOperatingSystems(Set<OperatingSystemRequest> requests) throws ShpurdpException;

  /**
   * Get all top-level services of Shpurdp, not related to certain cluster.
   *
   * @param requests the top-level services
   *
   * @return a set of top-level services
   *
   * @throws  ShpurdpException if the resources cannot be read
   */

  Set<RootServiceResponse> getRootServices(Set<RootServiceRequest> requests) throws ShpurdpException;
  /**
   * Get all components of top-level services of Shpurdp, not related to certain cluster.
   *
   * @param requests the components of top-level services
   *
   * @return a set of components
   *
   * @throws  ShpurdpException if the resources cannot be read
   */
  Set<RootServiceComponentResponse> getRootServiceComponents(Set<RootServiceComponentRequest> requests) throws ShpurdpException;


  // ----- Common utility methods --------------------------------------------

  /**
   * Get service name by cluster instance and component name
   *
   * @param cluster the cluster instance
   * @param componentName the component name in String type
   *
   * @return a service name
   *
   * @throws  ShpurdpException if service name is null or empty
   */
  String findServiceName(Cluster cluster, String componentName) throws ShpurdpException;

  /**
   * Get the clusters for this management controller.
   *
   * @return the clusters
   */
  Clusters getClusters();

  /**
   * Get config helper
   *
   * @return config helper
   */
  ConfigHelper getConfigHelper();

  /**
   * Get the meta info for this management controller.
   *
   * @return the meta info
   */
  ShpurdpMetaInfo getShpurdpMetaInfo();

  /**
   * Get the service component factory for this management controller.
   *
   * @return the service component factory
   */
  ServiceComponentFactory getServiceComponentFactory();

  /**
   * Get the root service response factory for this management controller.
   *
   * @return the root service response factory
   */
  AbstractRootServiceResponseFactory getRootServiceResponseFactory();

  /**
   * Get the config group factory for this management controller.
   *
   * @return the config group factory
   */
  ConfigGroupFactory getConfigGroupFactory();

  /**
   * Get the role graph factory for this management controller.
   *
   * @return the role graph factory
   */
  RoleGraphFactory getRoleGraphFactory();

  /**
    * Get the action manager for this management controller.
    *
    * @return the action manager
    */
  ActionManager getActionManager();

  /**
   * Get the authenticated user's name.
   *
   * @return the authenticated user's name
   */
  String getAuthName();

  /**
   * Get the authenticated user's id.
   *
   * @return the authenticated user's name
   */
  int getAuthId();

  /**
   * Create and persist the request stages and return a response containing the
   * associated request and resulting tasks.
   *
   * @param cluster             the cluster
   * @param requestProperties   the request properties
   * @param requestParameters   the request parameters; may be null
   * @param changedServices     the services being changed; may be null
   * @param changedComponents   the components being changed
   * @param changedHosts        the hosts being changed
   * @param ignoredHosts        the hosts to be ignored
   * @param runSmokeTest        indicates whether or not the smoke tests should be run
   * @param reconfigureClients  indicates whether or not the clients should be reconfigured
   *
   * @return the request response
   *
   * @throws ShpurdpException is thrown if the stages can not be created
   */
  RequestStatusResponse createAndPersistStages(Cluster cluster, Map<String, String> requestProperties,
                                                Map<String, String> requestParameters,
                                                Map<State, List<Service>> changedServices,
                                                Map<State, List<ServiceComponent>> changedComponents,
                                                Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                                                Collection<ServiceComponentHost> ignoredHosts,
                                                boolean runSmokeTest, boolean reconfigureClients)
                                                throws ShpurdpException;

  /**
   * Add stages to the request.
   *
   * @param requestStages       Stages currently associated with request
   * @param cluster             cluster being acted on
   * @param requestProperties   the request properties
   * @param requestParameters   the request parameters; may be null
   * @param changedServices     the services being changed; may be null
   * @param changedComponents   the components being changed
   * @param changedHosts        the hosts being changed
   * @param ignoredHosts        the hosts to be ignored
   * @param runSmokeTest        indicates whether or not the smoke tests should be run
   * @param reconfigureClients  indicates whether or not the clients should be reconfigured
   *
   * @return request stages
   *
   * @throws ShpurdpException if stages can't be created
   */
  RequestStageContainer addStages(RequestStageContainer requestStages, Cluster cluster, Map<String, String> requestProperties,
                             Map<String, String> requestParameters,
                             Map<State, List<Service>> changedServices,
                             Map<State, List<ServiceComponent>> changedComponents,
                             Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                             Collection<ServiceComponentHost> ignoredHosts,
                             boolean runSmokeTest, boolean reconfigureClients, boolean useGeneratedConfigs) throws ShpurdpException;

  /**
   * Add stages to the request.
   *
   * @param requestStages       Stages currently associated with request
   * @param cluster             cluster being acted on
   * @param requestProperties   the request properties
   * @param requestParameters   the request parameters; may be null
   * @param changedServices     the services being changed; may be null
   * @param changedComponents   the components being changed
   * @param changedHosts        the hosts being changed
   * @param ignoredHosts        the hosts to be ignored
   * @param runSmokeTest        indicates whether or not the smoke tests should be run
   * @param reconfigureClients  indicates whether or not the clients should be reconfigured
   * @param useGeneratedConfigs indicates whether or not the actual configs should be a part of the stage
   * @param useClusterHostInfo  indicates whether or not the cluster topology info  should be a part of the stage
   *
   * @return request stages
   *
   * @throws ShpurdpException if stages can't be created
   */
  RequestStageContainer addStages(RequestStageContainer requestStages, Cluster cluster, Map<String, String> requestProperties,
                             Map<String, String> requestParameters,
                             Map<State, List<Service>> changedServices,
                             Map<State, List<ServiceComponent>> changedComponents,
                             Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                             Collection<ServiceComponentHost> ignoredHosts,
                             boolean runSmokeTest, boolean reconfigureClients, boolean useGeneratedConfigs, boolean useClusterHostInfo) throws ShpurdpException;

  /**
   * Getter for the url of JDK, stored at server resources folder
   */
  String getJdkResourceUrl();

  /**
   * Getter for the java home, stored in shpurdp.properties
   */
  String getJavaHome();

  /**
   * Getter for the java home, stored in shpurdp.properties
   */
  String getShpurdpJavaHome();

  /**
   * Getter for the jdk name, stored in shpurdp.properties
   */
  String getJDKName();

  /**
   * Getter for the jce name, stored in shpurdp.properties
   */
  String getJCEName();

  /**
   * Getter for the name of server database
   */
  String getServerDB();

  /**
   * Getter for the url of Oracle JDBC driver, stored at server resources folder
   */
  String getOjdbcUrl();

  /**
   * Getter for the url of MySQL JDBC driver, stored at server resources folder
   */
  String getMysqljdbcUrl();

  /**
   * Filters hosts to only select healthy ones that are heartbeating.
   * <p/>
   * The host's {@link HostState} is used to determine if a host is healthy.
   *
   * @return a List of healthy hosts, or an empty List if none exist.
   * @throws ShpurdpException
   * @see {@link HostState#HEALTHY}
   */
  List<String> selectHealthyHosts(Set<String> hostList) throws ShpurdpException;

  /**
   * Chooses a healthy host from the list of candidate hosts randomly. If there
   * are no healthy hosts, then this method will return {@code null}.
   * <p/>
   * The host's {@link HostState} is used to determine if a host is healthy.
   *
   * @return a random healthy host, or {@code null}.
   * @throws ShpurdpException
   * @see {@link HostState#HEALTHY}
   */
  String getHealthyHost(Set<String> hostList) throws ShpurdpException;


  /**
   * Find configuration tags with applied overrides
   *
   * @param cluster   the cluster
   * @param hostName  the host name
   *
   * @return the configuration tags
   *
   * @throws ShpurdpException if configuration tags can not be obtained
   */
  Map<String, Map<String,String>> findConfigurationTagsWithOverrides(
          Cluster cluster, String hostName,
          @Nullable Map<String, DesiredConfig> desiredConfigs) throws ShpurdpException;

  /**
   * Returns parameters for RCA database
   *
   * @return the map with parameters for RCA db
   *
   */
  Map<String, String> getRcaParameters();

  /**
   * Get the Factory to create Request schedules
   * @return the request execution factory
   */
  RequestExecutionFactory getRequestExecutionFactory();

  /**
   * Get Execution Schedule Manager
   */
  ExecutionScheduleManager getExecutionScheduleManager();

  /**
   * Get cached clusterUpdateResults, used only for service config versions currently
   * @param clusterRequest
   * @return
   */
  ClusterResponse getClusterUpdateResults(ClusterRequest clusterRequest);

  /**
   * Get JobTracker hostname
   * HDP-1.x is not supported anymore
   */
  @Deprecated
  String getJobTrackerHost(Cluster cluster);

  /**
   * Gets the effective maintenance state for a host component
   * @param sch the service component host
   * @return the maintenance state
   * @throws ShpurdpException
   */
  MaintenanceState getEffectiveMaintenanceState(ServiceComponentHost sch)
      throws ShpurdpException;

  /**
   * Get Role Command Order
   */
  RoleCommandOrder getRoleCommandOrder(Cluster cluster);

  /**
   * Performs a test if LDAP server is reachable.
   *
   * @return true if connection to LDAP was established
   */
  boolean checkLdapConfigured();

  /**
   * Retrieves groups and users from external LDAP.
   *
   * @return ldap sync DTO
   * @throws ShpurdpException if LDAP is configured incorrectly
   */
  LdapSyncDto getLdapSyncInfo() throws ShpurdpException;

  /**
   * Synchronizes local users and groups with given data.
   *
   * @param userRequest  users to be synchronized
   * @param groupRequest groups to be synchronized
   *
   * @return the results of the LDAP synchronization
   *
   * @throws ShpurdpException if synchronization data was invalid
   */
  LdapBatchDto synchronizeLdapUsersAndGroups(
      LdapSyncRequest userRequest, LdapSyncRequest groupRequest) throws ShpurdpException;

  /**
   * Checks if LDAP sync process is running.
   *
   * @return true if LDAP sync is in progress
   */
  boolean isLdapSyncInProgress();

  /**
   * Get configurations which are specific for a cluster (!not a service).
   * @param requests
   * @return
   * @throws ShpurdpException
   */
  Set<StackConfigurationResponse> getStackLevelConfigurations(Set<StackLevelConfigurationRequest> requests) throws ShpurdpException;

  /**
   * @param serviceInfo service info for a given service
   * @param hostParams parameter map. May be changed during method execution
   * @param osFamily os family for host
   * @return a full list of package dependencies for a service that should be
   * installed on a host
   */
  List<ServiceOsSpecific.Package> getPackagesForServiceHost(ServiceInfo serviceInfo,
                                                            Map<String, String> hostParams, String osFamily);

  /**
   * Register a change in rack information for the hosts of the given cluster.
   *
   * @param clusterName  the name of the cluster
   *
   * @throws ShpurdpException if an error occurs during the rack change registration
   */
  void registerRackChange(String clusterName) throws ShpurdpException;

  /**
   * Initialize cluster scoped widgets and widgetLayouts for different stack
   * components.
   *
   * @param cluster @Cluster object
   * @param service @Service object
   */
  void initializeWidgetsAndLayouts(Cluster cluster, Service service) throws ShpurdpException;

  /**
   * Gets an execution command for host component life cycle command
   * @return
   */
  ExecutionCommand getExecutionCommand(Cluster cluster,
                                              ServiceComponentHost scHost,
                                              RoleCommand roleCommand) throws ShpurdpException;

  /**
   * Get configuration dependencies which are specific for a specific service configuration property
   * @param requests
   * @return
   */
  Set<StackConfigurationDependencyResponse> getStackConfigurationDependencies(Set<StackConfigurationDependencyRequest> requests) throws ShpurdpException;

  TimelineMetricCacheProvider getTimelineMetricCacheProvider();

  /**
   * Gets the {@link MetricPropertyProviderFactory} that was injected into this
   * class. This is a terrible pattern.
   *
   * @return the injected {@link MetricPropertyProviderFactory}
   */
  MetricPropertyProviderFactory getMetricPropertyProviderFactory();

  /**
   * Gets the LoggingSearchPropertyProvider instance.
   *
   * @return the injected {@link LoggingSearchPropertyProvider}
   */
  LoggingSearchPropertyProvider getLoggingSearchPropertyProvider();


  /**
   * Gets the LoggingService instance from the dependency injection framework.
   *
   * @param clusterName the cluster name associated with this LoggingService instance
   *
   * @return an instance of LoggingService associated with the specified cluster.
   */
  LoggingService getLoggingService(String clusterName);


  /**
   * Returns KerberosHelper instance
   * @return
   */
  KerberosHelper getKerberosHelper();

  /**
   * Returns the CredentialStoreService implementation associated with this
   * controller
   * @return CredentialStoreService
   */
  CredentialStoreService getCredentialStoreService();

  /**
   * Gets an {@link ShpurdpEventPublisher} which can be used to send and receive
   * {@link ShpurdpEvent}s.
   *
   * @return
   */
  ShpurdpEventPublisher getShpurdpEventPublisher();

  /**
   * Gets an {@link MetricsCollectorHAManager} which can be used to get/add collector host for a cluster
   *
   * @return {@link MetricsCollectorHAManager}
   */
  MetricsCollectorHAManager getMetricsCollectorHAManager();

  /**
   * @return the visibility controller that decides which quicklinks should be visible
   * based on the actual quick links profile. If no profile is set, all links will be shown.
   */
  QuickLinkVisibilityController getQuicklinkVisibilityController();

  ConfigGroupResponse getConfigGroupUpdateResults(ConfigGroupRequest configGroupRequest);

  void saveConfigGroupUpdate(ConfigGroupRequest configGroupRequest, ConfigGroupResponse configGroupResponse);

  MetadataUpdateEvent getClusterMetadataOnConfigsUpdate(Cluster cluster) throws ShpurdpException;

  TopologyUpdateEvent getAddedComponentsTopologyEvent(Set<ServiceComponentHostRequest> requests)
      throws ShpurdpException;

  Map<String, BlueprintProvisioningState> getBlueprintProvisioningStates(Long clusterId, Long hostId) throws ShpurdpException;

  /**
   * Fetch the module info for a given mpack.
   *
   * @param mpackId
   * @return List of modules
   */
  List<Module> getModules(Long mpackId);


  /***
   * Remove Mpack from the mpackMap and stackMap which is used to power the Mpack and Stack APIs.
   * @param mpackEntity
   * @param stackEntity
   * @throws IOException
   */
  void removeMpack(MpackEntity mpackEntity, StackEntity stackEntity) throws IOException;

  /**
   * Creates serviceconfigversions and corresponding new configurations if it is an initial request
   * OR
   * Rollbacks to an existing serviceconfigversion if request specifies.
   * @param requests
   *
   * @return
   *
   * @throws ShpurdpException
   *
   * @throws AuthorizationException
   */
  Set<ServiceConfigVersionResponse> createServiceConfigVersion(Set<ServiceConfigVersionRequest> requests) throws ShpurdpException, AuthorizationException;

  /***
   * Fetch all mpacks
   * @return
   */
  Set<MpackResponse> getMpacks();

  /***
   * Fetch an mpack based on id
   * @param mpackId
   * @return
   */
  MpackResponse getMpack(Long mpackId);
}

