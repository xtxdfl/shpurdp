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
package org.apache.shpurdp.server.api.predicate.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.shpurdp.server.controller.predicate.FilterPredicate;
import org.junit.Test;

public class FilterOperatorTest {
  @Test
  public void testGetName() {
    assertEquals("FilterOperator", new FilterOperator().getName());
  }

  @Test
  public void testToPredicate() throws Exception {
    assertEquals(new FilterPredicate("p1", ".*"),
                 new FilterOperator().toPredicate("p1", ".*"));
  }

  @Test
  public void testGetType() {
    assertSame(Operator.TYPE.FILTER, new FilterOperator().getType());
  }

  @Test
  public void testGetBasePrecedence() {
    assertEquals(-1, new FilterOperator().getBasePrecedence());
  }

  @Test
  public void testGetPrecedence() {
    assertEquals(-1, new FilterOperator().getPrecedence());
  }
}
