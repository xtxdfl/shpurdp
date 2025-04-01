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

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration;
import org.apache.shpurdp.server.ldap.service.ShpurdpLdapException;
import org.apache.shpurdp.server.ldap.service.AttributeDetector;
import org.apache.shpurdp.server.ldap.service.LdapAttributeDetectionService;
import org.apache.shpurdp.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service implementation that performs user and group attribute detection based on a sample set of entries returned by
 * an ldap search operation. A accuracy of detected values may depend on the size of the sample result set
 */
@Singleton
public class DefaultLdapAttributeDetectionService implements LdapAttributeDetectionService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapAttributeDetectionService.class);

  /**
   * The maximum size of the entry set the detection is performed on
   */
  private static final int SAMPLE_RESULT_SIZE = 50;

  @Inject
  private AttributeDetectorFactory attributeDetectorFactory;

  @Inject
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Inject
  public DefaultLdapAttributeDetectionService() {
  }

  @Override
  public ShpurdpLdapConfiguration detectLdapUserAttributes(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    LOG.info("Detecting LDAP user attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(shpurdpLdapConfiguration.userSearchBase())) {
      LOG.warn("No user search base provided");
      return shpurdpLdapConfiguration;
    }

    try {

      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration);
      AttributeDetector<Entry> userAttributeDetector = attributeDetectorFactory.userAttributDetector();

      SearchRequest searchRequest = assembleUserSearchRequest(ldapConnectionTemplate, shpurdpLdapConfiguration);

      // do the search
      List<Entry> entries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());

      for (Entry entry : entries) {
        LOG.info("Collecting user attribute information from the sample entry with dn: [{}]", entry.getDn());
        userAttributeDetector.collect(entry);
      }

      // select attributes based on the collected information
      Map<String, String> detectedUserAttributes = userAttributeDetector.detect();

      // setting the attributes into the configuration
      setDetectedAttributes(shpurdpLdapConfiguration, detectedUserAttributes);

      LOG.info("Decorated shpurdp ldap config : [{}]", shpurdpLdapConfiguration);

    } catch (Exception e) {

      LOG.error("Ldap operation failed while detecting user attributes", e);
      throw new ShpurdpLdapException(e);

    }

    return shpurdpLdapConfiguration;
  }


  @Override
  public ShpurdpLdapConfiguration detectLdapGroupAttributes(ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    LOG.info("Detecting LDAP group attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(shpurdpLdapConfiguration.groupSearchBase())) {
      LOG.warn("No group search base provided");
      return shpurdpLdapConfiguration;
    }

    try {

      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(shpurdpLdapConfiguration);
      AttributeDetector<Entry> groupAttributeDetector = attributeDetectorFactory.groupAttributeDetector();

      SearchRequest searchRequest = assembleGroupSearchRequest(ldapConnectionTemplate, shpurdpLdapConfiguration);

      // do the search
      List<Entry> groupEntries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());

      for (Entry groupEntry : groupEntries) {

        LOG.info("Collecting group attribute information from the sample entry with dn: [{}]", groupEntry.getDn());
        groupAttributeDetector.collect(groupEntry);

      }

      // select attributes based on the collected information
      Map<String, String> detectedGroupAttributes = groupAttributeDetector.detect();

      // setting the attributes into the configuration
      setDetectedAttributes(shpurdpLdapConfiguration, detectedGroupAttributes);

      LOG.info("Decorated shpurdp ldap config : [{}]", shpurdpLdapConfiguration);

    } catch (Exception e) {

      LOG.error("Ldap operation failed while detecting group attributes", e);
      throw new ShpurdpLdapException(e);

    }

    return shpurdpLdapConfiguration;
  }

  private void setDetectedAttributes(ShpurdpLdapConfiguration shpurdpLdapConfiguration, Map<String, String> detectedAttributes) {

    for (Map.Entry<String, String> detecteMapEntry : detectedAttributes.entrySet()) {
      LOG.info("Setting detected configuration value: [{}] - > [{}]", detecteMapEntry.getKey(), detecteMapEntry.getValue());
      ShpurdpServerConfigurationKey key = ShpurdpServerConfigurationKey.translate(LDAP_CONFIGURATION, detecteMapEntry.getKey());
      if(key != null) {
        shpurdpLdapConfiguration.setValueFor(key, detecteMapEntry.getValue());
      }
    }

  }

  private SearchRequest assembleUserSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(shpurdpLdapConfiguration.userSearchBase(),
        FilterBuilder.present("objectClass").toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOG.error("Could not assemble ldap search request", e);
      throw new ShpurdpLdapException(e);
    }
  }

  private SearchRequest assembleGroupSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, ShpurdpLdapConfiguration shpurdpLdapConfiguration) throws ShpurdpLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(shpurdpLdapConfiguration.groupSearchBase(),
        FilterBuilder.present("objectClass").toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOG.error("Could not assemble ldap search request", e);
      throw new ShpurdpLdapException(e);
    }
  }

  public EntryMapper<Entry> getEntryMapper() {
    return new EntryMapper<Entry>() {
      @Override
      public Entry map(Entry entry) throws LdapException {
        return entry;
      }
    };
  }
}
