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

package org.apache.shpurdp.server.serveraction.kerberos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shpurdp.server.ShpurdpException;
import org.apache.shpurdp.server.agent.CommandReport;
import org.apache.shpurdp.server.controller.KerberosHelper;
import org.apache.shpurdp.server.controller.RootComponent;
import org.apache.shpurdp.server.controller.RootService;
import org.apache.shpurdp.server.controller.UpdateConfigurationPolicy;
import org.apache.shpurdp.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.shpurdp.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.shpurdp.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.shpurdp.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.shpurdp.server.state.Cluster;
import org.apache.shpurdp.server.state.ConfigHelper;
import org.apache.shpurdp.server.state.ServiceComponentHost;
import org.apache.shpurdp.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.shpurdp.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.shpurdp.server.state.kerberos.KerberosDescriptor;
import org.apache.shpurdp.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.shpurdp.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.shpurdp.server.utils.StageUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public abstract class AbstractPrepareKerberosServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(AbstractPrepareKerberosServerAction.class);

  /**
   * KerberosHelper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory;

  @Inject
  private KerberosConfigDataFileWriterFactory kerberosConfigDataFileWriterFactory;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, boolean includedInFilter, Map<String, Object> requestSharedDataContext) throws ShpurdpException {
    throw new UnsupportedOperationException();
  }

  protected KerberosHelper getKerberosHelper() {
    return kerberosHelper;
  }

  public void processServiceComponentHosts(Cluster cluster, KerberosDescriptor kerberosDescriptor,
                                           List<ServiceComponentHost> schToProcess,
                                           Collection<String> identityFilter, String dataDirectory,
                                           Map<String, Map<String, String>> currentConfigurations,
                                           Map<String, Map<String, String>> kerberosConfigurations,
                                           boolean includeShpurdpIdentity,
                                           Map<String, Set<String>> propertiesToBeIgnored) throws ShpurdpException {
    List<Component> components = new ArrayList<>();
    for (ServiceComponentHost each : schToProcess) {
      components.add(Component.fromServiceComponentHost(each));
    }
    processServiceComponents(cluster, kerberosDescriptor, components, identityFilter, dataDirectory, currentConfigurations, kerberosConfigurations, includeShpurdpIdentity, propertiesToBeIgnored);
  }

  protected void processServiceComponents(Cluster cluster, KerberosDescriptor kerberosDescriptor,
                                          List<Component> schToProcess,
                                          Collection<String> identityFilter, String dataDirectory,
                                          Map<String, Map<String, String>> currentConfigurations,
                                          Map<String, Map<String, String>> kerberosConfigurations,
                                          boolean includeShpurdpIdentity,
                                          Map<String, Set<String>> propertiesToBeIgnored) throws ShpurdpException {

    actionLog.writeStdOut("Processing Kerberos identities and configurations");

    if (!schToProcess.isEmpty()) {
      if (dataDirectory == null) {
        String message = "The data directory has not been set.  Generated data can not be stored.";
        LOG.error(message);
        throw new ShpurdpException(message);
      }

      // Create the file used to store details about principals and keytabs to create
      File identityDataFile = new File(dataDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME);

      KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter;

      // Create the context to use for filtering Kerberos Identities based on the state of the cluster
      Map<String, Object> filterContext = new HashMap<>();
      filterContext.put("configurations", currentConfigurations);
      filterContext.put("services", cluster.getServices().keySet());

      actionLog.writeStdOut(String.format("Writing Kerberos identity data metadata file to %s", identityDataFile.getAbsolutePath()));
      try {
        kerberosIdentityDataFileWriter = kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(identityDataFile);
      } catch (IOException e) {
        String message = String.format("Failed to write index file - %s", identityDataFile.getAbsolutePath());
        LOG.error(message, e);
        actionLog.writeStdOut(message);
        actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
        throw new ShpurdpException(message, e);
      }

      HashMap<String, ResolvedKerberosKeytab> resolvedKeytabs = new HashMap<>();
      String realm = getDefaultRealm(getCommandParameters());

      try {
        Map<String, Set<String>> propertiesToIgnore = null;
        // Iterate over the components installed on the current host to get the service and
        // component-level Kerberos descriptors in order to determine which principals,
        // keytab files, and configurations need to be created or updated.
        for (Component sch : schToProcess) {
          String hostName = sch.getHostName();
          Long hostId = sch.getHostId();
          String serviceName = sch.getServiceName();
          String componentName = sch.getServiceComponentName();

          KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

          if (serviceDescriptor != null) {
            List<KerberosIdentityDescriptor> serviceIdentities = serviceDescriptor.getIdentities(true, filterContext);

            if (!StringUtils.isEmpty(hostName)) {
              // Update the configurations with the relevant hostname
              Map<String, String> generalProperties = currentConfigurations.computeIfAbsent("", k -> new HashMap<>());

              // Add the current hostname under "host" and "hostname"
              generalProperties.put("host", hostName);
              generalProperties.put("hostname", hostName);
            }

            // Add service-level principals (and keytabs)
            kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, serviceIdentities,
                identityFilter, hostName, hostId, serviceName, componentName, kerberosConfigurations, currentConfigurations,
                resolvedKeytabs, realm);
            propertiesToIgnore = gatherPropertiesToIgnore(serviceIdentities, propertiesToIgnore);

            KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(componentName);

            if (componentDescriptor != null) {
              List<KerberosIdentityDescriptor> componentIdentities = componentDescriptor.getIdentities(true, filterContext);

              // Calculate the set of configurations to update and replace any variables
              // using the previously calculated Map of configurations for the host.
              kerberosHelper.mergeConfigurations(kerberosConfigurations,
                  componentDescriptor.getConfigurations(true), currentConfigurations, null);

              // Add component-level principals (and keytabs)
              kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, componentIdentities,
                  identityFilter, hostName, hostId, serviceName, componentName, kerberosConfigurations, currentConfigurations,
                  resolvedKeytabs, realm);
              propertiesToIgnore = gatherPropertiesToIgnore(componentIdentities, propertiesToIgnore);
            }
          }
        }

        // Add shpurdp-server identities only if 'kerberos-env.create_shpurdp_principal = true'
        if (includeShpurdpIdentity && kerberosHelper.createShpurdpIdentities(currentConfigurations.get("kerberos-env"))) {
          List<KerberosIdentityDescriptor> shpurdpIdentities = kerberosHelper.getShpurdpServerIdentities(kerberosDescriptor);

          if (!shpurdpIdentities.isEmpty()) {
            for (KerberosIdentityDescriptor identity : shpurdpIdentities) {
              // If the identity represents the shpurdp-server user, use the component name "SHPURDP_SERVER_SELF"
              // so it can be distinguished between other identities related to the SHPURDP-SERVER
              // component.
              String componentName = KerberosHelper.SHPURDP_SERVER_KERBEROS_IDENTITY_NAME.equals(identity.getName())
                  ? "SHPURDP_SERVER_SELF"
                  : RootComponent.SHPURDP_SERVER.name();

              List<KerberosIdentityDescriptor> componentIdentities = Collections.singletonList(identity);
              kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, componentIdentities,
                  identityFilter, StageUtils.getHostName(), shpurdpServerHostID(), RootService.SHPURDP.name(), componentName, kerberosConfigurations, currentConfigurations,
                  resolvedKeytabs, realm);
              propertiesToIgnore = gatherPropertiesToIgnore(componentIdentities, propertiesToIgnore);
            }
          }
        }

        if ((propertiesToBeIgnored != null) && (propertiesToIgnore != null)) {
          propertiesToBeIgnored.putAll(propertiesToIgnore);
        }

        // create database records for keytab that must be presented on cluster
        List<KerberosKeytabPrincipalEntity> keytabList = kerberosKeytabPrincipalDAO.findAll();
        resolvedKeytabs.values().forEach(keytab -> kerberosHelper.createResolvedKeytab(keytab, keytabList));
      } catch (IOException e) {
        String message = String.format("Failed to write index file - %s", identityDataFile.getAbsolutePath());
        LOG.error(message, e);
        actionLog.writeStdOut(message);
        actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
        throw new ShpurdpException(message, e);
      } finally {
        if (kerberosIdentityDataFileWriter != null) {
          // Make sure the data file is closed
          try {
            kerberosIdentityDataFileWriter.close();
          } catch (IOException e) {
            String message = "Failed to close the index file writer";
            LOG.warn(message, e);
            actionLog.writeStdOut(message);
            actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
          }
        }
      }
    }
  }

  private Map<String, Set<String>> gatherPropertiesToIgnore(List<KerberosIdentityDescriptor> identities,
                                                            Map<String, Set<String>> propertiesToIgnore) {
    Map<String, Map<String, String>> identityConfigurations = kerberosHelper.getIdentityConfigurations(identities);
    if ((identityConfigurations != null) && !identityConfigurations.isEmpty()) {
      if (propertiesToIgnore == null) {
        propertiesToIgnore = new HashMap<>();
      }

      for (Map.Entry<String, Map<String, String>> entry : identityConfigurations.entrySet()) {
        String configType = entry.getKey();
        Map<String, String> properties = entry.getValue();

        if ((properties != null) && !properties.isEmpty()) {
          Set<String> propertyNames = propertiesToIgnore.get(configType);
          if (propertyNames == null) {
            propertyNames = new HashSet<>();
            propertiesToIgnore.put(configType, propertyNames);
          }
          propertyNames.addAll(properties.keySet());
        }
      }
    }

    return propertiesToIgnore;
  }

  /**
   * Processes configuration changes to determine if any work needs to be done.
   * <p/>
   * If work is to be done, a data file containing the details is created so it they changes may be
   * processed in the appropriate stage.
   *
   * @param dataDirectory             the directory in which to write the configuration changes data file
   * @param kerberosConfigurations    the Kerberos-specific configuration map
   * @param propertiesToBeRemoved     a map of properties to be removed from the current configuration,
   *                                  grouped by configuration type.
   * @param kerberosDescriptor        the Kerberos descriptor
   * @param updateConfigurationPolicy the policy used to determine which configurations to update
   * @throws ShpurdpException
   */
  protected void processConfigurationChanges(String dataDirectory,
                                             Map<String, Map<String, String>> kerberosConfigurations,
                                             Map<String, Set<String>> propertiesToBeRemoved,
                                             KerberosDescriptor kerberosDescriptor,
                                             UpdateConfigurationPolicy updateConfigurationPolicy)
      throws ShpurdpException {
    actionLog.writeStdOut("Determining configuration changes");

    // If there are configurations to set, create a (temporary) data file to store the configuration
    // updates and fill it will the relevant configurations.
    if (!kerberosConfigurations.isEmpty()) {
      if (dataDirectory == null) {
        String message = "The data directory has not been set.  Generated data can not be stored.";
        LOG.error(message);
        throw new ShpurdpException(message);
      }

      // Determine what the relevant Kerberos identity-related properties are...
      Map<String, Set<String>> kerberosIdentityProperties = getIdentityProperties(kerberosDescriptor, null);

      // Determine what the existing properties are...
      Map<String, Map<String, String>> existingProperties = configHelper.getEffectiveConfigProperties(getClusterName(), null);

      File configFile = new File(dataDirectory, KerberosConfigDataFileWriter.DATA_FILE_NAME);
      KerberosConfigDataFileWriter kerberosConfDataFileWriter = null;

      actionLog.writeStdOut(String.format("Writing configuration changes metadata file to %s", configFile.getAbsolutePath()));
      try {
        kerberosConfDataFileWriter = kerberosConfigDataFileWriterFactory.createKerberosConfigDataFileWriter(configFile);
        // add properties to be set
        for (Map.Entry<String, Map<String, String>> entry : kerberosConfigurations.entrySet()) {
          String type = entry.getKey();
          Map<String, String> properties = entry.getValue();

          if (properties != null) {
            for (Map.Entry<String, String> configTypeEntry : properties.entrySet()) {

              // Determine if this configuration should be written or not...
              String propertyName = configTypeEntry.getKey();
              if (includeConfiguration(type, propertyName, updateConfigurationPolicy, existingProperties, kerberosIdentityProperties)) {
                kerberosConfDataFileWriter.addRecord(type,
                    propertyName,
                    configTypeEntry.getValue(),
                    KerberosConfigDataFileWriter.OPERATION_TYPE_SET);
              }
            }
          }
        }
        // add properties to be removed
        if (propertiesToBeRemoved != null) {
          for (Map.Entry<String, Set<String>> entry : propertiesToBeRemoved.entrySet()) {
            String type = entry.getKey();
            Set<String> properties = entry.getValue();

            if (properties != null) {
              for (String property : properties) {
                kerberosConfDataFileWriter.addRecord(type,
                    property,
                    "",
                    KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE);
              }
            }
          }
        }
      } catch (IOException e) {
        String message = String.format("Failed to write kerberos configurations file - %s", configFile.getAbsolutePath());
        LOG.error(message, e);
        actionLog.writeStdOut(message);
        actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
        throw new ShpurdpException(message, e);
      } finally {
        if (kerberosConfDataFileWriter != null) {
          try {
            kerberosConfDataFileWriter.close();
          } catch (IOException e) {
            String message = "Failed to close the kerberos configurations file writer";
            LOG.warn(message, e);
            actionLog.writeStdOut(message);
            actionLog.writeStdErr(message + "\n" + e.getLocalizedMessage());
          }
        }
      }
    }
  }

  /**
   * Determine of the configuration should be included in the set of configurations to update.
   *
   * @param configType
   * @param propertyName
   * @param updateConfigurationPolicy
   * @param existingProperties
   * @param kerberosIdentityProperties
   * @return
   */
  private boolean includeConfiguration(String configType, String propertyName,
                                       UpdateConfigurationPolicy updateConfigurationPolicy,
                                       Map<String, Map<String, String>> existingProperties,
                                       Map<String, Set<String>> kerberosIdentityProperties) {

    // Determine if the property represents a Kerberos identity-related property
    boolean isIdentity;
    if (kerberosIdentityProperties == null) {
      isIdentity = false;
    } else {
      Set<String> propertyNames = kerberosIdentityProperties.get(configType);
      isIdentity = !CollectionUtils.isEmpty(propertyNames) && propertyNames.contains(propertyName);
    }

    if (isIdentity) {
      return updateConfigurationPolicy.applyIdentityChanges();
    }

    // Determine if the property is a new property
    boolean isNew;
    if (existingProperties == null) {
      isNew = true;
    } else {
      Map<String, String> propertyNames = existingProperties.get(configType);
      isNew = (propertyNames == null) || !propertyNames.containsKey(propertyName);
    }

    if (isNew) {
      return updateConfigurationPolicy.applyAdditions();
    }

    // All other properties...
    return updateConfigurationPolicy.applyOtherChanges();
  }

  /**
   * Recursively processes a Kerberos descriptor container and it children to find the
   * Kerberos identity-related properties.
   * <p>
   * Kerberos identity-related properties are those that contain the following information:
   * <ul>
   * <li>principal names</li>
   * <li>keytab file paths</li>
   * <li>auth-to-local rules</li>
   * </ul>
   *
   * @param container          the AbstractKerberosDescriptorContainer to process
   * @param identityProperties a map of config-types to sets of property names to append data
   * @return a map of config-types to sets of property names
   */
  private Map<String, Set<String>> getIdentityProperties(AbstractKerberosDescriptorContainer container, Map<String, Set<String>> identityProperties) {
    if (container != null) {
      if (identityProperties == null) {
        identityProperties = new HashMap<>();
      }

      // Process the Kerberos identities - principal and keytab file properties.
      List<KerberosIdentityDescriptor> identityDescriptors;
      try {
        // There is no need to resolve references since we just need to get the set of configurations that can be changed.
        identityDescriptors = container.getIdentities(false, null);
      } catch (ShpurdpException e) {
        LOG.error("An exception occurred getting the Kerberos identity descriptors.  No configurations will be identified.", e);
        identityDescriptors = null;
      }

      if (identityDescriptors != null) {
        Map<String, Map<String, String>> identityConfigurations = kerberosHelper.getIdentityConfigurations(identityDescriptors);

        if (identityConfigurations != null) {
          for (Map.Entry<String, Map<String, String>> entry : identityConfigurations.entrySet()) {
            Map<String, String> properties = entry.getValue();
            if (properties != null) {
              Set<String> configProperties = identityProperties.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
              configProperties.addAll(properties.keySet());
            }
          }
        }
      }

      // Process any auth-to-local rule properties
      Map<String, Set<String>> authToLocalProperties = kerberosHelper.translateConfigurationSpecifications(container.getAuthToLocalProperties());
      if (authToLocalProperties != null) {
        for (Map.Entry<String, Set<String>> entry : authToLocalProperties.entrySet()) {
          String configType = entry.getKey();
          Set<String> propertyNames = entry.getValue();

          if (propertyNames != null) {
            Set<String> configProperties = identityProperties.computeIfAbsent(configType, k -> new HashSet<>());
            configProperties.addAll(propertyNames);
          }
        }
      }

      // Process the children...
      Collection<? extends AbstractKerberosDescriptorContainer> childContainers = container.getChildContainers();
      if (childContainers != null) {
        for (AbstractKerberosDescriptorContainer childContainer : childContainers) {
          getIdentityProperties(childContainer, identityProperties);
        }
      }
    }

    return identityProperties;
  }
}
