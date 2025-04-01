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
package org.apache.shpurdp.server.events.listeners.upgrade;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.HashMap;
import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.events.StackUpgradeFinishEvent;
import org.apache.shpurdp.server.events.publishers.VersionEventPublisher;
import org.apache.shpurdp.server.metadata.RoleCommandOrderProvider;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.Service;
import org.apache.shpurdp.server.state.ServiceComponent;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Provider;


/**
 * StackVersionListener tests.
 */
@RunWith(EasyMockRunner.class)
public class StackUpgradeFinishListenerTest extends EasyMockSupport {

  public static final String STACK_NAME = "HDP-2.4.0.0";
  public static final String STACK_VERSION = "2.4.0.0";

  private VersionEventPublisher publisher = new VersionEventPublisher();

  private Cluster cluster;

  @TestSubject
  private StackUpgradeFinishListener listener = new StackUpgradeFinishListener(publisher);

  @Mock(type = MockType.NICE, fieldName = "roleCommandOrderProvider")
  private Provider<RoleCommandOrderProvider> roleCommandOrderProviderProviderMock;

  @Mock(type = MockType.NICE, fieldName = "shpurdpMetaInfo")
  private Provider<ShpurdpMetaInfo> shpurdpMetaInfoProvider = null;

  @Before
  public void setup() throws Exception {
    ShpurdpMetaInfo ami = createNiceMock(ShpurdpMetaInfo.class);

    cluster = createNiceMock(Cluster.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    Service service = createNiceMock(Service.class);
    Map<String, Service> services = new HashMap<>();
    Map<String, ServiceComponent> components = new HashMap<>();

    services.put("mock_service",service);
    components.put("mock_component", serviceComponent);

    expect(cluster.getServices()).andReturn(services);
    expect(service.getServiceComponents()).andReturn(components);
    expect(shpurdpMetaInfoProvider.get()).andReturn(ami);
    ami.reconcileAlertDefinitions(cluster,true);
    expectLastCall();

    serviceComponent.updateComponentInfo();
    service.updateServiceInfo();
  }

  @Test
  public void testupdateComponentInfo() throws ShpurdpException {
    replayAll();

    sendEventAndVerify();
  }


  private void sendEventAndVerify() {
    StackUpgradeFinishEvent event = new StackUpgradeFinishEvent(cluster);
    listener.onShpurdpEvent(event);

    verifyAll();
  }
}
