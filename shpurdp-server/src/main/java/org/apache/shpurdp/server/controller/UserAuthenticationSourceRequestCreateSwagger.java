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

package org.apache.shpurdp.server.controller;

import org.apache.shpurdp.server.controller.internal.UserAuthenticationSourceResourceProvider;
import org.apache.shpurdp.server.security.authorization.UserAuthenticationType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Interface to help correct Swagger documentation generation
 */
public interface UserAuthenticationSourceRequestCreateSwagger extends ApiModel {
  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_SOURCE_RESOURCE_CATEGORY)
  CreateUserAuthenticationSourceInfo getCreateUserAuthenticationSourceRequest();

  interface CreateUserAuthenticationSourceInfo {
    @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_TYPE_PROPERTY_ID, required = true)
    public UserAuthenticationType getAuthenticationType();

    @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.KEY_PROPERTY_ID, required = true)
    public String getKey();
  }
}
