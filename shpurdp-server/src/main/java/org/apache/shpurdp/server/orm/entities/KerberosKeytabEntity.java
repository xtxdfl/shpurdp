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

package org.apache.shpurdp.server.orm.entities;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;

import org.apache.shpurdp.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;

@Entity
@Table(name = "kerberos_keytab")
@NamedQueries({
  @NamedQuery(name = "KerberosKeytabEntity.findAll", query = "SELECT kk FROM KerberosKeytabEntity kk"),
  @NamedQuery(
    name = "KerberosKeytabEntity.findByPrincipalAndHost",
    query = "SELECT kk FROM KerberosKeytabEntity kk, KerberosKeytabPrincipalEntity kkp WHERE kkp.hostId=:hostId AND kkp.principalName=:principalName AND kkp.keytabPath = kk.keytabPath",
    hints = {
      @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
      @QueryHint(name = "eclipselink.query-results-cache.size", value = "500") }
  ),
  @NamedQuery(
    name = "KerberosKeytabEntity.findByPrincipalAndNullHost",
    query = "SELECT kk FROM KerberosKeytabEntity kk JOIN kk.kerberosKeytabPrincipalEntities kkp WHERE kkp.hostId IS NULL AND kkp.principalName=:principalName"
  )
})
public class KerberosKeytabEntity {
  @Id
  @Column(name = "keytab_path", updatable = false, nullable = false)
  private String keytabPath = null;

  @Column(name = "owner_name")
  private String ownerName;
  @Column(name = "owner_access")
  private String ownerAccess;
  @Column(name = "group_name")
  private String groupName;
  @Column(name = "group_access")
  private String groupAccess;
  @Column(name = "is_shpurdp_keytab")
  private Integer isShpurdpServerKeytab = 0;
  @Column(name = "write_shpurdp_jaas")
  private Integer writeShpurdpJaasFile = 0;

  @OneToMany(mappedBy = "kerberosKeytabEntity", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
  private Collection<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities = new ArrayList<>();

  public KerberosKeytabEntity() {

  }

  public KerberosKeytabEntity(String keytabPath) {
    setKeytabPath(keytabPath);
  }

  public KerberosKeytabEntity(ResolvedKerberosKeytab resolvedKerberosKeytab) {
    setKeytabPath(resolvedKerberosKeytab.getFile());
    setShpurdpServerKeytab(resolvedKerberosKeytab.isShpurdpServerKeytab());
    setWriteShpurdpJaasFile(resolvedKerberosKeytab.isMustWriteShpurdpJaasFile());
    setOwnerName(resolvedKerberosKeytab.getOwnerName());
    setOwnerAccess(resolvedKerberosKeytab.getOwnerAccess());
    setGroupName(resolvedKerberosKeytab.getGroupName());
    setGroupAccess(resolvedKerberosKeytab.getGroupAccess());
  }

  public String getKeytabPath() {
    return keytabPath;
  }

  public void setKeytabPath(String keytabPath) {
    this.keytabPath = keytabPath;
  }

  public Collection<KerberosKeytabPrincipalEntity> getKerberosKeytabPrincipalEntities() {
    return kerberosKeytabPrincipalEntities;
  }

  public void setKerberosKeytabPrincipalEntities(Collection<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities) {
    this.kerberosKeytabPrincipalEntities = kerberosKeytabPrincipalEntities;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getOwnerAccess() {
    return ownerAccess;
  }

  public void setOwnerAccess(String ownerAccess) {
    this.ownerAccess = ownerAccess;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupAccess() {
    return groupAccess;
  }

  public void setGroupAccess(String groupAccess) {
    this.groupAccess = groupAccess;
  }

  public boolean isShpurdpServerKeytab() {
    return isShpurdpServerKeytab == 1;
  }

  public void setShpurdpServerKeytab(boolean shpurdpServerKeytab) {
    this.isShpurdpServerKeytab = (shpurdpServerKeytab) ? 1 : 0;
  }

  public boolean isWriteShpurdpJaasFile() {
    return writeShpurdpJaasFile == 1;
  }

  public void setWriteShpurdpJaasFile(boolean writeShpurdpJaasFile) {
    this.writeShpurdpJaasFile = (writeShpurdpJaasFile) ? 1 : 0;
  }

  public void addKerberosKeytabPrincipal(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    if (!kerberosKeytabPrincipalEntities.contains(kerberosKeytabPrincipalEntity)) {
      kerberosKeytabPrincipalEntities.add(kerberosKeytabPrincipalEntity);
    }
  }

}
