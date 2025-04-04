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

package org.apache.shpurdp.view.pig;


import com.google.inject.Inject;
import org.apache.shpurdp.view.ViewContext;
import org.apache.shpurdp.view.ViewResourceHandler;
import org.apache.shpurdp.view.pig.persistence.Storage;
import org.apache.shpurdp.view.pig.services.HelpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

/**
 * Pig service
 */
public class PigServiceRouter {
  @Inject
  ViewContext context;

  @Inject
  protected ViewResourceHandler handler;

  protected final static Logger LOG =
      LoggerFactory.getLogger(PigServiceRouter.class);

  private Storage storage = null;

  /**
   * Help service
   * @return help service
   */
  @Path("/help")
  public HelpService help(){
    return new HelpService(context, handler);
  }
}
