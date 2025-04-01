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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.shpurdp.server.configuration.Configuration;
import org.apache.shpurdp.server.orm.entities.RemoteShpurdpClusterEntity;
import org.apache.shpurdp.view.ShpurdpHttpException;
import org.apache.shpurdp.view.ShpurdpStreamProvider;
import org.apache.shpurdp.view.cluster.Cluster;
import org.apache.commons.io.IOUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * View associated  Remote cluster implementation.
 */
public class RemoteShpurdpCluster implements Cluster {

  public static final String SHPURDP_OR_CLUSTER_ADMIN = "/api/v1/users/%s?privileges/PrivilegeInfo/permission_name=SHPURDP.ADMINISTRATOR|" +
    "(privileges/PrivilegeInfo/permission_name=CLUSTER.ADMINISTRATOR&privileges/PrivilegeInfo/cluster_name=%s)";

  /**
   * Name of the remote Shpurdp Cluster
   */
  private String name;

  /**
   * StreamProvider for the remote cluster
   * Base path will be http://host:port
   */
  private ShpurdpStreamProvider streamProvider;

  /**
   * Path for the cluster.
   * Value will be like : /api/v1/clusters/clusterName
   */
  private String clusterPath;

  /**
   * User for the cluster
   */
  private String username;

  private final LoadingCache<String, JsonElement> configurationCache = CacheBuilder.newBuilder()
    .expireAfterWrite(10, TimeUnit.SECONDS)
    .build(new CacheLoader<String, JsonElement>() {
      @Override
      public JsonElement load(String url) throws Exception {
        return readFromUrlJSON(url);
      }
    });


  /**
   * Constructor for Remote Shpurdp Cluster
   *
   * @param remoteShpurdpClusterEntity
   */
  public RemoteShpurdpCluster(RemoteShpurdpClusterEntity remoteShpurdpClusterEntity, Configuration config) throws MalformedURLException {

    this.name = getClusterName(remoteShpurdpClusterEntity);
    this.username = remoteShpurdpClusterEntity.getUsername();

    URL url = new URL(remoteShpurdpClusterEntity.getUrl());

    String portString = url.getPort() == -1 ? "" : ":" + url.getPort();
    String baseUrl = url.getProtocol() + "://" + url.getHost() + portString;

    this.clusterPath = url.getPath();

    this.streamProvider = new RemoteShpurdpStreamProvider(
      baseUrl, remoteShpurdpClusterEntity.getUsername(),
      remoteShpurdpClusterEntity.getPassword(), config.getRequestConnectTimeout(), config.getRequestReadTimeout());
  }

  private String getClusterName(RemoteShpurdpClusterEntity remoteShpurdpClusterEntity) {
    String[] urlSplit = remoteShpurdpClusterEntity.getUrl().split("/");

    // remoteShpurdpClusterEntity.getName() is not the actual name of Remote Cluster
    // We need to extract the name from cluster url which is like. http://host:port/api/vi/clusters/${clusterName}
    return urlSplit[urlSplit.length - 1];
  }

  /**
   * Constructor for Remote Shpurdp Cluster
   *
   * @param name
   * @param streamProvider
   */
  public RemoteShpurdpCluster(String name, String clusterPath, ShpurdpStreamProvider streamProvider) {
    this.name = name;
    this.clusterPath = clusterPath;
    this.streamProvider = streamProvider;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getConfigurationValue(String type, String key) {
    JsonElement config = null;
    try {
      String desiredTag = getDesiredConfig(type);
      if (desiredTag != null) {
        config = configurationCache.get(String.format("%s/configurations?(type=%s&tag=%s)",this.clusterPath, type, desiredTag));
      }
    } catch (ExecutionException e) {
      throw new RemoteShpurdpConfigurationReadException("Can't retrieve configuration from Remote Shpurdp", e);
    }

    if (config == null || !config.isJsonObject()) return null;
    JsonElement items = config.getAsJsonObject().get("items");

    if (items == null || !items.isJsonArray()) return null;
    JsonElement item = items.getAsJsonArray().get(0);

    if (item == null || !item.isJsonObject()) return null;
    JsonElement properties = item.getAsJsonObject().get("properties");

    if (properties == null || !properties.isJsonObject()) return null;
    JsonElement property = properties.getAsJsonObject().get(key);

    if (property == null || !property.isJsonPrimitive()) return null;

    return property.getAsJsonPrimitive().getAsString();
  }

  @Override
  public Map<String, String> getConfigByType(String type) {
    JsonElement config = null;
    try {
      String desiredTag = getDesiredConfig(type);
      if (desiredTag != null) {
        config = configurationCache.get(String.format("%s/configurations?(type=%s&tag=%s)",this.clusterPath, type, desiredTag));
      }
    } catch (ExecutionException e) {
      throw new RemoteShpurdpConfigurationReadException("Can't retrieve configuration from Remote Shpurdp", e);
    }
    if (config == null || !config.isJsonObject()) return null;
    JsonElement items = config.getAsJsonObject().get("items");

    if (items == null || !items.isJsonArray()) return null;
    JsonElement item = items.getAsJsonArray().get(0);

    if (item == null || !item.isJsonObject()) return null;
    JsonElement properties = item.getAsJsonObject().get("properties");

    if (properties == null || !properties.isJsonObject()) return null;

    Map<String, String> retMap = new Gson().fromJson(properties, new TypeToken<HashMap<String, String>>() {}.getType());
    return retMap;
  }

  @Override
  public List<String> getHostsForServiceComponent(String serviceName, String componentName) {
    String url = String.format("%s/services/%s/components/%s?" +
      "fields=host_components/HostRoles/host_name", this.clusterPath, serviceName, componentName);

    List<String> hosts = new ArrayList<>();

    try {
      JsonElement response = configurationCache.get(url);

      if (response == null || !response.isJsonObject()) return hosts;

      JsonElement hostComponents = response.getAsJsonObject().get("host_components");

      if (hostComponents == null || !hostComponents.isJsonArray()) return hosts;

      for (JsonElement element : hostComponents.getAsJsonArray()) {
        JsonElement hostRoles = element.getAsJsonObject().get("HostRoles");
        String hostName = hostRoles.getAsJsonObject().get("host_name").getAsString();
        hosts.add(hostName);
      }

    } catch (ExecutionException e) {
      throw new RemoteShpurdpConfigurationReadException("Can't retrieve host information from Remote Shpurdp", e);
    }

    return hosts;
  }

  /**
   * Get list of services installed on the remote cluster
   *
   * @return list of services Available on cluster
   */
  public Set<String> getServices() throws IOException, ShpurdpHttpException {
    Set<String> services = new HashSet<>();
    String path = this.clusterPath + "?fields=services/ServiceInfo/service_name";
    JsonElement config = configurationCache.getUnchecked(path);

    if (config != null && config.isJsonObject()) {
      JsonElement items = config.getAsJsonObject().get("services");
      if (items != null && items.isJsonArray()) {
        for (JsonElement item : items.getAsJsonArray()) {
          JsonElement serviceInfo = item.getAsJsonObject().get("ServiceInfo");
          if (serviceInfo != null && serviceInfo.isJsonObject()) {
            String serviceName = serviceInfo.getAsJsonObject().get("service_name").getAsString();
            services.add(serviceName);
          }
        }
      }
    }

    return services;
  }

  public boolean isShpurdpOrClusterAdmin() throws ShpurdpHttpException {

    if (username == null) return false;

    String url = String.format(SHPURDP_OR_CLUSTER_ADMIN, username, name);
    JsonElement response = configurationCache.getUnchecked(url);

    if (response != null && response.isJsonObject()) {
      JsonElement privileges = response.getAsJsonObject().get("privileges");
      if (privileges != null && privileges.isJsonArray()) {
        if (privileges.getAsJsonArray().size() > 0) return true;
      }
    }
    return false;
  }

  /**
   * Get the current tag for the config type
   *
   * @param type
   * @return
   * @throws ExecutionException
   */
  private String getDesiredConfig(String type) throws ExecutionException {
    JsonElement desiredConfigResponse = configurationCache.get(this.clusterPath +"?fields=services/ServiceInfo,hosts,Clusters");

    if (desiredConfigResponse == null || !desiredConfigResponse.isJsonObject()) return null;
    JsonElement clusters = desiredConfigResponse.getAsJsonObject().get("Clusters");

    if (clusters == null || !clusters.isJsonObject()) return null;
    JsonElement desiredConfig = clusters.getAsJsonObject().get("desired_configs");

    if (desiredConfig == null || !desiredConfig.isJsonObject()) return null;
    JsonElement desiredConfigForType = desiredConfig.getAsJsonObject().get(type);

    if (desiredConfigForType == null || !desiredConfigForType.isJsonObject()) return null;
    JsonElement typeJson = desiredConfigForType.getAsJsonObject().get("tag");

    if (typeJson == null || !(typeJson.isJsonPrimitive())) return null;

    return typeJson.getAsJsonPrimitive().getAsString();
  }

  /**
   * Read the content of the url from remote cluster
   *
   * @param url
   * @return
   * @throws IOException
   * @throws ShpurdpHttpException
   */
  private JsonElement readFromUrlJSON(String url) throws IOException, ShpurdpHttpException {
    InputStream inputStream = streamProvider.readFrom(url, "GET", (String) null, null);
    String response = IOUtils.toString(inputStream);
    return new JsonParser().parse(response);
  }

}
