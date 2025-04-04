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

package org.apache.shpurdp.server.security.authentication;

public class InvalidUsernamePasswordCombinationException extends ShpurdpAuthenticationException {

  public static final String MESSAGE = "Unable to sign in. Invalid username/password combination.";

  public InvalidUsernamePasswordCombinationException(String username) {
    super(username, MESSAGE, true);
  }

  public InvalidUsernamePasswordCombinationException(String username, boolean incrementFailureCount) {
    super(username, MESSAGE, incrementFailureCount);
  }

  public InvalidUsernamePasswordCombinationException(String username, Throwable t) {
    super(username, MESSAGE, true, t);
  }

  public InvalidUsernamePasswordCombinationException(String username, boolean incrementFailureCount, Throwable t) {
    super(username, MESSAGE, incrementFailureCount, t);
  }
}
