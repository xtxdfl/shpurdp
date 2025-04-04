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

package org.apache.shpurdp.server.api.services.stackadvisor.commands;

import java.io.File;

import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.shpurdp.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.shpurdp.server.controller.internal.ShpurdpServerConfigurationHandler;
import org.apache.shpurdp.server.state.ServiceInfo;

/**
 * {@link StackAdvisorCommand} implementation for configuration validation.
 */
public class ConfigurationValidationCommand extends StackAdvisorCommand<ValidationResponse> {

  public ConfigurationValidationCommand(File recommendationsDir,
                                        String recommendationsArtifactsLifetime,
                                        ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                        int requestId,
                                        StackAdvisorRunner saRunner,
                                        ShpurdpMetaInfo metaInfo,
                                        ShpurdpServerConfigurationHandler shpurdpServerConfigurationHandler) {
    super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, saRunner, metaInfo, shpurdpServerConfigurationHandler);
  }

  @Override
  protected StackAdvisorCommandType getCommandType() {
    return StackAdvisorCommandType.VALIDATE_CONFIGURATIONS;
  }

  @Override
  protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
    if (request.getHosts() == null || request.getHosts().isEmpty() || request.getServices() == null
        || request.getServices().isEmpty()) {
      throw new StackAdvisorException("Hosts, services and configurations must not be empty");
    }
  }

  @Override
  protected ValidationResponse updateResponse(StackAdvisorRequest request,
      ValidationResponse response) {
    return response;
  }

  @Override
  protected String getResultFileName() {
    return "configurations-validation.json";
  }
}
