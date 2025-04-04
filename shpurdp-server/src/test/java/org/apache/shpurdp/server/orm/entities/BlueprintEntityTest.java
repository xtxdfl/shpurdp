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

package org.apache.shpurdp.server.orm.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;


/**
 * BlueprintEntity unit tests
 */
public class BlueprintEntityTest {

  private StackEntity stackEntity = new StackEntity();

  @Before
  public void setup() {
    stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.0.6");
  }

  @Test
  public void testSetGetBlueprintName() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName("foo");
    assertEquals("foo", entity.getBlueprintName());
  }

  @Test
  public void testSetGetStack() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setStack(stackEntity);
    assertEquals(stackEntity, entity.getStack());
  }

  @Test
  public void testSetGetHostGroups() {
    BlueprintEntity entity = new BlueprintEntity();
    Collection<HostGroupEntity> hostGroups = Collections.emptyList();
    entity.setHostGroups(hostGroups);
    assertSame(hostGroups, entity.getHostGroups());
  }

  @Test
  public void testSetGetConfigurations() {
    BlueprintEntity entity = new BlueprintEntity();
    Collection<BlueprintConfigEntity> configurations = Collections.emptyList();
    entity.setConfigurations(configurations);
    assertSame(configurations, entity.getConfigurations());
  }

  /**
   * Test get and set of Setting object.
   */
  @Test
  public void testSetGetSetting() {
    BlueprintEntity entity = new BlueprintEntity();
    Collection<BlueprintSettingEntity> setting = Collections.emptyList();
    entity.setSettings(setting);
    assertSame(setting, entity.getSettings());
  }
}
