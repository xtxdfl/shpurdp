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

SHPURDP_SUDO_BINARY = "shpurdp-sudo.sh"

UPGRADE_TYPE_ROLLING = "rolling"
UPGRADE_TYPE_NON_ROLLING = "nonrolling"
UPGRADE_TYPE_HOST_ORDERED = "host_ordered"

AGENT_TMP_DIR = "/var/lib/shpurdp-agent/tmp"

LOGFEEDER_CONF_DIR = "/usr/lib/shpurdp-logsearch-logfeeder/conf"


class SERVICE:
  """
  Constants for service names to avoid hardcoding strings.
  """

  ATLAS = "ATLAS"
  FALCON = "FALCON"
  FLUME = "FLUME"
  HAWQ = "HAWQ"
  HDFS = "HDFS"
  HIVE = "HIVE"
  KAFKA = "KAFKA"
  KNOX = "KNOX"
  MAHOUT = "MAHOUT"
  OOZIE = "OOZIE"
  PIG = "PIG"
  PXF = "PXF"
  RANGER = "RANGER"
  SLIDER = "SLIDER"
  SPARK = "SPARK"
  SQOOP = "SQOOP"
  STORM = "STORM"
  TEZ = "TEZ"
  YARN = "YARN"
  ZEPPELIN = "ZEPPELIN"
  ZOOKEEPER = "ZOOKEEPER"
  HBASE = "HBASE"
