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

package org.apache.shpurdp.server.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.shpurdp.annotations.ConfigurationMarkdown;
import org.apache.shpurdp.annotations.Markdown;
import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.configuration.Configuration.ConfigurationProperty;
import org.apache.shpurdp.server.configuration.Configuration.ConnectionPoolType;
import org.apache.shpurdp.server.configuration.Configuration.DatabaseType;
import org.apache.shpurdp.server.controller.metrics.ThreadPoolEnabledPropertyProvider;
import org.apache.shpurdp.server.security.authentication.kerberos.ShpurdpKerberosAuthenticationProperties;
import org.apache.shpurdp.server.state.services.MetricsRetrievalService;
import org.apache.shpurdp.server.utils.PasswordUtils;
import org.apache.shpurdp.server.utils.StageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Charsets;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Configuration.class, PasswordUtils.class })
@PowerMockIgnore( {"javax.management.*", "javax.crypto.*"})
public class ConfigurationTest {
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setup() throws Exception {
    temp.create();
  }

  @After
  public void teardown() throws ShpurdpException {
    temp.delete();
  }

  /**
   * shpurdp.properties doesn't contain "security.agent.hostname.validate" option
   */
  @Test
  public void testValidateAgentHostnames() {
    Assert.assertTrue(new Configuration().validateAgentHostnames());
  }

  /**
   * shpurdp.properties contains "security.agent.hostname.validate=true" option
   */
  @Test
  public void testValidateAgentHostnamesOn() {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.SRVR_AGENT_HOSTNAME_VALIDATE.getKey(), "true");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertTrue(conf.validateAgentHostnames());
    Assert.assertEquals("true", conf.getConfigsMap().get(Configuration.SRVR_AGENT_HOSTNAME_VALIDATE.getKey()));
  }

  /**
   * shpurdp.properties contains "security.agent.hostname.validate=false" option
   */
  @Test
  public void testValidateAgentHostnamesOff() {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.SRVR_AGENT_HOSTNAME_VALIDATE.getKey(), "false");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertFalse(conf.validateAgentHostnames());
    Assert.assertEquals("false", conf.getConfigsMap().get(Configuration.SRVR_AGENT_HOSTNAME_VALIDATE.getKey()));
  }

  /**
   * shpurdp.properties doesn't contain "security.server.two_way_ssl" option
   * @throws Exception
   */
  @Test
  public void testDefaultTwoWayAuthNotSet() throws Exception {
    Assert.assertFalse(new Configuration().isTwoWaySsl());
  }

  /**
   * shpurdp.properties contains "security.server.two_way_ssl=true" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOn() throws Exception {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("security.server.two_way_ssl", "true");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertTrue(conf.isTwoWaySsl());
  }

  /**
   * shpurdp.properties contains "security.server.two_way_ssl=false" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOff() throws Exception {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("security.server.two_way_ssl", "false");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertFalse(conf.isTwoWaySsl());
  }

  @Test
  public void testGetClientSSLApiPort() throws Exception {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.CLIENT_API_SSL_PORT.getKey(), "6666");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertEquals(6666, conf.getClientSSLApiPort());
    conf = new Configuration();
    Assert.assertEquals(8443, conf.getClientSSLApiPort());
  }

  @Test
  public void testGetMpacksV2StagingPath() {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.MPACKS_V2_STAGING_DIR_PATH.getKey(), "/var/lib/shpurdp-server/resources/mpacks-v2/");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertEquals("/var/lib/shpurdp-server/resources/mpacks-v2/", conf.getMpacksV2StagingPath());
    conf = new Configuration();
    Assert.assertEquals(null, conf.getMpacksV2StagingPath());
  }

  @Test
  public void testGetClientHTTPSSettings() throws IOException {

    File passFile = File.createTempFile("https.pass.", "txt");
    passFile.deleteOnExit();

    String password = "pass12345";

    FileUtils.writeStringToFile(passFile, password, Charset.defaultCharset());

    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.API_USE_SSL.getKey(), "true");
    shpurdpProperties.setProperty(
        Configuration.CLIENT_API_SSL_KSTR_DIR_NAME.getKey(),
        passFile.getParent());
    shpurdpProperties.setProperty(
        Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey(),
        passFile.getName());


    String oneWayPort = RandomStringUtils.randomNumeric(4);
    String twoWayPort = RandomStringUtils.randomNumeric(4);

    shpurdpProperties.setProperty(Configuration.SRVR_TWO_WAY_SSL_PORT.getKey(), twoWayPort.toString());
    shpurdpProperties.setProperty(Configuration.SRVR_ONE_WAY_SSL_PORT.getKey(), oneWayPort.toString());

    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertTrue(conf.getApiSSLAuthentication());

    //Different certificates for two-way SSL and HTTPS
    Assert.assertFalse(conf.getConfigsMap().get(Configuration.KSTR_NAME.getKey()).
      equals(conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_KSTR_NAME.getKey())));

    Assert.assertEquals("keystore.p12", conf.getConfigsMap().get(
        Configuration.KSTR_NAME.getKey()));
    Assert.assertEquals("PKCS12", conf.getConfigsMap().get(
        Configuration.KSTR_TYPE.getKey()));
    Assert.assertEquals("keystore.p12", conf.getConfigsMap().get(
        Configuration.TSTR_NAME.getKey()));
    Assert.assertEquals("PKCS12", conf.getConfigsMap().get(
        Configuration.TSTR_TYPE.getKey()));

    Assert.assertEquals("https.keystore.p12", conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_KSTR_NAME.getKey()));
    Assert.assertEquals("PKCS12", conf.getConfigsMap().get(
        Configuration.CLIENT_API_SSL_KSTR_TYPE.getKey()));
    Assert.assertEquals("https.keystore.p12", conf.getConfigsMap().get(
        Configuration.CLIENT_API_SSL_TSTR_NAME.getKey()));
    Assert.assertEquals("PKCS12", conf.getConfigsMap().get(
        Configuration.CLIENT_API_SSL_TSTR_TYPE.getKey()));
    Assert.assertEquals(passFile.getName(), conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME.getKey()));
    Assert.assertEquals(password, conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_CRT_PASS.getKey()));
    Assert.assertEquals(Integer.parseInt(twoWayPort), conf.getTwoWayAuthPort());
    Assert.assertEquals(Integer.parseInt(oneWayPort), conf.getOneWayAuthPort());

  }

  @Test
  public void testLoadSSLParams_unencrypted() throws IOException {
    Properties shpurdpProperties = new Properties();
    String unencrypted = "fake-unencrypted-password";
    String encrypted = "fake-encrypted-password";
    shpurdpProperties.setProperty(Configuration.SSL_TRUSTSTORE_PASSWORD.getKey(), unencrypted);
    Configuration conf = spy(new Configuration(shpurdpProperties));
    PowerMock.stub(PowerMock.method(PasswordUtils.class, "readPasswordFromStore", String.class, Configuration.class)).toReturn(null);
    conf.loadSSLParams();
    Assert.assertEquals(System.getProperty(Configuration.JAVAX_SSL_TRUSTSTORE_PASSWORD, "unknown"), unencrypted);
  }

  @Test
  public void testLoadSSLParams_encrypted() throws IOException {
    Properties shpurdpProperties = new Properties();
    String unencrypted = "fake-unencrypted-password";
    String encrypted = "fake-encrypted-password";
    shpurdpProperties.setProperty(Configuration.SSL_TRUSTSTORE_PASSWORD.getKey(), unencrypted);
    Configuration conf = spy(new Configuration(shpurdpProperties));
    PowerMock.stub(PowerMock.method(PasswordUtils.class, "readPasswordFromStore", String.class, Configuration.class)).toReturn(encrypted);
    conf.loadSSLParams();
    Assert.assertEquals(System.getProperty(Configuration.JAVAX_SSL_TRUSTSTORE_PASSWORD, "unknown"), encrypted);
  }

  @Test
  public void testGetRcaDatabasePassword_fromStore() {
    String serverJdbcRcaUserPasswdKey = "key";
    String encrypted = "password";

    Properties properties = new Properties();
    properties.setProperty(Configuration.SERVER_JDBC_RCA_USER_PASSWD.getKey(), serverJdbcRcaUserPasswdKey);
    Configuration conf = spy(new Configuration(properties));
    PowerMock.stub(PowerMock.method(PasswordUtils.class, "readPassword")).toReturn(encrypted);

    Assert.assertEquals(encrypted, conf.getRcaDatabasePassword());
  }

  @Test
  public void testGetRcaDatabasePassword_fromFile() {
    Configuration conf = spy(new Configuration(new Properties()));
    Assert.assertEquals("mapred", conf.getRcaDatabasePassword());
  }

  @Test
  public void testGetLocalDatabaseUrl() {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("server.jdbc.database_name", "shpurdptestdatabase");
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertEquals(conf.getLocalDatabaseUrl(), Configuration.JDBC_LOCAL_URL.concat("shpurdptestdatabase"));
  }

  @Test
  public void testNoNewlineInPassword() throws Exception {
    Properties shpurdpProperties = new Properties();
    File f = temp.newFile("password.dat");
    FileOutputStream fos = new FileOutputStream(f);
    fos.write("shpurdptest\r\n".getBytes());
    fos.close();
    String passwordFile = temp.getRoot().getAbsolutePath()
      + System.getProperty("file.separator") + "password.dat";

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_USER_PASSWD.getKey(),
      passwordFile);

    Configuration conf = new Configuration(shpurdpProperties);
    PowerMock.stub(PowerMock.method(PasswordUtils.class,"readPasswordFromStore", String.class, Configuration.class)).toReturn(null);

    Assert.assertEquals("shpurdptest", conf.getDatabasePassword());
  }

  @Test
  public void testGetShpurdpProperties() throws Exception {
    Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("name", "value");
    Configuration conf = new Configuration(shpurdpProperties);
    mockStatic(Configuration.class);
    Method[] methods = MemberMatcher.methods(Configuration.class, "readConfigFile");
    PowerMock.expectPrivate(Configuration.class, methods[0]).andReturn(shpurdpProperties);
    replayAll();
    Map<String, String> props = conf.getShpurdpProperties();
    verifyAll();
    Assert.assertEquals("value", props.get("name"));
  }

  @Test
  public void testGetShpurdpBlacklistFile() {
    Properties shpurdpProperties = new Properties();
    Configuration conf = new Configuration(shpurdpProperties);
    Assert.assertEquals(null, conf.getShpurdpBlacklistFile());
    shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.PROPERTY_MASK_FILE.getKey(), "shpurdp-blacklist.properties");
    conf = new Configuration(shpurdpProperties);
    Assert.assertEquals("shpurdp-blacklist.properties", conf.getShpurdpBlacklistFile());
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test()
  public void testGetLocalDatabaseUrlThrowException() {
    Properties shpurdpProperties = new Properties();
    Configuration conf = new Configuration(shpurdpProperties);
    exception.expect(RuntimeException.class);
    exception.expectMessage("Server DB Name is not configured!");
    conf.getLocalDatabaseUrl();
  }

  @Test()
  public void testServerPoolSizes() {
    Properties shpurdpProperties = new Properties();
    Configuration conf = new Configuration(shpurdpProperties);

    Assert.assertEquals(25, conf.getClientThreadPoolSize());
    Assert.assertEquals(25, conf.getAgentThreadPoolSize());

    Assert.assertEquals(10, conf.getViewExtractionThreadPoolCoreSize());
    Assert.assertEquals(20, conf.getViewExtractionThreadPoolMaxSize());
    Assert.assertEquals(100000L, conf.getViewExtractionThreadPoolTimeout());

    shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("client.threadpool.size.max", "4");
    shpurdpProperties.setProperty("agent.threadpool.size.max", "82");

    shpurdpProperties.setProperty("view.extraction.threadpool.size.core", "83");
    shpurdpProperties.setProperty("view.extraction.threadpool.size.max", "56");
    shpurdpProperties.setProperty("view.extraction.threadpool.timeout", "6000");

    conf = new Configuration(shpurdpProperties);

    Assert.assertEquals(4, conf.getClientThreadPoolSize());
    Assert.assertEquals(82, conf.getAgentThreadPoolSize());

    Assert.assertEquals(83, conf.getViewExtractionThreadPoolCoreSize());
    Assert.assertEquals(56, conf.getViewExtractionThreadPoolMaxSize());
    Assert.assertEquals(6000L, conf.getViewExtractionThreadPoolTimeout());
  }

  @Test()
  public void testGetDefaultAgentTaskTimeout() {
    Properties shpurdpProperties = new Properties();
    Configuration conf = new Configuration(shpurdpProperties);

    Assert.assertEquals("900", conf.getDefaultAgentTaskTimeout(false));
    Assert.assertEquals("1800", conf.getDefaultAgentTaskTimeout(true));

    shpurdpProperties = new Properties();
    shpurdpProperties.setProperty("agent.task.timeout", "4");
    shpurdpProperties.setProperty("agent.package.install.task.timeout", "82");

    conf = new Configuration(shpurdpProperties);

    Assert.assertEquals("4", conf.getDefaultAgentTaskTimeout(false));
    Assert.assertEquals("82", conf.getDefaultAgentTaskTimeout(true));
  }


  @Test
  public void testGetDefaultServerTaskTimeout() {
    Properties shpurdpProperties = new Properties();
    Configuration conf = new Configuration(shpurdpProperties);

    Assert.assertEquals(Integer.valueOf(1200), conf.getDefaultServerTaskTimeout());

    shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.SERVER_TASK_TIMEOUT.getKey(), "3600");

    conf = new Configuration(shpurdpProperties);

    Assert.assertEquals(Integer.valueOf(3600), conf.getDefaultServerTaskTimeout());
  }

  @Test
  public void testIsViewValidationEnabled() throws Exception {
    final Properties shpurdpProperties = new Properties();
    Configuration configuration = new Configuration(shpurdpProperties);
    Assert.assertFalse(configuration.isViewValidationEnabled());

    shpurdpProperties.setProperty(Configuration.VIEWS_VALIDATE.getKey(), "false");
    configuration = new Configuration(shpurdpProperties);
    Assert.assertFalse(configuration.isViewValidationEnabled());

    shpurdpProperties.setProperty(Configuration.VIEWS_VALIDATE.getKey(), "true");
    configuration = new Configuration(shpurdpProperties);
    Assert.assertTrue(configuration.isViewValidationEnabled());
  }

  @Test
  public void testIsViewRemoveUndeployedEnabled() throws Exception {
    final Properties shpurdpProperties = new Properties();
    Configuration configuration = new Configuration(shpurdpProperties);
    Assert.assertFalse(configuration.isViewRemoveUndeployedEnabled());

    shpurdpProperties.setProperty(Configuration.VIEWS_REMOVE_UNDEPLOYED.getKey(), "false");
    configuration = new Configuration(shpurdpProperties);
    Assert.assertFalse(configuration.isViewRemoveUndeployedEnabled());

    shpurdpProperties.setProperty(Configuration.VIEWS_REMOVE_UNDEPLOYED.getKey(), "true");
    configuration = new Configuration(shpurdpProperties);
    Assert.assertTrue(configuration.isViewRemoveUndeployedEnabled());

    shpurdpProperties.setProperty(Configuration.VIEWS_REMOVE_UNDEPLOYED.getKey(),
        Configuration.VIEWS_REMOVE_UNDEPLOYED.getDefaultValue());

    configuration = new Configuration(shpurdpProperties);
    Assert.assertFalse(configuration.isViewRemoveUndeployedEnabled());
  }

  @Test
  public void testConnectionPoolingProperties() throws Exception {
    // test defaults
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    Assert.assertEquals(ConnectionPoolType.INTERNAL, configuration.getConnectionPoolType());
    Assert.assertEquals(5, configuration.getConnectionPoolAcquisitionSize());
    Assert.assertEquals(7200, configuration.getConnectionPoolIdleTestInternval());
    Assert.assertEquals(0, configuration.getConnectionPoolMaximumAge());
    Assert.assertEquals(0, configuration.getConnectionPoolMaximumExcessIdle());
    Assert.assertEquals(14400, configuration.getConnectionPoolMaximumIdle());
    Assert.assertEquals(32, configuration.getConnectionPoolMaximumSize());
    Assert.assertEquals(5, configuration.getConnectionPoolMinimumSize());
    Assert.assertEquals(30, configuration.getConnectionPoolAcquisitionRetryAttempts());
    Assert.assertEquals(1000, configuration.getConnectionPoolAcquisitionRetryDelay());

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL.getKey(), ConnectionPoolType.C3P0.getName());
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MIN_SIZE.getKey(), "1");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MAX_SIZE.getKey(), "2");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_AQUISITION_SIZE.getKey(), "3");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MAX_AGE.getKey(), "4");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME.getKey(), "5");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS.getKey(), "6");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL.getKey(), "7");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS.getKey(), "8");
    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY.getKey(), "9");


    Assert.assertEquals(ConnectionPoolType.C3P0, configuration.getConnectionPoolType());
    Assert.assertEquals(3, configuration.getConnectionPoolAcquisitionSize());
    Assert.assertEquals(7, configuration.getConnectionPoolIdleTestInternval());
    Assert.assertEquals(4, configuration.getConnectionPoolMaximumAge());
    Assert.assertEquals(6, configuration.getConnectionPoolMaximumExcessIdle());
    Assert.assertEquals(5, configuration.getConnectionPoolMaximumIdle());
    Assert.assertEquals(2, configuration.getConnectionPoolMaximumSize());
    Assert.assertEquals(1, configuration.getConnectionPoolMinimumSize());
    Assert.assertEquals(8, configuration.getConnectionPoolAcquisitionRetryAttempts());
    Assert.assertEquals(9, configuration.getConnectionPoolAcquisitionRetryDelay());
  }

  @Test
  public void testDatabaseType() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:oracle://server");
    Assert.assertEquals( DatabaseType.ORACLE, configuration.getDatabaseType() );

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:postgres://server");
    Assert.assertEquals( DatabaseType.POSTGRES, configuration.getDatabaseType() );

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:mysql://server");
    Assert.assertEquals( DatabaseType.MYSQL, configuration.getDatabaseType() );

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:derby://server");
    Assert.assertEquals( DatabaseType.DERBY, configuration.getDatabaseType() );

    shpurdpProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), "jdbc:sqlserver://server");
    Assert.assertEquals( DatabaseType.SQL_SERVER, configuration.getDatabaseType() );
  }

  @Test
  public void testGetAgentPackageParallelCommandsLimit() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    Assert.assertEquals(100, configuration.getAgentPackageParallelCommandsLimit());

    shpurdpProperties.setProperty(Configuration.AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(), "5");
    Assert.assertEquals(5, configuration.getAgentPackageParallelCommandsLimit());

    shpurdpProperties.setProperty(Configuration.AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(), "0");
    Assert.assertEquals(1, configuration.getAgentPackageParallelCommandsLimit());
  }

  @Test
  public void testGetExecutionSchedulerWait() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    //default
    Assert.assertEquals(new Long(1000L), configuration.getExecutionSchedulerWait());

    shpurdpProperties.setProperty(Configuration.EXECUTION_SCHEDULER_WAIT.getKey(), "5");
    Assert.assertEquals(new Long(5000L), configuration.getExecutionSchedulerWait());
    // > 60 secs
    shpurdpProperties.setProperty(Configuration.EXECUTION_SCHEDULER_WAIT.getKey(), "100");
    Assert.assertEquals(new Long(60000L), configuration.getExecutionSchedulerWait());
    //not a number
    shpurdpProperties.setProperty(Configuration.EXECUTION_SCHEDULER_WAIT.getKey(), "100m");
    Assert.assertEquals(new Long(1000L), configuration.getExecutionSchedulerWait());
  }

  @Test
  public void testServerLocksProfilingEnabled() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    Assert.assertFalse(configuration.isServerLocksProfilingEnabled());

    shpurdpProperties.setProperty(Configuration.SERVER_LOCKS_PROFILING.getKey(), Boolean.TRUE.toString());

    Assert.assertTrue(configuration.isServerLocksProfilingEnabled());
  }

  @Test
  public void testAlertCaching() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    Assert.assertFalse(configuration.isAlertCacheEnabled());

    shpurdpProperties.setProperty(Configuration.ALERTS_CACHE_ENABLED.getKey(), Boolean.TRUE.toString());
    shpurdpProperties.setProperty(Configuration.ALERTS_CACHE_FLUSH_INTERVAL.getKey(), "60");
    shpurdpProperties.setProperty(Configuration.ALERTS_CACHE_SIZE.getKey(), "1000");

    Assert.assertTrue(configuration.isAlertCacheEnabled());
    Assert.assertEquals(60, configuration.getAlertCacheFlushInterval());
    Assert.assertEquals(1000, configuration.getAlertCacheSize());
  }

  @Test
  public void testPropertyProviderThreadPoolSizes() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    Assert.assertEquals(2 * Runtime.getRuntime().availableProcessors(), configuration.getPropertyProvidersThreadPoolCoreSize());
    Assert.assertEquals(4 * Runtime.getRuntime().availableProcessors(), configuration.getPropertyProvidersThreadPoolMaxSize());

    shpurdpProperties.setProperty(Configuration.PROPERTY_PROVIDER_THREADPOOL_MAX_SIZE.getKey(), "44");
    shpurdpProperties.setProperty(Configuration.PROPERTY_PROVIDER_THREADPOOL_CORE_SIZE.getKey(), "22");

    Assert.assertEquals(22, configuration.getPropertyProvidersThreadPoolCoreSize());
    Assert.assertEquals(44, configuration.getPropertyProvidersThreadPoolMaxSize());
  }


  public void testGetHostRoleCommandStatusSummaryCacheSize() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE.getKey(), "3000");

    // When
    long actualCacheSize = configuration.getHostRoleCommandStatusSummaryCacheSize();

    // Then
    Assert.assertEquals(actualCacheSize, 3000L);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheSizeDefault() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    // When
    Long actualCacheSize = configuration.getHostRoleCommandStatusSummaryCacheSize();

    // Then
    Assert.assertEquals(actualCacheSize, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE.getDefaultValue());
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheExpiryDuration() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION.getKey(), "60");

    // When
    long actualCacheExpiryDuration = configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration();

    // Then
    Assert.assertEquals(actualCacheExpiryDuration, 60L);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheExpiryDurationDefault() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    // When
    Long actualCacheExpiryDuration = configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration();

    // Then
    Assert.assertEquals(actualCacheExpiryDuration, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION.getDefaultValue());
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheEnabled() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getKey(), "true");

    // When
    boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, true);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheDisabled() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getKey(), "false");

    // When
    boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, false);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheEnabledDefault() throws  Exception {
    // Given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    // When
    Boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED.getDefaultValue());
  }

  @Test
  public void testCustomDatabaseProperties() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty("server.jdbc.properties.foo", "fooValue");
    shpurdpProperties.setProperty("server.jdbc.properties.bar", "barValue");

    Properties properties = configuration.getDatabaseCustomProperties();
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("fooValue", properties.getProperty("eclipselink.jdbc.property.foo"));
    Assert.assertEquals("barValue", properties.getProperty("eclipselink.jdbc.property.bar"));
  }

  @Test
  public void testCustomPersistenceProperties() throws Exception {
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);
    shpurdpProperties.setProperty("server.persistence.properties.eclipselink.cache.coordination.channel", "FooChannel");
    shpurdpProperties.setProperty("server.persistence.properties.eclipselink.persistence-context.flush-mode", "commit");

    Properties properties = configuration.getPersistenceCustomProperties();
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("FooChannel", properties.getProperty("eclipselink.cache.coordination.channel"));
    Assert.assertEquals("commit", properties.getProperty("eclipselink.persistence-context.flush-mode"));
  }

  /**
   * Tests the default values for the {@link ThreadPoolEnabledPropertyProvider}.
   *
   * @throws Exception
   */
  @Test
  public void testThreadPoolEnabledPropertyProviderDefaults() throws Exception {
    final int SMALLEST_COMPLETION_SERIVCE_TIMEOUT_MS = 1000;
    final int LARGEST_COMPLETION_SERIVCE_TIMEOUT_MS = 5000;

    int processorCount = Runtime.getRuntime().availableProcessors();
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    long completionServiceTimeout = configuration.getPropertyProvidersCompletionServiceTimeout();
    int corePoolSize = configuration.getPropertyProvidersThreadPoolCoreSize();
    int maxPoolSize = configuration.getPropertyProvidersThreadPoolMaxSize();
    int workerQueueSize = configuration.getPropertyProvidersWorkerQueueSize();

    // test defaults
    Assert.assertEquals(5000, completionServiceTimeout);
    Assert.assertEquals(Configuration.PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT, corePoolSize);
    Assert.assertEquals(Configuration.PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT, maxPoolSize);
    Assert.assertEquals(Integer.MAX_VALUE, workerQueueSize);

    // now let's test to make sure these all make sense
    Assert.assertTrue(completionServiceTimeout >= SMALLEST_COMPLETION_SERIVCE_TIMEOUT_MS);
    Assert.assertTrue(completionServiceTimeout <= LARGEST_COMPLETION_SERIVCE_TIMEOUT_MS);
    Assert.assertTrue(corePoolSize <= maxPoolSize);
    Assert.assertTrue(corePoolSize > 2 && corePoolSize <= 128);
    Assert.assertTrue(maxPoolSize > 2 && maxPoolSize <= processorCount * 4);
    Assert.assertTrue(workerQueueSize > processorCount * 10);
  }

  /**
   * Tests that the Kerberos-authentication properties are read and properly and set in an
   * {@link ShpurdpKerberosAuthenticationProperties} instance when Kerberos authentication is enabled.
   */
  @Test
  public void testKerberosAuthenticationEnabled() throws IOException {
    File keytabFile = temp.newFile("spnego.service.keytab");

    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "true");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), keytabFile.getAbsolutePath());
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey(), "spnego/principal@REALM");
    properties.put(Configuration.KERBEROS_AUTH_AUTH_TO_LOCAL_RULES.getKey(), "DEFAULT");

    Configuration configuration = new Configuration(properties);

    ShpurdpKerberosAuthenticationProperties kerberosAuthenticationProperties = configuration.getKerberosAuthenticationProperties();

    Assert.assertTrue(kerberosAuthenticationProperties.isKerberosAuthenticationEnabled());
    Assert.assertEquals(keytabFile.getAbsolutePath(), kerberosAuthenticationProperties.getSpnegoKeytabFilePath());
    Assert.assertEquals("spnego/principal@REALM", kerberosAuthenticationProperties.getSpnegoPrincipalName());
    Assert.assertEquals("DEFAULT", kerberosAuthenticationProperties.getAuthToLocalRules());
  }

  /**
   * Tests that the Kerberos-authentication properties are read and properly and set in an
   * {@link ShpurdpKerberosAuthenticationProperties} instance when Kerberos authentication is enabled
   * and default values are expected to be used for unset properties.
   */
  @Test
  public void testKerberosAuthenticationEnabledUsingDefaults() throws IOException {
    File keytabFile = temp.newFile("spnego.service.keytab");

    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "true");
    // Force a specific path to the SPNEGO keytab file since internal validation expects to exist
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), keytabFile.getAbsolutePath());

    Configuration configuration = new Configuration(properties);

    ShpurdpKerberosAuthenticationProperties kerberosAuthenticationProperties = configuration.getKerberosAuthenticationProperties();

    Assert.assertTrue(kerberosAuthenticationProperties.isKerberosAuthenticationEnabled());
    Assert.assertEquals(keytabFile.getAbsolutePath(), kerberosAuthenticationProperties.getSpnegoKeytabFilePath());
    Assert.assertEquals("HTTP/" + StageUtils.getHostName(), kerberosAuthenticationProperties.getSpnegoPrincipalName());
    Assert.assertEquals("DEFAULT", kerberosAuthenticationProperties.getAuthToLocalRules());
  }

  /**
   * Tests that the Kerberos-authentication properties are read and properly set in an
   * {@link ShpurdpKerberosAuthenticationProperties} instance when Kerberos authentication is disabled.
   */
  @Test
  public void testKerberosAuthenticationDisabled() {
    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "false");

    Configuration configuration = new Configuration(properties);

    ShpurdpKerberosAuthenticationProperties kerberosAuthenticationProperties = configuration.getKerberosAuthenticationProperties();

    Assert.assertFalse(kerberosAuthenticationProperties.isKerberosAuthenticationEnabled());
    Assert.assertNull(kerberosAuthenticationProperties.getSpnegoKeytabFilePath());
    Assert.assertNull(kerberosAuthenticationProperties.getSpnegoPrincipalName());
    Assert.assertNull(kerberosAuthenticationProperties.getAuthToLocalRules());
  }

  @Test
  public void testKerberosAuthenticationDisabledWithValuesSet() {
    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "false");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), "/path/to/spnego/keytab/file");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey(), "spnego/principal@REALM");
    properties.put(Configuration.KERBEROS_AUTH_AUTH_TO_LOCAL_RULES.getKey(), "DEFAULT");

    Configuration configuration = new Configuration(properties);

    ShpurdpKerberosAuthenticationProperties kerberosAuthenticationProperties = configuration.getKerberosAuthenticationProperties();

    Assert.assertFalse(kerberosAuthenticationProperties.isKerberosAuthenticationEnabled());
    Assert.assertNull(kerberosAuthenticationProperties.getSpnegoKeytabFilePath());
    Assert.assertNull(kerberosAuthenticationProperties.getSpnegoPrincipalName());
    Assert.assertNull(kerberosAuthenticationProperties.getAuthToLocalRules());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKerberosAuthenticationEmptySPNEGOPrincipalName() throws IOException {
    File keytabFile = temp.newFile("spnego.service.keytab");

    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "true");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), keytabFile.getAbsolutePath());
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey(), "");
    properties.put(Configuration.KERBEROS_AUTH_AUTH_TO_LOCAL_RULES.getKey(), "DEFAULT");

    new Configuration(properties);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKerberosAuthenticationEmptySPNEGOKeytabFile() {
    Properties properties = new Properties();
    properties.put(Configuration.KERBEROS_AUTH_ENABLED.getKey(), "true");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_KEYTAB_FILE.getKey(), "");
    properties.put(Configuration.KERBEROS_AUTH_SPNEGO_PRINCIPAL.getKey(), "spnego/principal@REALM");
    properties.put(Configuration.KERBEROS_AUTH_AUTH_TO_LOCAL_RULES.getKey(), "DEFAULT");

    new Configuration(properties);
  }

  /**
   * Tests the default values for the {@link MetricsRetrievalService}.
   *
   * @throws Exception
   */
  @Test
  public void testMetricsRetrieveServiceDefaults() throws Exception {
    final int LOWEST_CACHE_TIMEOUT_MINUTES = 30;

    int processorCount = Runtime.getRuntime().availableProcessors();
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    int priority = configuration.getMetricsServiceThreadPriority();
    int cacheTimeout = configuration.getMetricsServiceCacheTimeout();
    int corePoolSize = configuration.getMetricsServiceThreadPoolCoreSize();
    int maxPoolSize = configuration.getMetricsServiceThreadPoolMaxSize();
    int workerQueueSize = configuration.getMetricsServiceWorkerQueueSize();

    // test defaults
    Assert.assertEquals(Thread.NORM_PRIORITY, priority);
    Assert.assertEquals(LOWEST_CACHE_TIMEOUT_MINUTES, cacheTimeout);
    Assert.assertEquals(Configuration.PROCESSOR_BASED_THREADPOOL_CORE_SIZE_DEFAULT, corePoolSize);
    Assert.assertEquals(Configuration.PROCESSOR_BASED_THREADPOOL_MAX_SIZE_DEFAULT, maxPoolSize);
    Assert.assertEquals(maxPoolSize * 10, workerQueueSize);

    // now let's test to make sure these all make sense
    Assert.assertTrue(priority <= Thread.NORM_PRIORITY);
    Assert.assertTrue(priority > Thread.MIN_PRIORITY);

    Assert.assertTrue(cacheTimeout >= LOWEST_CACHE_TIMEOUT_MINUTES);
    Assert.assertTrue(corePoolSize > 2 && corePoolSize <= 128);
    Assert.assertTrue(maxPoolSize > 2 && maxPoolSize <= processorCount * 4);
    Assert.assertTrue(workerQueueSize >= processorCount * 10);
  }

  /**
   * Tests that every {@link ConfigurationProperty} field in
   * {@link Configuration} has a property {@link Markdown} annotation.
   */
  @Test
  public void testAllPropertiesHaveMarkdownDescriptions() throws Exception {
    Field[] fields = Configuration.class.getDeclaredFields();
    for (Field field : fields) {
      if (field.getType() != ConfigurationProperty.class) {
        continue;
      }

      ConfigurationProperty<?> configurationProperty = (ConfigurationProperty<?>) field.get(null);
      Markdown markdown = field.getAnnotation(Markdown.class);
      if (null == markdown) {
        ConfigurationMarkdown configMarkdown = field.getAnnotation(ConfigurationMarkdown.class);
        markdown = configMarkdown != null ? configMarkdown.markdown() : null;
      }

      Assert.assertNotNull("The configuration property " + configurationProperty.getKey()
          + " is missing the Markdown annotation", markdown);

      Assert.assertFalse(
          "The configuration property " + configurationProperty.getKey()
              + " has a Markdown annotation with no description",
          StringUtils.isEmpty(markdown.description()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectsInvalidDtKeySize() {
    Properties properties = new Properties();
    properties.put(Configuration.TLS_EPHEMERAL_DH_KEY_SIZE.getKey(), "invalid");
    new Configuration(properties).getTlsEphemeralDhKeySize();
  }

  @Test
  public void testDefaultDhKeySizeIs2048() {
    Properties properties = new Properties();
    Assert.assertEquals(2048, new Configuration(properties).getTlsEphemeralDhKeySize());
  }

  @Test
  public void testOverridingDhtKeySize() {
    Properties properties = new Properties();
    properties.put(Configuration.TLS_EPHEMERAL_DH_KEY_SIZE.getKey(), "1024");
    Assert.assertEquals(1024, new Configuration(properties).getTlsEphemeralDhKeySize());
  }

  @Test
  public void canReadNonLatin1Properties() {
    Assert.assertEquals("árvíztűrő tükörfúrógép", new Configuration().getProperty("encoding.test"));
  }

  @Test
  public void testRemovingShpurdpProperties() throws Exception {
    final File shpurdpPropertiesFile = new File(Configuration.class.getClassLoader().getResource("shpurdp.properties").getPath());
    final String originalContent = FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8);
    assertTrue(originalContent.indexOf("testPropertyName") == -1);
    try {
      final String testshpurdpProperties = "\ntestPropertyName1=testValue1\ntestPropertyName2=testValue2\ntestPropertyName3=testValue3";
      FileUtils.writeStringToFile(shpurdpPropertiesFile, testshpurdpProperties, Charsets.UTF_8, true);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName1") > -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName2") > -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName3") > -1);

      final Configuration configuration = new Configuration();
      configuration.removePropertiesFromShpurdpProperties(Arrays.asList("testPropertyName2"));
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName1") > -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName2") == -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName3") > -1);

      configuration.removePropertiesFromShpurdpProperties(Arrays.asList("testPropertyName3"));
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName1") > -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName2") == -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName3") == -1);

      configuration.removePropertiesFromShpurdpProperties(Arrays.asList("testPropertyName1"));
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName1") == -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName2") == -1);
      assertTrue(FileUtils.readFileToString(shpurdpPropertiesFile, Charsets.UTF_8).indexOf("testPropertyName3") == -1);
    } finally {
      FileUtils.writeStringToFile(shpurdpPropertiesFile, originalContent, Charsets.UTF_8);
    }
  }

  @Test
  public void testMaxAuthenticationFailureConfiguration() {
    Configuration configuration;

    // Test default value is 0
    configuration = new Configuration();
    assertEquals(0, configuration.getMaxAuthenticationFailures());

    // Test configured value
    Properties properties = new Properties();
    properties.setProperty(Configuration.MAX_LOCAL_AUTHENTICATION_FAILURES.getKey(), "10");
    configuration = new Configuration(properties);
    assertEquals(10, configuration.getMaxAuthenticationFailures());

    properties.setProperty(Configuration.MAX_LOCAL_AUTHENTICATION_FAILURES.getKey(), "not a number");
    configuration = new Configuration(properties);
    try {
      configuration.getMaxAuthenticationFailures();
      Assert.fail("Expected NumberFormatException");
    } catch (NumberFormatException e) {
      // This is expected
    }
  }

  @Test
  public void testServerShowErrorStacksEnabled() throws  Exception {
    // given
    final Properties shpurdpProperties = new Properties();
    shpurdpProperties.setProperty(Configuration.SERVER_SHOW_ERROR_STACKS.getKey(), "true");
    final Configuration configuration = new Configuration(shpurdpProperties);

    // when
    boolean result = configuration.isServerShowErrorStacks();

    // then
    Assert.assertTrue(result);
  }

  @Test
  public void testServerShowErrorStacksDefault() throws  Exception {
    // given
    final Properties shpurdpProperties = new Properties();
    final Configuration configuration = new Configuration(shpurdpProperties);

    // when
    boolean result = configuration.isServerShowErrorStacks();

    // then
    Assert.assertEquals(result, Boolean.parseBoolean(Configuration.SERVER_SHOW_ERROR_STACKS.getDefaultValue()));
  }
}
