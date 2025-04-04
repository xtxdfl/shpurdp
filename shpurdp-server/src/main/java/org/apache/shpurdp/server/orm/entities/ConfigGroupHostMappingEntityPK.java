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
package org.apache.shpurdp.server.orm.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;

public class ConfigGroupHostMappingEntityPK implements Serializable {
  private Long configGroupId;
  private Long hostId;

  @Id
  @Column(name = "config_group_id", nullable = false, insertable = true, updatable = true)
  public Long getConfigGroupId() {
    return configGroupId;
  }

  public void setConfigGroupId(Long configGroupId) {
    this.configGroupId = configGroupId;
  }

  @Id
  @Column(name = "host_id", nullable = false, insertable = true, updatable = true)
  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigGroupHostMappingEntityPK that = (ConfigGroupHostMappingEntityPK) o;

    if (!configGroupId.equals(that.configGroupId)) return false;
    if (!hostId.equals(that.hostId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = configGroupId.hashCode();
    result = 31 * result + hostId.hashCode();
    return result;
  }
}
