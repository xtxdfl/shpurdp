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

package org.apache.shpurdp.server.security.encryption;

import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.security.credential.Credential;

public interface CredentialStoreService {
  /**
   * Adds a new credential to ether the persistent or the temporary CredentialStore
   * <p/>
   * The supplied key will be converted into UTF-8 bytes before being stored.
   *
   * @param clusterName         the name of the cluster the credential is related to
   * @param alias               a string declaring the alias (or name) of the credential
   * @param credential          the credential value to store
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @throws ShpurdpException if an error occurs while storing the new credential
   */
  void setCredential(String clusterName, String alias, Credential credential, CredentialStoreType credentialStoreType) throws ShpurdpException;

  /**
   * Retrieves the specified credential looking in the temporary and then the persistent CredentialStore
   *
   * @param clusterName the name of the cluster the credential is related to
   * @param alias       a string declaring the alias (or name) of the credential
   * @return the requested Credential
   * @throws ShpurdpException if an error occurs while retrieving the credential
   */
  Credential getCredential(String clusterName, String alias) throws ShpurdpException;

  /**
   * Retrieves the specified credential looking in ether the persistent or the temporary CredentialStore
   *
   * @param clusterName         the name of the cluster this credential is related to
   * @param alias               a string declaring the alias (or name) of the credential
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @return the requested Credential
   * @throws ShpurdpException if an error occurs while retrieving the credential
   */
  Credential getCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws ShpurdpException;

  /**
   * Removes the specified credential from all CredentialStores
   *
   * @param clusterName the name of the cluster this credential is related to
   * @param alias       a string declaring the alias (or name) of the credential
   * @throws ShpurdpException if an error occurs while removing the credential
   */
  void removeCredential(String clusterName, String alias) throws ShpurdpException;

  /**
   * Removes the specified credential from ether the persistent or the temporary CredentialStore
   *
   * @param clusterName         the name of the cluster this credential is related to
   * @param alias               a string declaring the alias (or name) of the credential
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @throws ShpurdpException if an error occurs while removing the credential
   */
  void removeCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws ShpurdpException;

  /**
   * Tests to see if the requested alias exists in any CredentialStore
   *
   * @param clusterName the name of the cluster this credential is related to
   * @param alias       a string declaring the alias (or name) of the credential
   * @return true if it exists; otherwise false
   * @throws ShpurdpException if an error occurs while searching for the credential
   */
  boolean containsCredential(String clusterName, String alias) throws ShpurdpException;

  /**
   * Tests to see if the requested alias exists in ether the persistent or the temporary CredentialStore
   *
   * @param clusterName         the name of the cluster this credential is related to
   * @param alias               a string declaring the alias (or name) of the credential
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @return true if it exists; otherwise false
   * @throws ShpurdpException if an error occurs while searching for the credential
   */
  boolean containsCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws ShpurdpException;

  /**
   * Gets the type of the credential store used to store the requested credential
   *
   * @param clusterName the name of the cluster this credential is related to
   * @param alias       a string declaring the alias (or name) of the credential
   * @return a CredentialStoreType
   * @throws ShpurdpException if an error occurs while searching for the credential
   */
  CredentialStoreType getCredentialStoreType(String clusterName, String alias) throws ShpurdpException;

  /**
   * Maps the existing alias names to their relevant credential store types.
   *
   * @param clusterName the name of the cluster this credential is related to
   * @return a map of alias names to CredentialStoreTypes
   * @throws ShpurdpException if an error occurs while searching for the credentials
   */
  Map<String, CredentialStoreType> listCredentials(String clusterName) throws ShpurdpException;

  /**
   * Tests this CredentialStoreService to check if it has been properly initialized
   *
   * @return true if initialized; otherwise false
   */
  boolean isInitialized();

  /**
   * Tests this CredentialStoreService to check if ether the persistent or the temporary CredentialStore
   * has been properly initialized
   *
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @return true if initialized; otherwise false
   */
  boolean isInitialized(CredentialStoreType credentialStoreType);
}
