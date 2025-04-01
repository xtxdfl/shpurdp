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

import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;

/**
 * Collection of operations for validating ldap configuration.
 * It's intended to decouple implementations using different libraries.
 */
public interface LdapConfigurationService {

  /**
   * Tests the connection based on the provided configuration.
   *
   * @param configuration the shpurdp ldap configuration instance
   * @throws ShpurdpLdapException if the connection is not possible
   */
  void checkConnection(ShpurdpLdapConfiguration configuration) throws ShpurdpLdapException;


  /**
   * Implements LDAP user related configuration settings validation logic.
   * Implementers communicate with the LDAP server (search, bind) to validate attributes in the provided configuration
   * instance
   *
   * @param testUserName  the test username
   * @param testPassword  the test password
   * @param configuration the available ldap configuration
   * @return The DN of the found user entry
   * @throws ShpurdpException if the connection couldn't be estabilisheds
   */
  String checkUserAttributes(String testUserName, String testPassword, ShpurdpLdapConfiguration configuration) throws ShpurdpLdapException;

  /**
   * Checks whether the group related LDAP attributes in the configuration are correct.
   *
   * @param userDn
   * @param shpurdpLdapConfiguration
   * @return
   * @throws ShpurdpLdapException
   */
  Set<String> checkGroupAttributes(String userDn, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException;

}
