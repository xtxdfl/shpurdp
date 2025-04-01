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

package org.apache.shpurdp.server.configuration;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShpurdpServerConfigurationCategory is an enumeration of the different Shpurdp server specific
 * configuration categories.
 */
public enum ShpurdpServerConfigurationCategory {
  LDAP_CONFIGURATION("ldap-configuration"),
  SSO_CONFIGURATION("sso-configuration"),
  TPROXY_CONFIGURATION("tproxy-configuration");

  private static final Logger LOG = LoggerFactory.getLogger(ShpurdpServerConfigurationCategory.class);
  private final String categoryName;

  ShpurdpServerConfigurationCategory(String categoryName) {
    this.categoryName = categoryName;
  }

  public String getCategoryName() {
    return categoryName;
  }

  /**
   * Safely returns an {@link ShpurdpServerConfigurationCategory} given the category's descriptive name
   *
   * @param categoryName a descriptive name
   * @return an {@link ShpurdpServerConfigurationCategory}
   */
  public static ShpurdpServerConfigurationCategory translate(String categoryName) {
    if (!StringUtils.isEmpty(categoryName)) {
      categoryName = categoryName.trim();
      for (ShpurdpServerConfigurationCategory category : values()) {
        if (category.getCategoryName().equals(categoryName)) {
          return category;
        }
      }
    }

    LOG.warn("Invalid Shpurdp server configuration category: {}", categoryName);
    return null;
  }

  /**
   * Safely returns the {@link ShpurdpServerConfigurationCategory}'s descriptive name or <code>null</code>
   * if no {@link ShpurdpServerConfigurationCategory} was supplied.
   *
   * @param category an {@link ShpurdpServerConfigurationCategory}
   * @return the descriptive name of an {@link ShpurdpServerConfigurationCategory}
   */
  public static String translate(ShpurdpServerConfigurationCategory category) {
    return (category == null) ? null : category.getCategoryName();
  }
}
