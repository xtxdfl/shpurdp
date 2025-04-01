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
package org.apache.shpurdp.server.security.authentication.jwt;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.orm.entities.UserAuthenticationEntity;
import org.apache.shpurdp.server.orm.entities.UserEntity;
import org.apache.shpurdp.server.security.authentication.AccountDisabledException;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationException;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserAuthentication;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserDetails;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserDetailsImpl;
import org.apache.shpurdp.server.security.authentication.TooManyLoginFailuresException;
import org.apache.shpurdp.server.security.authentication.UserNotFoundException;
import org.apache.shpurdp.server.security.authorization.AuthorizationHelper;
import org.apache.shpurdp.server.security.authorization.UserAuthenticationType;
import org.apache.shpurdp.server.security.authorization.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.google.inject.Inject;

/**
 * ShpurdpLocalAuthenticationProvider is an {@link org.springframework.security.authentication.AuthenticationProvider}
 * implementation used to authenticate users using username and password details from the local Shpurdp database.
 * <p>
 * Users will fail to authenticate, even if they supply the correct credentials if the account is locked out
 * by being disabled or locked due to too many consecutive failure.
 */
public class ShpurdpJwtAuthenticationProvider extends ShpurdpAuthenticationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(ShpurdpJwtAuthenticationProvider.class);

  /**
   * Constructor.
   *
   * @param users         the users helper
   * @param configuration the configuration
   */
  @Inject
  public ShpurdpJwtAuthenticationProvider(Users users, Configuration configuration) {
    super(users, configuration);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication.getName() == null) {
      LOG.info("Authentication failed: no username provided");
      throw new ShpurdpAuthenticationException(null, "Unexpected error due to missing username", false);
    }

    String userName = authentication.getName().trim();

    if (authentication.getCredentials() == null) {
      LOG.info("Authentication failed: no credentials provided: {}", userName);
      throw new ShpurdpAuthenticationException(userName, "Unexpected error due to missing JWT token", false);
    }

    Users users = getUsers();
    UserEntity userEntity = users.getUserEntity(userName);

    if (userEntity == null) {
      LOG.info("User not found: {}", userName);
      throw new UserNotFoundException(userName, "Cannot find user from JWT. Please, ensure LDAP is configured and users are synced.");
    }

    // If the user was found and allowed to log in, make sure that user is allowed to authenticate using a JWT token.
    boolean authOK = false;
    UserAuthenticationEntity authenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.JWT);
    if (authenticationEntity != null) {
      authOK = true;
    } else {
      // TODO: Determine if LDAP users can authenticate using JWT - for now we assume yes.
      // If a JWT entity was not found, see if an LDAP entity exists. If so, this user was synced
      // with a remote server and this should be allowed to authenticate using JWT
      authenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.LDAP);

      if (authenticationEntity != null) {
        try {
          users.addJWTAuthentication(userEntity, userName);
          authOK = true;
        } catch (ShpurdpException e) {
          LOG.error(String.format("Failed to add the JWT authentication method for %s: %s", userName, e.getLocalizedMessage()), e);
          throw new ShpurdpAuthenticationException(userName, "Unexpected error has occurred", false, e);
        }
      }
    }

    if (authOK) {
      // The user was  authenticated, return the authenticated user object
      LOG.debug("Authentication succeeded - a matching user was found: {}", userName);

      // Ensure the user account is allowed to log in
      try {
        users.validateLogin(userEntity, userName);
      } catch (AccountDisabledException | TooManyLoginFailuresException e) {
        if (getConfiguration().showLockedOutUserMessage()) {
          throw e;
        } else {
          // Do not give away information about the existence or status of a user
          throw new ShpurdpAuthenticationException(userName, "Unexpected error due to missing JWT token", false);
        }
      }

      ShpurdpUserDetails userDetails = new ShpurdpUserDetailsImpl(users.getUser(userEntity), null, users.getUserAuthorities(userEntity));
      
      String jwtTokenName = userDetails.getUsername().trim();
      //If JwtToken Provided Username and authenticatedUsername is different Add it to Alias
      if(!userName.equals(jwtTokenName)){
        AuthorizationHelper.addLoginNameAlias(userName,jwtTokenName);
      }
      return new ShpurdpUserAuthentication(authentication.getCredentials().toString(), userDetails, true);
    } else {
      // The user was not authenticated, fail
      LOG.debug("Authentication failed: password does not match stored value: {}", userName);
      throw new UserNotFoundException(userName, "Cannot find user from JWT. Please, ensure LDAP is configured and users are synced.");
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthenticationToken.class.isAssignableFrom(authentication);
  }
}