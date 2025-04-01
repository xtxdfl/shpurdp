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

package org.apache.shpurdp.server.security.authentication.kerberos;

import static org.easymock.EasyMock.expect;

import org.apache.shpurdp.server.configuration.Configuration;
import org.easymock.EasyMockSupport;
import org.junit.Test;

public class ShpurdpKerberosTicketValidatorTest extends EasyMockSupport {

  /**
   * Tests an {@link ShpurdpKerberosTicketValidator} to ensure that the Spnego identity is properly
   * set in the base class during construction.
   */
  @Test
  public void testConstructor() throws NoSuchMethodException {
    ShpurdpKerberosAuthenticationProperties properties = createMock(ShpurdpKerberosAuthenticationProperties.class);
    expect(properties.isKerberosAuthenticationEnabled()).andReturn(true).once();
    expect(properties.getSpnegoPrincipalName()).andReturn("HTTP/somehost.example.com").times(1);
    expect(properties.getSpnegoKeytabFilePath()).andReturn("/etc/security/keytabs/spnego.service.keytab").times(2);

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    replayAll();

    new ShpurdpKerberosTicketValidator(configuration);

    verifyAll();
  }
}