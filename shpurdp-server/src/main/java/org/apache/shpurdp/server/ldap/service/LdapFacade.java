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

import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;

/**
 * The contract defining all the operations required by the application when communicating with an arbitrary LDAP server.
 * This interface is intended to decouple LDAP specific details from the application.
 * <p>
 * Any operation that requires interaction with an LDAP server from within Shpurdp should go through this interface.
 * (LDAP)
 */
public interface LdapFacade {

  /**
   * Tests the connection to the LDAP server based on the provided configuration.
   *
   * @param shpurdpLdapConfiguration the available ldap related configuration
   * @throws ShpurdpLdapException if the connection fails or other problems occur during the operation
   */
  void checkConnection(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException;


  /**
   * Runs the user and group attribute detection algorithms.
   * The method is not intended to be used as a configuration factory, the returned instance may not be suitable for use.
   *
   * @param shpurdpLdapConfiguration partially filled configuration instance to be extended with detected properties
   * @return a configuration instance, with properties filled with potentially correct values
   * @throws ShpurdpLdapException if the attribute detection fails
   */
  ShpurdpLdapConfiguration detectAttributes(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException;

  /**
   * Checks user and group related LDAP configuration attributes in the configuration object with the help of the provided parameters
   *
   * @param parameters              a map of property name and value pairs holding information to facilitate checking the attributes
   * @param shpurdpLdapConfiguration configuration instance with available attributes
   * @return the set of groups assigned to the test user
   * @throws ShpurdpLdapException if the attribute checking fails
   */
  Set<String> checkLdapAttributes(Map<String, Object> parameters, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException;
}
