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

package org.apache.shpurdp.server.controller.internal;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.api.services.ShpurdpMetaInfo;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.shpurdp.server.api.services.stackadvisor.StackAdvisorResponse;
import org.apache.shpurdp.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.RequestStatus;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.state.Clusters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class RecommendationResourceProviderTest {

    @Mock
    private StackAdvisorHelper stackAdvisorHelper;
    @Mock
    private Configuration configuration;
    @Mock
    private Clusters clusters;
    @Mock
    private ShpurdpMetaInfo shpurdpMetaInfo;
    @Mock
    private ShpurdpManagementController managementController;

    private RecommendationResourceProvider provider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        provider = spy(new RecommendationResourceProvider(managementController));
        RecommendationResourceProvider.init(stackAdvisorHelper, configuration, clusters, shpurdpMetaInfo);
    }

    @Test
    public void testCreateConfigurationResources() throws Exception {
        Set<String> hosts = new HashSet<>(Arrays.asList("hostName1", "hostName2", "hostName3"));
        Set<String> services = new HashSet<>(Arrays.asList("serviceName1", "serviceName2", "serviceName3"));
        RequestStatus requestStatus = testCreateResources(hosts, services,
                StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS, true);

        assertNotNull(requestStatus);
        assertEquals(1, requestStatus.getAssociatedResources().size());
        assertEquals(Resource.Type.Recommendation, requestStatus.getAssociatedResources().iterator().next().getType());

        Map<String, Map<String, Object>> propertiesMap = requestStatus.getAssociatedResources().iterator().next().getPropertiesMap();
        assertEquals(2, propertiesMap.size());
        assertTrue(propertiesMap.containsKey("recommendations"));
        assertTrue(propertiesMap.containsKey("recommendations/blueprint/configurations"));

        assertEquals(1, propertiesMap.get("recommendations").size());
        assertTrue(propertiesMap.get("recommendations").containsKey("config-groups"));
        assertNotNull(propertiesMap.get("recommendations").get("config-groups"));

        assertEquals(0, propertiesMap.get("recommendations/blueprint/configurations").size());
    }

    @Test
    public void testCreateNotConfigurationResources() throws Exception {
        Set<String> hosts = new HashSet<>(Arrays.asList("hostName1", "hostName2", "hostName3"));
        Set<String> services = new HashSet<>(Arrays.asList("serviceName1", "serviceName2", "serviceName3"));
        RequestStatus requestStatus = testCreateResources(hosts, services,
                StackAdvisorRequest.StackAdvisorRequestType.HOST_GROUPS, false);

        assertNotNull(requestStatus);
        assertEquals(1, requestStatus.getAssociatedResources().size());
        assertEquals(Resource.Type.Recommendation, requestStatus.getAssociatedResources().iterator().next().getType());

        Map<String, Map<String, Object>> propertiesMap = requestStatus.getAssociatedResources().iterator().next().getPropertiesMap();
        assertEquals(7, propertiesMap.size());
        assertTrue(propertiesMap.containsKey(""));
        assertTrue(propertiesMap.containsKey("Recommendation"));
        assertTrue(propertiesMap.containsKey("Versions"));
        assertTrue(propertiesMap.containsKey("recommendations"));
        assertTrue(propertiesMap.containsKey("recommendations/blueprint"));
        assertTrue(propertiesMap.containsKey("recommendations/blueprint/configurations"));
        assertTrue(propertiesMap.containsKey("recommendations/blueprint_cluster_binding"));

        assertEquals(2, propertiesMap.get("").size());
        assertTrue(propertiesMap.get("").containsKey("hosts"));
        assertTrue(propertiesMap.get("").containsKey("services"));
        assertEquals(hosts, propertiesMap.get("").get("hosts"));
        assertEquals(services, propertiesMap.get("").get("services"));

        assertEquals(1, propertiesMap.get("Recommendation").size());
        assertTrue(propertiesMap.get("Recommendation").containsKey("id"));
        assertEquals(1, propertiesMap.get("Recommendation").get("id"));

        assertEquals(2, propertiesMap.get("Versions").size());
        assertTrue(propertiesMap.get("Versions").containsKey("stack_name"));
        assertTrue(propertiesMap.get("Versions").containsKey("stack_version"));
        assertEquals("stackName", propertiesMap.get("Versions").get("stack_name"));
        assertEquals("stackVersion", propertiesMap.get("Versions").get("stack_version"));

        assertEquals(1, propertiesMap.get("recommendations").size());
        assertTrue(propertiesMap.get("recommendations").containsKey("config-groups"));
        assertNotNull(propertiesMap.get("recommendations").get("config-groups"));

        assertEquals(1, propertiesMap.get("recommendations/blueprint").size());
        assertTrue(propertiesMap.get("recommendations/blueprint").containsKey("host_groups"));
        assertNotNull(propertiesMap.get("recommendations/blueprint").get("host_groups"));

        assertEquals(0, propertiesMap.get("recommendations/blueprint/configurations").size());

        assertEquals(1, propertiesMap.get("recommendations/blueprint_cluster_binding").size());
        assertTrue(propertiesMap.get("recommendations/blueprint_cluster_binding").containsKey("host_groups"));
        assertNotNull(propertiesMap.get("recommendations/blueprint_cluster_binding").get("host_groups"));
    }


    private RequestStatus testCreateResources(Set<String> hosts, Set<String> services,
                                              StackAdvisorRequest.StackAdvisorRequestType type,
                                              Boolean configsOnlyResponse) throws Exception {
        StackAdvisorRequest stackAdvisorRequest = StackAdvisorRequest.StackAdvisorRequestBuilder.
                forStack(null, null).ofType(type).
                withConfigsResponse(configsOnlyResponse).
                build();

        Request request = mock(Request.class);
        doReturn(stackAdvisorRequest).when(provider).prepareStackAdvisorRequest(eq(request));

        RecommendationResponse response = new RecommendationResponse();
        RecommendationResponse.Recommendation recommendation = new RecommendationResponse.Recommendation();

        recommendation.setConfigGroups(new HashSet<>());

        RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
        blueprint.setConfigurations(new HashMap<>());
        blueprint.setHostGroups(new HashSet<>());
        recommendation.setBlueprint(blueprint);

        RecommendationResponse.BlueprintClusterBinding blueprintClusterBinding = new RecommendationResponse.BlueprintClusterBinding();
        blueprintClusterBinding.setHostGroups(new HashSet<>());
        recommendation.setBlueprintClusterBinding(blueprintClusterBinding);

        response.setRecommendations(recommendation);

        response.setId(1);

        StackAdvisorResponse.Version version = new StackAdvisorResponse.Version();
        version.setStackName("stackName");
        version.setStackVersion("stackVersion");
        response.setVersion(version);

        response.setHosts(hosts);
        response.setServices(services);

        when(stackAdvisorHelper.recommend(any(StackAdvisorRequest.class))).thenReturn(response);

        return provider.createResources(request);
    }
}
