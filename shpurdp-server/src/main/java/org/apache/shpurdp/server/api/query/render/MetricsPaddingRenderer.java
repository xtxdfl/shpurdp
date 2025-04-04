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
package org.apache.shpurdp.server.api.query.render;

import java.util.Set;

import org.apache.shpurdp.server.api.query.QueryInfo;
import org.apache.shpurdp.server.api.util.TreeNode;
import org.apache.shpurdp.server.controller.metrics.MetricsPaddingMethod.PADDING_STRATEGY;

public class MetricsPaddingRenderer extends DefaultRenderer {
  PADDING_STRATEGY paddingMethod = PADDING_STRATEGY.ZEROS;

  public MetricsPaddingRenderer(String paddingMethod) {
    if (paddingMethod.equalsIgnoreCase("null_padding")) {
      this.paddingMethod = PADDING_STRATEGY.NULLS;
    } else if (paddingMethod.equalsIgnoreCase("no_padding")) {
      this.paddingMethod = PADDING_STRATEGY.NONE;
    }
  }

  @Override
  public TreeNode<Set<String>> finalizeProperties(TreeNode<QueryInfo> queryProperties,
                                                  boolean isCollection) {
    Set<String> properties = queryProperties.getObject().getProperties();
    if (properties != null) {
      properties.add("params/padding/" + paddingMethod.name());
    }
    return super.finalizeProperties(queryProperties, isCollection);
  }
}
