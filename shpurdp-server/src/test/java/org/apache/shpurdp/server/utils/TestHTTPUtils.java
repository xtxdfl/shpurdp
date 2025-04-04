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
package org.apache.shpurdp.server.utils;

import org.junit.Test;

import junit.framework.Assert;

public class TestHTTPUtils {

  @Test
  public void testGetHostAndPortFromProperty() {
    String value = null;
    HostAndPort hp = HTTPUtils.getHostAndPortFromProperty(value);
    Assert.assertNull(hp);

    value = "";
    hp = HTTPUtils.getHostAndPortFromProperty(value);
    Assert.assertNull(hp);

    value = "c6401.shpurdp.apache.org";
    hp = HTTPUtils.getHostAndPortFromProperty(value);
    Assert.assertNull(hp);

    value = "c6401.shpurdp.apache.org:";
    hp = HTTPUtils.getHostAndPortFromProperty(value);
    Assert.assertNull(hp);

    value = "c6401.shpurdp.apache.org:50070";
    hp = HTTPUtils.getHostAndPortFromProperty(value);
    Assert.assertEquals(hp.host, "c6401.shpurdp.apache.org");
    Assert.assertEquals(hp.port, 50070);

    value = "  c6401.shpurdp.apache.org:50070   ";
    Assert.assertEquals(hp.host, "c6401.shpurdp.apache.org");
    Assert.assertEquals(hp.port, 50070);
  }
}
