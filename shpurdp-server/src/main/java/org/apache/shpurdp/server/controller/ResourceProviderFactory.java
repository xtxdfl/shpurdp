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

import javax.inject.Named;

import org.apache.shpurdp.server.controller.internal.AlertTargetResourceProvider;
import org.apache.shpurdp.server.controller.internal.ClusterStackVersionResourceProvider;
import org.apache.shpurdp.server.controller.internal.UpgradeResourceProvider;
import org.apache.shpurdp.server.controller.internal.ViewInstanceResourceProvider;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;


public interface ResourceProviderFactory {
  @Named("host")
  ResourceProvider getHostResourceProvider(ShpurdpManagementController managementController);

  @Named("hostComponent")
  ResourceProvider getHostComponentResourceProvider(ShpurdpManagementController managementController);

  @Named("service")
  ResourceProvider getServiceResourceProvider(ShpurdpManagementController managementController);

  @Named("component")
  ResourceProvider getComponentResourceProvider(ShpurdpManagementController managementController);

  @Named("member")
  ResourceProvider getMemberResourceProvider(ShpurdpManagementController managementController);

  @Named("user")
  ResourceProvider getUserResourceProvider(ShpurdpManagementController managementController);

  @Named("auth")
  ResourceProvider getAuthResourceProvider(ShpurdpManagementController managementController);

  @Named("userAuthenticationSource")
  ResourceProvider getUserAuthenticationSourceResourceProvider();

  @Named("hostKerberosIdentity")
  ResourceProvider getHostKerberosIdentityResourceProvider(ShpurdpManagementController managementController);

  @Named("credential")
  ResourceProvider getCredentialResourceProvider(ShpurdpManagementController managementController);

  @Named("repositoryVersion")
  ResourceProvider getRepositoryVersionResourceProvider();

  @Named("kerberosDescriptor")
  ResourceProvider getKerberosDescriptorResourceProvider(ShpurdpManagementController managementController);

  @Named("upgrade")
  UpgradeResourceProvider getUpgradeResourceProvider(ShpurdpManagementController managementController);

  @Named("rootServiceHostComponentConfiguration")
  ResourceProvider getRootServiceHostComponentConfigurationResourceProvider();

  @Named("clusterStackVersion")
  ClusterStackVersionResourceProvider getClusterStackVersionResourceProvider(ShpurdpManagementController managementController);

  @Named("alertTarget")
  AlertTargetResourceProvider getAlertTargetResourceProvider();

  @Named("viewInstance")
  ViewInstanceResourceProvider getViewInstanceResourceProvider();

}