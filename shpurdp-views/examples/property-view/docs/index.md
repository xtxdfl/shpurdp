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

Property View Example
======

Description
-----
The Property View is an example of a basic REST service that serves the configuration parameter values.
It demonstrates the very basics of how to use the different options available with configuration
parameters such as label, placeholder, default and description.

Package
-----
All views are packaged as a view archive. The view archive contains the configuration
file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Shpurdp.

Build
-----

The view can be built as a maven project.

    cd shpurdp-views/examples/property-view
    mvn clean package

The build will produce the view archive.

    shpurdp-views/examples/property-view/target/property-view-0.1.0.jar

Place the view archive on the Shpurdp Server and restart to deploy.    

    cp property-view-0.1.0.jar /var/lib/shpurdp-server/resources/views/
    shpurdp-server restart
    
Create View Instance
-----

With the view deployed, from the Shpurdp Administration interface,
create an instance of the view (called PROPERTY_1) to be used by Shpurdp users.

Access the view service end point:

    /api/v1/views/PROPERTY/versions/0.1.0/instances/PROPERTY_1/resources/properties

Access the view UI:

    /views/PROPERTY/0.1.0/PROPERTY_1
