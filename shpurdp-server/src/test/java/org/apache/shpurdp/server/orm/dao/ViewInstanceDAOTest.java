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

package org.apache.shpurdp.server.orm.dao;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.orm.entities.ViewInstanceDataEntity;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * ViewInstanceDAO tests.
 */
public class ViewInstanceDAOTest {
  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);
  }

  @Test
  public void testMergeData() throws Exception {
    ViewInstanceDataEntity entity = new ViewInstanceDataEntity();
    ViewInstanceDataEntity entity2 = new ViewInstanceDataEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    replay(entityManager);

    ViewInstanceDAO dao = new ViewInstanceDAO();
    dao.entityManagerProvider = entityManagerProvider;
    assertSame(entity2, dao.mergeData(entity));

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testRemoveData() throws Exception {
    ViewInstanceDataEntity entity = new ViewInstanceDataEntity();
    ViewInstanceDataEntity entity2 = new ViewInstanceDataEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    entityManager.remove(eq(entity2));
    replay(entityManager);

    ViewInstanceDAO dao = new ViewInstanceDAO();
    dao.entityManagerProvider = entityManagerProvider;
    dao.removeData(entity);

    verify(entityManagerProvider, entityManager);
  }
}
