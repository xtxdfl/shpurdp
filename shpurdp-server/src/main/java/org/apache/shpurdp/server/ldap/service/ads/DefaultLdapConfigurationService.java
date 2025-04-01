/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.ldap.service.ads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapException;
import org.apache.shpurdp.server.ldap.service.LdapConfigurationService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the validation logic using the Apache Directory API.
 */
@Singleton
public class DefaultLdapConfigurationService implements LdapConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLdapConfigurationService.class);

  @Inject
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Inject
  public DefaultLdapConfigurationService() {
  }


  @Override
  public void checkConnection(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    LOGGER.info("Trying to connect to the LDAP server using provided configuration...");
    LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration);

    // check if the connection from the connection pool of the template is connected
    Boolean isConnected = ldapConnectionTemplate.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doWithConnection(LdapConnection connection) throws LdapException {
        return connection.isConnected() && connection.isAuthenticated();
      }
    });

    if (!isConnected) {
      LOGGER.error("Could not connect to the LDAP server");
      throw new ShpurdpLdapException("Could not connect to the LDAP server. Configuration: " + shpurdpLdapConfiguration);
    }

    LOGGER.info("Successfully conencted to the LDAP.");

  }

  /**
   * Checks the user attributes provided in the configuration instance by issuing a search for a (known) test user in the LDAP.
   * Attributes are considered correct if there is at least one entry found.
   *
   * Invalid attributes are signaled by throwing an exception.
   *
   * @param testUserName            the test username
   * @param testPassword            the test password
   * @param shpurdpLdapConfiguration the available LDAP configuration to be validated
   * @return the DN of the test user
   * @throws ShpurdpLdapException if an error occurs
   */
  @Override
  public String checkUserAttributes(String testUserName, String testPassword, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    String userDn;
    try {
      LOGGER.info("Checking user attributes for user [{}] ...", testUserName);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, shpurdpLdapConfiguration.userObjectClass()),
        FilterBuilder.equal(shpurdpLdapConfiguration.userNameAttribute(), testUserName))
        .toString();

      LOGGER.info("Searching for the user: [{}] using the search filter: [{}]", testUserName, filter);
      userDn = ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration).searchFirst(new Dn(shpurdpLdapConfiguration.userSearchBase()), filter, SearchScope.SUBTREE, getUserDnNameEntryMapper(shpurdpLdapConfiguration));

      if (null == userDn) {
        LOGGER.info("Could not find test user based on the provided configuration. User attributes may not be complete or the user may not exist.");
        throw new ShpurdpLdapException("Could not find test user based on the provided configuration. User attributes may not be complete or the user may not exist.");
      }
      LOGGER.info("Attribute validation succeeded. Filter: [{}]", filter);


    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new ShpurdpLdapException(e.getMessage(), e);

    }
    return userDn;
  }

  /**
   * Checks whether the provided group related settings are correct.
   *
   * @param userDn                  a user DN to check
   * @param shpurdpLdapConfiguration the available LDAP configuration to be validated
   * @return
   * @throws ShpurdpLdapException
   */
  @Override
  public Set<String> checkGroupAttributes(String userDn, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    List<String> groups;
    try {
      LOGGER.info("Checking group attributes for user dn: [{}] ...", userDn);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, shpurdpLdapConfiguration.groupObjectClass()),
        FilterBuilder.equal(shpurdpLdapConfiguration.groupMemberAttribute(), userDn)
      ).toString();

      LOGGER.info("Searching for the groups the user dn: [{}] is member of using the search filter: [{}]", userDn, filter);
      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration);

      // assemble a search request
      SearchRequest searchRequest = ldapConnectionTemplate.newSearchRequest(new Dn(shpurdpLdapConfiguration.groupSearchBase()), filter, SearchScope.SUBTREE);
      // attributes to be returned
      searchRequest.addAttributes(shpurdpLdapConfiguration.groupMemberAttribute(), shpurdpLdapConfiguration.groupNameAttribute());

      // perform the search
      groups = ldapConnectionTemplate.search(searchRequest, getGroupNameEntryMapper(shpurdpLdapConfiguration));

      if (groups == null || groups.isEmpty()) {
        LOGGER.info("No groups found for the user dn. Group attributes configuration is incomplete");
        throw new ShpurdpLdapException("Group attribute ldap configuration is incomplete");
      }

      LOGGER.info("Group attribute configuration check succeeded.");

    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new ShpurdpLdapException(e.getMessage(), e);

    }

    return new HashSet<>(groups);
  }


  /**
   * Entry mapper for handling user search results.
   *
   * @param shpurdpLdapConfiguration shpurdp ldap configuration values
   * @return user dn entry mapper instance
   */
  private EntryMapper<String> getGroupNameEntryMapper(ShpurdpLdapConfiguration shpurdpLdapConfiguration) {

    EntryMapper<String> entryMapper = new EntryMapper<String>() {
      @Override
      public String map(Entry entry) throws LdapException {
        return entry.get(shpurdpLdapConfiguration.groupNameAttribute()).get().getValue();
      }
    };

    return entryMapper;
  }

  /**
   * Entry mapper for handling group searches.
   *
   * @param shpurdpLdapConfiguration shpurdp ldap configuration values
   * @return
   */
  private EntryMapper<String> getUserDnNameEntryMapper(ShpurdpLdapConfiguration shpurdpLdapConfiguration) {

    EntryMapper<String> entryMapper = new EntryMapper<String>() {
      @Override
      public String map(Entry entry) throws LdapException {
        return entry.getDn().getNormName();
      }
    };

    return entryMapper;
  }


}



