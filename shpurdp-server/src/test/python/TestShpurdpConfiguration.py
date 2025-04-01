#!/usr/bin/env python3
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import os

from unittest import TestCase


class TestShpurdpConfiguration(TestCase):
  def setUp(self):
    import imp

    self.test_directory = os.path.dirname(os.path.abspath(__file__))

    relative_path = "../../main/resources/stacks/shpurdp_configuration.py"
    shpurdp_configuration_path = os.path.abspath(
      os.path.join(self.test_directory, relative_path)
    )
    class_name = "ShpurdpConfiguration"

    with open(shpurdp_configuration_path, "rb") as fp:
      shpurdp_configuration_impl = imp.load_module(
        "shpurdp_configuration",
        fp,
        shpurdp_configuration_path,
        (".py", "rb", imp.PY_SOURCE),
      )

    self.shpurdp_configuration_class = getattr(shpurdp_configuration_impl, class_name)

  def testMissingData(self):
    shpurdp_configuration = self.shpurdp_configuration_class("{}")
    self.assertIsNone(shpurdp_configuration.get_shpurdp_server_configuration())
    self.assertIsNone(shpurdp_configuration.get_shpurdp_sso_configuration())
    self.assertIsNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

  def testMissingSSOConfiguration(self):
    services_json = {"shpurdp-server-configuration": {}}

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNone(shpurdp_configuration.get_shpurdp_sso_configuration())
    self.assertIsNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertIsNone(shpurdp_sso_details.get_jwt_audiences())
    self.assertIsNone(shpurdp_sso_details.get_jwt_cookie_name())
    self.assertIsNone(shpurdp_sso_details.get_sso_provider_url())
    self.assertIsNone(shpurdp_sso_details.get_sso_provider_original_parameter_name())
    self.assertFalse(shpurdp_sso_details.should_enable_sso("SHPURDP"))

  def testShpurdpSSOConfigurationNotManagingServices(self):
    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {"shpurdp.sso.enabled_services": "SHPURDP"}
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertFalse(shpurdp_sso_details.is_managing_services())
    self.assertFalse(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))

    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.manage_services": "false",
          "shpurdp.sso.enabled_services": "SHPURDP, RANGER",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertFalse(shpurdp_sso_details.is_managing_services())
    self.assertFalse(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.manage_services": "false",
          "shpurdp.sso.enabled_services": "*",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertFalse(shpurdp_sso_details.is_managing_services())
    self.assertFalse(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("RANGER"))

  def testShpurdpSSOConfigurationManagingServices(self):
    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.manage_services": "true",
          "shpurdp.sso.enabled_services": "SHPURDP",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertTrue(shpurdp_sso_details.is_managing_services())
    self.assertTrue(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_enable_sso("RANGER"))
    self.assertTrue(shpurdp_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.manage_services": "true",
          "shpurdp.sso.enabled_services": "SHPURDP, RANGER",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertTrue(shpurdp_sso_details.is_managing_services())
    self.assertTrue(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))
    self.assertTrue(shpurdp_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("RANGER"))

    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.manage_services": "true",
          "shpurdp.sso.enabled_services": "*",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertTrue(shpurdp_sso_details.is_managing_services())
    self.assertTrue(shpurdp_sso_details.should_enable_sso("SHPURDP"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("SHPURDP"))
    self.assertTrue(shpurdp_sso_details.should_enable_sso("RANGER"))
    self.assertFalse(shpurdp_sso_details.should_disable_sso("RANGER"))

  def testShpurdpJWTProperties(self):
    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.provider.certificate": "-----BEGIN CERTIFICATE-----\nMIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==\n-----END CERTIFICATE-----",
          "shpurdp.sso.authentication.enabled": "true",
          "shpurdp.sso.provider.url": "https://knox.shpurdp.apache.org",
          "shpurdp.sso.jwt.cookieName": "hadoop-jwt",
          "shpurdp.sso.jwt.audiences": "",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_sso_configuration())

    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()
    self.assertIsNotNone(shpurdp_sso_details)
    self.assertEqual("", shpurdp_sso_details.get_jwt_audiences())
    self.assertEqual("hadoop-jwt", shpurdp_sso_details.get_jwt_cookie_name())
    self.assertEqual(
      "https://knox.shpurdp.apache.org", shpurdp_sso_details.get_sso_provider_url()
    )
    self.assertEqual(
      "MIICVTCCAb6gAwIBAg...2G2Vhj8vTYptEVg==",
      shpurdp_sso_details.get_sso_provider_certificate(),
    )

  def testCertWithHeaderAndFooter(self):
    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.provider.certificate": "-----BEGIN CERTIFICATE-----\n"
          "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
          "................................................................\n"
          "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n"
          "-----END CERTIFICATE-----\n"
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()

    self.assertEqual(
      "-----BEGIN CERTIFICATE-----\n"
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
      "................................................................\n"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n"
      "-----END CERTIFICATE-----",
      shpurdp_sso_details.get_sso_provider_certificate(True, False),
    )

    self.assertEqual(
      "-----BEGIN CERTIFICATE-----"
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD"
      "................................................................"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy"
      "-----END CERTIFICATE-----",
      shpurdp_sso_details.get_sso_provider_certificate(True, True),
    )

    self.assertEqual(
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
      "................................................................\n"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy",
      shpurdp_sso_details.get_sso_provider_certificate(False, False),
    )

    self.assertEqual(
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD"
      "................................................................"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy",
      shpurdp_sso_details.get_sso_provider_certificate(False, True),
    )

  def testCertWithoutHeaderAndFooter(self):
    services_json = {
      "shpurdp-server-configuration": {
        "sso-configuration": {
          "shpurdp.sso.provider.certificate": "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
          "................................................................\n"
          "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    shpurdp_sso_details = shpurdp_configuration.get_shpurdp_sso_details()

    self.assertEqual(
      "-----BEGIN CERTIFICATE-----\n"
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
      "................................................................\n"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n"
      "-----END CERTIFICATE-----",
      shpurdp_sso_details.get_sso_provider_certificate(True, False),
    )

    self.assertEqual(
      "-----BEGIN CERTIFICATE-----"
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD"
      "................................................................"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy"
      "-----END CERTIFICATE-----",
      shpurdp_sso_details.get_sso_provider_certificate(True, True),
    )

    self.assertEqual(
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n"
      "................................................................\n"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy",
      shpurdp_sso_details.get_sso_provider_certificate(False, False),
    )

    self.assertEqual(
      "MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD"
      "................................................................"
      "dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy",
      shpurdp_sso_details.get_sso_provider_certificate(False, True),
    )

  def testMissingLDAPConfiguration(self):
    services_json = {"shpurdp-server-configuration": {}}

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertFalse(shpurdp_ldap_details.is_ldap_enabled())
    self.assertIsNone(shpurdp_ldap_details.get_server_host())
    self.assertIsNone(shpurdp_ldap_details.get_server_port())
    self.assertIsNone(shpurdp_ldap_details.get_server_url())
    self.assertIsNone(shpurdp_ldap_details.get_secondary_server_host())
    self.assertIsNone(shpurdp_ldap_details.get_secondary_server_port())
    self.assertIsNone(shpurdp_ldap_details.get_secondary_server_url())
    self.assertFalse(shpurdp_ldap_details.is_use_ssl())
    self.assertFalse(shpurdp_ldap_details.is_anonymous_bind())
    self.assertIsNone(shpurdp_ldap_details.get_bind_dn())
    self.assertIsNone(shpurdp_ldap_details.get_bind_password())
    self.assertIsNone(shpurdp_ldap_details.get_dn_attribute())
    self.assertIsNone(shpurdp_ldap_details.get_user_object_class())
    self.assertIsNone(shpurdp_ldap_details.get_user_name_attribute())
    self.assertIsNone(shpurdp_ldap_details.get_user_search_base())
    self.assertIsNone(shpurdp_ldap_details.get_group_object_class())
    self.assertIsNone(shpurdp_ldap_details.get_group_name_attribute())
    self.assertIsNone(shpurdp_ldap_details.get_group_member_attribute())
    self.assertIsNone(shpurdp_ldap_details.get_group_search_base())
    self.assertIsNone(shpurdp_ldap_details.get_group_mapping_rules())
    self.assertIsNone(shpurdp_ldap_details.get_user_search_filter())
    self.assertIsNone(shpurdp_ldap_details.get_user_member_replace_pattern())
    self.assertIsNone(shpurdp_ldap_details.get_user_member_filter())
    self.assertIsNone(shpurdp_ldap_details.get_group_search_filter())
    self.assertIsNone(shpurdp_ldap_details.get_group_member_replace_pattern())
    self.assertIsNone(shpurdp_ldap_details.get_group_member_filter())
    self.assertFalse(shpurdp_ldap_details.is_force_lower_case_user_names())
    self.assertFalse(shpurdp_ldap_details.is_pagination_enabled())
    self.assertFalse(shpurdp_ldap_details.is_follow_referral_handling())
    self.assertFalse(shpurdp_ldap_details.is_disable_endpoint_identification())
    self.assertFalse(shpurdp_ldap_details.is_ldap_alternate_user_search_enabled())
    self.assertIsNone(shpurdp_ldap_details.get_alternate_user_search_filter())
    self.assertIsNone(shpurdp_ldap_details.get_sync_collision_handling_behavior())

  def testNotEmtpyLDAPConfiguration(self):
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {
          "shpurdp.ldap.authentication.enabled": "true",
          "shpurdp.ldap.connectivity.server.host": "host1",
          "shpurdp.ldap.connectivity.server.port": "336",
          "shpurdp.ldap.connectivity.secondary.server.host": "host2",
          "shpurdp.ldap.connectivity.secondary.server.port": "337",
          "shpurdp.ldap.connectivity.use_ssl": "true",
          "shpurdp.ldap.connectivity.anonymous_bind": "true",
          "shpurdp.ldap.connectivity.bind_dn": "bind_dn",
          "shpurdp.ldap.connectivity.bind_password": "bind_password",
          "shpurdp.ldap.attributes.dn_attr": "dn_attr",
          "shpurdp.ldap.attributes.user.object_class": "user.object_class",
          "shpurdp.ldap.attributes.user.name_attr": "user.name_attr",
          "shpurdp.ldap.attributes.user.search_base": "user.search_base",
          "shpurdp.ldap.attributes.group.object_class": "group.object_class",
          "shpurdp.ldap.attributes.group.name_attr": "group.name_attr",
          "shpurdp.ldap.attributes.group.member_attr": "group.member_attr",
          "shpurdp.ldap.attributes.group.search_base": "group.search_base",
          "shpurdp.ldap.advanced.group_mapping_rules": "group_mapping_rules",
          "shpurdp.ldap.advanced.user_search_filter": "user_search_filter",
          "shpurdp.ldap.advanced.user_member_replace_pattern": "user_member_replace_pattern",
          "shpurdp.ldap.advanced.user_member_filter": "user_member_filter",
          "shpurdp.ldap.advanced.group_search_filter": "group_search_filter",
          "shpurdp.ldap.advanced.group_member_replace_pattern": "group_member_replace_pattern",
          "shpurdp.ldap.advanced.group_member_filter": "group_member_filter",
          "shpurdp.ldap.advanced.force_lowercase_usernames": "true",
          "shpurdp.ldap.advanced.pagination_enabled": "true",
          "shpurdp.ldap.advanced.referrals": "true",
          "shpurdp.ldap.advanced.disable_endpoint_identification": "true",
          "shpurdp.ldap.advanced.alternate_user_search_enabled": "true",
          "shpurdp.ldap.advanced.alternate_user_search_filter": "alternate_user_search_filter",
          "shpurdp.ldap.advanced.collision_behavior": "collision_behavior",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())
    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)

    self.assertTrue(shpurdp_ldap_details.is_ldap_enabled())
    self.assertEqual(shpurdp_ldap_details.get_server_host(), "host1")
    self.assertEqual(shpurdp_ldap_details.get_server_port(), 336)
    self.assertEqual(shpurdp_ldap_details.get_server_url(), "host1:336")
    self.assertEqual(shpurdp_ldap_details.get_secondary_server_host(), "host2")
    self.assertEqual(shpurdp_ldap_details.get_secondary_server_port(), 337)
    self.assertEqual(shpurdp_ldap_details.get_secondary_server_url(), "host2:337")
    self.assertTrue(shpurdp_ldap_details.is_use_ssl())
    self.assertTrue(shpurdp_ldap_details.is_anonymous_bind())
    self.assertEqual(shpurdp_ldap_details.get_bind_dn(), "bind_dn")
    self.assertEqual(shpurdp_ldap_details.get_bind_password(), "bind_password")
    self.assertEqual(shpurdp_ldap_details.get_dn_attribute(), "dn_attr")
    self.assertEqual(shpurdp_ldap_details.get_user_object_class(), "user.object_class")
    self.assertEqual(shpurdp_ldap_details.get_user_name_attribute(), "user.name_attr")
    self.assertEqual(shpurdp_ldap_details.get_user_search_base(), "user.search_base")
    self.assertEqual(shpurdp_ldap_details.get_group_object_class(), "group.object_class")
    self.assertEqual(shpurdp_ldap_details.get_group_name_attribute(), "group.name_attr")
    self.assertEqual(
      shpurdp_ldap_details.get_group_member_attribute(), "group.member_attr"
    )
    self.assertEqual(shpurdp_ldap_details.get_group_search_base(), "group.search_base")
    self.assertEqual(
      shpurdp_ldap_details.get_group_mapping_rules(), "group_mapping_rules"
    )
    self.assertEqual(shpurdp_ldap_details.get_user_search_filter(), "user_search_filter")
    self.assertEqual(
      shpurdp_ldap_details.get_user_member_replace_pattern(),
      "user_member_replace_pattern",
    )
    self.assertEqual(shpurdp_ldap_details.get_user_member_filter(), "user_member_filter")
    self.assertEqual(
      shpurdp_ldap_details.get_group_search_filter(), "group_search_filter"
    )
    self.assertEqual(
      shpurdp_ldap_details.get_group_member_replace_pattern(),
      "group_member_replace_pattern",
    )
    self.assertEqual(
      shpurdp_ldap_details.get_group_member_filter(), "group_member_filter"
    )
    self.assertTrue(shpurdp_ldap_details.is_force_lower_case_user_names())
    self.assertTrue(shpurdp_ldap_details.is_pagination_enabled())
    self.assertTrue(shpurdp_ldap_details.is_follow_referral_handling())
    self.assertTrue(shpurdp_ldap_details.is_disable_endpoint_identification())
    self.assertTrue(shpurdp_ldap_details.is_ldap_alternate_user_search_enabled())
    self.assertEqual(
      shpurdp_ldap_details.get_alternate_user_search_filter(),
      "alternate_user_search_filter",
    )
    self.assertEqual(
      shpurdp_ldap_details.get_sync_collision_handling_behavior(), "collision_behavior"
    )

  def testShpurdpNotMangingLdapConfiguration(self):
    ## Case 1: missing the boolean flag indicating that Shpurdp manages LDAP configuration
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {"shpurdp.ldap.enabled_services": "SHPURDP"}
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertFalse(shpurdp_ldap_details.is_managing_services())
    self.assertFalse(shpurdp_ldap_details.should_enable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("SHPURDP"))

    ## Case 2: setting the boolean flag to false indicating that Shpurdp shall NOT manage LDAP configuration
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {
          "shpurdp.ldap.manage_services": "false",
          "shpurdp.ldap.enabled_services": "SHPURDP, RANGER",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertFalse(shpurdp_ldap_details.is_managing_services())
    self.assertFalse(shpurdp_ldap_details.should_enable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("RANGER"))

    ## Case 3: setting the boolean flag to false indicating that Shpurdp shall NOT manage LDAP configuration and indicating it should be done for ALL services
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {
          "shpurdp.ldap.manage_services": "false",
          "shpurdp.ldap.enabled_services": "*",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertFalse(shpurdp_ldap_details.is_managing_services())
    self.assertFalse(shpurdp_ldap_details.should_enable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("RANGER"))

  def testShpurdpMangingLdapConfiguration(self):
    ## Case 1: setting the boolean flag to false indicating that Shpurdp shall manage LDAP configuration for SHPURDP and RANGER
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {
          "shpurdp.ldap.manage_services": "true",
          "shpurdp.ldap.enabled_services": "SHPURDP, RANGER",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertTrue(shpurdp_ldap_details.is_managing_services())
    self.assertTrue(shpurdp_ldap_details.should_enable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("SHPURDP"))
    self.assertTrue(shpurdp_ldap_details.should_enable_ldap("RANGER"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("RANGER"))

    ## Case 2: setting the boolean flag to false indicating that Shpurdp shall manage LDAP configuration for ALL services
    services_json = {
      "shpurdp-server-configuration": {
        "ldap-configuration": {
          "shpurdp.ldap.manage_services": "true",
          "shpurdp.ldap.enabled_services": "*",
        }
      }
    }

    shpurdp_configuration = self.shpurdp_configuration_class(services_json)
    self.assertIsNotNone(shpurdp_configuration.get_shpurdp_ldap_configuration())

    shpurdp_ldap_details = shpurdp_configuration.get_shpurdp_ldap_details()
    self.assertIsNotNone(shpurdp_ldap_details)
    self.assertTrue(shpurdp_ldap_details.is_managing_services())
    self.assertTrue(shpurdp_ldap_details.should_enable_ldap("SHPURDP"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("SHPURDP"))
    self.assertTrue(shpurdp_ldap_details.should_enable_ldap("HDFS"))
    self.assertFalse(shpurdp_ldap_details.should_disable_ldap("HDFS"))
