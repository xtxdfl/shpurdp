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
package org.apache.shpurdp.server.security.authorization;

import java.util.Properties;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.ControllerModule;

import com.google.inject.AbstractModule;

public class AuthorizationTestModuleForLdapDNWithSpace extends AbstractModule {
  @Override
  protected void configure() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY.getKey(), "ldap");
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(),"src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(),"src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION.getKey(),"centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
    //make shpurdp detect active configuration
    properties.setProperty(ShpurdpServerConfigurationKey.USER_SEARCH_BASE.key(), "dc=shpurdp,dc=the apache,dc=org");
    properties.setProperty(ShpurdpServerConfigurationKey.GROUP_BASE.key(), "ou=the groups,dc=shpurdp,dc=the apache,dc=org");

    try {
      install(new ControllerModule(properties));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
