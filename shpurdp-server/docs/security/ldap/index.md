
<!--
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

Shpurdp LDAP Configuration
=========

- [Introduction](#introduction)
- [How it Works](#how-it-works)
- [Setting LDAP Configuration Using the CLI](#setup-using-cli)
  - [Silent Mode](#setup-using-cli-silent)
  - [Interactive Mode](#setup-using-cli-interactive)
- [Managing LDAP Configuration Using the REST API](#setup-using-api)
  - [Getting the LDAP Configuration](#setup-using-api-get)
  - [Setting the LDAP Configuration](#setup-using-api-post)
  - [Updating the LDAP Configuration](#setup-using-api-put)
  - [Deleting the LDAP Configuration](#setup-using-api-delete)
- [Implementation Details](#implementation-details)
  - [Configuration Data](#implementation-details-data)
  - [Stack Advisor](#implementation-details-stack-advisor)
    - [ShpurdpConfiguration class](#implementation-details-stack-advisor-shpurdpconfiguration)
    - [ShpurdpLDAPDetails class](#implementation-details-stack-advisor-shpurdpldapdetails)
    - [Example](#implementation-details-stack-advisor-example)
- [Synching LDAP users with Shpurdp Using the CLI](#sync-using-cli)

<a name="introduction"></a>
## Introduction

Shpurdp has a feature to integrate with custom LDAP providers to 
provide LDAP authentication services. To set this up, the Shpurdp Server CLI may be used by executing
the following on the command line:

```
shpurdp-server setup-ldap
```
   
After answering a few prompts, the CLI utility would update the `/etc/shpurdp-server/conf/shpurdp.properties` 
file and Shpurdp's own database with the appropriate properties so that when Shpurdp was restarted, it would rely on the configured LDAP server for authentication. One caveat is that Shpurdp would need to know about the same set of users who you would like to use in Shpurdp for authentication.
This is typically done by having Shpurdp sync with the configured LDAP server. 

As of Shpurdp 2.7.0, Shpurdp has the ability to collect LDAP configuration details and configure itself,  as well as, any eligible services for LDAP.  This is done using the Shpurdp Server CLI, as before; however, once complete, the collected information is stored in the Shpurdp database and then used to configure selected service. Any altered configurations would cause the relevant services to request to be restarted.  Shpurdp, on the other hand, will load the LDAP configuration automatically and change its behavior without needing to be restarted (there is only one exception : in case you setup a custom trust store for LDAP; in this case you need to restart Shpurdp).

<a name="how-it-works"></a>
## How it works

Shpurdp, by default, is eligible to be configured for LDAP. Any other service that is to be eligible 
for LDAP configuration by Shpurdp needs to declare this by adding an `ldap` declaration in its service 
definition's `metainfo.xml` file.  The declaration is as follows:

```
<metainfo>
  ...
  <services>
    <service>
    ...
      <ldap>
        <supported>true</supported>
        <ldapEnabledTest>
          {
            "equals": [
              "service-properties/ldap.enabled",
              "true"
            ]
          }      
        <ldapEnabledTest>
      </ldap>
    ...
    </service>
  </services>
</metainfo>
```

Inside the `<ldap>` block, the `<supported>` element with the value of `true` tells Shpurdp that this
service is eligible to be configured for LDAP.  The `<ldapEnabledTest>` element contains a `JSON structure` 
that describes a Boolean expression indicating whether the service has been configured for LDAP or 
not. 

For example, the `metainfo.xml` file for Ranger:

```
    <ldap>
      <supported>true</supported>
      <ldapEnabledTest>
        {
          "equals": [
            "ranger-admin-site/ranger.authentication.method",
            "LDAP"
          ]
        }      
      <ldapEnabledTest>
    </ldap>
```

This indicates automated LDAP configuration by Shpurdp is enabled.  It also declares how to test the  service configurations for the LDAP integration status.  If the property value for `ranger.authentication.method` in the `ranger-admin-site` configuration type is `LDAP`, then LDAP has been enabled for Ranger; else LDAP has not yet been enabled.

Once support is declared by a service **and** it is installed, it will be listed as an eligible service
while selecting services for which to enable LDAP via the Shpurdp Server CLI.

Example:

```
# shpurdp-server setup-ldap
...
Manage LDAP configurations for eligible services [y/n] (n)? y
 Use LDAP for all services [y/n] (n)? n
   Use LDAP for RANGER [y/n] (n)? y
...
```

When the Shpurdp CLI is complete, the LDAP configuration data will be stored in the Shpurdp database (and, optionally, some in `shpurdp.properties`) 
and then the relevant service configurations will be updated.  Service configurations are updated using 
the stack advisor. Any LDAP-specific recommendations generated by the stack advisor are silently 
and automatically applied. If changes are detected, the relevant services will request to be restarted.

To indicate LDAP-specific changes via the stack advisor. The stack's stack advisor or the service's 
service advisor needs to override the `ServiceAdvisor.getServiceConfigurationRecommendationsForLDAP` 
function:

```
  def getServiceConfigurationRecommendationsForLDAP(self, configurations, clusterData, services, hosts):
    ...
```

<a name="setup-using-cli"></a>
## Setting LDAP Configuration Using the CLI

To enable or disable LDAP configuration via the Shpurdp Server CLI, use the following command:

```
shpurdp-server setup-ldap
```

This command works in a silent or an interactive mode. 

<a name="setup-using-cli-silent"></a>
### Silent Mode

In silent mode all configuration details may be set on the command line via arguments.  However, some of the arguments are passwords that are

 - needed for authentication to use Shpurdp's REST API
 - needed for the LDAP manager to connect to the LDAP server

These arguments may be left off of the command causing the CLI to prompt for it. 

For example:

```
# shpurdp-server setup-ldap --shpurdp-admin-username=admin ...
Using python  /usr/bin/python
Enter Shpurdp Admin password:
```

The following arguments must be supplied when **enabling** LDAP:

```
  --ldap-url=<primary LDAP URL in host:port format>
                        Primary URL for LDAP (must not be used together with
                        --ldap-primary-host and --ldap-primary-port)
  --ldap-primary-host=<primary LDAP host>
                        Primary Host for LDAP (must not be used together with
                        --ldap-url)
  --ldap-primary-port=<primary LDAP port>
                        Primary Port for LDAP (must not be used together with
                        --ldap-url)
  --ldap-ssl=<true|false>
                        Whether to use SSL for LDAP or not
  --ldap-type=<type>
                        Specifies the LDAP provider type [AD/IPA/Generic] for
                        offering defaults for missing options.
  --ldap-user-class=<user object class attribute>
                        User Object Class attribute name for LDAP
  --ldap-user-attr=<user name attribute>
                        User Name attribute name for LDAP
  --ldap-user-group-member-attr=<user group member attribute>
                        User Group Member Attribute for LDAP
  --ldap-group-class=<group object class>
                        Group Object Class attribute for LDAP
  --ldap-group-attr=<group name attrbiute>
                        Group Name attribute for LDAP
  --ldap-member-attr=<group member attribute>
                        Group Membership attribute name for LDAP
  --ldap-dn=<dn>
                        Distinguished Name attribute for LDAP
  --ldap-base-dn=<base DN>
                        Base DN for LDAP
  --ldap-manager-dn=<manager DN>
                        Manager DN for LDAP
  --ldap-save-settings
                        Saves the given configuration without giving a chance
                        for review
  --ldap-referral=<referral method>
                        Referral method [follow/ignore] for LDAP
  --ldap-bind-anonym=<true|false>
                        Whether to bind anonymously for LDAP
  --ldap-sync-username-collisions-behavior=<convert|skip>
                        Handling behavior for username collisions for LDAP sync
  --ldap-sync-disable-endpoint-identification=<true|false>
                        Determines whether to disable endpoint identification
                        (hostname verification) during SSL handshake for LDAP
                        sync. This option takes effect only if --ldap-ssl is
                        set to 'true'
  --ldap-force-lowercase-usernames=<true|false>
                        Declares whether to force the ldap user name to be
                        lowercase or leave as-is
  --ldap-pagination-enabled=LDAP_PAGINATION_ENABLED
                        Determines whether results from LDAP are paginated
                        when requested
  --ldap-force-setup
                        Forces the use of LDAP even if other (i.e. PAM)
                        authentication method is configured already or if
                        there is no authentication method configured at all
  --ldap-enabled-shpurdp=<true|false>
                        Whether to enable/disable LDAP authentication
                        for Shpurdp, itself
  --ldap-manage-services=<true|false>
                        Whether Shpurdp should manage the LDAP configurations
                        for specified services
  --ldap-enabled-services=<*|service names>
                        A comma separated list of services that are expected to be 
                        configured for LDAP (you are allowed to use '*' to indicate
                        ALL services)
  --shpurdp-admin-username=<username>
                        Shpurdp administrator username for accessing Shpurdp's
                        REST API
```

Optionally, the following arguments may be set:

```
  --shpurdp-admin-password=<password>
                        Shpurdp administrator password for accessing Shpurdp's
                        REST API
  --ldap-manager-password=LDAP_MANAGER_PASSWORD
                        Manager Password For LDAP
```

For more options and up-to-date information, execute the following command:
```
shpurdp-server setup-ldap --help
```

<a name="setup-using-cli-interactive"></a>
### Interactive Mode

In interactive mode some configuration details may be set on the command line via arguments and the CLI will prompt for the rest.

```
# shpurdp-server setup-ldap
Using python  /usr/bin/python

Enter Shpurdp Admin login: admin
Enter Shpurdp Admin password:

Fetching LDAP configuration from DB. No configuration.
Please select the type of LDAP you want to use [AD/IPA/Generic](Generic):
Primary LDAP Host (ldap.shpurdp.apache.org):
Primary LDAP Port (389):
Secondary LDAP Host <Optional>:
Secondary LDAP Port <Optional>:
Use SSL [true/false] (false):
User object class (posixUser):
User ID attribute (uid):
User group member attribute (memberof):
Group object class (posixGroup):
Group name attribute (cn):
Group member attribute (memberUid):
Distinguished name attribute (dn):
Search Base (dc=shpurdp,dc=apache,dc=org):
Referral method [follow/ignore] (follow):
Bind anonymously [true/false] (false):
Bind DN (uid=ldapbind,cn=users,dc=shpurdp,dc=apache,dc=org):
Enter Bind DN Password:
Confirm Bind DN Password:
Handling behavior for username collisions [convert/skip] for LDAP sync (skip):
Force lower-case user names [true/false]:false
Results from LDAP are paginated when requested [true/false]:false
Use LDAP authentication for Shpurdp [y/n] (n)? y
Manage LDAP configurations for eligible services [y/n] (n)? y
Manage LDAP for all services [y/n] (n)? y
====================
Review Settings
====================
Primary LDAP Host (ldap.shpurdp.apache.org):  ldap.shpurdp.apache.org
Primary LDAP Port (389):  389
Use SSL [true/false] (false):  false
User object class (posixUser):  posixUser
User ID attribute (uid):  uid
User group member attribute (memberof):  memberof
Group object class (posixGroup):  posixGroup
Group name attribute (cn):  cn
Group member attribute (memberUid):  memberUid
Distinguished name attribute (dn):  dn
Search Base (dc=shpurdp,dc=apache,dc=org):  dc=shpurdp,dc=apache,dc=org
Referral method [follow/ignore] (follow):  follow
Bind anonymously [true/false] (false):  false
Handling behavior for username collisions [convert/skip] for LDAP sync (skip):  skip
Force lower-case user names [true/false]: false
Results from LDAP are paginated when requested [true/false]: false
shpurdp.ldap.connectivity.bind_dn: uid=ldapbind,cn=users,dc=shpurdp,dc=apache,dc=org
shpurdp.ldap.connectivity.bind_password: *****
shpurdp.ldap.manage_services: true
shpurdp.ldap.enabled_services: *
Save settings [y/n] (y)? y
Saving LDAP properties...
Saving LDAP properties finished
Shpurdp Server 'setup-ldap' completed successfully.
```

In either case, the CLI collects the data and submits it to Shpurdp via the REST API.  This then 
triggers processes in Shpurdp to enable LDAP as needed.
  
<a name="setup-using-api"></a>
## Managing LDAP Configuration Using the REST API

The LDAP configuration may be managed using Shpurdp's REST API, via the following entry point:
 
```
/api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations
```

This entry point supports the following request types:
- GET - retrieve the LDAP configuration data
- POST - explicitly set the LDAP configuration data, replacing all properties
- PUT - update the LDAP configuration data, only the specified properties are updated 
- DELETE - removes the LDAP configuration data

<a name="setup-using-api-get"></a>
### Getting the LDAP Configuration
To retrieve the LDAP configuration data:
```
GET /api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations/ldap-configuration
```
Example 404 response:
```
{
  "status" : 404,
  "message" : "The requested resource doesn't exist: RootServiceComponentConfiguration not found where Configuration/service_name=SHPURDP AND Configuration/component_name=SHPURDP_SERVER AND Configuration/category=ldap-configuration."
}
```

Example 200 response:
```
{
  "href" : "http://shpurdp_server.host:8080/api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations/ldap-configuration",
  "Configuration" : {
    "category" : "ldap-configuration",
    "component_name" : "SHPURDP_SERVER",
    "service_name" : "SHPURDP",
    "properties" : {
      "shpurdp.ldap.advanced.collision_behavior" : "skip",
      "shpurdp.ldap.advanced.force_lowercase_usernames" : "false",
      "shpurdp.ldap.advanced.pagination_enabled" : "false",
      "shpurdp.ldap.advanced.referrals" : "follow",
      "shpurdp.ldap.attributes.dn_attr" : "dn",
      "shpurdp.ldap.attributes.group.member_attr" : "memberUid",
      "shpurdp.ldap.attributes.group.name_attr" : "cn",
      "shpurdp.ldap.attributes.group.object_class" : "posixGroup",
      "shpurdp.ldap.attributes.user.group_member_attr" : "memberof",
      "shpurdp.ldap.attributes.user.name_attr" : "uid",
      "shpurdp.ldap.attributes.user.object_class" : "posixUser",
      "shpurdp.ldap.attributes.user.search_base" : "dc=shpurdp,dc=apache,dc=org",
      "shpurdp.ldap.authentication.enabled" : "true",
      "shpurdp.ldap.connectivity.anonymous_bind" : "false",
      "shpurdp.ldap.connectivity.bind_dn" : "uid=ldapbind,cn=users,dc=shpurdp,dc=apache,dc=org",
      "shpurdp.ldap.connectivity.bind_password" : "/etc/shpurdp-server/conf/ldap-password.dat",
      "shpurdp.ldap.connectivity.server.host" : "ldap.shpurdp.apache.org",
      "shpurdp.ldap.connectivity.server.port" : "389",
      "shpurdp.ldap.connectivity.use_ssl" : "false",
      "shpurdp.ldap.enabled_services" : "*",
      "shpurdp.ldap.manage_services" : "true"
    },
    "property_types" : {
      "shpurdp.ldap.advanced.collision_behavior" : "PLAINTEXT",
      "shpurdp.ldap.advanced.force_lowercase_usernames" : "PLAINTEXT",
      "shpurdp.ldap.advanced.pagination_enabled" : "PLAINTEXT",
      "shpurdp.ldap.advanced.referrals" : "PLAINTEXT",
      "shpurdp.ldap.attributes.dn_attr" : "PLAINTEXT",
      "shpurdp.ldap.attributes.group.member_attr" : "PLAINTEXT",
      "shpurdp.ldap.attributes.group.name_attr" : "PLAINTEXT",
      "shpurdp.ldap.attributes.group.object_class" : "PLAINTEXT",
      "shpurdp.ldap.attributes.user.group_member_attr" : "PLAINTEXT",
      "shpurdp.ldap.attributes.user.name_attr" : "PLAINTEXT",
      "shpurdp.ldap.attributes.user.object_class" : "PLAINTEXT",
      "shpurdp.ldap.attributes.user.search_base" : "PLAINTEXT",
      "shpurdp.ldap.authentication.enabled" : "PLAINTEXT",
      "shpurdp.ldap.connectivity.anonymous_bind" : "PLAINTEXT",
      "shpurdp.ldap.connectivity.bind_dn" : "PLAINTEXT",
      "shpurdp.ldap.connectivity.bind_password" : "PASSWORD",
      "shpurdp.ldap.connectivity.server.host" : "PLAINTEXT",
      "shpurdp.ldap.connectivity.server.port" : "PLAINTEXT",
      "shpurdp.ldap.connectivity.use_ssl" : "PLAINTEXT",
      "shpurdp.ldap.enabled_services" : "PLAINTEXT",
      "shpurdp.ldap.manage_services" : "PLAINTEXT"
    }
  }
}
```

<a name="setup-using-api-post"></a>
### Setting the LDAP Configuration
To set the LDAP configuration data, replacing any previously existing data:
```
POST /api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations
```
Example payload:
```
{
    "Configuration": {
        "category": "ldap-configuration",
        "properties": {
            "shpurdp.ldap.connectivity.server.port": "389",
            "shpurdp.ldap.advanced.pagination_enabled": "false",
            "shpurdp.ldap.attributes.user.search_base": "dc=shpurdp,dc=apache,dc=org",
            "shpurdp.ldap.attributes.user.object_class": "posixUser",
            "shpurdp.ldap.attributes.user.group_member_attr": "memberof",
            "shpurdp.ldap.attributes.group.member_attr": "memberUid",
            "shpurdp.ldap.enabled_services": "*",
            "shpurdp.ldap.authentication.enabled": "true",
            "shpurdp.ldap.attributes.user.name_attr": "uid",
            "shpurdp.ldap.advanced.collision_behavior": "skip",
            "shpurdp.ldap.advanced.force_lowercase_usernames": "false",
            "shpurdp.ldap.connectivity.bind_password": "/etc/shpurdp-server/conf/ldap-password.dat",
            "shpurdp.ldap.attributes.group.object_class": "posixGroup",
            "shpurdp.ldap.manage_services": "true",
            "shpurdp.ldap.advanced.referrals": "follow",
            "shpurdp.ldap.attributes.dn_attr": "dn",
            "shpurdp.ldap.connectivity.anonymous_bind": "false",
            "shpurdp.ldap.connectivity.use_ssl": "false",
            "shpurdp.ldap.connectivity.server.host": "ldap.shpurdp.apache.org",
            "shpurdp.ldap.attributes.group.name_attr": "cn",
            "shpurdp.ldap.connectivity.bind_dn": "uid=ldapbind,cn=users,dc=shpurdp,dc=apache,dc=org"
        }
    }
}
```

<a name="setup-using-api-put"></a>
### Updating the LDAP Configuration
To update the LDAP configuration data, only replacing or adding specific properties:
```
PUT /api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations/ldap-configuration
```
Example payload:
```
{
  "Configuration": {    
    "properties": {
      "shpurdp.ldap.manage_services" : "true",
      "shpurdp.ldap.enabled_services": "SHPURDP, RANGER"
    }
  }
}
```

<a name="setup-using-api-delete"></a>
### Deleting the LDAP Configuration
To delete the LDAP configuration data, removing all properties:
```
DELETE /api/v1/services/SHPURDP/components/SHPURDP_SERVER/configurations/ldap-configuration
```

<a name="implementation-details"></a>
## Implementation Details

<a name="implementation-details-data"></a>
### Configuration Data

The LDAP configuration data is stored in the `shpurdp_configuration` table of the Shpurdp database. This table contains a set of property names and their values, grouped by a _category_ name.  For the LDAP configuration data, the properties are stored using the category name `ldap-configuration`. 

The only properties that can be inserted in this table are listed in `org.apache.shpurdp.server.configuration.ShpurdpServerConfigurationKey` with `category` of `ShpurdpServerConfigurationCategory.LDAP_CONFIGURATION`. In addition to this enumeration a helper class has been created to provide a common Java interface for  LDAP related properties; this class is called `org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration`

Within `Guice`  a custom `Provider<ShpurdpLdapConfiguration>` implementation should be used to get an instance of LDAP related data; this class is called `org.apache.shpurdp.server.ldap.service.ShpurdpLdapConfigurationProvider`. It's very important that this provider cannot be used until the `JPA` context is not initialized (since the underlying implementation fetches the configuration from the database).
 
 
<a name="implementation-details-stack-advisor"></a>
### Stack Advisor

After updating the LDAP configuration data via the CLI (which invokes the REST API) or the REST API, Shpurdp triggers a process to reconfigure services as needed.  This happens in the background where as the only indication that something has changed is that services may request to be restarted.

The process that is triggered in the background invokes the Stack Advisor, requesting LDAP-specific recommendations.  It is expected that a service supporting this feature overrides the 
`getServiceConfigurationRecommendationsForLDAP` function and adheres to the convention that only the LDAP-relevant configurations are altered.  
 
 ```
def getServiceConfigurationRecommendationsForLDAP(self, configurations, clusterData, services, hosts):
  ...
 
 ```    
 
To help determine how this function should behave, the following classes are available:
 
 - `ShpurdpConfiguration`
 - `ShpurdpLDAPDetails`
 
<a name="implementation-details-stack-advisor-shpurdpconfiguration"></a>
#### ShpurdpConfiguration class

`ShpurdpConfiguration` (located in `shpurdp-server/src/main/resources/stacks/shpurdp_configuration.py`) is an entry point into configuration-specific information about Shpurdp.  It 
uses data from the `shpurdp-server-configuration` section of the `services.json` file to build 
_category_-specific utility classes. An instance of this object is created using the base StackAdvisor
class's  `get_shpurdp_configuration` function. For example:

```
shpurdp_configuration = self.get_shpurdp_configuration(services)
```

<a name="implementation-details-stack-advisor-shpurdpldapdetails"></a>
#### ShpurdpLDAPDetails class

The `ShpurdpLDAPDetails` class contains utility functions to get and interpret data from the 
`ldap-configiration` data parsed from the `shpurdp-server-configuration` section of the `services.json` file.  To create an instance of this class, the `ShpurdpLDAPDetails.get_shpurdp_ldap_details` function is used. For example:
```
shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details() if shpurdp_configuration else None
```

The public API of this class mirrors above mentioned Java class's (`org.apache.shpurdp.server.ldap.domain.ShpurdpLdapConfiguration`) public API; except for trust store related API and getLdapServerProperties which we do not need in Pyton side.

 <a name="implementation-details-stack-advisor-example"></a>
 #### Stack Advisor Example
 
```
   def recommendConfigurationsForLDAP(self, configurations, clusterData, services, hosts):
1    shpurdp_configuration = self.get_shpurdp_configuration(services)
2    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details() if shpurdp_configuration else None

3    if shpurdp_ldap_details and shpurdp_ldap_details.is_managing_services():
       putProperty = self.putProperty(configurations, "service-site", services)

4      if shpurdp_ldap_details.should_enable_ldap('MY_SERVICE'):
        if shpurdp_ldap_details.get_user_search_base() is not None:
          putProperty('my.service.ldap.searchBase', shpurdp_ldap_details.get_user_search_base())
```

Explanations:
1. Obtain an instance of the `ShpurdpConfiguration` object using data from the `services` dictionary.
2. Retrieve the `ShpurdpLDAPDetails` object by calling the `ShpurdpConfiguration`'s `get_shpurdp_ldap_details` 
function. 
3. Test to see if Shpurdp should be managing the LDAP configuration for installed services.  If so, 
continue; else, there is nothing left to do but exit the function. 
4. Test to see if Shpurdp should enable LDAP for the service with the name of `MY_SERVICE`. If so, set the relevant properties using values from the `ShpurdpLDAPDetails` object.

**Note:**  `should_enable_ldap` should be called to determine what action
to take. This is because even though Shpurdp is managing the LDAP configuration for installed services, it may not be managing the LDAP-configuration for the particular service. So one of the following actions are to be performed: enable or ignore.

<a name="sync-using-cli"></a>
## Synching LDAP users with Shpurdp Using the CLI

After setting up your LDAP integration, you must synchronize LDAP users and groups with Shpurdp, using the `shpurdp-server sync-ldap [option]` utility. Please read [this guide](https://docs.hortonworks.com/HDPDocuments/HDP3/HDP-3.0.1/shpurdp-authentication-ldap-ad/content/authe_ldapad_synchronizing_ldap_users_and_groups.html) carefully for further information.
