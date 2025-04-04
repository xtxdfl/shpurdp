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

package org.apache.shpurdp.server.controller;


import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.state.PropertyDependencyInfo;
import org.apache.shpurdp.server.state.PropertyInfo.PropertyType;
import org.apache.shpurdp.server.state.ValueAttributesInfo;

public class StackLevelConfigurationResponse extends StackConfigurationResponse {
  public StackLevelConfigurationResponse(String propertyName,
      String propertyValue, String propertyDescription, String propertyDisplayName, String type,
      Boolean isRequired, Set<PropertyType> propertyTypes,
      Map<String, String> propertyAttributes,
      ValueAttributesInfo propertyValueAttributes,
      Set<PropertyDependencyInfo> dependsOnProperties) {
    super(propertyName, propertyValue, propertyDescription, propertyDisplayName, type, isRequired,
        propertyTypes, propertyAttributes, propertyValueAttributes,
        dependsOnProperties);
  }
  
  public StackLevelConfigurationResponse(String propertyName, String propertyValue, String propertyDescription,
      String type, Map<String, String> propertyAttributes) {
    super(propertyName, propertyValue, propertyDescription, type, propertyAttributes);
  }
}
