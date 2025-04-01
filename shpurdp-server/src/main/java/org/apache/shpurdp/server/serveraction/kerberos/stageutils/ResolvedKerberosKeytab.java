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

package org.apache.shpurdp.server.serveraction.kerberos.stageutils;

import java.util.HashSet;
import java.util.Set;

import org.apache.shpurdp.server.state.kerberos.VariableReplacementHelper;

/**
 * Class that represents keytab. Contains principals that mapped to host.
 * Same keytab can have different set of principals on different hosts for different services.
 * Each principal identified by host and keytab it belongs to and contain mapping that shows in which services and
 * components given principal used.
 */
public class ResolvedKerberosKeytab {

  private String ownerName = null;
  private String ownerAccess = null;
  private String groupName = null;
  private String groupAccess = null;
  private String file = null;
  private Set<ResolvedKerberosPrincipal> principals = new HashSet<>();
  private boolean isShpurdpServerKeytab = false;
  private boolean mustWriteShpurdpJaasFile = false;

  public ResolvedKerberosKeytab(
    String file,
    String ownerName,
    String ownerAccess,
    String groupName,
    String groupAccess,
    Set<ResolvedKerberosPrincipal> principals,
    boolean isShpurdpServerKeytab,
    boolean writeShpurdpJaasFile
  ) {
    this.ownerName = ownerName;
    this.ownerAccess = ownerAccess;
    this.groupName = groupName;
    this.groupAccess = groupAccess;
    this.file = file;
    setPrincipals(principals);
    this.isShpurdpServerKeytab = isShpurdpServerKeytab;
    this.mustWriteShpurdpJaasFile = writeShpurdpJaasFile;

  }

  /**
   * Gets the path to the keytab file
   *
   * @return a String declaring the keytab file's absolute path
   * @see VariableReplacementHelper#replaceVariables(String, java.util.Map)
   */
  public String getFile() {
    return file;
  }

  /**
   * Sets the path to the keytab file
   *
   * @param file a String declaring this keytab's file path
   * @see #getFile()
   */
  public void setFile(String file) {
    this.file = file;
  }

  /**
   * Gets the local username to set as the owner of the keytab file
   *
   * @return a String declaring the name of the user to own the keytab file
   */
  public String getOwnerName() {
    return ownerName;
  }

  /**
   * Sets the local username to set as the owner of the keytab file
   *
   * @param name a String declaring the name of the user to own the keytab file
   */
  public void setOwnerName(String name) {
    this.ownerName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's owner
   * @see #ownerAccess
   */
  public String getOwnerAccess() {
    return ownerAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's owner
   * @see #ownerAccess
   */
  public void setOwnerAccess(String access) {
    this.ownerAccess = access;
  }

  /**
   * Gets the local group name to set as the group owner of the keytab file
   *
   * @return a String declaring the name of the group to own the keytab file
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the local group name to set as the group owner of the keytab file
   *
   * @param name a String declaring the name of the group to own the keytab file
   */
  public void setGroupName(String name) {
    this.groupName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's group
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's group
   * @see #groupAccess
   */
  public String getGroupAccess() {
    return groupAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's group
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's group
   * @see #groupAccess
   */
  public void setGroupAccess(String access) {
    this.groupAccess = access;
  }

  /**
   * Gets evaluated host-to-principal set associated with given keytab.
   *
   * @return a Set with principals associated with given keytab
   */
  public Set<ResolvedKerberosPrincipal> getPrincipals() {
    return principals;
  }

  /**
   * Sets evaluated host-to-principal set associated with given keytab.
   *
   * @param principals set of principals to add
   */
  public void setPrincipals(Set<ResolvedKerberosPrincipal> principals) {
    this.principals = principals;
    if (principals != null) {
      for (ResolvedKerberosPrincipal principal : this.principals) {
        principal.setResolvedKerberosKeytab(this);
      }
    }
  }

  /**
   * Add principal to keytab.
   *
   * @param principal resolved principal to add
   */
  public void addPrincipal(ResolvedKerberosPrincipal principal) {
    if (!principals.contains(principal)) {
      principal.setResolvedKerberosKeytab(this);
      principals.add(principal);
    }
  }

  /**
   * Indicates if given keytab is Shpurdp Server keytab and can be distributed to host with Shpurdp Server side action.
   *
   * @return true, if given keytab is Shpurdp Server keytab.
   */
  public boolean isShpurdpServerKeytab() {
    return isShpurdpServerKeytab;
  }

  /**
   * Sets flag to indicate if given keytab is Shpurdp Server keytab and can be distributed to host with Shpurdp Server
   * side action.
   *
   * @param isShpurdpServerKeytab flag value
   */
  public void setShpurdpServerKeytab(boolean isShpurdpServerKeytab) {
    this.isShpurdpServerKeytab = isShpurdpServerKeytab;
  }

  /**
   * Indicates if this keytab must be written to Shpurdp Server jaas file.
   *
   * @return true, if this keytab must be written to Shpurdp Server jaas file.
   */
  public boolean isMustWriteShpurdpJaasFile() {
    return mustWriteShpurdpJaasFile;
  }

  /**
   * Sets flag to indicate if this keytab must be written to Shpurdp Server jaas file.
   *
   * @param mustWriteShpurdpJaasFile flag value
   */
  public void setMustWriteShpurdpJaasFile(boolean mustWriteShpurdpJaasFile) {
    this.mustWriteShpurdpJaasFile = mustWriteShpurdpJaasFile;
  }

  /**
   * Merge principals from one keytab to given.
   *
   * @param otherKeytab keytab to merge principals from
   */
  public void mergePrincipals(ResolvedKerberosKeytab otherKeytab) {
    for (ResolvedKerberosPrincipal rkp : otherKeytab.getPrincipals()) {
      ResolvedKerberosPrincipal existent = findPrincipal(rkp.getHostId(), rkp.getPrincipal(), rkp.getKeytabPath());
      if (existent != null) {
        existent.mergeComponentMapping(rkp);
      } else {
        principals.add(rkp);
      }
    }
  }

  private ResolvedKerberosPrincipal findPrincipal(Long hostId, String principal, String keytabPath) {
    for (ResolvedKerberosPrincipal rkp : principals) {
      boolean hostIdIsSame;
      if(hostId != null && rkp.getHostId() != null){
        hostIdIsSame = hostId.equals(rkp.getHostId());
      } else if(hostId == null && rkp.getHostId() == null) {
        hostIdIsSame = true;
      } else {
        hostIdIsSame = false;
      }
      if (hostIdIsSame && principal.equals(rkp.getPrincipal())&& keytabPath.equals(rkp.getKeytabPath())) {
        return rkp;
      }
    }
    return null;
  }
}
