/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.shpurdp.view.assets.model;

import org.apache.oozie.shpurdp.view.AssetDefinitionRefType;
import org.apache.oozie.shpurdp.view.EntityStatus;
import org.apache.oozie.shpurdp.view.model.BaseModel;
import org.apache.oozie.shpurdp.view.model.Indexed;

public class ActionAsset extends BaseModel implements Indexed {
  private static final long serialVersionUID = 1L;
  private String id;
  private String name;
  private String description;
  private String type;
  private String definitionRefType = AssetDefinitionRefType.DB.name();//can be db or fs
  private String definitionRef;//point to dbid or filesystem
  private String status = EntityStatus.DRAFT.name();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDefinitionRef() {
    return definitionRef;
  }

  public void setDefinitionRef(String definitionRef) {
    this.definitionRef = definitionRef;
  }

  public String getDefinitionRefType() {
    return definitionRefType;
  }

  public void setDefinitionRefType(String definitionRefType) {
    this.definitionRefType = definitionRefType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
