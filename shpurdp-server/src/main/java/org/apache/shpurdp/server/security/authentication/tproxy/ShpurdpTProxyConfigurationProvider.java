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

import java.util.Collection;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory;
import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationProvider;
import org.apache.shpurdp.server.events.publishers.ShpurdpEventPublisher;
import org.apache.shpurdp.server.orm.entities.ShpurdpConfigurationEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.ShpurdpJpaPersistService;

/**
 * Provider implementation for {@link ShpurdpTProxyConfiguration} objects.
 * Returned {@link ShpurdpTProxyConfiguration} instances are expected to contain the current values
 * for the tproxy-configuration category of the Shpurdp Server Configuration data.
 * <p>
 * It needs to be registered in the related GUICE module as a provider.
 * <p>
 * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
 * configuration instance accordingly.
 *
 * @see ShpurdpServerConfigurationProvider
 */
@Singleton
public class ShpurdpTProxyConfigurationProvider extends ShpurdpServerConfigurationProvider<ShpurdpTProxyConfiguration> {

  @Inject
  public ShpurdpTProxyConfigurationProvider(ShpurdpEventPublisher shpurdpEventPublisher, ShpurdpJpaPersistService persistService) {
    super(ShpurdpServerConfigurationCategory.TPROXY_CONFIGURATION, shpurdpEventPublisher, persistService);
  }

  /**
   * Creates a ShpurdpTProxyConfiguration from a list of {@link ShpurdpConfigurationEntity}s.
   *
   * @param configurationEntities a list of {@link ShpurdpConfigurationEntity}s
   * @return a filled in {@link ShpurdpTProxyConfiguration}
   */
  @Override
  protected ShpurdpTProxyConfiguration loadInstance(Collection<ShpurdpConfigurationEntity> configurationEntities) {
    return new ShpurdpTProxyConfiguration(toProperties(configurationEntities));
  }
}
