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

package org.apache.shpurdp.server.ldap.service.ads.detectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UserNameAttrDetector extends OccurrenceAndWeightBasedDetector {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserNameAttrDetector.class);

  private enum UserNameAttrs {
    SAM_ACCOUNT_NAME("sAMAccountName", 5),
    UID("uid", 3),
    CN("cn", 1);

    private String attrName;
    private Integer weight;

    UserNameAttrs(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public UserNameAttrDetector() {
    for (UserNameAttrs nameAttr : UserNameAttrs.values()) {
      occurrenceMap().put(nameAttr.attrName(), 0);
      weightsMap().put(nameAttr.attrName(), nameAttr.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    LOGGER.info("Checking for attribute  [{}] in entry [{}]", attribute, entry.getDn());
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return ShpurdpServerConfigurationKey.USER_NAME_ATTRIBUTE.key();
  }

}
