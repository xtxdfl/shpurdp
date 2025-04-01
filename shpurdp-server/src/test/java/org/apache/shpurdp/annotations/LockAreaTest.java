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
package org.apache.shpurdp.annotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.shpurdp.annotations.TransactionalLock.LockArea;
import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.H2DatabaseCleaner;
import org.apache.shpurdp.server.orm.GuiceJpaInitializer;
import org.apache.shpurdp.server.orm.InMemoryDefaultTestModule;
import org.apache.shpurdp.server.orm.TransactionalLocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests {@link TransactionalLocks} and {@link LockArea} and associated classes.
 */
public class LockAreaTest {

  private Injector m_injector;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws ShpurdpException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(m_injector);
  }

  /**
   * Tests that for each {@link LockArea}, there's a single {@link Lock}.
   */
  @Test
  public void testTransactionalLockInstantiation() {
    TransactionalLocks locks = m_injector.getInstance(TransactionalLocks.class);
    List<ReadWriteLock> lockList = new ArrayList<>();
    Set<LockArea> lockAreas = EnumSet.allOf(LockArea.class);
    for (LockArea lockArea : lockAreas) {
      ReadWriteLock lock = locks.getLock(lockArea);
      Assert.assertNotNull(lock);
      lockList.add(lock);
    }

    for (LockArea lockArea : lockAreas) {
      Assert.assertTrue(lockList.contains(locks.getLock(lockArea)));
    }
  }
}
