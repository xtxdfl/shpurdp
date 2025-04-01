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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.ResourceProvider;
import org.apache.shpurdp.server.controller.utilities.PredicateBuilder;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.shpurdp.server.orm.DBAccessor;
import org.apache.shpurdp.server.orm.dao.ClusterDAO;
import org.apache.shpurdp.server.orm.dao.GroupDAO;
import org.apache.shpurdp.server.orm.dao.MemberDAO;
import org.apache.shpurdp.server.orm.dao.PermissionDAO;
import org.apache.shpurdp.server.orm.dao.PrincipalDAO;
import org.apache.shpurdp.server.orm.dao.PrivilegeDAO;
import org.apache.shpurdp.server.orm.dao.ResourceDAO;
import org.apache.shpurdp.server.orm.dao.ResourceTypeDAO;
import org.apache.shpurdp.server.orm.dao.UserDAO;
import org.apache.shpurdp.server.orm.dao.ViewDAO;
import org.apache.shpurdp.server.orm.dao.ViewInstanceDAO;
import org.apache.shpurdp.server.orm.entities.ClusterEntity;
import org.apache.shpurdp.server.orm.entities.GroupEntity;
import org.apache.shpurdp.server.orm.entities.PermissionEntity;
import org.apache.shpurdp.server.orm.entities.PrincipalEntity;
import org.apache.shpurdp.server.orm.entities.PrincipalTypeEntity;
import org.apache.shpurdp.server.orm.entities.PrivilegeEntity;
import org.apache.shpurdp.server.orm.entities.ResourceEntity;
import org.apache.shpurdp.server.orm.entities.ResourceTypeEntity;
import org.apache.shpurdp.server.orm.entities.UserEntity;
import org.apache.shpurdp.server.orm.entities.ViewEntity;
import org.apache.shpurdp.server.orm.entities.ViewInstanceEntity;
import org.apache.shpurdp.server.security.SecurityHelper;
import org.apache.shpurdp.server.security.TestAuthenticationFactory;
import org.apache.shpurdp.server.security.authorization.AuthorizationException;
import org.apache.shpurdp.server.security.authorization.ResourceType;
import org.apache.shpurdp.server.view.ViewInstanceHandlerList;
import org.apache.shpurdp.server.view.ViewRegistry;
import org.apache.shpurdp.server.view.ViewRegistryTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * ShpurdpPrivilegeResourceProvider tests.
 */
public class ShpurdpPrivilegeResourceProviderTest extends EasyMockSupport {

  @Before
  public void resetGlobalMocks() {
    resetAll();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_NonAdministrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_NonAdministrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResource_Administrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testGetResource_Administrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test
  public void testUpdateResources_Administrator_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_Administrator_Other() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_NonAdministrator_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_NonAdministrator_Other() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_NonAdministrator() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResources_allTypes() throws Exception {
    Injector injector = createInjector();

    PrivilegeEntity shpurdpPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity shpurdpResourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity shpurdpResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity shpurdpUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity shpurdpPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity shpurdpPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity shpurdpPermissionEntity = createNiceMock(PermissionEntity.class);
    expect(shpurdpPrivilegeEntity.getResource()).andReturn(shpurdpResourceEntity).anyTimes();
    expect(shpurdpPrivilegeEntity.getId()).andReturn(31).anyTimes();
    expect(shpurdpPrivilegeEntity.getPrincipal()).andReturn(shpurdpPrincipalEntity).anyTimes();
    expect(shpurdpPrivilegeEntity.getPermission()).andReturn(shpurdpPermissionEntity).anyTimes();
    expect(shpurdpResourceEntity.getResourceType()).andReturn(shpurdpResourceTypeEntity).anyTimes();
    expect(shpurdpResourceTypeEntity.getId()).andReturn(ResourceType.SHPURDP.getId()).anyTimes();
    expect(shpurdpResourceTypeEntity.getName()).andReturn(ResourceType.SHPURDP.name()).anyTimes();
    expect(shpurdpPrincipalEntity.getId()).andReturn(1L).anyTimes();
    expect(shpurdpUserEntity.getPrincipal()).andReturn(shpurdpPrincipalEntity).anyTimes();
    expect(shpurdpUserEntity.getUserName()).andReturn("joe").anyTimes();
    expect(shpurdpPermissionEntity.getPermissionName()).andReturn("SHPURDP.ADMINISTRATOR").anyTimes();
    expect(shpurdpPermissionEntity.getPermissionLabel()).andReturn("Shpurdp Administrator").anyTimes();
    expect(shpurdpPrincipalEntity.getPrincipalType()).andReturn(shpurdpPrincipalTypeEntity).anyTimes();
    expect(shpurdpPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();

    PrivilegeEntity viewPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity viewResourceEntity = createNiceMock(ResourceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ResourceTypeEntity viewResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity viewUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity viewPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity viewPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity viewPermissionEntity = createNiceMock(PermissionEntity.class);
    expect(viewPrivilegeEntity.getResource()).andReturn(viewResourceEntity).anyTimes();
    expect(viewPrivilegeEntity.getPrincipal()).andReturn(viewPrincipalEntity).anyTimes();
    expect(viewPrivilegeEntity.getPermission()).andReturn(viewPermissionEntity).anyTimes();
    expect(viewPrivilegeEntity.getId()).andReturn(33).anyTimes();
    expect(viewResourceEntity.getResourceType()).andReturn(viewResourceTypeEntity).anyTimes();
    expect(viewResourceTypeEntity.getId()).andReturn(ResourceType.VIEW.getId()).anyTimes();
    expect(viewResourceTypeEntity.getName()).andReturn(ResourceType.VIEW.name()).anyTimes();
    expect(viewPrincipalEntity.getId()).andReturn(5L).anyTimes();
    expect(viewEntity.getInstances()).andReturn(Arrays.asList(viewInstanceEntity)).anyTimes();
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).anyTimes();
    expect(viewEntity.getCommonName()).andReturn("view").anyTimes();
    expect(viewEntity.getVersion()).andReturn("1.0.1").anyTimes();
    expect(viewEntity.isDeployed()).andReturn(true).anyTimes();
    expect(viewInstanceEntity.getName()).andReturn("inst1").anyTimes();
    expect(viewInstanceEntity.getResource()).andReturn(viewResourceEntity).anyTimes();
    expect(viewUserEntity.getPrincipal()).andReturn(viewPrincipalEntity).anyTimes();
    expect(viewUserEntity.getUserName()).andReturn("bob").anyTimes();
    expect(viewPermissionEntity.getPermissionName()).andReturn("VIEW.USER").anyTimes();
    expect(viewPermissionEntity.getPermissionLabel()).andReturn("View User").anyTimes();
    expect(viewPrincipalEntity.getPrincipalType()).andReturn(viewPrincipalTypeEntity).anyTimes();
    expect(viewPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();

    PrivilegeEntity clusterPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity clusterResourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity clusterResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity clusterUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity clusterPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity clusterPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity clusterPermissionEntity = createNiceMock(PermissionEntity.class);
    ClusterEntity clusterEntity = createNiceMock(ClusterEntity.class);
    expect(clusterPrivilegeEntity.getResource()).andReturn(clusterResourceEntity).anyTimes();
    expect(clusterPrivilegeEntity.getPrincipal()).andReturn(clusterPrincipalEntity).anyTimes();
    expect(clusterPrivilegeEntity.getPermission()).andReturn(clusterPermissionEntity).anyTimes();
    expect(clusterPrivilegeEntity.getId()).andReturn(32).anyTimes();
    expect(clusterResourceEntity.getId()).andReturn(7L).anyTimes();
    expect(clusterResourceEntity.getResourceType()).andReturn(clusterResourceTypeEntity).anyTimes();
    expect(clusterResourceTypeEntity.getId()).andReturn(ResourceType.CLUSTER.getId()).anyTimes();
    expect(clusterResourceTypeEntity.getName()).andReturn(ResourceType.CLUSTER.name()).anyTimes();
    expect(clusterPrincipalEntity.getId()).andReturn(8L).anyTimes();
    expect(clusterUserEntity.getPrincipal()).andReturn(clusterPrincipalEntity).anyTimes();
    expect(clusterUserEntity.getUserName()).andReturn("jeff").anyTimes();
    expect(clusterPermissionEntity.getPermissionName()).andReturn("CLUSTER.USER").anyTimes();
    expect(clusterPermissionEntity.getPermissionLabel()).andReturn("Cluster User").anyTimes();
    expect(clusterPrincipalEntity.getPrincipalType()).andReturn(clusterPrincipalTypeEntity).anyTimes();
    expect(clusterPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();
    expect(clusterEntity.getResource()).andReturn(clusterResourceEntity).anyTimes();
    expect(clusterEntity.getClusterName()).andReturn("cluster1").anyTimes();

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(shpurdpUserEntity);
    userEntities.add(viewUserEntity);
    userEntities.add(clusterUserEntity);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();
    privilegeEntities.add(shpurdpPrivilegeEntity);
    privilegeEntities.add(viewPrivilegeEntity);
    privilegeEntities.add(clusterPrivilegeEntity);

    List<ClusterEntity> clusterEntities = new LinkedList<>();
    clusterEntities.add(clusterEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(clusterEntities).atLeastOnce();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities).atLeastOnce();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUsersByPrincipal(EasyMock.anyObject())).andReturn(userEntities).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    ResourceProvider provider = getResourceProvider(injector);

    ViewRegistry.getInstance().addDefinition(viewEntity);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(3, resources.size());

    Map<Object, Resource> resourceMap = new HashMap<>();

    for (Resource resource : resources) {
      resourceMap.put(resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRIVILEGE_ID), resource);
    }

    Resource resource1 = resourceMap.get(31);

    Assert.assertEquals(6, resource1.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("SHPURDP.ADMINISTRATOR", resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Shpurdp Administrator", resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("joe", resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_TYPE));
    Assert.assertEquals(31, resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRIVILEGE_ID));
    Assert.assertEquals("SHPURDP", resource1.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    Resource resource2 = resourceMap.get(32);

    Assert.assertEquals(7, resource2.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("CLUSTER.USER", resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Cluster User", resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("jeff", resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_TYPE));
    Assert.assertEquals(32, resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRIVILEGE_ID));
    Assert.assertEquals("cluster1", resource2.getPropertyValue(ClusterPrivilegeResourceProvider.CLUSTER_NAME));
    Assert.assertEquals("CLUSTER", resource2.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    Resource resource3 = resourceMap.get(33);

    Assert.assertEquals(9, resource3.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("VIEW.USER", resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("View User", resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("bob", resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_TYPE));
    Assert.assertEquals(33, resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRIVILEGE_ID));
    Assert.assertEquals("view", resource3.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.0.1", resource3.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals("inst1", resource3.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("VIEW", resource3.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_SHPURDP() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Shpurdp Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(1L).atLeastOnce();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("SHPURDP").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    replay(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, resourceEntity, privilegeEntity);

    Map<Long, UserEntity> userEntities = new HashMap<>();
    Map<Long, GroupEntity> groupEntities = new HashMap<>();
    Map<Long, PermissionEntity> roleEntities = new HashMap<>();
    Map<Long, Object> resourceEntities = new HashMap<>();

    ShpurdpPrivilegeResourceProvider provider = new ShpurdpPrivilegeResourceProvider();
    Resource resource = provider.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, provider.getPropertyIds());

    Assert.assertEquals(ResourceType.SHPURDP.name(), resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    verify(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, resourceEntity, privilegeEntity);
  }

  @Test
  public void testToResource_CLUSTER() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(1L).atLeastOnce();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ClusterEntity clusterEntity = createMock(ClusterEntity.class);
    expect(clusterEntity.getClusterName()).andReturn("TestCluster").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("CLUSTER").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    replay(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, clusterEntity, resourceEntity, privilegeEntity);

    Map<Long, UserEntity> userEntities = new HashMap<>();
    Map<Long, GroupEntity> groupEntities = new HashMap<>();
    Map<Long, PermissionEntity> roleEntities = new HashMap<>();

    Map<Long, Object> resourceEntities = new HashMap<>();
    resourceEntities.put(resourceEntity.getId(), clusterEntity);

    ShpurdpPrivilegeResourceProvider provider = new ShpurdpPrivilegeResourceProvider();
    Resource resource = provider.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, provider.getPropertyIds());

    Assert.assertEquals("TestCluster", resource.getPropertyValue(ClusterPrivilegeResourceProvider.CLUSTER_NAME));
    Assert.assertEquals(ResourceType.CLUSTER.name(), resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    verify(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, clusterEntity, resourceEntity, privilegeEntity);
  }

  @Test
  public void testToResource_VIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(1L).atLeastOnce();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ViewEntity viewEntity = createMock(ViewEntity.class);
    expect(viewEntity.getCommonName()).andReturn("TestView").atLeastOnce();
    expect(viewEntity.getVersion()).andReturn("1.2.3.4").atLeastOnce();

    ViewInstanceEntity viewInstanceEntity = createMock(ViewInstanceEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).atLeastOnce();
    expect(viewInstanceEntity.getName()).andReturn("Test View").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("VIEW").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    replay(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, viewInstanceEntity, viewEntity, resourceEntity, privilegeEntity);

    Map<Long, UserEntity> userEntities = new HashMap<>();
    Map<Long, GroupEntity> groupEntities = new HashMap<>();
    Map<Long, PermissionEntity> roleEntities = new HashMap<>();

    Map<Long, Object> resourceEntities = new HashMap<>();
    resourceEntities.put(resourceEntity.getId(), viewInstanceEntity);

    ShpurdpPrivilegeResourceProvider provider = new ShpurdpPrivilegeResourceProvider();
    Resource resource = provider.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    verify(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, viewInstanceEntity, viewEntity, resourceEntity, privilegeEntity);
  }

  @Test
  public void testToResource_SpecificVIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(1L).atLeastOnce();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ViewEntity viewEntity = createMock(ViewEntity.class);
    expect(viewEntity.getCommonName()).andReturn("TestView").atLeastOnce();
    expect(viewEntity.getVersion()).andReturn("1.2.3.4").atLeastOnce();

    ViewInstanceEntity viewInstanceEntity = createMock(ViewInstanceEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).atLeastOnce();
    expect(viewInstanceEntity.getName()).andReturn("Test View").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("TestView{1.2.3.4}").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    replay(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, viewInstanceEntity, viewEntity, resourceEntity, privilegeEntity);

    Map<Long, UserEntity> userEntities = new HashMap<>();
    Map<Long, GroupEntity> groupEntities = new HashMap<>();
    Map<Long, PermissionEntity> roleEntities = new HashMap<>();

    Map<Long, Object> resourceEntities = new HashMap<>();
    resourceEntities.put(resourceEntity.getId(), viewInstanceEntity);

    ShpurdpPrivilegeResourceProvider provider = new ShpurdpPrivilegeResourceProvider();
    Resource resource = provider.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.TYPE));

    verify(permissionEntity, principalTypeEntity, principalEntity, resourceTypeEntity, viewInstanceEntity, viewEntity, resourceEntity, privilegeEntity);
  }

  private void createResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity = createMockPrincipalEntity(2L, createMockPrincipalTypeEntity("USER"));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);

    PrivilegeEntity privilegeEntity = createMockPrivilegeEntity(2, clusterResourceEntity, principalEntity, permissionEntity);

    Set<PrivilegeEntity> privilegeEntities = new HashSet<>();
    privilegeEntities.add(privilegeEntity);

    expect(principalEntity.getPrivileges()).andReturn(privilegeEntities).atLeastOnce();

    UserEntity userEntity = createMockUserEntity(principalEntity, "User1");

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.exists(anyObject(PrivilegeEntity.class))).andReturn(false).atLeastOnce();
    privilegeDAO.create(anyObject(PrivilegeEntity.class));
    expectLastCall().once();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName("User1")).andReturn(userEntity).atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.findById(2L)).andReturn(principalEntity).atLeastOnce();
    expect(principalDAO.merge(principalEntity)).andReturn(principalEntity).once();

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(Collections.emptyList()).atLeastOnce();

    ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    expect(resourceDAO.findById(1L)).andReturn(clusterResourceEntity).atLeastOnce();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResourceTypeEntity))
        .andReturn(permissionEntity)
        .atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "CLUSTER.OPERATOR");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, "User1");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    ResourceProvider provider = getResourceProvider(injector);
    provider.createResources(request);

    verifyAll();
  }

  private void getResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();

    PrincipalEntity principalEntity = createMockPrincipalEntity(1L, createMockPrincipalTypeEntity("USER"));

    List<PrincipalEntity> principalEntities = new LinkedList<>();
    principalEntities.add(principalEntity);

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(createMockUserEntity(principalEntity, "User1"));

    ResourceTypeEntity shpurdpResourceTypeEntity = createMockResourceTypeEntity(ResourceType.SHPURDP);
    ResourceEntity shpurdpResourceEntity = createMockResourceEntity(1L, shpurdpResourceTypeEntity);

    privilegeEntities.add(createMockPrivilegeEntity(
        1, shpurdpResourceEntity,
        principalEntity,
        createMockPermissionEntity("SHPURDP.ADMINISTRATOR", "Shpurdp Administrator", shpurdpResourceTypeEntity)));

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities).atLeastOnce();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities).atLeastOnce();

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(Collections.emptyList()).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("SHPURDP.ADMINISTRATOR", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Shpurdp Administrator", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("User1", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_TYPE));

    verifyAll();
  }

  private void getResourceTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity1 = createMockPrincipalEntity(1L, createMockPrincipalTypeEntity("USER"));
    PrincipalEntity principalEntity2 = createMockPrincipalEntity(2L, createMockPrincipalTypeEntity("USER"));

    List<PrincipalEntity> principalEntities = new LinkedList<>();
    principalEntities.add(principalEntity1);
    principalEntities.add(principalEntity2);

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(createMockUserEntity(principalEntity1, requestedUsername));
    userEntities.add(createMockUserEntity(principalEntity2, "Not" + requestedUsername));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();
    privilegeEntities.add(createMockPrivilegeEntity(1, clusterResourceEntity, principalEntity1, permissionEntity));
    privilegeEntities.add(createMockPrivilegeEntity(2, clusterResourceEntity, principalEntity2, permissionEntity));

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities).atLeastOnce();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities).atLeastOnce();

    List<ClusterEntity> clusterEntities = new ArrayList<>();
    clusterEntities.add(createMockClusterEntity("c1", clusterResourceEntity));

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(clusterEntities).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), createPredicate(1L));

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("CLUSTER.OPERATOR", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Cluster Operator", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals(requestedUsername, resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource.getPropertyValue(ShpurdpPrivilegeResourceProvider.PRINCIPAL_TYPE));

    verifyAll();
  }

  private void updateResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity1 = createMockPrincipalEntity(1L, createMockPrincipalTypeEntity("USER"));
    PrincipalEntity principalEntity2 = createMockPrincipalEntity(2L, createMockPrincipalTypeEntity("USER"));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);

    PrivilegeEntity privilegeEntity1 = createMockPrivilegeEntity(1, clusterResourceEntity, principalEntity1, permissionEntity);
    PrivilegeEntity privilegeEntity2 = createMockPrivilegeEntity(2, clusterResourceEntity, principalEntity2, permissionEntity);

    Set<PrivilegeEntity> privilege1Entities = new HashSet<>();
    privilege1Entities.add(privilegeEntity1);

    Set<PrivilegeEntity> privilege2Entities = new HashSet<>();
    privilege2Entities.add(privilegeEntity2);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();
    privilegeEntities.addAll(privilege1Entities);
    privilegeEntities.addAll(privilege2Entities);

    expect(principalEntity2.getPrivileges()).andReturn(privilege2Entities).atLeastOnce();

    UserEntity userEntity = createMockUserEntity(principalEntity1, requestedUsername);

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findByResourceId(1L)).andReturn(privilegeEntities).atLeastOnce();
    privilegeDAO.remove(privilegeEntity2);
    expectLastCall().atLeastOnce();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.findById(1L)).andReturn(principalEntity1).atLeastOnce();
    expect(principalDAO.merge(principalEntity2)).andReturn(principalEntity2).atLeastOnce();

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(Collections.emptyList()).atLeastOnce();

    ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    expect(resourceDAO.findById(1L)).andReturn(clusterResourceEntity).atLeastOnce();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResourceTypeEntity))
        .andReturn(permissionEntity)
        .atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "CLUSTER.OPERATOR");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, requestedUsername);
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    ResourceProvider provider = getResourceProvider(injector);
    provider.updateResources(request, createPredicate(1L));

    verifyAll();
  }

  private void deleteResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity1 = createMockPrincipalEntity(1L, createMockPrincipalTypeEntity("USER"));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);

    PrivilegeEntity privilegeEntity1 = createMockPrivilegeEntity(1, clusterResourceEntity, principalEntity1, permissionEntity);

    Set<PrivilegeEntity> privilege1Entities = new HashSet<>();
    privilege1Entities.add(privilegeEntity1);

    expect(principalEntity1.getPrivileges()).andReturn(privilege1Entities).atLeastOnce();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findById(1)).andReturn(privilegeEntity1).atLeastOnce();
    privilegeDAO.remove(privilegeEntity1);
    expectLastCall().atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.merge(principalEntity1)).andReturn(principalEntity1).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(1L));

    verifyAll();
  }

  private Predicate createPredicate(Long id) {
    return new PredicateBuilder()
        .property(ShpurdpPrivilegeResourceProvider.PRIVILEGE_ID)
        .equals(id)
        .toPredicate();
  }

  private ClusterEntity createMockClusterEntity(String clusterName, ResourceEntity resourceEntity) {
    ClusterEntity clusterEntity = createMock(ClusterEntity.class);
    expect(clusterEntity.getClusterName()).andReturn(clusterName).anyTimes();
    expect(clusterEntity.getResource()).andReturn(resourceEntity).anyTimes();
    return clusterEntity;
  }

  private UserEntity createMockUserEntity(PrincipalEntity principalEntity, String username) {
    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    return userEntity;
  }

  private PermissionEntity createMockPermissionEntity(String name, String label, ResourceTypeEntity resourceTypeEntity) {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn(name).anyTimes();
    expect(permissionEntity.getPermissionLabel()).andReturn(label).anyTimes();
    expect(permissionEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    return permissionEntity;
  }

  private PrincipalTypeEntity createMockPrincipalTypeEntity(String typeName) {
    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn(typeName).anyTimes();
    return principalTypeEntity;
  }

  private PrincipalEntity createMockPrincipalEntity(Long id, PrincipalTypeEntity principalTypeEntity) {
    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(id).anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    return principalEntity;
  }

  private ResourceTypeEntity createMockResourceTypeEntity(ResourceType resourceType) {
    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getId()).andReturn(resourceType.getId()).anyTimes();
    expect(resourceTypeEntity.getName()).andReturn(resourceType.name()).anyTimes();
    return resourceTypeEntity;
  }

  private ResourceEntity createMockResourceEntity(Long id, ResourceTypeEntity resourceTypeEntity) {
    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(id).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    return resourceEntity;
  }

  private PrivilegeEntity createMockPrivilegeEntity(Integer id, ResourceEntity resourceEntity, PrincipalEntity principalEntity, PermissionEntity permissionEntity) {
    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(id).anyTimes();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    return privilegeEntity;
  }

  private ResourceProvider getResourceProvider(Injector injector) {
    ViewRegistry.initInstance(ViewRegistryTest.getRegistry(
        injector.getInstance(ViewDAO.class),
        injector.getInstance(ViewInstanceDAO.class),
        injector.getInstance(UserDAO.class),
        injector.getInstance(MemberDAO.class),
        injector.getInstance(PrivilegeDAO.class),
        injector.getInstance(PermissionDAO.class),
        injector.getInstance(ResourceDAO.class),
        injector.getInstance(ResourceTypeDAO.class),
        injector.getInstance(SecurityHelper.class),
        injector.getInstance(ViewInstanceHandlerList.class),
        null,
        null,
        null));
    PrivilegeResourceProvider.init(injector.getInstance(PrivilegeDAO.class),
        injector.getInstance(UserDAO.class),
        injector.getInstance(GroupDAO.class),
        injector.getInstance(PrincipalDAO.class),
        injector.getInstance(PermissionDAO.class),
        injector.getInstance(ResourceDAO.class));
    ShpurdpPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
    return new ShpurdpPrivilegeResourceProvider();
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
        bind(ViewInstanceDAO.class).toInstance(createNiceMock(ViewInstanceDAO.class));
        bind(ViewInstanceHandlerList.class).toInstance(createNiceMock(ViewInstanceHandlerList.class));
        bind(MemberDAO.class).toInstance(createNiceMock(MemberDAO.class));

        bind(PrivilegeDAO.class).toInstance(createMock(PrivilegeDAO.class));
        bind(PrincipalDAO.class).toInstance(createMock(PrincipalDAO.class));
        bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
        bind(UserDAO.class).toInstance(createMock(UserDAO.class));
        bind(GroupDAO.class).toInstance(createMock(GroupDAO.class));
        bind(ResourceDAO.class).toInstance(createMock(ResourceDAO.class));
        bind(ClusterDAO.class).toInstance(createMock(ClusterDAO.class));
      }
    });
  }
}
