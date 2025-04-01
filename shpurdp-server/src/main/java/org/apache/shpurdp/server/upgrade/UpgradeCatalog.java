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

import java.sql.SQLException;
import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;

/**
 * Interface for upgrading Shpurdp DB
 */
public interface UpgradeCatalog {
  /**
   * Run the upgrade scripts for upgrading shpurdp server from current version
   * to the new version.
   * @throws ShpurdpException
   */
  void upgradeSchema() throws ShpurdpException, SQLException;

  /**
   * perform data insertion before running normal upgrade of data, requires started persist service
   * @throws ShpurdpException
   * @throws SQLException
   */
  void preUpgradeData() throws ShpurdpException, SQLException;

  /**
   * perform data updates as necessary, requires started persist service
   * @throws ShpurdpException
   * @throws SQLException
   */
  void upgradeData() throws ShpurdpException, SQLException;

  /**
   * Set the file name, to store all config changes during upgrade
   * @param shpurdpUpgradeConfigUpdatesFileName
   */
  void setConfigUpdatesFileName(String shpurdpUpgradeConfigUpdatesFileName);


  /**
   * Defines if Upgrade Catalog should be executed last
   * @return
   */
  boolean isFinal();

  /**
   * Called after {@link #upgradeSchema()} and {@link #upgradeData()}, this
   * method is used to perform any operations after the catalog has finished.
   * Usually, this is cleanup work that does not directly affect the upgrade.
   *
   * @throws ShpurdpException
   * @throws SQLException
   */
  void onPostUpgrade() throws ShpurdpException, SQLException;

  /**
   * Return the version that will be upgraded to
   * 
   * @return
   */
  String getTargetVersion();

  /**
   * Return latest source version that can be upgraded from.
   * Return null since no UpgradeCatalogs exist before this one.
   *
   * @return null : default
   */
  String getSourceVersion();

  /**
   * Update schema version in the database to the Target one
   */
  void updateDatabaseSchemaVersion();

  /*
  Get upgrade json output, which is sent to python executing process.
   */
  Map<String,String> getUpgradeJsonOutput();
}
