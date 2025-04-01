/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType.SSO_CONFIGURATIONS;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.SSO_MANAGE_SERVICES;

import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.dao.ShpurdpConfigurationDAO;
import org.apache.shpurdp.server.state.Clusters;
import org.apache.shpurdp.server.state.ConfigHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * ShpurdpServerSSOConfigurationHandler is an {@link ShpurdpServerConfigurationHandler} implementation
 * handing changes to the SSO configuration
 */
@Singleton
public class ShpurdpServerSSOConfigurationHandler extends ShpurdpServerStackAdvisorAwareConfigurationHandler {

  @Inject
  public ShpurdpServerSSOConfigurationHandler(Clusters clusters, ConfigHelper configHelper, ShpurdpManagementController managementController,
      StackAdvisorHelper stackAdvisorHelper, ShpurdpConfigurationDAO shpurdpConfigurationDAO, ShpurdpEventPublisher publisher) {
    super(shpurdpConfigurationDAO, publisher, clusters, configHelper, managementController, stackAdvisorHelper);
  }

  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws ShpurdpException {
    // Use the default implementation of #updateComponentCategory; however if Shpurdp is managing the SSO implementations
    // always process them, even the of sso-configuration properties have not been changed since we do not
    // know of the Shpurdp SSO data has changed in the shpurdp.properties file.  For example the authentication.jwt.providerUrl
    // or authentication.jwt.publicKey values.
    super.updateComponentCategory(categoryName, properties, removePropertiesIfNotSpecified);

    // Determine if Shpurdp is managing SSO configurations...
    final Map<String, String> ssoProperties = getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    final boolean manageSSOConfigurations = (ssoProperties != null) && "true".equalsIgnoreCase(ssoProperties.get(SSO_MANAGE_SERVICES.key()));

    if (manageSSOConfigurations) {
      processClusters(SSO_CONFIGURATIONS);
    }
  }

  /**
   * Gets the set of services for which the user declared  Shpurdp to enable SSO integration.
   * <p>
   * If Shpurdp is not managing SSO integration configuration for services the set of names will be empry.
   *
   * @return a set of service names
   */
  public Set<String> getSSOEnabledServices() {
    return getEnabledServices(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), SSO_ENABLED_SERVICES.key());
  }
  
  @Override
  protected String getServiceVersionNote() {
    return "Shpurdp-managed single sign-on configurations";
  }
}
