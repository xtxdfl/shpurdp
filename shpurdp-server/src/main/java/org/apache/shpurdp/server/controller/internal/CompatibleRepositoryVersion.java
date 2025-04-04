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
package org.apache.shpurdp.server.controller.internal;

import java.util.HashSet;
import java.util.Set;

import org.apache.shpurdp.server.orm.entities.RepositoryVersionEntity;
import org.apache.shpurdp.spi.upgrade.UpgradeType;

/**
 * Helper class for maintaining RepositoryVersionEntity along with
 * supported Upgrade Type(s).
 */
public class CompatibleRepositoryVersion {
  private RepositoryVersionEntity repositoryVersionEntity;
  private Set<UpgradeType> supportedTypes;

  public CompatibleRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    this.repositoryVersionEntity = repositoryVersionEntity;
    this.supportedTypes = new HashSet<>();
  }

  /**
   * Maintains the List of Upgrade Types.
   *
   * @param type Supported Upgrade Type.
   */
  public void addUpgradePackType(UpgradeType type) {
    supportedTypes.add(type);
  }

  /**
   * @return List of supported Upgrade Type(s).
   */
  public Set<UpgradeType> getSupportedTypes() {
    return supportedTypes;
  }

  /**
   * @return RepositoryVersionEntity instance.
   */
  public RepositoryVersionEntity getRepositoryVersionEntity() {
    return repositoryVersionEntity;
  }

}
