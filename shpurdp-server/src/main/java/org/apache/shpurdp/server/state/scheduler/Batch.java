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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Batch {
  private final List<BatchRequest> batchRequests = new ArrayList<>();
  private BatchSettings batchSettings;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("batch_requests")
  public List<BatchRequest> getBatchRequests() {
    return batchRequests;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("batch_settings")
  public BatchSettings getBatchSettings() {
    return batchSettings;
  }

  public void setBatchSettings(BatchSettings batchSettings) {
    this.batchSettings = batchSettings;
  }

}
