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

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.apache.shpurdp.server.controller.RootComponent;
import org.apache.shpurdp.server.controller.RootService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * RootServiceComponentConfigurationHandlerFactory produces RootServiceComponentConfigurationHandler
 * implementations for the relevant service, component, and category.
 */
@Singleton
public class RootServiceComponentConfigurationHandlerFactory {

  @Inject
  private ShpurdpServerConfigurationHandler defaultConfigurationHandler;

  @Inject
  private ShpurdpServerLDAPConfigurationHandler ldapConfigurationHandler;

  @Inject
  private ShpurdpServerSSOConfigurationHandler ssoConfigurationHandler;

  @Inject
  private ShpurdpServerConfigurationHandler tproxyConfigurationHandler;

  /**
   * Returns the internal configuration handler used to support various configuration storage facilities.
   *
   * @param serviceName   the service name
   * @param componentName the component name
   * @param categoryName  the category name
   * @return a {@link RootServiceComponentConfigurationHandler}
   */
  public RootServiceComponentConfigurationHandler getInstance(String serviceName, String componentName, String categoryName) {
    if (RootService.SHPURDP.name().equals(serviceName)) {
      if (RootComponent.SHPURDP_SERVER.name().equals(componentName)) {
        if (ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return ldapConfigurationHandler;
        } else if (ShpurdpServerConfigurationCategory.SSO_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return ssoConfigurationHandler;
        } else if (ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return tproxyConfigurationHandler;
        } else {
          return defaultConfigurationHandler;
        }
      }
    }

    return null;
  }
}
