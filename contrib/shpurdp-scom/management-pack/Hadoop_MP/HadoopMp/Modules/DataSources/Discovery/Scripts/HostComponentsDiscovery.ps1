﻿## Licensed to the Apache Software Foundation (ASF) under one
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

Param ($TemplateName, $ClusterName, $HostName, $HostIpAddress, $HostShpurdpUri, $Username, $PAssword)

function Main() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, '$MPElement$', '$Target/Id$')

    $componentsResult = InvokeRestAPI (JoinUri "$HostShpurdpUri" 'host_components') $Username $Password

    $baseMonitoringUri = $HostShpurdpUri -replace '(?i)/hosts/.*', ''
    $parentServices = @{}

    foreach ($component in $componentsResult.items) {
        $componentClassId = GetComponentClassId $component.HostRoles.component_name
        if ($componentClassId -eq $null) { continue }

        $componentName = $component.HostRoles.component_name

        $componentEntity = $discoveryData.CreateClassInstance($componentClassId)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.Host.Private"]/TemplateName$', $TemplateName)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.Host.Private"]/ClusterName$', $ClusterName)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.Host.Private"]/HostName$', $HostName)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.Host"]/IpAddress$', $HostIpAddress)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ShpurdpManagedEntity"]/ShpurdpUri$', $component.href)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.HostComponent"]/ClusterName$', $ClusterName)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.HostComponent"]/ComponentName$', $componentName)
        $componentEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.HostComponent"]/ParentHostName$', $HostName)
        $componentEntity.AddProperty('$MPElement[Name="System!System.Entity"]/DisplayName$', (FormatHostComponentName $componentName))
        $discoveryData.AddInstance($componentEntity)

        $parentServiceName = GetParentServiceName $componentName

        if (!$parentServiceName) { continue }

        if (!$parentServices.ContainsKey($parentServiceName)) {
            $parentServices[$parentServiceName] = CreateParentService $discoveryData $parentServiceName
        }

        $parentServiceRelationship = $discoveryData.CreateRelationshipInstance((GetParentServiceRelationshipId $componentName))
        $parentServiceRelationship.Source = $parentServices[$parentServiceName]
        $parentServiceRelationship.Target = $componentEntity
        $discoveryData.AddInstance($parentServiceRelationship)
    }

    $discoveryData
}

function GetComponentClassId($componentName) {
    switch ($componentName) {
        'namenode' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.NameNode"]$' }
        'secondary_namenode' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.SecondaryNameNode"]$' }
        'jobtracker' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.JobTracker"]$' }
        'tasktracker' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.TaskTracker"]$' }
        'datanode' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.DataNode"]$' }
        'hive_server' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.HiveServer"]$' }
        'hive_metastore' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.HiveMetastore"]$' }
        'hive_client' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.HiveClient"]$' }
        { 'templeton', 'webhcat_server' -contains $_ } { '$MPElement[Name="Shpurdp.SCOM.HostComponent.TempletonServer"]$' }
        'oozie_server' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.OozieServer"]$' }
        'pig' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.Pig"]$' }
        'sqoop' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.Sqoop"]$' }
        'historyserver' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.HistoryServer"]$' }
        'mapreduce2_client' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.MapReduce2Client"]$' }
        'nodemanager' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.NodeManager"]$' }
        'resourcemanager' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.ResourceManager"]$' }
        'yarn_client' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.YarnClient"]$' }
        'zookeeper_client' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.ZooKeeperClient"]$' }
        'zookeeper_server' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.ZooKeeperServer"]$' }
        'zkfc' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.ZKFC"]$' }
        'journalnode' { '$MPElement[Name="Shpurdp.SCOM.HostComponent.JournalNode"]$' }
        default { $null }
    }
}

function GetParentServiceName($componentName) {
    switch ($componentName) {
        { 'namenode', 'secondary_namenode', 'datanode', 'zkfc', 'journalnode'  -contains $_ } { 'HDFS' }
        { 'jobtracker', 'tasktracker' -contains $_ } { 'MAPREDUCE' }
        { 'hive_server', 'hive_metastore','webhcat_server','hive_client' -contains $_ } { 'HIVE' }
        'templeton' { 'TEMPLETON' }
        'oozie_server' { 'OOZIE' }
        'pig' { 'PIG' }
        'sqoop' { 'SQOOP' }
        { 'historyserver', 'mapreduce2_client' -contains $_ } { 'MAPREDUCE2' }
        { 'nodemanager', 'resourcemanager', 'yarn_client' -contains $_ } { 'YARN' }
        { 'zookeeper_server', 'zookeeper_client' -contains $_ } { 'ZOOKEEPER' }
        default { $null }
    }
}

function CreateParentService($discoveryData, $serviceName) {
    $serviceClassId = switch ($serviceName) {
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
    }

    $serviceDisplayName = FormatClusterServiceName $serviceName

    $privateEntity = $discoveryData.CreateClassInstance('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]$')
    $privateEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/TemplateName$', $TemplateName)
    $privateEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ClusterName$', $ClusterName)
    $privateEntity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ServiceName$', $serviceName)
    $privateEntity.AddProperty('$MPElement[Name="System!System.Entity"]/DisplayName$', (GetPrivateEntityDisplayName $serviceDisplayName))
    $discoveryData.AddInstance($privateEntity)

    $entity = $discoveryData.CreateClassInstance("$serviceClassId")
    $entity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/TemplateName$', $TemplateName)
    $entity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ClusterName$', $ClusterName)
    $entity.AddProperty('$MPElement[Name="Shpurdp.SCOM.ClusterService.Private"]/ServiceName$', $serviceName)
    $entity.AddProperty('$MPElement[Name="System!System.Entity"]/DisplayName$', $serviceDisplayName)
    $discoveryData.AddInstance($entity)

    $entity
}

function GetParentServiceRelationshipId($componentName) {
    switch ($componentName) {
        'namenode' { '$MPElement[Name="Shpurdp.SCOM.HdfsServiceContainsNameNodeComponent"]$' }
        'secondary_namenode' { '$MPElement[Name="Shpurdp.SCOM.HdfsServiceContainsSecondaryNameNodeComponent"]$' }
        'jobtracker' { '$MPElement[Name="Shpurdp.SCOM.MapReduceServiceContainsJobTrackerComponent"]$' }
        'hive_server' { '$MPElement[Name="Shpurdp.SCOM.HiveServiceContainsHiveServerComponent"]$' }
        'hive_metastore' { '$MPElement[Name="Shpurdp.SCOM.HiveServiceContainsHiveMetastoreComponent"]$' }
        {'templeton', 'webhcat_server' -contains $_ } { '$MPElement[Name="Shpurdp.SCOM.TempletonServiceContainsTempletonServerComponent"]$' }
        'oozie_server' { '$MPElement[Name="Shpurdp.SCOM.OozieServiceContainsOozieServerComponent"]$' }
		'historyserver' { '$MPElement[Name="Shpurdp.SCOM.MapReduce2ServiceContainsHistoryServerComponent"]$' }
		'nodemanager' { '$MPElement[Name="Shpurdp.SCOM.YarnServiceContainsNodeManagerComponent"]$' }
		'resourcemanager' { '$MPElement[Name="Shpurdp.SCOM.YarnServiceContainsResourceManagerComponent"]$' }
        default { '$MPElement[Name="Shpurdp.SCOM.ClusterServiceContainsHostComponent"]$' }
    }
}
