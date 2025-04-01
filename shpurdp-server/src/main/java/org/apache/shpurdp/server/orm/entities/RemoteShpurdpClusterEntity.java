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

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.shpurdp.server.view.DefaultMasker;
import org.apache.shpurdp.view.MaskException;
import org.apache.shpurdp.view.Masker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote Shpurdp Managed Cluster
 */
@Table(name = "remoteshpurdpcluster")
@TableGenerator(name = "remote_cluster_id_generator",
  table = "shpurdp_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "remote_cluster_id_seq"
  , initialValue = 1
)

@NamedQueries({
  @NamedQuery(name = "allRemoteShpurdpClusters",
    query = "SELECT remoteShpurdpCluster FROM RemoteShpurdpClusterEntity remoteshpurdpcluster"),
  @NamedQuery(name = "remoteShpurdpClusterByName", query =
    "SELECT remoteShpurdpCluster " +
      "FROM RemoteShpurdpClusterEntity remoteShpurdpCluster " +
      "WHERE remoteShpurdpCluster.name=:clusterName"),
  @NamedQuery(name = "remoteShpurdpClusterById", query =
    "SELECT remoteShpurdpCluster " +
      "FROM RemoteShpurdpClusterEntity remoteShpurdpCluster " +
      "WHERE remoteShpurdpCluster.id=:clusterId")})
@Entity
public class RemoteShpurdpClusterEntity {

  /**
   * The logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(RemoteShpurdpClusterEntity.class);

  @Id
  @Column(name = "cluster_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "remote_cluster_id_generator")
  private Long id;

  @Column(name = "name", nullable = false, insertable = true, updatable = true)
  private String name;

  @Column(name = "url", nullable = false, insertable = true, updatable = true)
  private String url;

  @Column(name = "username", nullable = false, insertable = true, updatable = true)
  private String username;

  @Column(name = "password", nullable = false, insertable = true, updatable = true)
  private String password;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "cluster")
  private Collection<RemoteShpurdpClusterServiceEntity> services;

  private static Masker masker = new DefaultMasker();

  /**
   * Get the id
   *
   * @return id
   */
  public Long getId() {
    return id;
  }

  /**
   * Set the id of cluster
   *
   * @param id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the name of cluster
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the cluster name
   *
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the cluster url
   *
   * @return url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Set the url
   *
   * @param url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Get username
   *
   * @return username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the username
   *
   * @param username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   *  Get the password
   *
   * @return password
   */
  public String getPassword() {
    try {
      return masker.unmask(password);
    } catch (MaskException e) {
      // Log exception
      LOG.error("Unable to unmask password for Remote Cluster : "+name , e);
    }
    return null;
  }

  /**
   *  Set the password
   *
   * @param password
   * @throws MaskException
   */
  public void setPassword(String password) throws MaskException {
    this.password = masker.mask(password);
  }

  /**
   * Get the services installed on the cluster
   *
   * @return services
   */
  public Collection<RemoteShpurdpClusterServiceEntity> getServices() {
    return services;
  }

  /**
   * Set the services installed on the cluster
   *
   * @param services
   */
  public void setServices(Collection<RemoteShpurdpClusterServiceEntity> services) {
    this.services = services;
  }
}
