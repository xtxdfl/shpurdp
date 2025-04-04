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
package org.apache.shpurdp.server.events;


/**
 * The {@link AggregateAlertRecalculateEvent} is used to trigger the
 * recalculation of all aggregate alerts.
 */
public class AggregateAlertRecalculateEvent extends AlertEvent {

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster to recalculate aggregate alerts for.
   */
  public AggregateAlertRecalculateEvent(long clusterId) {
    super(clusterId, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("AggregateAlertRecalculateEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append("}");
    return buffer.toString();
  }
}
