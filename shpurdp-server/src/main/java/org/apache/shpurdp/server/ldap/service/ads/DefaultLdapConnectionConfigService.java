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

package org.apache.shpurdp.server.ldap.service.ads;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapException;
import org.apache.shpurdp.server.ldap.service.LdapConnectionConfigService;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLdapConnectionConfigService implements LdapConnectionConfigService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapConnectionConfigService.class);

  @Inject
  public DefaultLdapConnectionConfigService() {
  }

  @Override
  public LdapConnectionConfig createLdapConnectionConfig(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {

    LOG.debug("Assembling ldap connection config based on: {}", shpurdpLdapConfiguration);

    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(shpurdpLdapConfiguration.serverHost());
    config.setLdapPort(shpurdpLdapConfiguration.serverPort());
    config.setName(shpurdpLdapConfiguration.bindDn());
    config.setCredentials(shpurdpLdapConfiguration.bindPassword());
    config.setUseSsl(shpurdpLdapConfiguration.useSSL());

    if ("custom".equals(shpurdpLdapConfiguration.trustStore())) {
      LOG.info("Using custom trust manager configuration");
      config.setTrustManagers(trustManagers(shpurdpLdapConfiguration));
    }

    return config;
  }


  /**
   * Configure the trust managers to use the custom keystore.
   *
   * @param shpurdpLdapConfiguration congiguration instance holding current values
   * @return the array of trust managers
   * @throws ShpurdpLdapException if an error occurs while setting up the connection
   */
  private TrustManager[] trustManagers(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    try {

      TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
      tmFactory.init(keyStore(shpurdpLdapConfiguration));
      return tmFactory.getTrustManagers();

    } catch (Exception e) {

      LOG.error("Failed to initialize trust managers", e);
      throw new ShpurdpLdapException(e);

    }

  }

  private KeyStore keyStore(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {

    // validating configuration settings
    if (Strings.isEmpty(shpurdpLdapConfiguration.trustStoreType())) {
      throw new ShpurdpLdapException("Key Store Type must be specified");
    }

    if (Strings.isEmpty(shpurdpLdapConfiguration.trustStorePath())) {
      throw new ShpurdpLdapException("Key Store Path must be specified");
    }

    try {

      KeyStore ks = KeyStore.getInstance(shpurdpLdapConfiguration.trustStoreType());
      FileInputStream fis = new FileInputStream(shpurdpLdapConfiguration.trustStorePath());
      ks.load(fis, shpurdpLdapConfiguration.trustStorePassword().toCharArray());
      return ks;

    } catch (Exception e) {

      LOG.error("Failed to create keystore", e);
      throw new ShpurdpLdapException(e);

    }
  }
}
