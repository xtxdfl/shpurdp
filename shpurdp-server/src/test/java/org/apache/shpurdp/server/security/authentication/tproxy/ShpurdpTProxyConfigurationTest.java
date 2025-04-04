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

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_ALLOWED_GROUPS;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_ALLOWED_HOSTS;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_ALLOWED_USERS;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ShpurdpTProxyConfigurationTest {

  @Test
  public void testEmptyConfiguration() {
    ShpurdpTProxyConfiguration configuration = new ShpurdpTProxyConfiguration(null);

    Assert.assertFalse(configuration.isEnabled());

    Assert.assertEquals(TPROXY_ALLOWED_HOSTS.getDefaultValue(), configuration.getAllowedHosts("knox"));
    Assert.assertEquals(TPROXY_ALLOWED_USERS.getDefaultValue(), configuration.getAllowedUsers("knox"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getDefaultValue(), configuration.getAllowedGroups("knox"));
    Assert.assertEquals(TPROXY_ALLOWED_HOSTS.getDefaultValue(), configuration.getAllowedHosts("otherproxyuser"));
    Assert.assertEquals(TPROXY_ALLOWED_USERS.getDefaultValue(), configuration.getAllowedUsers("otherproxyuser"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getDefaultValue(), configuration.getAllowedGroups("otherproxyuser"));
  }

  @Test
  public void testNonEmptyConfiguration() {
    Map<String, String> expectedProperties = new HashMap<>();
    expectedProperties.put(TPROXY_AUTHENTICATION_ENABLED.key(), "true");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.hosts", "c7401.shpurdp.apache.org");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.users", "*");
    expectedProperties.put("shpurdp.tproxy.proxyuser.knox.groups", "users");

    ShpurdpTProxyConfiguration configuration = new ShpurdpTProxyConfiguration(expectedProperties);

    Assert.assertNotSame(expectedProperties, configuration.toMap());
    Assert.assertEquals(expectedProperties, configuration.toMap());

    Assert.assertTrue(configuration.isEnabled());
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.hosts"), configuration.getAllowedHosts("knox"));
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.users"), configuration.getAllowedUsers("knox"));
    Assert.assertEquals(expectedProperties.get("shpurdp.tproxy.proxyuser.knox.groups"), configuration.getAllowedGroups("knox"));

    Assert.assertEquals(TPROXY_ALLOWED_HOSTS.getDefaultValue(), configuration.getAllowedHosts("otherproxyuser"));
    Assert.assertEquals(TPROXY_ALLOWED_USERS.getDefaultValue(), configuration.getAllowedUsers("otherproxyuser"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getDefaultValue(), configuration.getAllowedGroups("otherproxyuser"));
  }
}