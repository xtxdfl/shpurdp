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

package org.apache.shpurdp.server.state.theme;

import org.apache.shpurdp.server.controller.ApiModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WidgetEntry implements ApiModel {
	@JsonProperty("config")
	private String config;

	@JsonProperty("widget")
	private Widget widget;

  @ApiModelProperty(name = "config")
  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  @ApiModelProperty(name = "widget")
  public Widget getWidget() {
    return widget;
  }

  public void setWidget(Widget widget) {
    this.widget = widget;
  }
}