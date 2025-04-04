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

package org.apache.shpurdp.funtest.server.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Properties;

import com.google.common.base.Charsets;
import org.apache.shpurdp.funtest.server.LocalShpurdpServer;
import org.apache.shpurdp.server.audit.AuditLoggerModule;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.ControllerModule;
import org.apache.shpurdp.server.ldap.LdapModule;
import org.apache.shpurdp.server.orm.DBAccessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Base test infrastructure.
 */
public class ServerTestBase {
    private static Log LOG = LogFactory.getLog(ServerTestBase.class);

    /**
     * Run the shpurdp server on a thread.
     */
    protected static Thread serverThread = null;

    /**
     * Instance of the local shpurdp server, which wraps the actual
     * shpurdp server with test configuration.
     */
    protected static LocalShpurdpServer server = null;

    /**
     * Server port
     */
    protected static int serverPort = 9995;

    /**
     * Server agent port
     */
    protected static int serverAgentPort = 9000;

    /**
     * Guice injector using an in-memory DB.
     */
    protected static Injector injector = null;

    /**
     * Server URL
     */
    protected static String SERVER_URL_FORMAT = "http://localhost:%d";

    /**
     * Initialize the ShpurdpServer and database once for the entire
     * duration of the tests since ShpurdpServer is a singleton.
     */
    private static boolean isInitialized;

    /**
     * Create and populate the DB. Start the ShpurdpServer.
     * @throws Exception
     */
    @BeforeClass
    public static void setupTest() throws Exception {
        if (!isInitialized) {
            Properties properties = readConfigFile();
            properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "remote");
            properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), Configuration.JDBC_IN_MEMORY_URL);
            properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), Configuration.JDBC_IN_MEMORY_DRIVER);
            properties.setProperty(Configuration.OS_VERSION.getKey(), "centos7");

            properties.setProperty(Configuration.AGENT_USE_SSL.getKey(), "false");
            properties.setProperty(Configuration.CLIENT_API_PORT.getKey(), Integer.toString(serverPort));
            properties.setProperty(Configuration.SRVR_ONE_WAY_SSL_PORT.getKey(), Integer.toString(serverAgentPort));
            String tmpDir = System.getProperty("java.io.tmpdir");
            properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(), tmpDir);

            ControllerModule testModule = new ControllerModule(properties);

            injector = Guice.createInjector(testModule, new AuditLoggerModule(), new LdapModule());
            injector.getInstance(PersistService.class).start();
            initDB();

            server = injector.getInstance(LocalShpurdpServer.class);
            serverThread = new Thread(server);
            serverThread.start();
            waitForServer();

            isInitialized = true;
        }
    }

    /**
     * Creates the basic authentication string for admin:admin
     *
     * @return
     */
    protected static String getBasicAdminAuthentication() {
        String authString = getAdminUserName() + ":" + getAdminPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);

        return "Basic " + authStringEnc;
    }

    /**
     * Creates the DB and populates it.
     *
     * @throws IOException
     * @throws SQLException
     */
    protected static void initDB() throws IOException, SQLException {
        createSourceDatabase();
    }

    /**
     * Drops the Derby DB.
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    protected static void dropDatabase() throws ClassNotFoundException, SQLException {
        String DROP_DERBY_URL = "jdbc:derby:memory:myDB/shpurdp;drop=true";
        Class.forName(Configuration.JDBC_IN_MEMORY_DRIVER);
        try {
            DriverManager.getConnection(DROP_DERBY_URL);
        } catch (SQLNonTransientConnectionException ignored) {
            LOG.info("Database dropped ", ignored); //error 08006 expected
        }
    }

    /**
     * Executes Shpurdp-DDL-Derby-CREATE.sql
     *
     * @throws IOException
     * @throws SQLException
     */
    private static void createSourceDatabase() throws IOException, SQLException {
        //create database
        File projectDir = new File(System.getProperty("user.dir"));
        File ddlFile = new File(projectDir.getParentFile(), "shpurdp-server/src/main/resources/Shpurdp-DDL-H2-CREATE.sql");
        String ddlFilename = ddlFile.getPath();
        DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);
        dbAccessor.executeScript(ddlFilename);
    }

    /**
     * Gets the default administration user name
     *
     * @return
     */
    protected static String getAdminUserName() {
        return "admin";
    }

    /**
     * Gets the default administrator password
     *
     * @return
     */
    protected static String getAdminPassword() {
        return "admin";
    }

    /**
     * Waits for the local server until it is ready to accept requests.
     *
     * @throws Exception
     */
    private static void waitForServer() throws Exception {
        int count = 1;

        while (!isServerUp()) {
            serverThread.join(count * 10000);     // Give a few seconds for the shpurdp server to start up
            //count += 1; // progressive back off
            //count *= 2; // exponential back off
        }
    }

    /**
     * Attempt to query the server for the stack. If the server is up,
     * we will get a response. If not, an exception will be thrown.
     *
     * @return - True if the local server is responsive to queries.
     *           False, otherwise.
     */
    private static boolean isServerUp() throws IOException {
        String apiPath = "/api/v1/stacks";

        String apiUrl = String.format(SERVER_URL_FORMAT, serverPort) + apiPath;
        CloseableHttpClient httpClient = HttpClients.createDefault();;

        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.addHeader("Authorization", getBasicAdminAuthentication());
            httpGet.addHeader("X-Requested-By", "shpurdp");
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;

            return true;
        } catch (IOException ex) {

        } finally {
            httpClient.close();
        }

        return false;
    }

    /**
     * Perform common initialization for each test case.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {

    }

    /**
     * Perform common clean up for each test case.
     *
     * @throws Exception
     */
    @After
    public void teardown() throws Exception {
    }

    private static Properties readConfigFile() {
        Properties properties = new Properties();
        String configFileName = "shpurdp.properties";
        //Get property file stream from classpath
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(configFileName);
        if (inputStream == null) {
            throw new RuntimeException(configFileName + " not found in classpath");
        }
        // load the properties
        try {
            properties.load(new InputStreamReader(inputStream, Charsets.UTF_8));
            inputStream.close();
        } catch (FileNotFoundException fnf) {
            LOG.info("No configuration file " + configFileName + " found in classpath.", fnf);
        } catch (IOException ie) {
            throw new IllegalArgumentException("Can't read configuration file " +
                    configFileName, ie);
        }
        return properties;
    }
}
