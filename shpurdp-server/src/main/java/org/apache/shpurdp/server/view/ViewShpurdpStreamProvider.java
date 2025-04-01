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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.ShpurdpSessionManager;
import org.apache.shpurdp.server.controller.internal.URLStreamProvider;
import org.apache.shpurdp.server.proxy.ProxyService;
import org.apache.shpurdp.view.ShpurdpHttpException;
import org.apache.shpurdp.view.ShpurdpStreamProvider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider of an input stream for a request to the Shpurdp server.
 */
public class ViewShpurdpStreamProvider implements ShpurdpStreamProvider {
  /**
   * Internal stream provider.
   */
  private final URLStreamProvider streamProvider;

  /**
   * The Shpurdp session manager.
   */
  private final ShpurdpSessionManager shpurdpSessionManager;

  /**
   * The Shpurdp management controller.
   */
  private final ShpurdpManagementController controller;

  private static final Logger LOG = LoggerFactory.getLogger(ViewShpurdpStreamProvider.class);


  // ----- Constructor -----------------------------------------------------

  /**
   * Construct a view Shpurdp stream provider.
   *
   * @param streamProvider        the underlying stream provider
   * @param shpurdpSessionManager  the Shpurdp session manager
   * @param controller         the Shpurdp configuration
   *
   * @throws IllegalStateException if the Shpurdp stream provider can not be created
   */
  protected ViewShpurdpStreamProvider(URLStreamProvider streamProvider, ShpurdpSessionManager shpurdpSessionManager,
                                     ShpurdpManagementController controller) {
    this.streamProvider       = streamProvider;
    this.shpurdpSessionManager = shpurdpSessionManager;
    this.controller           = controller;
  }


  // ----- ShpurdpStreamProvider -----------------------------------------------

  @Override
  public InputStream readFrom(String path, String requestMethod, String body, Map<String, String> headers
                              ) throws IOException, ShpurdpHttpException {
    return getInputStream(path, requestMethod, headers, body == null ? null : body.getBytes());
  }

  @Override
  public InputStream readFrom(String path, String requestMethod, InputStream body, Map<String, String> headers
                              ) throws IOException, ShpurdpHttpException {

    return getInputStream(path, requestMethod, headers, body == null ? null : IOUtils.toByteArray(body));
  }


  // ----- helper methods ----------------------------------------------------

  private InputStream getInputStream(String path, String requestMethod, Map<String, String> headers
                                     , byte[] body) throws IOException, ShpurdpHttpException {
    // add the Shpurdp session cookie to the given headers
    String sessionId = shpurdpSessionManager.getCurrentSessionId();
    if (sessionId != null) {

      String shpurdpSessionCookie = shpurdpSessionManager.getSessionCookie() + "=" + sessionId;

      if (headers == null || headers.isEmpty()) {
        headers = Collections.singletonMap(URLStreamProvider.COOKIE, shpurdpSessionCookie);
      } else {
        headers = new HashMap<>(headers);

        String cookies = headers.get(URLStreamProvider.COOKIE);

        headers.put(URLStreamProvider.COOKIE, URLStreamProvider.appendCookie(cookies, shpurdpSessionCookie));
      }
    }

    // adapt the headers for the internal URLStreamProvider signature
    Map<String, List<String>> headerMap = new HashMap<>();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }

    return getInputStream(streamProvider.processURL(controller.getShpurdpServerURI(path.startsWith("/") ? path : "/" + path),
        requestMethod, body, headerMap));
  }

  private InputStream getInputStream(HttpURLConnection connection) throws IOException, ShpurdpHttpException {
    int responseCode = connection.getResponseCode();
    if (responseCode >= ProxyService.HTTP_ERROR_RANGE_START) {
      String message = connection.getResponseMessage();
      if (connection.getErrorStream() != null) {
        message = IOUtils.toString(connection.getErrorStream());
      }
      LOG.error("Got error response for url {}. Response code:{}. {}", connection.getURL(), responseCode, message);
      throw new ShpurdpHttpException(message, responseCode);
    }
    return connection.getInputStream();
  }
}

