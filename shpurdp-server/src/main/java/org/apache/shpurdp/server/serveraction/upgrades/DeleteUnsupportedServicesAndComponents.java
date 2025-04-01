/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.shpurdp.server.serveraction.upgrades;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.union;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import org.apache.shpurdp.annotations.Experimental;
import org.apache.shpurdp.annotations.ExperimentalFeature;
import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.agent.CommandReport;
import org.apache.shpurdp.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.server.orm.entities.UpgradeEntity;
import org.apache.shpurdp.server.orm.entities.UpgradeHistoryEntity;
import org.apache.shpurdp.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.apache.shpurdp.server.state.ServiceComponentSupport;
import org.apache.shpurdp.server.topology.STOMPComponentsDeleteHandler;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
 * Upgrade Server Action that deletes the components and services which are no
 * longer supported in the target stack. The deletable component or service
 * should be in deletable state (stopped) before executing this.
 * <p/>
 * This will orphan Kerberos keytabs and identities that belonged to the removed
 * services and components. Since removing these automatically is a new and
 * untested feature, its best to leave this type of cleanup to a future
 * implementation.
 */
@Experimental(
    feature = ExperimentalFeature.ORPHAN_KERBEROS_IDENTITY_REMOVAL,
    comment = "Not removing identities yet")
public class DeleteUnsupportedServicesAndComponents extends AbstractUpgradeServerAction {
  @Inject
  private ServiceComponentSupport serviceComponentSupport;

  @Inject
  private STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws ShpurdpException, InterruptedException {
    Cluster cluster = getClusters().getCluster(getExecutionCommand().getClusterName());
    if (cluster.getUpgradeInProgress().isDowngradeAllowed()) {
      throw new ShpurdpException(this.getClass() + " should not be used in upgrade packs with downgrade support");
    }
    UpgradeContext upgradeContext = getUpgradeContext(cluster);
    Set<String> removedComponents = deleteUnsupportedComponents(cluster, upgradeContext.getRepositoryVersion());
    Set<String> removedServices = deleteUnsupportedServices(cluster, upgradeContext.getRepositoryVersion());
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
      "Removed services: " + StringUtils.join(union(removedComponents, removedServices), ", "), "");
  }

  private Set<String> deleteUnsupportedServices(Cluster cluster, RepositoryVersionEntity repoVersion) throws ShpurdpException {
    Set<String> servicesToBeRemoved = serviceComponentSupport.unsupportedServices(cluster, repoVersion.getStackName(), repoVersion.getStackVersion());
    for (String serviceName : servicesToBeRemoved) {
      DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();
      cluster.deleteService(serviceName, deleteMetaData);
      STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
      STOMPComponentsDeleteHandler.processDeleteByMetaData(deleteMetaData);
      deleteUpgradeHistory(cluster, history -> serviceName.equals(history.getServiceName()));
    }
    return servicesToBeRemoved;
  }

  private Set<String> deleteUnsupportedComponents(Cluster cluster, RepositoryVersionEntity repoVersion) throws ShpurdpException {
    Set<String> deletedComponents = new HashSet<>();
    for (ServiceComponent component : serviceComponentSupport.unsupportedComponents(cluster, repoVersion.getStackName(), repoVersion.getStackVersion())) {
      DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();
      cluster.getService(component.getServiceName()).deleteServiceComponent(component.getName(), deleteMetaData);
      STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
      STOMPComponentsDeleteHandler.processDeleteByMetaData(deleteMetaData);
      deleteUpgradeHistory(cluster, history -> component.getName().equals(history.getComponentName()));
      deletedComponents.add(component.getName());
    }
    return deletedComponents;
  }

  private void deleteUpgradeHistory(Cluster cluster, Predicate<UpgradeHistoryEntity> predicate) {
    UpgradeEntity upgradeInProgress = cluster.getUpgradeInProgress();
    List<UpgradeHistoryEntity> removed = upgradeInProgress.getHistory().stream()
      .filter(each -> each != null && predicate.test(each))
      .collect(toList());
    upgradeInProgress.removeHistories(removed);
    m_upgradeDAO.merge(upgradeInProgress);
  }
}
