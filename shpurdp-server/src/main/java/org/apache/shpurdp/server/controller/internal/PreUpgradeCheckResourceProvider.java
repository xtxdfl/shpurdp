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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.StaticallyInject;
import org.apache.shpurdp.server.checks.UpgradeCheckRegistry;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.internal.URLStreamProvider.ShpurdpHttpUrlConnectionProvider;
import org.apache.shpurdp.server.controller.spi.NoSuchParentResourceException;
import org.apache.shpurdp.server.controller.spi.NoSuchResourceException;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.Resource.Type;
import org.apache.shpurdp.server.controller.spi.SystemException;
import org.apache.shpurdp.server.controller.spi.UnsupportedPropertyException;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.shpurdp.server.orm.dao.RepositoryVersionDAO;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.stack.upgrade.Direction;
import org.apache.shpurdp.server.stack.upgrade.UpgradePack;
import org.apache.shpurdp.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.shpurdp.server.state.CheckHelper;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.StackId;
import org.apache.shpurdp.spi.ClusterInformation;
import org.apache.shpurdp.spi.RepositoryVersion;
import org.apache.shpurdp.spi.upgrade.UpgradeCheck;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckRequest;
import org.apache.shpurdp.spi.upgrade.UpgradeCheckResult;
import org.apache.shpurdp.spi.upgrade.UpgradeType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Resource provider for pre-upgrade checks.
 */
@StaticallyInject
public class PreUpgradeCheckResourceProvider extends ReadOnlyResourceProvider {
  private static final Logger LOG = LoggerFactory.getLogger(PreUpgradeCheckResourceProvider.class);

  //----- Property ID constants ---------------------------------------------

  public static final String UPGRADE_CHECK_ID_PROPERTY_ID                  = PropertyHelper.getPropertyId("UpgradeChecks", "id");
  public static final String UPGRADE_CHECK_CHECK_PROPERTY_ID               = PropertyHelper.getPropertyId("UpgradeChecks", "check");
  public static final String UPGRADE_CHECK_STATUS_PROPERTY_ID              = PropertyHelper.getPropertyId("UpgradeChecks", "status");
  public static final String UPGRADE_CHECK_REASON_PROPERTY_ID              = PropertyHelper.getPropertyId("UpgradeChecks", "reason");
  public static final String UPGRADE_CHECK_FAILED_ON_PROPERTY_ID           = PropertyHelper.getPropertyId("UpgradeChecks", "failed_on");
  public static final String UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "failed_detail");
  public static final String UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID          = PropertyHelper.getPropertyId("UpgradeChecks", "check_type");
  public static final String UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID        = PropertyHelper.getPropertyId("UpgradeChecks", "cluster_name");
  public static final String UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID        = PropertyHelper.getPropertyId("UpgradeChecks", "upgrade_type");
  public static final String UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID = PropertyHelper.getPropertyId("UpgradeChecks", "repository_version_id");
  public static final String UPGRADE_CHECK_TARGET_REPOSITORY_VERSION       = PropertyHelper.getPropertyId("UpgradeChecks", "repository_version");

  /**
   * Optional parameter to specify the preferred Upgrade Pack to use.
   */
  public static final String UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "upgrade_pack");
  public static final String UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("UpgradeChecks", "repository_version");
  public static final String UPGRADE_CHECK_FOR_REVERT_PROPERTY_ID = PropertyHelper.getPropertyId("UpgradeChecks", "for_revert");

  @Inject
  private static Provider<Clusters> clustersProvider;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  /**
   * Used a {@link Provider} around this instance to force lazy loading so it
   * doesn't hold up Shpurdp's startup process.
   */
  @Inject
  private static Provider<UpgradeCheckRegistry> upgradeCheckRegistryProvider;

  @Inject
  private static Provider<UpgradeHelper> upgradeHelper;

  @Inject
  private static Provider<Configuration> config;

  @Inject
  private static CheckHelper checkHelper;

  private static final Set<String> pkPropertyIds = Collections.singleton(UPGRADE_CHECK_ID_PROPERTY_ID);

  public static final Set<String> propertyIds = ImmutableSet.of(
      UPGRADE_CHECK_ID_PROPERTY_ID,
      UPGRADE_CHECK_CHECK_PROPERTY_ID,
      UPGRADE_CHECK_STATUS_PROPERTY_ID,
      UPGRADE_CHECK_REASON_PROPERTY_ID,
      UPGRADE_CHECK_FAILED_ON_PROPERTY_ID,
      UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID,
      UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID,
      UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID,
      UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID,
      UPGRADE_CHECK_FOR_REVERT_PROPERTY_ID,
      UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID,
      UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID);


  @SuppressWarnings("serial")
  public static final Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String>builder()
    .put(Type.PreUpgradeCheck, UPGRADE_CHECK_ID_PROPERTY_ID)
    .put(Type.Cluster, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID)
    .build();

  /**
   * Constructor.
   *
   * @param managementController management controller
   */
  public PreUpgradeCheckResourceProvider(ShpurdpManagementController managementController) {
    super(Type.PreUpgradeCheck, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String clusterName = propertyMap.get(UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).toString();

      UpgradeType upgradeType = UpgradeType.ROLLING;
      if (propertyMap.containsKey(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID)) {
        try {
          upgradeType = UpgradeType.valueOf(propertyMap.get(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).toString());
        } catch(Exception e){
          throw new SystemException(String.format("Property %s has an incorrect value of %s.", UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID, propertyMap.get(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID)));
        }
      }

      final Cluster cluster;

      try {
        cluster = clustersProvider.get().getCluster(clusterName);
      } catch (ShpurdpException shpurdpException) {
        throw new NoSuchResourceException(shpurdpException.getMessage());
      }

      StackId sourceStackId = cluster.getCurrentStackVersion();

      String repositoryVersionId = (String) propertyMap.get(
          UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID);

      if (StringUtils.isBlank(repositoryVersionId)) {
        throw new SystemException(
            String.format("%s is a required property when executing upgrade checks",
                UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID));
      }

      RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(
          Long.valueOf(repositoryVersionId));

      //shpurdpMetaInfo.getStack(stackName, cluster.getCurrentStackVersion().getStackVersion()).getUpgradePacks()
      // TODO SHPURDP-12698, filter the upgrade checks to run based on the stack and upgrade type, or the upgrade pack.
      UpgradePack upgradePack = null;
      String preferredUpgradePackName = propertyMap.containsKey(UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID) ?
          (String) propertyMap.get(UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID) : null;
      try{
        // Hint: PreChecks currently executing only before UPGRADE direction
        upgradePack = upgradeHelper.get().suggestUpgradePack(clusterName, sourceStackId,
            repositoryVersion.getStackId(), Direction.UPGRADE, upgradeType,
            preferredUpgradePackName);
      } catch (ShpurdpException e) {
        throw new SystemException(e.getMessage(), e);
      }

      if (upgradePack == null) {
        throw new SystemException(
            String.format("Upgrade pack not found for the target repository version %s",
                repositoryVersion));
      }

      ClusterInformation clusterInformation = cluster.buildClusterInformation();

      StackId stackId = repositoryVersion.getStackId();
      RepositoryVersion targetRepositoryVersion = new RepositoryVersion(repositoryVersion.getId(),
          stackId.getStackName(), stackId.getStackVersion(), stackId.getStackId(),
          repositoryVersion.getVersion(), repositoryVersion.getType());

      final UpgradeCheckRequest upgradeCheckRequest = new UpgradeCheckRequest(clusterInformation,
          upgradeType, targetRepositoryVersion,
          upgradePack.getPrerequisiteCheckConfig().getAllProperties(),
          new ShpurdpHttpUrlConnectionProvider());

      if (propertyMap.containsKey(UPGRADE_CHECK_FOR_REVERT_PROPERTY_ID)) {
        Boolean forRevert = BooleanUtils.toBooleanObject(propertyMap.get(UPGRADE_CHECK_FOR_REVERT_PROPERTY_ID).toString());
        upgradeCheckRequest.setRevert(forRevert);
      }

      UpgradeCheckRegistry upgradeCheckRegistry = upgradeCheckRegistryProvider.get();

      // ToDo: properly handle exceptions, i.e. create fake check with error description
      final List<UpgradeCheck> upgradeChecksToRun;
      try {
        upgradeChecksToRun = upgradeCheckRegistry.getFilteredUpgradeChecks(upgradePack);
      } catch (ShpurdpException shpurdpException) {
        throw new SystemException("Unable to load upgrade checks", shpurdpException);
      }

      List<UpgradeCheckResult> results = checkHelper.performChecks(upgradeCheckRequest,
          upgradeChecksToRun, config.get());

      for (UpgradeCheckResult prerequisiteCheck : results) {
        final Resource resource = new ResourceImpl(Resource.Type.PreUpgradeCheck);
        setResourceProperty(resource, UPGRADE_CHECK_ID_PROPERTY_ID, prerequisiteCheck.getId(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_PROPERTY_ID, prerequisiteCheck.getDescription(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_STATUS_PROPERTY_ID, prerequisiteCheck.getStatus(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_REASON_PROPERTY_ID, prerequisiteCheck.getFailReason(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_FAILED_ON_PROPERTY_ID, prerequisiteCheck.getFailedOn(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID,prerequisiteCheck.getFailedDetail(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID, prerequisiteCheck.getType(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID, cluster.getClusterName(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID, upgradeType, requestedIds);

        setResourceProperty(resource, UPGRADE_CHECK_TARGET_REPOSITORY_VERSION_ID_ID, repositoryVersion.getId(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_TARGET_REPOSITORY_VERSION, repositoryVersion.getVersion(), requestedIds);

        resources.add(resource);
      }
    }
    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
