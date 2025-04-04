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

package org.apache.shpurdp.server.serveraction.kerberos;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.actionmanager.HostRoleStatus;
import org.apache.shpurdp.server.agent.CommandReport;
import org.apache.shpurdp.server.agent.stomp.TopologyHolder;
import org.apache.shpurdp.server.events.TopologyUpdateEvent;
import org.apache.shpurdp.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.shpurdp.server.utils.ShellCommandUtil;
import org.apache.shpurdp.server.utils.StageUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;

@SuppressWarnings("UnstableApiUsage")
public class FinalizeKerberosServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(FinalizeKerberosServerAction.class);

  private final TopologyHolder topologyHolder;

  /**
   * Used to prevent multiple threads from working with the same keytab.
   */
  private Striped<Lock> m_locksByKeytab = Striped.lazyWeakLock(25);

  @Inject
  public FinalizeKerberosServerAction(TopologyHolder topologyHolder) {
    this.topologyHolder = topologyHolder;
  }

  /**
   * Processes an identity as necessary.
   * <p/>
   * This implementation ensures that keytab files for the Shpurdp identities have the correct
   * permissions.  This is important in the event a secure cluster was created via Blueprints since
   * some user accounts and groups may not have been available (at the OS level) when the keytab files
   * were created.
   *
   * @param resolvedPrincipal        a ResolvedKerberosPrincipal object to process
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param includedInFilter         a Boolean value indicating whather the principal is included in
   *                                 the current filter or not
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request  @return null, always
   * @throws ShpurdpException
   */
  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          boolean includedInFilter,
                                          Map<String, Object> requestSharedDataContext)
      throws ShpurdpException {

    if (resolvedPrincipal != null) {
      // If the record's HOSTNAME value is "shpurdp-server", rather than an actual hostname it will
      // not match the Shpurdp server's host name. This will occur if the there is no agent installed
      // on the Shpurdp server host.  This is ok, since any keytab files installed on the Shpurdp server
      // host will already have the permissions set so that only the Shpurdp server can read it.
      // There is no need to update the permissions for those keytab files so that installed services
      // can access them since no services will be installed on the host.
      if (StageUtils.getHostName().equals(resolvedPrincipal.getHostName())) {

        // If the principal name exists in one of the shared data maps, it has been processed by the
        // current "Enable Kerberos" or "Add component" workflow and therefore should already have
        // the correct permissions assigned. The relevant keytab files can be skipped.
        Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);
        if ((principalPasswordMap == null) || !principalPasswordMap.containsKey(resolvedPrincipal.getPrincipal())) {

          String keytabFilePath = resolvedPrincipal.getKeytabPath();

          if (StringUtils.isEmpty(keytabFilePath)) {
            return null;
          }

          Lock lock = m_locksByKeytab.get(keytabFilePath);
          lock.lock();
          try {
            Set<String> visited = (Set<String>) requestSharedDataContext.get(this.getClass().getName() + "_visited");

            if (!visited.contains(keytabFilePath)) {
              String ownerName = resolvedPrincipal.getResolvedKerberosKeytab().getOwnerName();
              String ownerAccess = resolvedPrincipal.getResolvedKerberosKeytab().getOwnerAccess();
              boolean ownerWritable = "w".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
              boolean ownerReadable = "r".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
              String groupName = resolvedPrincipal.getResolvedKerberosKeytab().getGroupName();
              String groupAccess = resolvedPrincipal.getResolvedKerberosKeytab().getGroupAccess();
              boolean groupWritable = "w".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);
              boolean groupReadable = "r".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);

              ShellCommandUtil.Result result;
              String message;

              result = ShellCommandUtil.setFileOwner(keytabFilePath, ownerName);
              if (result.isSuccessful()) {
                message = String.format("Updated the owner of the keytab file at %s to %s",
                    keytabFilePath, ownerName);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the owner of the keytab file at %s to %s: %s",
                    keytabFilePath, ownerName, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              result = ShellCommandUtil.setFileGroup(keytabFilePath, groupName);
              if (result.isSuccessful()) {
                message = String.format("Updated the group of the keytab file at %s to %s",
                    keytabFilePath, groupName);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the group of the keytab file at %s to %s: %s",
                    keytabFilePath, groupName, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              result = ShellCommandUtil.setFileMode(keytabFilePath,
                  ownerReadable, ownerWritable, false,
                  groupReadable, groupWritable, false,
                  false, false, false);
              if (result.isSuccessful()) {
                message = String.format("Updated the access mode of the keytab file at %s to owner:'%s' and group:'%s'",
                    keytabFilePath, ownerAccess, groupAccess);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the access mode of the keytab file at %s to owner:'%s' and group:'%s': %s",
                    keytabFilePath, ownerAccess, groupAccess, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              visited.add(keytabFilePath);
            }
          } finally {
            lock.unlock();
          }
        }
      }
    }

    return null;
  }

  /**
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return
   * @throws ShpurdpException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws ShpurdpException, InterruptedException {
    String dataDirectoryPath = getCommandParameterValue(DATA_DIRECTORY);
    if(getKDCType(getCommandParameters()) != KDCType.NONE) {
      // Ensure the keytab files for the Shpurdp identities have the correct permissions
      // This is important in the event a secure cluster was created via Blueprints since some
      // user accounts and group may not have been created when the keytab files were created.
      requestSharedDataContext.put(this.getClass().getName() + "_visited", new HashSet<String>());
      processIdentities(requestSharedDataContext);
      requestSharedDataContext.remove(this.getClass().getName() + "_visited");
    }
    deleteDataDirectory(dataDirectoryPath);

    return sendTopologyUpdateEvent();
  }

  private CommandReport sendTopologyUpdateEvent() throws ShpurdpException, InterruptedException {
    CommandReport commandReport = null;
    try {
      final TopologyUpdateEvent updateEvent = topologyHolder.getCurrentData();
      topologyHolder.updateData(updateEvent);
    } catch (Exception e) {
      String message = "Could not send topology update event when enabling kerberos";
      actionLog.writeStdErr(message);
      LOG.error(message, e);
      commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }
    return commandReport == null ? createCompletedCommandReport() : commandReport;
  }

}
