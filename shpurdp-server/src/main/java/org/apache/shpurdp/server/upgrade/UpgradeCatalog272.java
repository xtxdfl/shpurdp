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
package org.apache.shpurdp.server.upgrade;

import static org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION;
import static org.apache.shpurdp.server.security.authorization.RoleAuthorization.SHPURDP_VIEW_STATUS_INFO;
import static org.apache.shpurdp.server.security.authorization.RoleAuthorization.CLUSTER_MANAGE_WIDGETS;
import static org.apache.shpurdp.server.upgrade.UpgradeCatalog270.SHPURDP_CONFIGURATION_CATEGORY_NAME_COLUMN;
import static org.apache.shpurdp.server.upgrade.UpgradeCatalog270.SHPURDP_CONFIGURATION_PROPERTY_NAME_COLUMN;
import static org.apache.shpurdp.server.upgrade.UpgradeCatalog270.SHPURDP_CONFIGURATION_TABLE;

import java.sql.SQLException;
import java.util.Collections;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.orm.DBAccessor;
import org.apache.shpurdp.server.state.BlueprintProvisioningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog272} upgrades Shpurdp from 2.7.1 to 2.7.2.
 */
public class UpgradeCatalog272 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog272.class);

  private static final String LDAP_CONFIGURATION_WRONG_COLLISION_BEHAVIOR_PROPERTY_NAME = "shpurdp.ldap.advance.collision_behavior";
  private static final String LDAP_CONFIGURATION_CORRECT_COLLISION_BEHAVIOR_PROPERTY_NAME = "shpurdp.ldap.advanced.collision_behavior";
  static final String RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL = String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s' AND %s = '%s'", SHPURDP_CONFIGURATION_TABLE,
      SHPURDP_CONFIGURATION_PROPERTY_NAME_COLUMN, LDAP_CONFIGURATION_CORRECT_COLLISION_BEHAVIOR_PROPERTY_NAME, SHPURDP_CONFIGURATION_CATEGORY_NAME_COLUMN,
      LDAP_CONFIGURATION.getCategoryName(), SHPURDP_CONFIGURATION_PROPERTY_NAME_COLUMN, LDAP_CONFIGURATION_WRONG_COLLISION_BEHAVIOR_PROPERTY_NAME);

  protected static final String HOST_COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  protected static final String CLUSTERS_TABLE = "clusters";
  protected static final String BLUEPRINT_PROVISIONING_STATE_COLUMN = "blueprint_provisioning_state";

  @Inject
  public UpgradeCatalog272(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.1";
  }

  @Override
  public String getTargetVersion() {
    return "2.7.2";
  }

  @Override
  protected void executeDDLUpdates() throws ShpurdpException, SQLException {
    moveBlueprintProvisioningState();
  }

  @Override
  protected void executePreDMLUpdates() throws ShpurdpException, SQLException {
    // nothing to do
  }

  @Override
  protected void executeDMLUpdates() throws ShpurdpException, SQLException {
    renameLdapSynchCollisionBehaviorValue();
    createRoleAuthorizations();
  }

  protected int renameLdapSynchCollisionBehaviorValue() throws SQLException {
    int numberOfRecordsRenamed = 0;
    if (dbAccessor.tableExists(SHPURDP_CONFIGURATION_TABLE)) {
      LOG.debug("Executing: {}", RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL);
      numberOfRecordsRenamed = dbAccessor.executeUpdate(RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL);
      LOG.info("Renamed {} {} with incorrect LDAP configuration property name", numberOfRecordsRenamed, 1 >= numberOfRecordsRenamed ? "record" : "records");
    } else {
      LOG.info("{} table does not exists; nothing to update", SHPURDP_CONFIGURATION_TABLE);
    }
    return numberOfRecordsRenamed;
  }

  protected void createRoleAuthorizations() throws SQLException {
    addRoleAuthorization(SHPURDP_VIEW_STATUS_INFO.getId(), "View status information", Collections.singleton("SHPURDP.ADMINISTRATOR:SHPURDP"));
    LOG.info("Added new role authorization {}", SHPURDP_VIEW_STATUS_INFO.getId());
    addRoleAuthorization(CLUSTER_MANAGE_WIDGETS.getId(), "Manage widgets", Sets.newHashSet( "SHPURDP.ADMINISTRATOR:SHPURDP", "CLUSTER.ADMINISTRATOR:CLUSTER", "CLUSTER.OPERATOR:CLUSTER"));
    LOG.info("Added new role authorization {}", CLUSTER_MANAGE_WIDGETS.getId());
  }

  protected void moveBlueprintProvisioningState() throws SQLException {
    dbAccessor.dropColumn(CLUSTERS_TABLE, BLUEPRINT_PROVISIONING_STATE_COLUMN);
    dbAccessor.addColumn(HOST_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(BLUEPRINT_PROVISIONING_STATE_COLUMN, String.class, 255,
            BlueprintProvisioningState.NONE, true));
  }

}
