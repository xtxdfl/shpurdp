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
package org.apache.shpurdp.server.view.configuration;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * View persistence configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PersistenceConfig {
  /**
   * The persistence entities.
   */
  @XmlElement(name="entity")
  private List<EntityConfig> entities;

  /**
   * Get the entity configurations.
   *
   * @return the entity configurations
   */
  public List<EntityConfig> getEntities() {
    return entities == null ? Collections.emptyList() : entities;
  }
}
