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

package org.apache.shpurdp.server.view;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterEntity;
import org.apache.shpurdp.view.ShpurdpHttpException;
import org.apache.shpurdp.view.ShpurdpStreamProvider;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteShpurdpClusterTest {

  @Rule
  public ExpectedException thrown= ExpectedException.none();


  @Test
  public void testGetConfigurationValue() throws Exception {

    ShpurdpStreamProvider clusterStreamProvider = createNiceMock(ShpurdpStreamProvider.class);

    final String desiredConfigsString = "{\"Clusters\": {\"desired_configs\": {\"test-site\": {\"tag\": \"TAG\"}}}}";
    final String configurationString = "{\"items\": [{\"properties\": {\"test.property.name\": \"test property value\"}}]}";
    final int[] desiredConfigPolls = {0};
    final int[] testConfigPolls = {0};
    final String clusterPath = "/api/v1/clusters/Test";

    expect(clusterStreamProvider.readFrom(eq( clusterPath + "?fields=services/ServiceInfo,hosts,Clusters"),
      eq("GET"), (String) isNull(), EasyMock.anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        desiredConfigPolls[0] += 1;
        return new ByteArrayInputStream(desiredConfigsString.getBytes());
      }
    }).anyTimes();

    expect(clusterStreamProvider.readFrom(eq(clusterPath + "/configurations?(type=test-site&tag=TAG)"),
      eq("GET"), (String)isNull(), EasyMock.anyObject())).andAnswer(new IAnswer<InputStream>() {
      @Override
      public InputStream answer() throws Throwable {
        testConfigPolls[0] += 1;
        return new ByteArrayInputStream(configurationString.getBytes());
      }
    }).anyTimes();

    RemoteShpurdpClusterEntity entity = createNiceMock(RemoteShpurdpClusterEntity.class);

    replay(clusterStreamProvider,entity);

    RemoteShpurdpCluster cluster = new RemoteShpurdpCluster("Test", clusterPath, clusterStreamProvider);

    String value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);
    assertEquals(testConfigPolls[0], 1);

    value = cluster.getConfigurationValue("test-site", "test.property.name");
    assertEquals(value, "test property value");
    assertEquals(desiredConfigPolls[0], 1);  // cache hit
    assertEquals(testConfigPolls[0], 1);
  }

  @Test
  public void testGetHostsForServiceComponent() throws IOException, ShpurdpHttpException {

    final String componentHostsString = "{\"host_components\": [{" +
      "\"HostRoles\": {" +
      "\"cluster_name\": \"Shpurdp\"," +
      "\"host_name\": \"host1\"}}, {" +
      "\"HostRoles\": {" +
      "\"cluster_name\": \"Shpurdp\"," +
      "\"host_name\": \"host2\"}}]}";

    ShpurdpStreamProvider clusterStreamProvider = createNiceMock(ShpurdpStreamProvider.class);

    String service = "SERVICE";
    String component = "COMPONENT";
    final String clusterPath = "/api/v1/clusters/Test";

    expect(clusterStreamProvider.readFrom(eq(String.format("%s/services/%s/components/%s?" +
        "fields=host_components/HostRoles/host_name", clusterPath, service, component)),
      eq("GET"), (String) isNull(), EasyMock.anyObject()))
      .andReturn(new ByteArrayInputStream(componentHostsString.getBytes()));

    RemoteShpurdpClusterEntity entity = createNiceMock(RemoteShpurdpClusterEntity.class);

    replay(clusterStreamProvider,entity);

    RemoteShpurdpCluster cluster = new RemoteShpurdpCluster("Test", clusterPath, clusterStreamProvider);

    List<String> hosts = cluster.getHostsForServiceComponent(service,component);
    Assert.assertEquals(2, hosts.size());
    Assert.assertEquals("host1",hosts.get(0));
    Assert.assertEquals("host2",hosts.get(1));

    verify(clusterStreamProvider, entity);

  }
}
