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

package org.apache.shpurdp.server.api.resources;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * ViewVersionResourceDefinition tests.
 */
public class ViewVersionResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    ViewVersionResourceDefinition viewVersionResourceDefinition = new ViewVersionResourceDefinition();
    Assert.assertEquals("versions", viewVersionResourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    ViewVersionResourceDefinition viewVersionResourceDefinition = new ViewVersionResourceDefinition();
    Assert.assertEquals("version", viewVersionResourceDefinition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewVersionResourceDefinition viewVersionResourceDefinition = new ViewVersionResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = viewVersionResourceDefinition.getSubResourceDefinitions ();

    Assert.assertEquals(2, subResourceDefinitions.size());

    for (SubResourceDefinition subResourceDefinition : subResourceDefinitions) {
      String name = subResourceDefinition.getType().name();
      Assert.assertTrue(name.equals("ViewInstance") || name.equals("ViewPermission"));
    }
  }
}
