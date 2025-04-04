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

package org.apache.shpurdp.view.capacityscheduler;

import org.apache.shpurdp.view.ViewContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler configuration service
 */
public class HelpService {

  private static final Logger logger = LoggerFactory.getLogger(HelpService.class);
  private ViewContext context;

  /**
   * Constructor
   * @param context View Context instance
   */
  public HelpService(ViewContext context) {
    this.context = context;
  }

  /**
   * Version
   * @return version
   */
  @GET
  @Path("/version")
  @Produces(MediaType.TEXT_PLAIN)
  public Response version() {
    return Response.ok("0.0.1-SNAPSHOT").build();
  }

  /**
   * Description
   * @return description
   */
  @GET
  @Path("/description")
  @Produces(MediaType.TEXT_PLAIN)
  public Response description() {
    return Response.ok("Application to manage YARN Capacity Scheduler").build();
  }

}
