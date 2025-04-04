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

package org.apache.shpurdp.server.api.predicate.expressions;

import org.apache.shpurdp.server.api.predicate.InvalidQueryException;
import org.apache.shpurdp.server.api.predicate.operators.RelationalOperator;
import org.apache.shpurdp.server.controller.spi.Predicate;

/**
 * Relational Expression.
 * Consists of a property name for the left operand, a relational operator
 * and a value as the right operand.
 */
public class RelationalExpression extends AbstractExpression<String> {

  /**
   * Constructor.
   *
   * @param op  relational operator
   */
  public RelationalExpression(RelationalOperator op) {
    super(op);
  }

  @Override
  public Predicate toPredicate() throws InvalidQueryException {
    return ((RelationalOperator) getOperator()).
        toPredicate(getLeftOperand(), getRightOperand());
  }

  @Override
  public String toString() {
    return "RelationalExpression{ property='" + getLeftOperand() + "\', value='"
        + getRightOperand() + "\', op=" + getOperator() + " }";
  }
}
