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
package org.apache.shpurdp.server.security.authentication.pam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.orm.entities.GroupEntity;
import org.apache.shpurdp.server.orm.entities.MemberEntity;
import org.apache.shpurdp.server.orm.entities.UserAuthenticationEntity;
import org.apache.shpurdp.server.orm.entities.UserEntity;
import org.apache.shpurdp.server.security.ClientSecurityType;
import org.apache.shpurdp.server.security.authentication.AccountDisabledException;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationException;
import org.apache.shpurdp.server.security.authentication.ShpurdpAuthenticationProvider;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserAuthentication;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserDetails;
import org.apache.shpurdp.server.security.authentication.ShpurdpUserDetailsImpl;
import org.apache.shpurdp.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.apache.shpurdp.server.security.authentication.TooManyLoginFailuresException;
import org.apache.shpurdp.server.security.authorization.GroupType;
import org.apache.shpurdp.server.security.authorization.UserAuthenticationType;
import org.apache.shpurdp.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.google.inject.Inject;

/**
 * Provides PAM user authentication logic for Shpurdp Server
 * <p>
 * It is expected that PAM is properly configured in the underlying operating system for this
 * authentication provider to work properly.
 */
public class ShpurdpPamAuthenticationProvider extends ShpurdpAuthenticationProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ShpurdpPamAuthenticationProvider.class);

  private final PamAuthenticationFactory pamAuthenticationFactory;

  @Inject
  public ShpurdpPamAuthenticationProvider(Users users, PamAuthenticationFactory pamAuthenticationFactory, Configuration configuration) {
    super(users, configuration);
    this.pamAuthenticationFactory = pamAuthenticationFactory;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (isPamEnabled()) {
      if (authentication.getName() == null) {
        LOG.info("Authentication failed: no username provided");
        throw new InvalidUsernamePasswordCombinationException("");
      }

      String userName = authentication.getName().trim();

      if (authentication.getCredentials() == null) {
        LOG.info("Authentication failed: no credentials provided: {}", userName);
        throw new InvalidUsernamePasswordCombinationException(userName);
      }

      Users users = getUsers();

      UserEntity userEntity = users.getUserEntity(userName);
      String password = String.valueOf(authentication.getCredentials());
      String shpurdpUsername;
      String localUsername;

      // Determine what the Shpurdp and local username values are.  Most of the time these should be
      // the same, however it is possible for the user names to be different in the event a user has
      // multiple authentication sources.
      if (userEntity == null) {
        shpurdpUsername = userName;
        localUsername = userName;
      } else {
        // If the user exists, the username to be used with PAM may be stored with the PAM-specific UserAuthenticationEntity
        // Else, use the UserEntity#getLocalUsername value
        // Else, use the UserEntity#getUserName value
        UserAuthenticationEntity authenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.PAM);

        shpurdpUsername = userEntity.getUserName();

        if (authenticationEntity == null) {
          localUsername = userEntity.getLocalUsername();
        } else {
          localUsername = authenticationEntity.getAuthenticationKey();

          if (StringUtils.isEmpty(localUsername)) {
            localUsername = userEntity.getLocalUsername();
          }
        }

        if (StringUtils.isEmpty(localUsername)) {
          localUsername = shpurdpUsername;
        }
      }

      // Perform authentication....
      UnixUser unixUser = performPAMAuthentication(shpurdpUsername, localUsername, password);

      if (unixUser != null) {
        // Authentication was successful via PAM.  Make sure that the user exists and has a PAM
        // authentication entry.
        if (userEntity == null) {
          // TODO: Ensure automatically creating users when authenticating with PAM is allowed.
          try {
            userEntity = users.createUser(shpurdpUsername, unixUser.getUserName(), shpurdpUsername, true);
          } catch (ShpurdpException e) {
            LOG.error(String.format("Failed to add the user, %s: %s", shpurdpUsername, e.getLocalizedMessage()), e);
            throw new ShpurdpAuthenticationException(shpurdpUsername, "Unexpected error has occurred", false, e);
          }
        } else {
          // Ensure the user is allowed to login....
          try {
            users.validateLogin(userEntity, shpurdpUsername);
          } catch (AccountDisabledException | TooManyLoginFailuresException e) {
            if (getConfiguration().showLockedOutUserMessage()) {
              throw e;
            } else {
              // Do not give away information about the existence or status of a user
              throw new InvalidUsernamePasswordCombinationException(userName, false, e);
            }
          }
        }

        UserAuthenticationEntity authenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.PAM);
        // TODO: Ensure automatically adding the PAM authentication method for users when authenticating is allowed.
        if (authenticationEntity == null) {
          try {
            users.addPamAuthentication(userEntity, unixUser.getUserName());
          } catch (ShpurdpException e) {
            LOG.error(String.format("Failed to add the PAM authentication method for %s: %s", shpurdpUsername, e.getLocalizedMessage()), e);
            throw new ShpurdpAuthenticationException(shpurdpUsername, "Unexpected error has occurred", false, e);
          }
        }

        if (isAutoGroupCreationAllowed()) {
          synchronizeGroups(unixUser, userEntity);
        }

        ShpurdpUserDetails userDetails = new ShpurdpUserDetailsImpl(users.getUser(userEntity), null, users.getUserAuthorities(userEntity));
        return new ShpurdpUserAuthentication(password, userDetails, true);
      }


      // The user was not authenticated, catch-all fail
      LOG.debug(String.format("Authentication failed: password does not match stored value: %s", localUsername));
      throw new InvalidUsernamePasswordCombinationException(shpurdpUsername);
    } else {
      return null;
    }
  }

  /**
   * Perform the OS-level PAM authentication routine.
   *
   * @param shpurdpUsername the Shpurdp username, used for logging and notifications
   * @param localUsername  the username to use for authenticating
   * @param password       the password to use for authenticating
   * @return the resulting user object
   */
  private UnixUser performPAMAuthentication(String shpurdpUsername, String localUsername, String password) {
    PAM pam = pamAuthenticationFactory.createInstance(getConfiguration());

    if (pam == null) {
      String message = "Failed to authenticate the user using the PAM authentication method: unexpected error";
      LOG.error(message);
      throw new ShpurdpAuthenticationException(shpurdpUsername, message, false);
    } else {
      if (LOG.isDebugEnabled() && !shpurdpUsername.equals(localUsername)) {
        LOG.debug("Authenticating Shpurdp user {} using the local username {}", shpurdpUsername, localUsername);
      }

      try {
        // authenticate using PAM
        return pam.authenticate(localUsername, password);
      } catch (PAMException e) {
        // The user was not authenticated, fail
        LOG.debug(String.format("Authentication failed: password does not match stored value: %s", localUsername), e);
        throw new InvalidUsernamePasswordCombinationException(shpurdpUsername, true, e);
      } finally {
        pam.dispose();
      }
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Check if PAM authentication is enabled in server properties
   *
   * @return true if enabled
   */
  private boolean isPamEnabled() {
    return getConfiguration().getClientSecurityType() == ClientSecurityType.PAM;
  }

  /**
   * Check if PAM authentication is enabled in server properties
   *
   * @return true if enabled
   */
  private boolean isAutoGroupCreationAllowed() {
    return getConfiguration().getAutoGroupCreation().equals("true");
  }


  /**
   * Synchornizes the OS-level groups assigned to the OS-level user with the groups assigned to the
   * Shpurdp user in Shpurdp
   *
   * @param unixUser   the user
   * @param userEntity the shpurdp user
   */
  private void synchronizeGroups(UnixUser unixUser, UserEntity userEntity) {
    LOG.debug("Synchronizing groups for PAM user: {}", unixUser.getUserName());

    Users users = getUsers();

    try {
      //Get all the groups that user belongs to
      //Change all group names to lower case.
      Set<String> unixUserGroups = convertToLowercase(unixUser.getGroups());

      // Add the user to the specified groups, create the group if needed...
      for (String group : unixUserGroups) {
        GroupEntity groupEntity = users.getGroupEntity(group, GroupType.PAM);
        if (groupEntity == null) {
          LOG.info("Synchronizing groups for {}, adding new PAM group: {}", userEntity.getUserName(), group);
          groupEntity = users.createGroup(group, GroupType.PAM);
        }

        if (!users.isUserInGroup(userEntity, groupEntity)) {
          LOG.info("Synchronizing groups for {}, adding user to PAM group: {}", userEntity.getUserName(), group);
          users.addMemberToGroup(groupEntity, userEntity);
        }
      }

      // Remove the user from any other PAM-specific group that the user may have been previously
      // added to. If the user belongs to non-PAM-specific groups, do not alter those assignments.
      Set<MemberEntity> memberEntities = userEntity.getMemberEntities();
      if (memberEntities != null) {
        Collection<GroupEntity> groupsToRemove = new ArrayList<>();
        // Collect the groups to remove...
        for (MemberEntity memberEntity : memberEntities) {
          GroupEntity groupEntity = memberEntity.getGroup();
          if ((groupEntity.getGroupType() == GroupType.PAM) && !unixUserGroups.contains(groupEntity.getGroupName())) {
            groupsToRemove.add(groupEntity);
          }
        }

        // Perform the removals...
        for(GroupEntity groupEntity :groupsToRemove) {
          LOG.info("Synchronizing groups for {}, removing user from PAM group: {}", userEntity.getUserName(), groupEntity.getGroupName());
          users.removeMemberFromGroup(groupEntity, userEntity);
        }
      }
    } catch (ShpurdpException e) {
      e.printStackTrace();
    }
  }

  private Set<String> convertToLowercase(Set<String> groups) {
    Set<String> lowercaseGroups = new HashSet<>();

    if (groups != null) {
      for (String group : groups) {
        lowercaseGroups.add(group.toLowerCase());
      }
    }

    return lowercaseGroups;
  }
}
