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

Shpurdp Agent

"""

from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.utils import checked_unite
from resource_management.core import sudo

__all__ = ["Source", "Template", "InlineTemplate", "StaticFile", "DownloadSource"]

import os
import time
import urllib.request, urllib.error, urllib.parse

from shpurdp_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from shpurdp_commons import OSConst


class Source(object):
  def __init__(self, name):
    self.env = Environment.get_instance()
    self.name = name

  def get_content(self):
    raise NotImplementedError()

  def get_checksum(self):
    return None

  def __call__(self):
    return self.get_content()

  def __repr__(self):
    return self.__class__.__name__ + "('" + self.name + "')"

  def __eq__(self, other):
    return isinstance(other, self.__class__) and (
      (self.name.startswith(os.sep) and self.name == other.name)
      or self.get_content() == other.get_content()
    )


class StaticFile(Source):
  def __init__(self, name):
    super(StaticFile, self).__init__(name)

  def get_content(self):
    # absolute path
    if self.name.startswith(os.path.sep):
      path = self.name
    # relative path
    else:
      basedir = self.env.config.basedir
      path = os.path.join(basedir, "files", self.name)

    if not sudo.path_isfile(path):
      raise Fail(f"{repr(self)} Source file {path} is not found")

    return self.read_file(path)

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def read_file(self, path):
    return sudo.read_file(path)

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def read_file(self, path):
    with open(path, "rb") as fp:
      return fp.read()


try:
  from shpurdp_jinja2 import (
    Environment as JinjaEnvironment,
    BaseLoader,
    TemplateNotFound,
    FunctionLoader,
    StrictUndefined,
  )
except ImportError:

  class Template(Source):
    def __init__(self, name, variables=None, env=None):
      raise Exception("Jinja2 required for Template/InlineTemplate")

  class InlineTemplate(Source):
    def __init__(self, name, variables=None, env=None):
      raise Exception("Jinja2 required for Template/InlineTemplate")
else:

  class TemplateLoader(BaseLoader):
    def __init__(self, env=None):
      self.env = env or Environment.get_instance()

    def get_source(self, environment, template_name):
      # absolute path
      if template_name.startswith(os.path.sep):
        path = template_name
      # relative path
      else:
        basedir = self.env.config.basedir
        path = os.path.join(basedir, "templates", template_name)

      if not os.path.exists(path):
        raise TemplateNotFound(f"{template_name} at {path}")
      mtime = os.path.getmtime(path)
      with open(path, "rt") as fp:
        source = fp.read()
      return source, path, lambda: mtime == os.path.getmtime(path)

  class Template(Source):
    def __init__(self, name, extra_imports=[], **kwargs):
      """
      @param kwargs: Additional variables passed to template
      """
      super(Template, self).__init__(name)
      params = self.env.config.params
      variables = checked_unite(params, kwargs)
      self.imports_dict = dict((module.__name__, module) for module in extra_imports)
      self.context = variables.copy() if variables else {}
      if not hasattr(self, "template_env"):
        self.template_env = JinjaEnvironment(
          loader=TemplateLoader(self.env),
          autoescape=False,
          undefined=StrictUndefined,
          trim_blocks=True,
        )

      self.template = self.template_env.get_template(self.name)

    def get_content(self):
      default_variables = {
        "env": self.env,
        "repr": repr,
        "str": str,
        "bool": bool,
        "unicode": str,
      }
      variables = checked_unite(default_variables, self.imports_dict)
      self.context.update(variables)

      rendered = self.template.render(self.context)
      return rendered

  class InlineTemplate(Template):
    def __init__(self, name, extra_imports=[], **kwargs):
      self.template_env = JinjaEnvironment(loader=FunctionLoader(lambda text: text))
      super(InlineTemplate, self).__init__(name, extra_imports, **kwargs)

    def __repr__(self):
      return "InlineTemplate(...)"


class DownloadSource(Source):
  """
  redownload_files = True/False -- if file with the same name exists in tmp_dir
  it won't be downloaded again (be if files are different this won't replace them)

  ignore_proxy = True/False -- determines if http_proxy / https_proxy environment variables
  should be ignored or not
  """

  def __init__(self, name, redownload_files=False, ignore_proxy=True):
    super(DownloadSource, self).__init__(name)

    self.url = self.name
    self.cache = not redownload_files and bool(self.env.tmp_dir)
    self.download_path = self.env.tmp_dir
    self.ignore_proxy = ignore_proxy

  def get_content(self):
    if self.download_path and not os.path.exists(self.download_path):
      raise Fail(
        f"Directory {self.download_path} doesn't exist, please provide valid download path"
      )

    if urllib.parse.urlparse(self.url).path:
      filename = os.path.basename(urllib.parse.urlparse(self.url).path)
    else:
      filename = f"index.html.{time.time()}"

    filepath = os.path.join(self.download_path, filename)

    if not self.cache or not os.path.exists(filepath):
      Logger.info(f"Downloading the file from {self.url}")

      if self.ignore_proxy:
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
      else:
        opener = urllib.request.build_opener()

      req = urllib.request.Request(self.url)

      try:
        web_file = opener.open(req)
      except urllib.error.HTTPError as ex:
        raise Fail(
          f"Failed to download file from {self.url} due to HTTP error: {str(ex)}"
        )

      content = web_file.read()

      if self.cache:
        sudo.create_file(filepath, content)
    else:
      Logger.info(
        f"Not downloading the file from {self.url}, because {filepath} already exists"
      )
      content = sudo.read_file(filepath)

    return content
