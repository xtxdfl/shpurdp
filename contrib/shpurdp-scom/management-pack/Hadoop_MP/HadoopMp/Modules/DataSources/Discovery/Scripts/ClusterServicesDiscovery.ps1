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

Param ($TemplateName, $ClusterName, $ClusterShpurdpUri, $WatcherNodesList = "", $Username, $Password)

function DiscoverServices($discoveryData, $healthServices) {
    $parent = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ClusterSoftwareProjection"]$")
    $parent.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $parent.AddProperty("$MPElement[Name="Shpurdp.SCOM.Cluster.Private"]/ClusterName$", $ClusterName)

    $clusterServices = InvokeRestAPI (JoinUri "$ClusterShpurdpUri" "services") $Username $Password

    foreach ($clusterService in $clusterServices.items) {
        $serviceClassId = GetServiceClassId $clusterService.ServiceInfo.service_name
        if ($serviceClassId -eq $null) { continue }

        $serviceName = FormatClusterServiceName $clusterService.ServiceInfo.service_name

        $servicePrivateEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]$")
        $servicePrivateEntity.AddProperty("$MPElement[Name='Shpurdp.SCOM.ShpurdpManagedEntity']/ShpurdpUri$", $clusterService.href)
        $servicePrivateEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/TemplateName$", $TemplateName)
        $servicePrivateEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ClusterName$", $ClusterName)
        $servicePrivateEntity.AddProperty("$MPElement[Name='Shpurdp.SCOM.ClusterService.Private']/ServiceName$", $clusterService.ServiceInfo.service_name)
        $servicePrivateEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", (GetPrivateEntityDisplayName $serviceName))
        $discoveryData.AddInstance($servicePrivateEntity)

        AddManagementRelationship $discoveryData $healthServices (MergeStrings $ClusterName $clusterService.name) $servicePrivateEntity

        $serviceEntity = $discoveryData.CreateClassInstance($serviceClassId)
        $serviceEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/TemplateName$", $TemplateName)
        $serviceEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ClusterName$", $ClusterName)
        $serviceEntity.AddProperty("$MPElement[Name='Shpurdp.SCOM.ClusterService.Private']/ServiceName$", $clusterService.ServiceInfo.service_name)
        $serviceEntity.AddProperty("$MPElement[Name="Shpurdp.SCOM.ClusterServiceBase"]/ClusterName$", $ClusterName)
        $serviceEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", $serviceName)
        $discoveryData.AddInstance($serviceEntity)

        $parentRelationship = $discoveryData.CreateRelationshipInstance("$MPElement[Name="Shpurdp.SCOM.ClusterSoftwareProjectionContainsClusterService"]$")
        $parentRelationship.Source = $parent
        $parentRelationship.Target = $serviceEntity
        $discoveryData.AddInstance($parentRelationship)
    }
}

function GetServiceClassId($monitoringUri) {
    switch ($monitoringUri -replace ".*/", "") {
        'hdfs' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Hdfs"]$' }
        'mapreduce' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.MapReduce"]$' }
        'hive' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Hive"]$' }
        { 'templeton', 'webhcat' -contains $_ } { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Templeton"]$' }
        'oozie' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Oozie"]$' }
        'pig' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Pig"]$' }
        'sqoop' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Sqoop"]$' }
        'mapreduce2' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.MapReduce2"]$' }
        'yarn' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.Yarn"]$' }
		'zookeeper' { '$MPElement[Name="Shpurdp.SCOM.ClusterService.ZooKeeper"]$' }
        default: { $null }
    }
}

function Main() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, "$MPElement$", "$Target/Id$")

    $healthServices = CreateHealthServicesFromWatcherNodesList $discoveryData $WatcherNodesList
    if ($healthServices.Count -eq 0) { return $discoveryData }

    DiscoverServices $discoveryData $healthServices

    $discoveryData
}
