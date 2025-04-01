<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

## SNMP MIB and Script Extension

This extension is meant to be used with Shpurdp 2.1 or later and uses the capabilities introduced in SHPURDP-9919 to delegate alerts to an external script.

In this case, the external script `snmp_mib_script.sh` is used to invoke the `snmptrap` command from the `net-snmp` Linux packages to issue an SNMP trap to a specific destination.

In order to wire this script up to Shpurdp the following procedures should be followed as the root user, or with a user with equivalent sudo access:

Install SNMP Utils

    yum install net-snmp net-snmp-utils net-snmp-libs -y

Make SNMP Utils aware of the Apache Shpurdp MIB

    cp /var/lib/shpurdp-server/resources/APACHE-SHPURDP-MIB.txt /usr/share/snmp/mibs

Startup a simple SNMP trap daemon to log all traps to the `/tmp/traps.log` file for testing purposes.

    nohup snmptrapd -m ALL -A -n -Lf /tmp/traps.log &

Invoke a test trap to ensure that the snmptrapd is logging appropriately to `/tmp/traps.log` and the Apache Shpurdp MIB is being respected.

    snmptrap -v 2c -c public localhost '' APACHE-SHPURDP-MIB::apacheShpurdpAlert alertDefinitionName s "definitionName" alertDefinitionHash s "definitionHash" alertName s "name" alertText s "text" alertState i 0 alertHost s "host" alertService s "service" alertComponent s "component"

You should see this in /tmp/traps.log.

    2015-09-03 05:14:30 UDP: [127.0.0.1]:45431->[127.0.0.1] [UDP: [127.0.0.1]:45431->[127.0.0.1]]:
    DISMAN-EVENT-MIB::sysUpTimeInstance = Timeticks: (15638958) 1 day, 19:26:29.58	SNMPv2-MIB::snmpTrapOID.0 = OID: APACHE-SHPURDP-MIB::apacheShpurdpAlert	APACHE-SHPURDP-MIB::alertDefinitionName = STRING: "definitionName"	APACHE-SHPURDP-MIB::alertDefinitionHash = STRING: "definitionHash"	APACHE-SHPURDP-MIB::alertName = STRING: "name"	APACHE-SHPURDP-MIB::alertText = STRING: "text"	APACHE-SHPURDP-MIB::alertState = INTEGER: ok(0)	APACHE-SHPURDP-MIB::alertHost = STRING: "host"	APACHE-SHPURDP-MIB::alertService = STRING: "service"	APACHE-SHPURDP-MIB::alertComponent = STRING: "component"

Once that output has been validated, it's time to make Shpurdp aware of the script to begin sending SNMP traps that conform to it.

Create a file that contains the script, named `/tmp/snmp_mib_script.sh`, in this example.  It's recommended to create this file in a more permanent directory for actual use.

Add the following line to the `/etc/shpurdp-server/conf/shpurdp.properties` file

    org.apache.shpurdp.contrib.snmp.script=/tmp/snmp_mib_script.sh

Restart Shpurdp using `shpurdp-server restart`

Now, we need to use the API to add an alert target for this script.  The following content needs to be POST'd to /api/v1/alert_targets.

    {
      "AlertTarget": {
        "name": "SNMP_MIB",
        "description": "SNMP MIB Target",
        "notification_type": "ALERT_SCRIPT",
        "global": true,
        "properties": {
            "shpurdp.dispatch-property.script": "org.apache.shpurdp.contrib.snmp.script"
         }
      }
    }

You'll notice the link between the alert target and the script is this property reference: `org.apache.shpurdp.contrib.snmp.script`.

At this point each alert will send an SNMP trap to the local trap daemon and an entry will be recorded in `/tmp/traps.log`.
