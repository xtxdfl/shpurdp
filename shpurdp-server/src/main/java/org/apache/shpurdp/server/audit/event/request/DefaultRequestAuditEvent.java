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
package org.apache.shpurdp.server.audit.event.request;

import javax.annotation.concurrent.Immutable;

import org.apache.shpurdp.server.audit.request.RequestAuditEvent;
import org.apache.shpurdp.server.audit.request.RequestAuditLogger;

/**
 * Default audit event for {@link RequestAuditLogger}.
 */
@Immutable
public class DefaultRequestAuditEvent extends RequestAuditEvent {

  public static class DefaultRequestAuditEventBuilder
    extends RequestAuditEvent.RequestAuditEventBuilder<DefaultRequestAuditEvent, DefaultRequestAuditEventBuilder> {

    private DefaultRequestAuditEventBuilder() {
      super(DefaultRequestAuditEventBuilder.class);
    }

    @Override
    protected DefaultRequestAuditEvent newAuditEvent() {
      return new DefaultRequestAuditEvent(this);
    }
  }

  protected DefaultRequestAuditEvent() {
  }

  private DefaultRequestAuditEvent(DefaultRequestAuditEventBuilder builder) {
    super(builder);
  }

  public static DefaultRequestAuditEventBuilder builder() {
    return new DefaultRequestAuditEventBuilder();
  }
}
