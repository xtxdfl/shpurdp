/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.controller.internal;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.BIND_PASSWORD;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_ALLOWED_GROUPS;
import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;
import static org.apache.shpurdp.server.configuration.ConfigurationPropertyType.PASSWORD;
import static org.apache.shpurdp.server.configuration.ConfigurationPropertyType.UNKNOWN;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.junit.Assert;
import org.junit.Test;

public class ShpurdpServerConfigurationUtilsTest {

  @Test
  public void testGetConfigurationKey() {
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED,
        ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED,
        ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.knox.groups"));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.not.knox.groups"));
    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey((ShpurdpServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertNull(ShpurdpServerConfigurationUtils.getConfigurationKey(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void testGetConfigurationPropertyType() {
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.knox.groups"));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.not.knox.groups"));
    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType((ShpurdpServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertSame(UNKNOWN, ShpurdpServerConfigurationUtils.getConfigurationPropertyType(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void testGetConfigurationPropertyTypeName() {
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType().name(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType().name(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.knox.groups"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.not.knox.groups"));
    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName((ShpurdpServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertEquals(UNKNOWN.name(), ShpurdpServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void isPassword() {
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType() == PASSWORD,
        ShpurdpServerConfigurationUtils.isPassword(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType() == PASSWORD,
        ShpurdpServerConfigurationUtils.isPassword(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        ShpurdpServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        ShpurdpServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.knox.groups"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        ShpurdpServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "shpurdp.tproxy.proxyuser.not.knox.groups"));

    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword((ShpurdpServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertFalse(ShpurdpServerConfigurationUtils.isPassword(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));

    // This is known to be a password
    Assert.assertTrue(ShpurdpServerConfigurationUtils.isPassword(BIND_PASSWORD));
  }
}