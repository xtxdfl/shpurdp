<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Capacity Scheduler View
============

Description
-----
This View provides a UI to manage queues for the YARN Capacity Scheduler.

Requirements
-----

- Shpurdp 2.1.0 or later
- YARN

Build
-----

The view can be built as a maven project.

    mvn clean install

The build will produce the view archive.

    target/capacity-scheduler-???-SNAPSHOT.jar

Place the view archive on the Shpurdp Server and restart to deploy.    

    cp capacity-scheduler-???-SNAPSHOT /var/lib/shpurdp-server/resources/views/
    shpurdp-server restart

Deploying the View
-----

Use the [Shpurdp Vagrant](https://cwiki.apache.org/confluence/display/SHPURDP/Quick+Start+Guide) setup to create a cluster:

Deploy the Capacity Scheduler view into Shpurdp.

    cp capacity-scheduler-???-SNAPSHOT /var/lib/shpurdp-server/resources/views/
    shpurdp-server restart

From the Shpurdp Administration interface, create a view instance.

|Property|Value|
|---|---|
| Details: Instance Name | CS_1 |
| Details: Display Name | Queue Manager |
| Details: Description | Browse and manage YARN Capacity Scheduler queues |
| Properties: Shpurdp Cluster URL | http://c6401.shpurdp.apache.org:8080/api/v1/clusters/MyCluster |
| Properties: Operator Username | admin |
| Properties: Operator Password | password |

Login to Shpurdp and browse to the view instance.

    http://c6401.shpurdp.apache.org:8080/#/main/views/CAPACITY-SCHEDULER/???/CS_1

Local Development
-----
If you want to perform UI development without having to build and redeploy the view package,
you can mount the build target output as a symlink to your vagrant instance.
UI changes will be picked-up with a browser refresh.

After building and deploying the View, delete the view work directory on the Shpurdp Server.

    cd /var/lib/shpurdp-server/resources/views/work
    rm -rf CAPACITY-SCHEDULER\{0.3.0\}/

Create a symlink from the vagrant machine running your Shpurdp Server to your local machine.

    ln -s /vagrant/shpurdp/contrib/views/capacity-scheduler/target/classes/ CAPACITY-SCHEDULER\{0.3.0\}
    
Restart Shpurdp Server, login and browse to the view.

    shpurdp-server restart
    http://c6401.shpurdp.apache.org:8080/#/main/views/CAPACITY-SCHEDULER/0.3.0/CS_1
    
If you modify the view UI code on your machine and re-build, the UI will pickup
the changes on browser refresh.