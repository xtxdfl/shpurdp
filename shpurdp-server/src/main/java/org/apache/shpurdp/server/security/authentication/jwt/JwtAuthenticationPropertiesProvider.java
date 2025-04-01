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

package org.apache.shpurdp.server.security.authentication.jwt;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.SSO_CONFIGURATION;

import java.util.Collection;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationProvider;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.ShpurdpJpaPersistService;

/**
 * JwtAuthenticationPropertiesProvider manages a {@link JwtAuthenticationProperties} instance by
 * lazily loading the properties if needed and refreshing the properties if updates are made to the
 * sso-configuration category of the Shpurdp configuration data.
 * <p>
 * The {@link JwtAuthenticationProperties} are updated upon events received from the {@link ShpurdpEventPublisher}.
 */
@Singleton
public class JwtAuthenticationPropertiesProvider extends ShpurdpServerConfigurationProvider<JwtAuthenticationProperties> {

  @Inject
  public JwtAuthenticationPropertiesProvider(ShpurdpEventPublisher shpurdpEventPublisher, ShpurdpJpaPersistService shpurdpJpaPersistService) {
    super(SSO_CONFIGURATION, shpurdpEventPublisher, shpurdpJpaPersistService);
  }

  /**
   * Creates a JwtAuthenticationProperties from a list of {@link ShpurdpConfigurationEntity}s.
   *
   * @param configurationEntities a list of {@link ShpurdpConfigurationEntity}s
   * @return a filled in {@link JwtAuthenticationProperties}
   */
  @Override
  protected JwtAuthenticationProperties loadInstance(Collection<ShpurdpConfigurationEntity> configurationEntities) {
    return new JwtAuthenticationProperties(toProperties(configurationEntities));
  }
}
