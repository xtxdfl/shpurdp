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

package org.apache.shpurdp.funtest.server;

import com.google.inject.Inject;
import org.apache.shpurdp.server.controller.ShpurdpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.inject.Injector;

/**
* Wrap ShpurdpServer as a testable unit.
*/
public class LocalShpurdpServer implements Runnable {

  private static Log LOG = LogFactory.getLog(ShpurdpServer.class);

  /**
   * Actual shpurdp server instance.
   */
  private ShpurdpServer shpurdpServer = null;

  @Inject
  private Injector injector;

  public LocalShpurdpServer() {}

  /**
   * Thread entry point.
   */
  @Override
  public void run(){
    try {
      startServer();
    }
    catch (Exception ex) {
      LOG.info("Exception received ", ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Configures the Guice injector to use the in-memory test DB
   * and attempts to start an instance of ShpurdpServer.
   *
   * @throws Exception
   */
  private void startServer() throws Exception {
    try {
      LOG.info("Attempting to start shpurdp server...");

      ShpurdpServer.setupProxyAuth();
      shpurdpServer = injector.getInstance(ShpurdpServer.class);
      shpurdpServer.initViewRegistry();
      shpurdpServer.run();
    } catch (InterruptedException ex) {
      LOG.info(ex);
    } catch (Throwable t) {
      LOG.error("Failed to run the Shpurdp Server", t);
      stopServer();
      throw t;
    }
  }

  /**
   * Attempts to stop the test ShpurdpServer instance.
   * @throws Exception
   */
  public void stopServer() throws Exception {
    LOG.info("Stopping shpurdp server...");

    if (shpurdpServer != null) {
      shpurdpServer.stop();
    }
  }
}
