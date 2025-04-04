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

package org.apache.shpurdp.server.security.authentication.kerberos;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shpurdp.server.audit.AuditLogger;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationEventHandler;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationException;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationFilter;
import org.apache.shpurdp.server.security.authentication.tproxy.TrustedProxyAuthenticationDetailsSource;
import org.apache.shpurdp.server.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * ShpurdpKerberosAuthenticationFilter extends the {@link SpnegoAuthenticationProcessingFilter} class
 * to perform Kerberos-based authentication for Shpurdp.
 * <p>
 * If configured, auditing is performed using {@link AuditLogger}.
 */
@Component
@Order(2)
public class ShpurdpKerberosAuthenticationFilter extends SpnegoAuthenticationProcessingFilter implements ShpurdpAuthenticationFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ShpurdpKerberosAuthenticationFilter.class);

  /**
   * Shpurdp authentication event handler
   */
  private final ShpurdpAuthenticationEventHandler eventHandler;

  /**
   * A Boolean value indicating whether Kerberos authentication is enabled or not.
   */
  private final boolean kerberosAuthenticationEnabled;

  /**
   * Constructor.
   * <p>
   * Given supplied data, sets up the the {@link SpnegoAuthenticationProcessingFilter} to perform
   * authentication and audit logging if configured do to so.
   *
   * @param authenticationManager the Spring authentication manager
   * @param entryPoint            the Spring entry point
   * @param configuration         the Shpurdp configuration data
   * @param eventHandler          the Shpurdp authentication event handler
   */
  public ShpurdpKerberosAuthenticationFilter(AuthenticationManager authenticationManager,
                                            final AuthenticationEntryPoint entryPoint,
                                            Configuration configuration,
                                            final ShpurdpAuthenticationEventHandler eventHandler) {
    ShpurdpKerberosAuthenticationProperties kerberosAuthenticationProperties = (configuration == null)
        ? null
        : configuration.getKerberosAuthenticationProperties();

    kerberosAuthenticationEnabled = (kerberosAuthenticationProperties != null) && kerberosAuthenticationProperties.isKerberosAuthenticationEnabled();

    if (eventHandler == null) {
      throw new IllegalArgumentException("The ShpurdpAuthenticationEventHandler must not be null");
    }

    this.eventHandler = eventHandler;

    setAuthenticationManager(authenticationManager);

    setAuthenticationDetailsSource(new TrustedProxyAuthenticationDetailsSource());

    setFailureHandler(new AuthenticationFailureHandler() {
      @Override
      public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        ShpurdpAuthenticationException cause;
        if (e instanceof ShpurdpAuthenticationException) {
          cause = (ShpurdpAuthenticationException) e;
        } else {
          cause = new ShpurdpAuthenticationException(null, e.getLocalizedMessage(), false, e);
        }
        eventHandler.onUnsuccessfulAuthentication(ShpurdpKerberosAuthenticationFilter.this, httpServletRequest, httpServletResponse, cause);

        entryPoint.commence(httpServletRequest, httpServletResponse, e);
      }
    });

    setSuccessHandler(new AuthenticationSuccessHandler() {
      @Override
      public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        eventHandler.onSuccessfulAuthentication(ShpurdpKerberosAuthenticationFilter.this, httpServletRequest, httpServletResponse, authentication);
      }
    });
  }

  /**
   * Tests to determine if this authentication filter is applicable given the Shpurdp configuration
   * and the user's HTTP request.
   * <p>
   * If the Shpurdp configuration indicates the Kerberos authentication is enabled and the HTTP request
   * contains the appropriate <code>Authorization</code> header, than this filter may be applied;
   * otherwise it should be skipped.
   *
   * @param httpServletRequest the request
   * @return true if this filter should be applied; false otherwise
   */
  @Override
  public boolean shouldApply(HttpServletRequest httpServletRequest) {
    if (LOG.isDebugEnabled()) {
      RequestUtils.logRequestHeadersAndQueryParams(httpServletRequest, LOG);
    }

    if (kerberosAuthenticationEnabled) {
      String header = httpServletRequest.getHeader("Authorization");
      return (header != null) && (header.startsWith("Negotiate ") || header.startsWith("Kerberos "));
    } else {
      return false;
    }
  }

  @Override
  public boolean shouldIncrementFailureCount() {
    // Always return false since authentication happens remotely.
    return false;
  }

  /**
   * Performs the logic for this filter.
   * <p>
   * Checks whether the authentication information is filled. If it is not, then a login failed audit event is logged.
   * <p>
   * Then, forwards the workflow to {@link SpnegoAuthenticationProcessingFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param filterChain     the Spring filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    if (eventHandler != null) {
      eventHandler.beforeAttemptAuthentication(ShpurdpKerberosAuthenticationFilter.this, servletRequest, servletResponse);
    }

    super.doFilter(servletRequest, servletResponse, filterChain);
  }
}
