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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shpurdp.annotations.ApiIgnore;
import org.apache.shpurdp.server.api.resources.ResourceInstance;
import org.apache.shpurdp.server.controller.spi.Resource;

/**
 * Service responsible for validation of host-layout and configurations.
 */
@Path("/stacks/{stackName}/versions/{stackVersion}/validations")
public class ValidationService extends BaseService {

  /**
   * Returns validation of host-layout.
   * 
   * @param body http body
   * @param headers http headers
   * @param ui uri info
   * @param stackName stack name
   * @param stackVersion stack version
   * @return validation items if any
   */
  @POST @ApiIgnore // until documented
  @Produces(MediaType.TEXT_PLAIN)
  public Response getValidation(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("stackName") String stackName, @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createValidationResource(stackName, stackVersion));
  }

  ResourceInstance createValidationResource(String stackName, String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.Validation, mapIds);
  }

}
