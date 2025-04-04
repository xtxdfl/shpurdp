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

package org.apache.shpurdp.server.configuration;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ShpurdpServerConfigurationKeyTest {

  @Test
  public void testTranslateNullCategory() {
    Assert.assertNull(ShpurdpServerConfigurationKey.translate(null, "some.property"));
  }

  @Test
  public void testTranslateNullPropertyName() {
    Assert.assertNull(ShpurdpServerConfigurationKey.translate(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, null));
  }

  @Test
  public void testTranslateInvalidPropertyName() {
    Assert.assertNull(ShpurdpServerConfigurationKey.translate(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, "invalid_property_name"));
  }

  @Test
  public void testTranslateExpected() {
    Assert.assertSame(ShpurdpServerConfigurationKey.LDAP_ENABLED,
        ShpurdpServerConfigurationKey.translate(ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION, ShpurdpServerConfigurationKey.LDAP_ENABLED.key()));
  }

  @Test
  public void testTranslateRegex() {
    ShpurdpServerConfigurationKey keyWithRegex = ShpurdpServerConfigurationKey.TPROXY_ALLOWED_HOSTS;
    Assert.assertTrue(keyWithRegex.isRegex());

    Assert.assertSame(keyWithRegex,
        ShpurdpServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "shpurdp.tproxy.proxyuser.knox.hosts"));
    Assert.assertSame(keyWithRegex,
        ShpurdpServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "shpurdp.tproxy.proxyuser.not.knox.hosts"));

    ShpurdpServerConfigurationKey translatedKey = ShpurdpServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "shpurdp.tproxy.proxyuser.not.knox.groups");
    Assert.assertNotNull(translatedKey);
    Assert.assertNotSame(keyWithRegex, translatedKey);

    Assert.assertNull(ShpurdpServerConfigurationKey.translate(keyWithRegex.getConfigurationCategory(), "shpurdp.tproxy.proxyuser.not.knox.invalid"));
  }
  
  @Test
  public void testFindPasswordConfigurations() throws Exception {
    final Set<String> passwordConfigurations = ShpurdpServerConfigurationKey.findPasswordConfigurations();
    Assert.assertEquals(2, passwordConfigurations.size());
    Assert.assertTrue(passwordConfigurations.contains(ShpurdpServerConfigurationKey.BIND_PASSWORD.key()));
    Assert.assertTrue(passwordConfigurations.contains(ShpurdpServerConfigurationKey.TRUST_STORE_PASSWORD.key()));
  }

}
