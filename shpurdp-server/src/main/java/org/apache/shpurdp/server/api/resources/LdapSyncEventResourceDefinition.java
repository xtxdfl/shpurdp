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

import java.util.Collections;
import java.util.Set;

import org.apache.shpurdp.server.controller.spi.Resource;

/**
 * LDAP sync event resource definition.
 */
public class LdapSyncEventResourceDefinition extends BaseResourceDefinition {

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an event resource definition.
   */
  public LdapSyncEventResourceDefinition() {
    super(Resource.Type.LdapSyncEvent);
  }


  // ----- ResourceDefinition ------------------------------------------------

  @Override
  public String getPluralName() {
    return "ldap_sync_events";
  }

  @Override
  public String getSingularName() {
    return "ldap_sync_event";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return Collections.emptySet();
  }
}
