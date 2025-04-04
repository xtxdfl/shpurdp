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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.controller.ShpurdpManagementController;
import org.apache.shpurdp.server.controller.StackConfigurationResponse;
import org.apache.shpurdp.server.controller.StackLevelConfigurationRequest;
import org.apache.shpurdp.server.controller.spi.NoSuchParentResourceException;
import org.apache.shpurdp.server.controller.spi.NoSuchResourceException;
import org.apache.shpurdp.server.controller.spi.Predicate;
import org.apache.shpurdp.server.controller.spi.Request;
import org.apache.shpurdp.server.controller.spi.Resource;
import org.apache.shpurdp.server.controller.spi.Resource.Type;
import org.apache.shpurdp.server.controller.spi.SystemException;
import org.apache.shpurdp.server.controller.spi.UnsupportedPropertyException;
import org.apache.shpurdp.server.controller.utilities.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class StackLevelConfigurationResourceProvider extends
    ReadOnlyResourceProvider {

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "stack_version");

  public static final String PROPERTY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_name");

  public static final String PROPERTY_DISPLAY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_display_name");

  public static final String PROPERTY_VALUE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_value");

  public static final String PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_value_attributes");

  public static final String DEPENDS_ON_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_depends_on");

  public static final String PROPERTY_DESCRIPTION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_description");
  
  public static final String PROPERTY_PROPERTY_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "property_type");

  public static final String PROPERTY_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "type");

  public static final String PROPERTY_FINAL_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackLevelConfigurations", "final");

  /**
   * The key property ids for a StackLevelConfiguration resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .put(Type.StackLevelConfiguration, PROPERTY_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a StackLevelConfiguration resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      PROPERTY_NAME_PROPERTY_ID,
      PROPERTY_DISPLAY_NAME_PROPERTY_ID,
      PROPERTY_VALUE_PROPERTY_ID,
      PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID,
      DEPENDS_ON_PROPERTY_ID,
      PROPERTY_DESCRIPTION_PROPERTY_ID,
      PROPERTY_PROPERTY_TYPE_PROPERTY_ID,
      PROPERTY_TYPE_PROPERTY_ID,
      PROPERTY_FINAL_PROPERTY_ID);

  protected StackLevelConfigurationResourceProvider(ShpurdpManagementController managementController) {
    super(Type.StackLevelConfiguration, propertyIds, keyPropertyIds, managementController);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackLevelConfigurationRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackConfigurationResponse> responses = getResources(new Command<Set<StackConfigurationResponse>>() {
      @Override
      public Set<StackConfigurationResponse> invoke() throws ShpurdpException {
        return getManagementController().getStackLevelConfigurations(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    
    for (StackConfigurationResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackLevelConfiguration);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, PROPERTY_NAME_PROPERTY_ID,
          response.getPropertyName(), requestedIds);

      setResourceProperty(resource, PROPERTY_VALUE_PROPERTY_ID,
          response.getPropertyValue(), requestedIds);

      setResourceProperty(resource, PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID,
          response.getPropertyValueAttributes(), requestedIds);

      setResourceProperty(resource, DEPENDS_ON_PROPERTY_ID,
          response.getDependsOnProperties(), requestedIds);

      setResourceProperty(resource, PROPERTY_DESCRIPTION_PROPERTY_ID,
          response.getPropertyDescription(), requestedIds);

      //should not be returned if empty
      if (StringUtils.isNotEmpty(response.getPropertyDisplayName())) {
        setResourceProperty(resource, PROPERTY_DISPLAY_NAME_PROPERTY_ID,
            response.getPropertyDisplayName(), requestedIds);
      }
      
      setResourceProperty(resource, PROPERTY_PROPERTY_TYPE_PROPERTY_ID, 
          response.getPropertyType(), requestedIds);
      
      setResourceProperty(resource, PROPERTY_TYPE_PROPERTY_ID,
          response.getType(), requestedIds);

      setDefaultPropertiesAttributes(resource, requestedIds);

      for (Map.Entry<String, String> attribute : response.getPropertyAttributes().entrySet()) {
        setResourceProperty(resource, PropertyHelper.getPropertyId("StackLevelConfigurations", attribute.getKey()),
            attribute.getValue(), requestedIds);
      }

      resources.add(resource);
    }

    return resources;
  }

  /**
   * Set default values for properties attributes before applying original ones
   * to prevent absence in case of empty attributes map
   */
  private void setDefaultPropertiesAttributes(Resource resource, Set<String> requestedIds) {
    setResourceProperty(resource, PROPERTY_FINAL_PROPERTY_ID,
        "false", requestedIds);
  }

  private StackLevelConfigurationRequest getRequest(Map<String, Object> properties) {
    return new StackLevelConfigurationRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(PROPERTY_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
