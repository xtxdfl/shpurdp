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

package org.apache.shpurdp.server.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data model for Shpurdp Agent to send heartbeat to Shpurdp Controller.
 */
public class Register {
  private int responseId = -1;
  private long timestamp;
  private long agentStartTime;
  private String hostname;
  private int currentPingPort;
  private HostInfo hardwareProfile;
  private String publicHostname;
  private AgentEnv agentEnv;
  private String agentVersion;
  private String prefix;

  @JsonProperty("id")
  @JsonAlias({"id", "responseId"})
  public int getResponseId() {
    return responseId;
  }

  @JsonProperty("id")
  @JsonAlias( { "id", "responseId"})
  public void setResponseId(int responseId) {
    this.responseId = responseId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public HostInfo getHardwareProfile() {
    return hardwareProfile;
  }

  public void setHardwareProfile(HostInfo hardwareProfile) {
    this.hardwareProfile = hardwareProfile;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getPublicHostname() {
    return publicHostname;
  }

  public void setPublicHostname(String name) {
    this.publicHostname = name;
  }

  public AgentEnv getAgentEnv() {
    return agentEnv;
  }

  public void setAgentEnv(AgentEnv env) {
    this.agentEnv = env;
  }

  public String getAgentVersion() {
    return agentVersion;
  }

  public String getPrefix() { return prefix; }

  public void setPrefix(String prefix) { this.prefix = prefix; }

  public void setAgentVersion(String agentVersion) {
    this.agentVersion = agentVersion;
  }

  public int getCurrentPingPort() {
    return currentPingPort;
  }

  public void setCurrentPingPort(int currentPingPort) {
    this.currentPingPort = currentPingPort;
  }

  public long getAgentStartTime() {
    return agentStartTime;
  }

  public void setAgentStartTime(long agentStartTime) {
    this.agentStartTime = agentStartTime;
  }

  @Override
  public String toString() {
    String ret = "responseId=" + responseId + "\n" +
             "timestamp=" + timestamp + "\n" +
             "startTime=" + agentStartTime + "\n" +
             "hostname="  + hostname + "\n" +
             "currentPingPort=" + currentPingPort + "\n" +
             "prefix=" + prefix + "\n";

    if (hardwareProfile != null)
      ret = ret + "hardwareprofile=" + this.hardwareProfile;
    return ret;
  }
}
