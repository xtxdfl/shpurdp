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


package org.apache.shpurdp.server.ldap.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ShpurdpLdapFacade implements LdapFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShpurdpLdapFacade.class);

  /**
   * Additional parameters expected to be provided along with the configuration
   */
  public enum Parameters {
    TEST_USER_NAME("shpurdp.ldap.test.user.name"),
    TEST_USER_PASSWORD("shpurdp.ldap.test.user.password");

    private String parameterKey;

    Parameters(String parameterKey) {
      this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
      return parameterKey;
    }

  }

  @Inject
  private LdapConfigurationService ldapConfigurationService;

  @Inject
  private LdapAttributeDetectionService ldapAttributeDetectionService;

  @Inject
  public ShpurdpLdapFacade() {
  }

  @Override
  public void checkConnection(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    try {

      ldapConfigurationService.checkConnection(shpurdpLdapConfiguration);
      LOGGER.info("Validating LDAP connection related configuration: SUCCESS");

    } catch (Exception e) {

      LOGGER.error("Validating LDAP connection configuration failed", e);
      throw new ShpurdpLdapException(e);

    }

  }


  @Override
  public ShpurdpLdapConfiguration detectAttributes(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    LOGGER.info("Detecting LDAP configuration attributes ...");

    try {
      LOGGER.info("Detecting user attributes ....");
      // decorate the configuration with detected user attributes
      shpurdpLdapConfiguration = ldapAttributeDetectionService.detectLdapUserAttributes(shpurdpLdapConfiguration);

      LOGGER.info("Detecting group attributes ....");
      // decorate the configuration with detected group attributes
      shpurdpLdapConfiguration = ldapAttributeDetectionService.detectLdapGroupAttributes(shpurdpLdapConfiguration);

      LOGGER.info("Attribute detection finished.");
      return shpurdpLdapConfiguration;

    } catch (Exception e) {

      LOGGER.error("Error during LDAP attribute detection", e);
      throw new ShpurdpLdapException(e);

    }
  }

  @Override
  public Set<String> checkLdapAttributes(Map<String, Object> parameters, ShpurdpLdapConfiguration ldapConfiguration) throws ShpurdpLdapException {
    String userName = getTestUserNameFromParameters(parameters);
    String testUserPass = getTestUserPasswordFromParameters(parameters);

    if (null == userName) {
      throw new IllegalArgumentException("No test user available for testing LDAP attributes");
    }

    LOGGER.info("Testing LDAP user attributes with test user: {}", userName);
    String userDn = ldapConfigurationService.checkUserAttributes(userName, testUserPass, ldapConfiguration);

    // todo handle the case where group membership is stored in the user rather than the group
    LOGGER.info("Testing LDAP group attributes with test user dn: {}", userDn);
    return ldapConfigurationService.checkGroupAttributes(userDn, ldapConfiguration);
  }


  private String getTestUserNameFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_NAME);
  }

  private String getTestUserPasswordFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_PASSWORD);
  }

  private Object parameterValue(Map<String, Object> parameters, Parameters parameter) {
    Object value = null;
    if (parameters.containsKey(parameter.getParameterKey())) {
      value = parameters.get(parameter.getParameterKey());
    } else {
      LOGGER.warn("Parameter [{}] is missing from parameters", parameter.getParameterKey());
    }
    return value;
  }
}
