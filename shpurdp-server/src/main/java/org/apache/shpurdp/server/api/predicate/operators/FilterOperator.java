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

import org.apache.shpurdp.server.api.predicate.InvalidQueryException;
import org.apache.shpurdp.server.controller.predicate.FilterPredicate;
import org.apache.shpurdp.server.controller.spi.Predicate;

/**
 * This is a binary operator which takes right operand as a regular
 * expression and applies it to the left operand
 */
public class FilterOperator extends AbstractOperator implements RelationalOperator {

  public FilterOperator() {
    super(0);
  }

  @Override
  public String getName() {
    return "FilterOperator";
  }

  @Override
  public Predicate toPredicate(String prop, String val) throws InvalidQueryException {
    if (val == null) {
      throw new InvalidQueryException("Filter operator is missing a required right operand.");
    }
    return new FilterPredicate(prop, val);
  }

  @Override
  public TYPE getType() {
    return TYPE.FILTER;
  }
}
