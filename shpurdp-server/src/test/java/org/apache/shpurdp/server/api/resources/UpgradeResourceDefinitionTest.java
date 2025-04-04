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

import static org.junit.Assert.assertEquals;

import org.apache.shpurdp.server.controller.spi.Resource;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * UpgradeResourceDefinition tests.
 */
public class UpgradeResourceDefinitionTest {

  @Test
  public void testGetSingularName() {
    ResourceDefinition resourceDefinition = new UpgradeResourceDefinition();
    assertEquals("upgrade", resourceDefinition.getSingularName());
  }

  @Test
  public void testGetPluralName() {
    ResourceDefinition resourceDefinition = new UpgradeResourceDefinition();
    assertEquals("upgrades", resourceDefinition.getPluralName());
  }

  @Test
  public void testGetType() {
    ResourceDefinition resourceDefinition = new UpgradeResourceDefinition();
    assertEquals(Resource.Type.Upgrade, resourceDefinition.getType());
  }

  @Test
  public void testGetCreateDirectives() {
    ResourceDefinition resourceDefinition = new UpgradeResourceDefinition();

    assertEquals(Sets.newHashSet(UpgradeResourceDefinition.SKIP_SERVICE_CHECKS_DIRECTIVE),
        resourceDefinition.getCreateDirectives());
  }
}
