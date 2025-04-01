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

package org.apache.shpurdp.server.security.authentication.tproxy;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;
import org.junit.Assert;
import org.junit.Test;

public class ShpurdpTProxyConfigurationProviderTest {

  @Test
  public void testLoadInstance() {
    ShpurdpTProxyConfigurationProvider provider = new ShpurdpTProxyConfigurationProvider(null, null);

    Map<String, String> expectedProperties = new HashMap<>();
    expectedProperties.put(TPROXY_AUTHENTICATION_ENABLED.key(), "true");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.hosts", "c7401.shpurdp.apache.org");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.users", "*");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.groups", "users");

    ShpurdpTProxyConfiguration instance = provider.loadInstance(createShpurdpConfigurationEntities(expectedProperties));
    Assert.assertNotNull(instance);
    Assert.assertNotSame(expectedProperties, instance.toMap());
    Assert.assertEquals(expectedProperties, instance.toMap());

    Assert.assertTrue(instance.isEnabled());
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.hosts"), instance.getAllowedHosts("knox"));
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.users"), instance.getAllowedUsers("knox"));
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.groups"), instance.getAllowedGroups("knox"));
  }

  private Collection<ShpurdpConfigurationEntity> createShpurdpConfigurationEntities(Map<String, String> properties) {
    List<ShpurdpConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      ShpurdpConfigurationEntity entity = new ShpurdpConfigurationEntity();
      entity.setCategoryName(TPROXY_CONFIGURATION.getCategoryName());
      entity.setPropertyName(entry.getKey());
      entity.setPropertyValue(entry.getValue());
      entities.add(entity);
    }

    return entities;
  }

}