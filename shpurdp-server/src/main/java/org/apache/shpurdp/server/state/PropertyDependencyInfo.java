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


import org.apache.shpurdp.server.controller.StackConfigurationDependencyResponse;

public class PropertyDependencyInfo {

  private String type;

  private String name;

  public PropertyDependencyInfo() {

  }

  public PropertyDependencyInfo(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PropertyDependencyInfo that = (PropertyDependencyInfo) o;

    if (name != null ? !name.equals(that.name) : that.name != null)
      return false;
    if (type != null ? !type.equals(that.type) : that.type != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PropertyDependencyInfo{" +
        "type='" + type + '\'' +
        ", name='" + name + '\'' +
        '}';
  }

  public StackConfigurationDependencyResponse convertToResponse() {
    return new StackConfigurationDependencyResponse(getName(), getType());
  }

}

