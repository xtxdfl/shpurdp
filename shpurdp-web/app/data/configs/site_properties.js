/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 * Returns list of config properties
 * @param {string[]} serviceNames list of service names
 * @return {object[]}
 */
var includeSiteProperties = function() {
  var serviceNames = Array.prototype.slice.call(arguments);
  return serviceNames.reduce(function(acc, serviceName) {
    return acc.concat(require('data/configs/services/' + serviceName + '_properties'));
  }, []);
};

module.exports = {
  configProperties: includeSiteProperties(
    'accumulo',
    'shpurdp_infra_solr',
    'shpurdp_metrics',
    'falcon',
    'flume',
    'glusterfs',
    'hawq',
    'hbase',
    'hdfs',
    'hive',
    'kafka',
    'kerberos',
    'knox',
    'logsearch',
    'mapreduce2',
    'oozie',
    'ranger',
    'storm',
    'tez',
    'yarn',
    'zookeeper'
  )
};
