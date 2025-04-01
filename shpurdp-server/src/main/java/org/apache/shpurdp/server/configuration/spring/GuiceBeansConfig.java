/**
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
package org.apache.shpurdp.server.configuration.spring;

import org.apache.shpurdp.server.agent.stomp.AgentsRegistrationQueue;
import org.apache.shpurdp.server.audit.AuditLogger;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationEventHandlerImpl;
import org.apache.shpurdp.server.security.authentication.ShpurdpLocalAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.jwt.ShpurdpJwtAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.apache.shpurdp.server.security.authentication.pam.ShpurdpPamAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.tproxy.ShpurdpTProxyConfigurationProvider;
import org.apache.shpurdp.server.security.authorization.ShpurdpLdapAuthenticationProvider;
import org.apache.shpurdp.server.security.authorization.ShpurdpUserAuthorizationFilter;
import org.apache.shpurdp.server.security.authorization.PermissionHelper;
import org.apache.shpurdp.server.security.authorization.internal.ShpurdpInternalAuthenticationProvider;
import org.apache.shpurdp.server.security.ldap.ShpurdpLdapDataPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Injector;

@Configuration
public class GuiceBeansConfig {

  @Autowired
  //ignore warning, inherited from parent context, injected as field to reduce number of warnings
  private Injector injector;

  @Bean
  public org.apache.shpurdp.server.configuration.Configuration shpurdpConfig() {
    return injector.getInstance(org.apache.shpurdp.server.configuration.Configuration.class);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return injector.getInstance(PasswordEncoder.class);
  }

  @Bean
  public AuditLogger auditLogger() {
    return injector.getInstance(AuditLogger.class);
  }

  @Bean
  public PermissionHelper permissionHelper() {
    return injector.getInstance(PermissionHelper.class);
  }

  @Bean
  public ShpurdpLdapAuthenticationProvider shpurdpLdapAuthenticationProvider() {
    return injector.getInstance(ShpurdpLdapAuthenticationProvider.class);
  }

  @Bean
  public ShpurdpLdapDataPopulator shpurdpLdapDataPopulator() {
    return injector.getInstance(ShpurdpLdapDataPopulator.class);
  }

  @Bean
  public ShpurdpUserAuthorizationFilter shpurdpUserAuthorizationFilter() {
    return injector.getInstance(ShpurdpUserAuthorizationFilter.class);
  }

  @Bean
  public ShpurdpInternalAuthenticationProvider shpurdpInternalAuthenticationProvider() {
    return injector.getInstance(ShpurdpInternalAuthenticationProvider.class);
  }
  @Bean
  public ShpurdpJwtAuthenticationProvider shpurdpJwtAuthenticationProvider() {
    return injector.getInstance(ShpurdpJwtAuthenticationProvider.class);
  }

  @Bean
  public JwtAuthenticationPropertiesProvider jwtAuthenticationPropertiesProvider() {
    return injector.getInstance(JwtAuthenticationPropertiesProvider.class);
  }

  @Bean
  public ShpurdpPamAuthenticationProvider shpurdpPamAuthenticationProvider() {
    return injector.getInstance(ShpurdpPamAuthenticationProvider.class);
  }

  @Bean
  public ShpurdpLocalAuthenticationProvider shpurdpLocalAuthenticationProvider() {
    return injector.getInstance(ShpurdpLocalAuthenticationProvider.class);
  }

  @Bean
  public ShpurdpAuthenticationEventHandlerImpl shpurdpAuthenticationEventHandler() {
    return injector.getInstance(ShpurdpAuthenticationEventHandlerImpl.class);
  }


  @Bean
  public AgentRegisteringQueueChecker agentRegisteringQueueChecker() {
    return new AgentRegisteringQueueChecker();
  }

  @Bean
  public AgentsRegistrationQueue agentsRegistrationQueue() {
    return new AgentsRegistrationQueue(injector);
  }

  @Bean
  public ShpurdpTProxyConfigurationProvider shpurdpTProxyConfigurationProvider() {
    return injector.getInstance(ShpurdpTProxyConfigurationProvider.class);
  }

}
