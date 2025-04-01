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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shpurdp.server.events.ShpurdpConfigurationChangedEvent;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapException;
import org.apache.shpurdp.server.ldap.service.LdapConnectionConfigService;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Factory for creating LdapConnectionTemplate instances.
 * Depending on the usage context, the instance can be constructed based on the provided configuration or based on the persisted settings.
 */
@Singleton
public class LdapConnectionTemplateFactory {

  private static final Logger LOG = LoggerFactory.getLogger(LdapConnectionTemplateFactory.class);

  // Inject the persisted configuration (when available) check the provider implementation for details.
  @Inject
  private Provider<ShpurdpLdapConfiguration> shpurdpLdapConfigurationProvider;


  @Inject
  private LdapConnectionConfigService ldapConnectionConfigService;

  // cached instance that only changes when the underlying configuration changes.
  private LdapConnectionTemplate ldapConnectionTemplateInstance;


  @Inject
  public LdapConnectionTemplateFactory() {
  }

  /**
   * Creates a new instance based on the provided configuration. Use this factory method whle operating with shpurdp configuration not yet persisted.
   *
   * @param shpurdpLdapConfiguration shpurdp ldap configuration instance
   * @return an instance of LdapConnectionTemplate
   */
  public LdapConnectionTemplate create(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    LOG.info("Constructing new instance based on the provided shpurdp ldap configuration: {}", shpurdpLdapConfiguration);

    // create the connection config
    LdapConnectionConfig ldapConnectionConfig = ldapConnectionConfigService.createLdapConnectionConfig(shpurdpLdapConfiguration);

    // create the connection factory
    LdapConnectionFactory ldapConnectionFactory = new DefaultLdapConnectionFactory(ldapConnectionConfig);

    // create the connection pool
    LdapConnectionPool ldapConnectionPool = new LdapConnectionPool(new ValidatingPoolableLdapConnectionFactory(ldapConnectionFactory));

    LdapConnectionTemplate template = new LdapConnectionTemplate(ldapConnectionPool);
    LOG.info("Ldap connection template instance: {}", template);

    return template;

  }

  /**
   * Loads the persisted LDAP configuration.
   *
   * @return theh persisted
   */
  public LdapConnectionTemplate load() throws ShpurdpLdapException {

    if (null == ldapConnectionTemplateInstance) {
      ldapConnectionTemplateInstance = create(shpurdpLdapConfigurationProvider.get());
    }
    return ldapConnectionTemplateInstance;
  }

  /**
   * The returned connection template instance is recreated whenever the shpurdp ldap configuration changes
   *
   * @param event
   * @throws ShpurdpLdapException
   */
  @Subscribe
  public void onConfigChange(ShpurdpConfigurationChangedEvent event) throws ShpurdpLdapException {
    ldapConnectionTemplateInstance = create(shpurdpLdapConfigurationProvider.get());
  }


}
