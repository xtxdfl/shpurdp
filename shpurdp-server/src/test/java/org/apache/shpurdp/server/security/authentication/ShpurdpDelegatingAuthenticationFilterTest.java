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

package org.apache.shpurdp.server.security.authentication;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMockSupport;
import org.junit.Test;

public class ShpurdpDelegatingAuthenticationFilterTest extends EasyMockSupport {
  @Test
  public void testInit() throws Exception {
    FilterConfig filterConfig = createMock(FilterConfig.class);

    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    filter1.init(filterConfig);
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);
    filter2.init(filterConfig);
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);
    filter3.init(filterConfig);
    expectLastCall().once();

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filter3));
    filter.init(filterConfig);

    verifyAll();
  }

  @Test
  public void testDoFilterNoneApply() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = createMock(HttpServletResponse.class);

    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter1.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter2.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter3.shouldApply(httpServletRequest)).andReturn(false).once();

    FilterChain filterChain = createMock(FilterChain.class);
    filterChain.doFilter(httpServletRequest, httpServletResponse);
    expectLastCall().once();

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filter3));
    filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilterFirstApplies() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);

    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter1.shouldApply(httpServletRequest)).andReturn(true).once();
    filter1.doFilter(httpServletRequest, httpServletResponse, filterChain);
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filter3));
    filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilterLastApplies() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);

    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter1.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter2.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter3.shouldApply(httpServletRequest)).andReturn(true).once();
    filter3.doFilter(httpServletRequest, httpServletResponse, filterChain);
    expectLastCall().once();

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filter3));
    filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilterNthApplies() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);

    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter1.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);
    expect(filter2.shouldApply(httpServletRequest)).andReturn(false).once();

    ShpurdpAuthenticationFilter filterN = createMock(ShpurdpAuthenticationFilter.class);
    expect(filterN.shouldApply(httpServletRequest)).andReturn(true).once();
    filterN.doFilter(httpServletRequest, httpServletResponse, filterChain);
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filterN, filter3));
    filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDestroy() throws Exception {
    ShpurdpAuthenticationFilter filter1 = createMock(ShpurdpAuthenticationFilter.class);
    filter1.destroy();
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter2 = createMock(ShpurdpAuthenticationFilter.class);
    filter2.destroy();
    expectLastCall().once();

    ShpurdpAuthenticationFilter filter3 = createMock(ShpurdpAuthenticationFilter.class);
    filter3.destroy();
    expectLastCall().once();

    replayAll();

    Filter filter = new ShpurdpDelegatingAuthenticationFilter(Arrays.asList(filter1, filter2, filter3));
    filter.destroy();

    verifyAll();

  }

}