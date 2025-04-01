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

var App = require('app');

App.MainViewsController = Em.Controller.extend({
  name: 'mainViewsController',

  isDataLoaded: false,

  shpurdpViews: [],

  visibleShpurdpViews: Em.computed.filterBy('shpurdpViews', 'visible', true),

  dataLoading: function () {
    var viewsController = this;
    var dfd = $.Deferred();
    if (this.get('isDataLoaded')) {
      dfd.resolve(this.get('shpurdpViews'));
    } else {
      var interval = setInterval(function () {
        if (viewsController.get('isDataLoaded')) {
          dfd.resolve(viewsController.get('shpurdpViews'));
          clearInterval(interval);
        }
      }, 50);
    }
    return dfd.promise();
  },


  loadShpurdpViews: function () {
    if (App.router.get('loggedIn')) {
      return App.ajax.send({
        name: 'views.info',
        sender: this,
        success: 'loadShpurdpViewsSuccess',
        error: 'loadShpurdpViewsError'
      });
    }
  },

  loadShpurdpViewsSuccess: function (data, opt, params) {
    if (data.items.length) {
      App.ajax.send({
        name: 'views.instances',
        sender: this,
        success: 'loadViewInstancesSuccess',
        error: 'loadViewInstancesError'
      });
    } else {
      this.set('shpurdpViews', []);
      this.set('isDataLoaded', true);
    }
  },

  loadShpurdpViewsError: function () {
    this.set('shpurdpViews', []);
    this.set('isDataLoaded', true);
  },

  loadViewInstancesSuccess: function (data, opt, params) {
    this.set('shpurdpViews', []);
    var instances = [];
    data.items.forEach(function (view) {
      view.versions.forEach(function (version) {
        version.instances.forEach(function (instance) {
          var info = instance.ViewInstanceInfo;
          var currentInstance = App.ViewInstance.create({
            iconPath: info.icon_path || '/img/shpurdp-view-default.png',
            label: info.label || version.ViewVersionInfo.label || info.view_name,
            visible: info.visible || false,
            version: info.version,
            description: info.description || Em.I18n.t('views.main.instance.noDescription'),
            viewName: info.view_name,
            shortUrl:info.short_url,
            instanceName: info.instance_name,
            href: info.context_path + '/'
          });
          if (currentInstance.visible) {
            instances.push(currentInstance);
          }
        }, this);
      }, this);
    }, this);
    this.get('shpurdpViews').pushObjects(instances);
    this.set('isDataLoaded', true);
  },

  loadViewInstancesError: function () {
    this.set('shpurdpViews', []);
    this.set('isDataLoaded', true);
  },

  setView: function (event) {
    if (event.context) {
      window.open(event.context.get('internalShpurdpUrl'));
    }
  }
});
