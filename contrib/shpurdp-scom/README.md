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

Shpurdp SCOM Management Pack
============

The *Shpurdp SCOM Management Pack* gives insight into the performance and health of an Apache Hadoop cluster to users of
Microsoft System Center Operations Manager (SCOM). *Shpurdp SCOM* integrates with the Shpurdp REST API which aggregates and exposes cluster information and metrics.

## Documentation

Look for *Shpurdp SCOM* documentation on the [Apache Shpurdp Wiki](https://cwiki.apache.org/confluence/display/SHPURDP/Shpurdp+SCOM+Management+Pack). Please also visit the [Apache Shpurdp Project](http://incubator.apache.org/shpurdp/) page for more information.

## License

*Shpurdp SCOM* is released under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Issue Tracking

Report any issues via the [Shpurdp JIRA](https://issues.apache.org/jira/browse/SHPURDP) using component `Shpurdp-SCOM`.

## Build

####Shpurdp-SCOM and Metrics Sink

######Requirements
* JDK 1.6
* Maven 3.0
    
######Maven modules
* shpurdp-scom-project (Parent POM for all modules)
  * shpurdp-scom (shpurdp MSI and SQL Server provider)
  * metrics-sink (Metrics SQL Server sink)       
  
######Maven build goals
 * Clean : mvn clean
 * Compile : mvn compile
 * Run tests : mvn test 
 * Create JAR : mvn package
 * Install JAR in M2 cache : mvn install     
    
######Tests options
  * -DskipTests to skip tests when running the following Maven goals:
    'package', 'install', 'deploy' or 'verify'
  * -Dtest=\<TESTCLASSNAME>,\<TESTCLASSNAME#METHODNAME>,....
  * -Dtest.exclude=\<TESTCLASSNAME>
  * -Dtest.exclude.pattern=\*\*/\<TESTCLASSNAME1>.java,\*\*/\<TESTCLASSNAME2>.java

####Management Pack

See [Building the Management Pack](management-pack/Hadoop_MP/BUILDING.md) for more information.


