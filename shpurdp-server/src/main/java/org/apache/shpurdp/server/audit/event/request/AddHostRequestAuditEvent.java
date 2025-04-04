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

package org.apache.shpurdp.server.audit.event.request;

import javax.annotation.concurrent.Immutable;

import org.apache.shpurdp.server.audit.request.RequestAuditEvent;

/**
 * Audit event for adding host
 */
@Immutable
public class AddHostRequestAuditEvent extends RequestAuditEvent {

  public static class AddHostRequestAuditEventBuilder extends RequestAuditEventBuilder<AddHostRequestAuditEvent, AddHostRequestAuditEventBuilder> {

    /**
     * Hostname that is added
     */
    private String hostName;

    public AddHostRequestAuditEventBuilder() {
      super(AddHostRequestAuditEventBuilder.class);
      super.withOperation("Host addition");
    }

    @Override
    protected AddHostRequestAuditEvent newAuditEvent() {
      return new AddHostRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Hostname(")
        .append(hostName)
        .append(")");
    }

    public AddHostRequestAuditEventBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }
  }

  protected AddHostRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddHostRequestAuditEvent(AddHostRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddHostRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddHostRequestAuditEventBuilder builder() {
    return new AddHostRequestAuditEventBuilder();
  }

}
