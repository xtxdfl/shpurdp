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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.shpurdp.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.Host;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.server.state.ValueAttributesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShpurdpServerStackAdvisorAwareConfigurationHandler handles Shpurdp server specific configuration properties using the stack advisor.
 */
class ShpurdpServerStackAdvisorAwareConfigurationHandler extends ShpurdpServerConfigurationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShpurdpServerStackAdvisorAwareConfigurationHandler.class);

  private final Clusters clusters;
  private final ConfigHelper configHelper;
  private final ShpurdpManagementController managementController;
  private final StackAdvisorHelper stackAdvisorHelper;

  ShpurdpServerStackAdvisorAwareConfigurationHandler(ShpurdpConfigurationDAO shpurdpConfigurationDAO, ShpurdpEventPublisher publisher,
      Clusters clusters, ConfigHelper configHelper, ShpurdpManagementController managementController, StackAdvisorHelper stackAdvisorHelper) {
    super(shpurdpConfigurationDAO, publisher);
    this.clusters = clusters;
    this.configHelper = configHelper;
    this.managementController = managementController;
    this.stackAdvisorHelper = stackAdvisorHelper;
  }
  
  /**
   * Build the stack advisor request, call the stack advisor, then automatically
   * handle the recommendations on all clusters that managed by Shpurdp.
   * 
   * @param stackAdvisorRequestType
   *          the stack advisor request type
   */
  protected void processClusters(StackAdvisorRequest.StackAdvisorRequestType stackAdvisorRequestType) {
    final Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null) {
      for (Cluster cluster : clusterMap.values()) {
        try {
          LOGGER.info("Managing the {} configuration for the cluster named '{}'", stackAdvisorRequestType.toString(), cluster.getClusterName());
          processCluster(cluster, stackAdvisorRequestType);
        } catch (ShpurdpException | StackAdvisorException e) {
          LOGGER.warn("Failed to update the {} for the cluster named '{}': ", stackAdvisorRequestType.toString(), cluster.getClusterName(), e);
        }
      }
    }
  }

  /**
   * Build the stack advisor request, call the stack advisor, then automatically
   * handle the recommendations.
   * <p>
   * Any recommendation coming back from the Stack/service advisor is expected to
   * be only related configurations of the given request type (LDAP/SSO)
   * <p>
   * If there are no changes to the current configurations, no new configuration
   * versions will be created.
   *
   * @param cluster
   *          the cluster to process
   * @param stackAdvisorRequestType
   *          the stack advisor request type
   * @throws ShpurdpException
   * @throws StackAdvisorException
   */
  protected void processCluster(Cluster cluster, StackAdvisorRequest.StackAdvisorRequestType stackAdvisorRequestType) throws ShpurdpException, StackAdvisorException {
    final StackId stackVersion = cluster.getCurrentStackVersion();
    final List<String> hosts = cluster.getHosts().stream().map(Host::getHostName).collect(Collectors.toList());
    final Set<String> serviceNames = cluster.getServices().values().stream().map(Service::getName).collect(Collectors.toSet());

    final StackAdvisorRequest request = StackAdvisorRequest.StackAdvisorRequestBuilder
        .forStack(stackVersion.getStackName(), stackVersion.getStackVersion())
        .ofType(stackAdvisorRequestType)
        .forHosts(hosts)
        .forServices(serviceNames)
        .withComponentHostsMap(cluster.getServiceComponentHostMap(null, null))
        .withConfigurations(calculateExistingConfigurations(cluster))
        .build();

    // Execute the stack advisor
    final RecommendationResponse response = stackAdvisorHelper.recommend(request);

    // Process the recommendations and automatically apply them. Ideally this is what the user wanted
    final RecommendationResponse.Recommendation recommendation = (response == null) ? null : response.getRecommendations();
    final RecommendationResponse.Blueprint blueprint = (recommendation == null) ? null : recommendation.getBlueprint();
    final Map<String, RecommendationResponse.BlueprintConfigurations> configurations = (blueprint == null) ? null : blueprint.getConfigurations();

    if (configurations != null) {
      for (Map.Entry<String, RecommendationResponse.BlueprintConfigurations> configuration : configurations.entrySet()) {
        processConfigurationType(cluster, configuration.getKey(), configuration.getValue());
      }
    }
  }

  /**
   * Process the configuration to add, update, and remove properties as needed.
   *
   * @param cluster
   *          the cluster
   * @param configType
   *          the configuration type
   * @param configurations
   *          the recommended configuration values
   * @throws ShpurdpException
   */
  private void processConfigurationType(Cluster cluster, String configType, RecommendationResponse.BlueprintConfigurations configurations) throws ShpurdpException {
    Map<String, String> updates = new HashMap<>();
    Collection<String> removals = new HashSet<>();

    // Gather the updates
    Map<String, String> recommendedConfigProperties = configurations.getProperties();
    if (recommendedConfigProperties != null) {
      updates.putAll(recommendedConfigProperties);
    }

    // Determine if any properties need to be removed
    Map<String, ValueAttributesInfo> recommendedConfigPropertyAttributes = configurations.getPropertyAttributes();
    if (recommendedConfigPropertyAttributes != null) {
      for (Map.Entry<String, ValueAttributesInfo> entry : recommendedConfigPropertyAttributes.entrySet()) {
        ValueAttributesInfo info = entry.getValue();

        if ((info != null) && "true".equalsIgnoreCase(info.getDelete())) {
          updates.remove(entry.getKey());
          removals.add(entry.getKey());
        }
      }
    }

    configHelper.updateConfigType(cluster, cluster.getCurrentStackVersion(), managementController, configType, updates, removals, "internal",
        getServiceVersionNote());
  }
  
  protected String getServiceVersionNote() {
    return "Shpurdp-managed configuration change";
  }

  /**
   * Calculate the current configurations for all services
   *
   * @param cluster
   *          the cluster
   * @return a map of services and their configurations
   * @throws ShpurdpException
   */
  private Map<String, Map<String, Map<String, String>>> calculateExistingConfigurations(Cluster cluster) throws ShpurdpException {
    Map<String, Map<String, String>> configurationTags = configHelper.getEffectiveDesiredTags(cluster, null);
    Map<String, Map<String, String>> effectiveConfigs = configHelper.getEffectiveConfigProperties(cluster, configurationTags);

    Map<String, Map<String, Map<String, String>>> requestConfigurations = new HashMap<>();
    if (effectiveConfigs != null) {
      for (Map.Entry<String, Map<String, String>> configuration : effectiveConfigs.entrySet()) {
        Map<String, Map<String, String>> properties = new HashMap<>();
        String configType = configuration.getKey();
        Map<String, String> configurationProperties = new HashMap<>(configuration.getValue());

        properties.put("properties", configurationProperties);
        requestConfigurations.put(configType, properties);
      }
    }

    return requestConfigurations;
  }

}
