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
package org.apache.shpurdp.server.state.repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * For a version, collects summary information for a cluster.
 */
public class ClusterVersionSummary {

  @SerializedName("services")
  @JsonProperty("services")
  private Map<String, ServiceVersionSummary> m_services;

  private transient Set<String> m_available = new HashSet<>();

  ClusterVersionSummary(Map<String, ServiceVersionSummary> services) {
    m_services = services;

    for (Map.Entry<String, ServiceVersionSummary> entry : services.entrySet()) {
      if (entry.getValue().isUpgrade()) {
        m_available.add(entry.getKey());
      }
    }
  }

  /**
   * @return service names that should participate in an upgrade, based on
   * the VDF contents.
   */
  @JsonIgnore
  public Set<String> getAvailableServiceNames() {
    return m_available;
  }

}
