/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.audit.request.eventcreator;

import java.util.Set;

import org.apache.shpurdp.server.api.services.Request;
import org.apache.shpurdp.server.api.services.Result;
import org.apache.shpurdp.server.api.services.ResultStatus;
import org.apache.shpurdp.server.audit.event.AuditEvent;
import org.apache.shpurdp.server.audit.event.request.AddBlueprintRequestAuditEvent;
import org.apache.shpurdp.server.audit.event.request.DeleteBlueprintRequestAuditEvent;
import org.apache.shpurdp.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * This creator handles blueprint add and remove requests
 * For resource type {@link Resource.Type#Blueprint}
 * and request types {@link Request.Type#POST} and {@link Request.Type#POST}
 */
public class BlueprintEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.Blueprint).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    switch (request.getRequestType()) {
      case POST:
        return AddBlueprintRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withBlueprintName(request.getResource().getKeyValueMap().get(Resource.Type.Blueprint))
          .build();
      case DELETE:
        return DeleteBlueprintRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withBlueprintName(request.getResource().getKeyValueMap().get(Resource.Type.Blueprint))
          .build();
      default:
        return null;
    }
  }
}
