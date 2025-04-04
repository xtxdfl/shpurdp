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

package org.apache.shpurdp.server.controller.spi;

import java.util.List;

/**
 *  Interface to allow the plugging in of resource adapters.
 */
public interface ProviderModule {
  /**
   * Get a resource adapter for the given resource type.
   *
   * @param type  the resource type
   *
   * @return the resource adapter
   */
  ResourceProvider getResourceProvider(Resource.Type type);

  /**
   * Get the list of property providers for the given resource type.
   *
   * @param type  the resource type
   *
   * @return the list of property providers
   */
  List<PropertyProvider> getPropertyProviders(Resource.Type type);
}
