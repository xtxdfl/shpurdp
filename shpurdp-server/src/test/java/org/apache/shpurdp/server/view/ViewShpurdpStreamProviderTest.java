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

package org.apache.shpurdp.server.view;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.ShpurdpSessionManager;
import org.apache.shpurdp.server.controller.internal.URLStreamProvider;
import org.junit.Assert;
import org.junit.Test;

public class ViewShpurdpStreamProviderTest {

  @Test
  public void testReadFrom() throws Exception {
    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    ShpurdpSessionManager sessionManager = createNiceMock(ShpurdpSessionManager.class);
    ShpurdpManagementController controller = createNiceMock(ShpurdpManagementController.class);

    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");
    headers.put("Cookie", "FOO=bar");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar; SHPURDPSESSIONID=abcdefg"));

    expect(sessionManager.getCurrentSessionId()).andReturn("abcdefg");
    expect(sessionManager.getSessionCookie()).andReturn("SHPURDPSESSIONID");

    expect(controller.getShpurdpServerURI("/spec")).andReturn("http://c6401.shpurdp.apache.org:8080/spec");
    expect(streamProvider.processURL(eq("http://c6401.shpurdp.apache.org:8080/spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, sessionManager, controller, urlConnection, inputStream);

    ViewShpurdpStreamProvider viewShpurdpStreamProvider = new ViewShpurdpStreamProvider(streamProvider, sessionManager, controller);

    Assert.assertEquals(inputStream, viewShpurdpStreamProvider.readFrom("spec", "requestMethod", "params", headers));

    verify(streamProvider, sessionManager, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNew() throws Exception {
    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    ShpurdpSessionManager sessionManager = createNiceMock(ShpurdpSessionManager.class);
    ShpurdpManagementController controller = createNiceMock(ShpurdpManagementController.class);

    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");
    headers.put("Cookie", "FOO=bar");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar; SHPURDPSESSIONID=abcdefg"));

    expect(sessionManager.getCurrentSessionId()).andReturn("abcdefg");
    expect(sessionManager.getSessionCookie()).andReturn("SHPURDPSESSIONID");

    expect(controller.getShpurdpServerURI("/spec")).andReturn("http://c6401.shpurdp.apache.org:8080/spec");
    expect(streamProvider.processURL(eq("http://c6401.shpurdp.apache.org:8080/spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, sessionManager, controller, urlConnection, inputStream);

    ViewShpurdpStreamProvider viewShpurdpStreamProvider = new ViewShpurdpStreamProvider(streamProvider, sessionManager, controller);

    Assert.assertEquals(inputStream, viewShpurdpStreamProvider.readFrom("spec", "requestMethod", body, headers));

    verify(streamProvider, sessionManager, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullStringBody() throws Exception {
    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    ShpurdpSessionManager sessionManager = createNiceMock(ShpurdpSessionManager.class);
    ShpurdpManagementController controller = createNiceMock(ShpurdpManagementController.class);

    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    String body = null;

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");
    headers.put("Cookie", "FOO=bar");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar; SHPURDPSESSIONID=abcdefg"));

    expect(sessionManager.getCurrentSessionId()).andReturn("abcdefg");
    expect(sessionManager.getSessionCookie()).andReturn("SHPURDPSESSIONID");

    expect(controller.getShpurdpServerURI("/spec")).andReturn("http://c6401.shpurdp.apache.org:8080/spec");
    expect(streamProvider.processURL(eq("http://c6401.shpurdp.apache.org:8080/spec"), eq("requestMethod"), aryEq((byte[])null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, sessionManager, controller, urlConnection, inputStream);

    ViewShpurdpStreamProvider viewShpurdpStreamProvider = new ViewShpurdpStreamProvider(streamProvider, sessionManager, controller);

    Assert.assertEquals(inputStream, viewShpurdpStreamProvider.readFrom("spec", "requestMethod", body, headers));

    verify(streamProvider, sessionManager, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullInputStreamBody() throws Exception {
    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    ShpurdpSessionManager sessionManager = createNiceMock(ShpurdpSessionManager.class);
    ShpurdpManagementController controller = createNiceMock(ShpurdpManagementController.class);

    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    InputStream body = null;

    Map<String, String> headers = new HashMap<>();
    headers.put("header", "headerValue");
    headers.put("Cookie", "FOO=bar");

    Map<String, List<String>> headerMap = new HashMap<>();
    headerMap.put("header", Collections.singletonList("headerValue"));
    headerMap.put("Cookie", Collections.singletonList("FOO=bar; SHPURDPSESSIONID=abcdefg"));

    expect(sessionManager.getCurrentSessionId()).andReturn("abcdefg");
    expect(sessionManager.getSessionCookie()).andReturn("SHPURDPSESSIONID");

    expect(controller.getShpurdpServerURI("/spec")).andReturn("http://c6401.shpurdp.apache.org:8080/spec");
    expect(streamProvider.processURL(eq("http://c6401.shpurdp.apache.org:8080/spec"), eq("requestMethod"), aryEq((byte[])null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, sessionManager, controller, urlConnection, inputStream);

    ViewShpurdpStreamProvider viewShpurdpStreamProvider = new ViewShpurdpStreamProvider(streamProvider, sessionManager, controller);

    Assert.assertEquals(inputStream, viewShpurdpStreamProvider.readFrom("spec", "requestMethod", body, headers));

    verify(streamProvider, sessionManager, urlConnection, inputStream);
  }
}
