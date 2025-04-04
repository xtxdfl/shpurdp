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

package org.apache.shpurdp.server.topology;

import java.util.Map;

import org.apache.shpurdp.server.state.ValueAttributesInfo;

public class AdvisedConfiguration {
  private final Map<String, String> properties;
  private final Map<String, ValueAttributesInfo> propertyValueAttributes;

  public AdvisedConfiguration(Map<String, String> properties,
                              Map<String, ValueAttributesInfo> propertyValueAttributes) {
    this.properties = properties;
    this.propertyValueAttributes = propertyValueAttributes;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Map<String, ValueAttributesInfo> getPropertyValueAttributes() {
    return propertyValueAttributes;
  }
}
