/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * @class
 *
 * This is a view for showing HBase RegionServer requests
 *
 * @extends App.ChartServiceMetricsAMS_RegionServerBaseView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsAMS_RegionServerRequests = App.ChartServiceMetricsAMS_RegionServerBaseView.extend({
  id: "service-metrics-shpurdp-metrics-region-server-requests",
  title: Em.I18n.t('services.service.info.metrics.shpurdpMetrics.regionServer.requests'),
  ajaxIndex: 'service.metrics.shpurdp_metrics.region_server.request',

  loadGroup: {
    name: 'service.metrics.shpurdp_metrics.aggregated',
    fields: ['metrics/hbase/regionserver/requests._rate']
  },

  displayName: Em.I18n.t('services.service.info.metrics.shpurdpMetrics.regionServer.displayNames.requestCount'),
  regionServerName: 'requests._rate'
});