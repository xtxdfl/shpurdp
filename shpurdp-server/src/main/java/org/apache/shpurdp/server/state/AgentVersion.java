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

package org.apache.shpurdp.server.state;

/**
 * Agent Version representation
 */
public class AgentVersion {

  private final String version;

  public AgentVersion(String version) {
    this.version = version;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof AgentVersion)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    AgentVersion a = (AgentVersion) object;
    return a.version.equals(this.version);
  }

  @Override
  public int hashCode() {
    int result = version != null ? version.hashCode() : 0;
    return result;
  }
}
