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
package org.apache.shpurdp.server.api.services;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shpurdp.annotations.ApiIgnore;
import org.apache.shpurdp.server.api.resources.ResourceInstance;
import org.apache.shpurdp.server.controller.spi.Resource;

/**
 * Endpoint for alert data.
 */
public class AlertService extends BaseService {

  private String clusterName = null;
  private String serviceName = null;
  private String hostName = null;

  AlertService(String clusterName, String serviceName, String hostName) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.hostName = hostName;
  }

  /**
   * Gets all the definitions for the target
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getAlerts(
      @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
      createResourceInstance(null));
  }

  /**
   * Gets a specific alert's instance
   */
  @GET @ApiIgnore // until documented
  @Path("{alertId}")
  @Produces("text/plain")
  public Response getAlert(
      @Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("alertId") Long id) {
    return handleRequest(headers, null, ui, Request.Type.GET,
      createResourceInstance(id));
  }

  /**
   * Create an alert resource instance
   * @param alertId the alert id, if requesting a specific one
   * @return the resource instance
   */
  private ResourceInstance createResourceInstance(Long alertId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Service, serviceName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.Alert, null == alertId ? null : alertId.toString());

    return createResource(Resource.Type.Alert, mapIds);
  }

}
