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
import org.apache.shpurdp.server.api.services.Request;
import org.apache.shpurdp.server.api.services.Result;
import org.apache.shpurdp.server.api.services.ResultPostProcessor;
import org.apache.shpurdp.server.api.services.ResultPostProcessorImpl;
import org.apache.shpurdp.server.api.util.TreeNode;
import org.apache.shpurdp.server.api.util.TreeNodeImpl;

/**
 * Default resource renderer.
 * Provides the default "native" rendering for all resources.
 */
public class DefaultRenderer extends BaseRenderer implements Renderer {

  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryTree, boolean isCollection) {

    QueryInfo queryInfo = queryTree.getObject();
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
      null, queryInfo.getProperties(), queryTree.getName());

    copyPropertiesToResult(queryTree, resultTree);

    boolean addKeysToEmptyResource = true;
    if (! isCollection && isRequestWithNoProperties(queryTree)) {
      addSubResources(queryTree, resultTree);
      addKeysToEmptyResource = false;
    }
    ensureRequiredProperties(resultTree, addKeysToEmptyResource);

    return resultTree;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    // simply return the native rendering
    return new ResultPostProcessorImpl(request);
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    return queryResult;
  }
}
