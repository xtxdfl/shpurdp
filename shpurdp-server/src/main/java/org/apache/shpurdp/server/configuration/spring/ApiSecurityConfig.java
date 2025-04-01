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

import org.apache.shpurdp.server.security.ShpurdpEntryPoint;
import org.apache.shpurdp.server.security.authentication.ShpurdpDelegatingAuthenticationFilter;
import org.apache.shpurdp.server.security.authentication.ShpurdpLocalAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.jwt.ShpurdpJwtAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.kerberos.ShpurdpAuthToLocalUserDetailsService;
import org.apache.shpurdp.server.security.authentication.kerberos.ShpurdpKerberosAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.kerberos.ShpurdpKerberosTicketValidator;
import org.apache.shpurdp.server.security.authentication.kerberos.ShpurdpProxiedUserDetailsService;
import org.apache.shpurdp.server.security.authentication.pam.ShpurdpPamAuthenticationProvider;
import org.apache.shpurdp.server.security.authorization.ShpurdpAuthorizationFilter;
import org.apache.shpurdp.server.security.authorization.ShpurdpLdapAuthenticationProvider;
import org.apache.shpurdp.server.security.authorization.internal.ShpurdpInternalAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Import(GuiceBeansConfig.class)
@ComponentScan("org.apache.shpurdp.server.security")
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter{

  private final GuiceBeansConfig guiceBeansConfig;

  @Autowired
  private ShpurdpEntryPoint shpurdpEntryPoint;
  @Autowired
  private ShpurdpDelegatingAuthenticationFilter delegatingAuthenticationFilter;
  @Autowired
  private ShpurdpAuthorizationFilter authorizationFilter;

  public ApiSecurityConfig(GuiceBeansConfig guiceBeansConfig) {
    this.guiceBeansConfig = guiceBeansConfig;
  }

  @Autowired
  public void configureAuthenticationManager(AuthenticationManagerBuilder auth,
                                             ShpurdpJwtAuthenticationProvider shpurdpJwtAuthenticationProvider,
                                             ShpurdpPamAuthenticationProvider shpurdpPamAuthenticationProvider,
                                             ShpurdpLocalAuthenticationProvider shpurdpLocalAuthenticationProvider,
                                             ShpurdpLdapAuthenticationProvider shpurdpLdapAuthenticationProvider,
                                             ShpurdpInternalAuthenticationProvider shpurdpInternalAuthenticationProvider,
                                             ShpurdpKerberosAuthenticationProvider shpurdpKerberosAuthenticationProvider
  ) {
    auth.authenticationProvider(shpurdpJwtAuthenticationProvider)
        .authenticationProvider(shpurdpPamAuthenticationProvider)
        .authenticationProvider(shpurdpLocalAuthenticationProvider)
        .authenticationProvider(shpurdpLdapAuthenticationProvider)
        .authenticationProvider(shpurdpInternalAuthenticationProvider)
        .authenticationProvider(shpurdpKerberosAuthenticationProvider);
  }

  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests().anyRequest().authenticated()
        .and()
        .headers().httpStrictTransportSecurity().disable()
        .frameOptions().disable().and()
        .exceptionHandling().authenticationEntryPoint(shpurdpEntryPoint)
        .and()
        .addFilterBefore(guiceBeansConfig.shpurdpUserAuthorizationFilter(), BasicAuthenticationFilter.class)
        .addFilterAt(delegatingAuthenticationFilter, BasicAuthenticationFilter.class)
        .addFilterBefore(authorizationFilter, FilterSecurityInterceptor.class);
  }

  @Bean
  public ShpurdpKerberosAuthenticationProvider shpurdpKerberosAuthenticationProvider(
      ShpurdpKerberosTicketValidator shpurdpKerberosTicketValidator,
      ShpurdpAuthToLocalUserDetailsService authToLocalUserDetailsService,
      ShpurdpProxiedUserDetailsService proxiedUserDetailsService) {

    return new ShpurdpKerberosAuthenticationProvider(authToLocalUserDetailsService,
        proxiedUserDetailsService,
        shpurdpKerberosTicketValidator);
  }
}
