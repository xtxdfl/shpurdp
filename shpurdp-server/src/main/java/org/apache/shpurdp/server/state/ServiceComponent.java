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

package org.apache.shpurdp.server.state;

import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.controller.ServiceComponentResponse;
import org.apache.shpurdp.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;

public interface ServiceComponent {

  String getName();

  /**
   * Get a true or false value specifying
   * if auto start was enabled for this component.
   * @return true or false
   */
  boolean isRecoveryEnabled();

  /**
   * Set a true or false value specifying if this
   * component is to be enabled for auto start or not.
   * @param recoveryEnabled - true or false
   */
  void setRecoveryEnabled(boolean recoveryEnabled);

  String getServiceName();

  String getDisplayName();

  long getClusterId();

  String getClusterName();

  State getDesiredState();

  void setDesiredState(State state);

  /**
   * Gets the desired repository for this service component.
   *
   * @return
   */
  RepositoryVersionEntity getDesiredRepositoryVersion();

  StackId getDesiredStackId();

  String getDesiredVersion();

  void setDesiredRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity);

  /**
   * Refresh Component info due to current stack
   * @throws ShpurdpException
   */
  void updateComponentInfo() throws ShpurdpException;

  Map<String, ServiceComponentHost> getServiceComponentHosts();

  Set<String> getServiceComponentsHosts();

  ServiceComponentHost getServiceComponentHost(String hostname)
      throws ShpurdpException;

  void addServiceComponentHosts(Map<String, ServiceComponentHost>
      hostComponents) throws ShpurdpException ;

  void addServiceComponentHost(ServiceComponentHost hostComponent)
      throws ShpurdpException ;

  ServiceComponentResponse convertToResponse();

  void debugDump(StringBuilder sb);

  boolean isClientComponent();

  boolean isMasterComponent();

  boolean isVersionAdvertised();

  boolean canBeRemoved();

  void deleteAllServiceComponentHosts(DeleteHostComponentStatusMetaData deleteMetaData) throws ShpurdpException;

  void deleteServiceComponentHosts(String hostname, DeleteHostComponentStatusMetaData deleteMetaData)
      throws ShpurdpException;

  ServiceComponentHost addServiceComponentHost(
      String hostName) throws ShpurdpException;

  void delete(DeleteHostComponentStatusMetaData deleteMetaData);

  /**
   * This method computes the state of the repository that's associated with the desired
   * version.  It is used, for example, when a host component reports its version and the
   * state can be in flux.
   *
   * @param reportedVersion
   * @throws ShpurdpException
   */
  void updateRepositoryState(String reportedVersion) throws ShpurdpException;

  /**
   * @return the repository state for the desired version
   */
  RepositoryVersionState getRepositoryState();
}
