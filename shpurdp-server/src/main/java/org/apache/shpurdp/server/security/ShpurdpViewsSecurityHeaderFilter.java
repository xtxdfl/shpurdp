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

package org.apache.shpurdp.server.security;

import javax.servlet.ServletRequest;

import org.apache.shpurdp.server.configuration.Configuration;

import com.google.inject.Singleton;

/**
 * ShpurdpViewsSecurityHeaderFilter adds security-related headers to HTTP response messages for Shpurdp Views
 */
@Singleton
public class ShpurdpViewsSecurityHeaderFilter extends AbstractSecurityHeaderFilter {


  @Override
  protected boolean checkPrerequisites(ServletRequest servletRequest) {
    servletRequest.setAttribute(AbstractSecurityHeaderFilter.DENY_HEADER_OVERRIDES_FLAG, "true");
    return true;
  }

  @Override
  protected void processConfig(Configuration configuration) {
    setSslEnabled(configuration.getApiSSLAuthentication());
    setStrictTransportSecurity(configuration.getViewsStrictTransportSecurityHTTPResponseHeader());
    setxFrameOptionsHeader(configuration.getViewsXFrameOptionsHTTPResponseHeader());
    setxXSSProtectionHeader(configuration.getViewsXXSSProtectionHTTPResponseHeader());
    setContentSecurityPolicyHeader(configuration.getViewsContentSecurityPolicyHTTPResponseHeader());
    setXContentTypeHeader(configuration.getViewsXContentTypeHTTPResponseHeader());
    setCacheControlHeader(configuration.getViewsCacheControlHTTPResponseHeader());
    setPragmaHeader(configuration.getViewsPragmaHTTPResponseHeader());
    setCharset(configuration.getViewsCharsetHTTPResponseHeader());
  }
}
