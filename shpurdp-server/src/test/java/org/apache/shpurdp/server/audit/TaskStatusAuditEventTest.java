/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.audit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.audit.event.TaskStatusAuditEvent;
import org.junit.Test;

public class TaskStatusAuditEventTest {

  @Test
  public void testAuditMessage() throws Exception {
    // Given
    String testUserName = "USER1";

    String testRemoteIp = "127.0.0.1";
    String testOperation = "START MYCOMPONENT";
    String testRequestDetails = "Start MyComponent";
    String testHostName = "shpurdp.example.com";
    HostRoleStatus testStatus = HostRoleStatus.IN_PROGRESS;
    Long testRequestId = 100L;
    Long testTaskId = 99L;

    TaskStatusAuditEvent event = TaskStatusAuditEvent.builder()
      .withTimestamp(System.currentTimeMillis())
      .withUserName(testUserName)
      .withRemoteIp(testRemoteIp)
      .withOperation(testOperation)
      .withRequestId(testRequestId.toString())
      .withDetails(testRequestDetails)
      .withHostName(testHostName)
      .withStatus(testStatus.name())
      .withTaskId(testTaskId.toString())
      .build();

    // When
    String actualAuditMessage = event.getAuditMessage();

    // Then
    String expectedAuditMessage = String.format("User(%s), RemoteIp(%s), Operation(%s), Details(%s), Status(%s), RequestId(%d), TaskId(%d), Hostname(%s)", testUserName, testRemoteIp, testOperation, testRequestDetails, testStatus, testRequestId, testTaskId, testHostName);

    assertThat(actualAuditMessage, equalTo(expectedAuditMessage));

  }

}
