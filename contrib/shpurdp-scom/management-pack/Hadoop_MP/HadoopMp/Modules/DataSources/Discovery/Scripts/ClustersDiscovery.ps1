## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.

Param ($TemplateName, $SeedComputerName, $ClusterSeedClassId, $ShpurdpUri, $ClustersQueryUriSegment, $WatcherNodesList = "", $Username, $Password)

function CreateClusterEntity($discoveryData, $cluster, $healthServices) {
    $clusterName = $cluster.Clusters.cluster_name

    $clusterSeedEntity = $discoveryData.CreateClassInstance("$ClusterSeedClassId")
    $clusterSeedEntity.AddProperty("$MPElement[Name='Shpurdp.SCOM.ShpurdpManagedEntity']/ShpurdpUri$", $cluster.href)
    $clusterSeedEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterSeed"]/TemplateName$", $TemplateName)
    $clusterSeedEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterSeed"]/ClusterName$", $clusterName)
    $clusterSeedEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", "Hadoop Cluster Seed ($clusterName)")
    $discoveryData.AddInstance($clusterSeedEntity)

    AddManagementRelationship $discoveryData $healthServices $cluster.name $clusterSeedEntity

    $clusterPrivateEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]$")
    $clusterPrivateEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $clusterPrivateEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/ClusterName$", $clusterName)
    $clusterPrivateEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", (GetPrivateEntityDisplayName $clusterName))
    $discoveryData.AddInstance($clusterPrivateEntity)

    $clusterEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.Cluster"]$")
    $clusterEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $clusterEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/ClusterName$", $clusterName)
    $clusterEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", $clusterName)
    $discoveryData.AddInstance($clusterEntity)

    $nodesGroup = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ClusterHardwareProjection"]$")
    $nodesGroup.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $nodesGroup.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/ClusterName$", $clusterName)
    $nodesGroup.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", "Hosts")
    $discoveryData.AddInstance($nodesGroup)

    $servicesGroup = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ClusterSoftwareProjection"]$")
    $servicesGroup.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $servicesGroup.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/ClusterName$", $clusterName)
    $servicesGroup.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", "Services")
    $discoveryData.AddInstance($servicesGroup)

    $clusterEntity
}

function DiscoverClusters($discoveryData, $healthServices) {
    $parent = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ShpurdpSeed"]$")
    $parent.AddProperty("$MPElement[Name="Windows!Microsoft.Windows.Computer"]/PrincipalName$", $SeedComputerName)
    $parent.AddProperty("$MPElement[Name="Shpurdp.SCOM.ShpurdpSeed"]/TemplateName$", $TemplateName)

    $clustersResult = InvokeRestAPI (JoinUri "$ShpurdpUri" "$ClustersQueryUriSegment") $Username $Password
    foreach ($cluster in $clustersResult.items) {
        $clusterEntity = CreateClusterEntity $discoveryData $cluster $healthServices

        $parentRelationship = $discoveryData.CreateRelationshipInstance("$MPElement[Name="Shpurdp.SCOM.ShpurdpSeedContainsCluster"]$")
        $parentRelationship.Source = $parent
        $parentRelationship.Target = $clusterEntity
        $discoveryData.AddInstance($parentRelationship)
    }
}

function Main() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, "$MPElement$", "$Target/Id$")

    $healthServices = @()
    foreach ($watcherNodeName in $WatcherNodesList.Split(';', [StringSplitOptions]::RemoveEmptyEntries)) {
        $healthServices += CreateHealthService $discoveryData $watcherNodeName

        $watcherNodeEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ShpurdpWatcherNode"]$")
        $watcherNodeEntity.AddProperty("$MPElement[Name="Windows!Microsoft.Windows.Computer"]/PrincipalName$", $watcherNodeName)
        $watcherNodeEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", "Hadoop Watcher Node")
        $discoveryData.AddInstance($watcherNodeEntity)
    }
    if ($healthServices.Count -eq 0) { return $discoveryData }

    DiscoverClusters $discoveryData $healthServices

    $discoveryData
}
