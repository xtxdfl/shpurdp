package org.apache.shpurdp.server.controller.logging;

import org.apache.shpurdp.server.controller.ShpurdpManagementController;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface LoggingRequestHelperFactory {

  /**
   * Obtain a new instance of a LoggingRequestHelper, which can
   * be used to connect to the given cluster
   * @param shpurdpManagementController
   * @param clusterName name of cluster that includes a LogSearch deployment
   *
   * @return an instance of LoggingRequestHelper that can be used to
   *         connect to this cluster's LogSearch service
   */
  LoggingRequestHelper getHelper(ShpurdpManagementController shpurdpManagementController, String clusterName);

}
