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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

/**
 * The {@link StackEntity} class is used to model an alert that needs
 * to run in the system. Each received alert from an agent will essentially be
 * an instance of this template.
 */
@Entity
@Table(name = "stack", uniqueConstraints = @UniqueConstraint(columnNames = {
    "stack_name", "stack_version" }))
@TableGenerator(name = "stack_id_generator", table = "shpurdp_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "stack_id_seq", initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "StackEntity.findByMpack", query = "SELECT stack FROM StackEntity stack where stack.mpackId = :mpackId"),
    @NamedQuery(
      name = "StackEntity.findAll",
      query = "SELECT stack FROM StackEntity stack"
    ),
    @NamedQuery(
      name = "StackEntity.findByNameAndVersion",
      query = "SELECT stack FROM StackEntity stack WHERE stack.stackName = :stackName AND stack.stackVersion = :stackVersion",
      hints = {
        @QueryHint(name = QueryHints.QUERY_RESULTS_CACHE, value = HintValues.TRUE),
        @QueryHint(name = QueryHints.QUERY_RESULTS_CACHE_SIZE, value = "100")
      }
    )
})
public class StackEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "stack_id_generator")
  @Column(name = "stack_id", nullable = false, updatable = false)
  private Long stackId;

  @Column(name = "stack_name", length = 255, nullable = false)
  private String stackName;

  @Column(name = "stack_version", length = 255, nullable = false)
  private String stackVersion;

  @Column(name = "mpack_id")
  private Long mpackId;

  public Long getMpackId() {
    return mpackId;
  }

  public void setMpackId(Long mpackId) {
    this.mpackId = mpackId;
  }


  /**
   * Constructor.
   */
  public StackEntity() {
  }

  /**
   * Gets the unique identifier for this stack.
   *
   * @return the ID.
   */
  public Long getStackId() {
    return stackId;
  }

  /**
   * Gets the name of the stack, such as "HDP".
   *
   * @return the name of the stack (never {@code null}).
   */
  public String getStackName() {
    return stackName;
  }

  /**
   * Sets the name of the stack, such as "HDP".
   *
   * @param stackName
   *          the stack name (not {@code null}).
   */
  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  /**
   * Gets the version of the stack, such as "2.2".
   *
   * @return the stack version (never {@code null}).
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * Sets the version of the stack, such as "2.2".
   *
   * @param stackVersion
   *          the stack version (not {@code null}).
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  /**
   *
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    StackEntity that = (StackEntity) object;

    if (stackId != null ? !stackId.equals(that.stackId) : that.stackId != null) {
      return false;
    }

    return true;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = null != stackId ? stackId.hashCode() : 0;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(stackId);
    buffer.append(", name=").append(stackName);
    buffer.append(", version=").append(stackVersion);
    buffer.append(", mpack_id=").append(mpackId);
    buffer.append("}");
    return buffer.toString();
  }
}