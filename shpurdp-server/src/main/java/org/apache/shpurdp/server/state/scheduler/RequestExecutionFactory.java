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
package org.apache.shpurdp.server.state.scheduler;

import org.apache.shpurdp.server.orm.entities.RequestScheduleEntity;
import org.apache.shpurdp.server.state.Cluster;

import com.google.inject.assistedinject.Assisted;

public interface RequestExecutionFactory {
  RequestExecution createNew(@Assisted("cluster") Cluster cluster,
                             @Assisted("batch") Batch batch,
                             @Assisted("schedule") Schedule schedule);

  RequestExecution createExisting(Cluster cluster,
                                  RequestScheduleEntity requestScheduleEntity);
}
