#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
shpurdp_jinja2.testsuite.security
~~~~~~~~~~~~~~~~~~~~~~~~~

Checks the sandbox and other security features.

:copyright: (c) 2010 by the Jinja Team.
:license: BSD, see LICENSE for more details.
"""

import os
import time
import tempfile
import unittest

from shpurdp_jinja2.testsuite import JinjaTestCase

from shpurdp_jinja2 import Environment
from shpurdp_jinja2.sandbox import (
  SandboxedEnvironment,
  ImmutableSandboxedEnvironment,
  unsafe,
)
from shpurdp_jinja2 import Markup, escape
from shpurdp_jinja2.exceptions import SecurityError, TemplateSyntaxError


class PrivateStuff(object):
  def bar(self):
    return 23

  @unsafe
  def foo(self):
    return 42

  def __repr__(self):
    return "PrivateStuff"


class PublicStuff(object):
  bar = lambda self: 23
  _foo = lambda self: 42

  def __repr__(self):
    return "PublicStuff"


class SandboxTestCase(JinjaTestCase):
  def test_unsafe(self):
    env = SandboxedEnvironment()
    self.assert_raises(
      SecurityError, env.from_string("{{ foo.foo() }}").render, foo=PrivateStuff()
    )
    self.assert_equal(
      env.from_string("{{ foo.bar() }}").render(foo=PrivateStuff()), "23"
    )

    self.assert_raises(
      SecurityError, env.from_string("{{ foo._foo() }}").render, foo=PublicStuff()
    )
    self.assert_equal(
      env.from_string("{{ foo.bar() }}").render(foo=PublicStuff()), "23"
    )
    self.assert_equal(env.from_string("{{ foo.__class__ }}").render(foo=42), "")
    self.assert_equal(
      env.from_string("{{ foo.func_code }}").render(foo=lambda: None), ""
    )
    self.assert_raises(
      SecurityError,
      env.from_string("{{ foo.__class__.__subclasses__() }}").render,
      foo=42,
    )

  def test_immutable_environment(self):
    env = ImmutableSandboxedEnvironment()
    self.assert_raises(SecurityError, env.from_string("{{ [].append(23) }}").render)
    self.assert_raises(SecurityError, env.from_string("{{ {1:2}.clear() }}").render)

  def test_restricted(self):
    env = SandboxedEnvironment()
    self.assert_raises(
      TemplateSyntaxError,
      env.from_string,
      "{% for item.attribute in seq %}...{% endfor %}",
    )
    self.assert_raises(
      TemplateSyntaxError,
      env.from_string,
      "{% for foo, bar.baz in seq %}...{% endfor %}",
    )

  def test_markup_operations(self):
    # adding two strings should escape the unsafe one
    unsafe = '<script type="application/x-some-script">alert("foo");</script>'
    safe = Markup("<em>username</em>")
    assert unsafe + safe == str(escape(unsafe)) + str(safe)

    # string interpolations are safe to use too
    assert Markup("<em>%s</em>") % "<bad user>" == "<em>&lt;bad user&gt;</em>"
    assert (
      Markup("<em>%(username)s</em>") % {"username": "<bad user>"}
      == "<em>&lt;bad user&gt;</em>"
    )

    # an escaped object is markup too
    assert type(Markup("foo") + "bar") is Markup

    # and it implements __html__ by returning itself
    x = Markup("foo")
    assert x.__html__() is x

    # it also knows how to treat __html__ objects
    class Foo(object):
      def __html__(self):
        return "<em>awesome</em>"

      def __unicode__(self):
        return "awesome"

    assert Markup(Foo()) == "<em>awesome</em>"
    assert Markup("<strong>%s</strong>") % Foo() == "<strong><em>awesome</em></strong>"

    # escaping and unescaping
    assert escape("\"<>&'") == "&#34;&lt;&gt;&amp;&#39;"
    assert Markup("<em>Foo &amp; Bar</em>").striptags() == "Foo & Bar"
    assert Markup("&lt;test&gt;").unescape() == "<test>"

  def test_template_data(self):
    env = Environment(autoescape=True)
    t = env.from_string(
      "{% macro say_hello(name) %}"
      "<p>Hello {{ name }}!</p>{% endmacro %}"
      '{{ say_hello("<blink>foo</blink>") }}'
    )
    escaped_out = "<p>Hello &lt;blink&gt;foo&lt;/blink&gt;!</p>"
    assert t.render() == escaped_out
    assert str(t.module) == escaped_out
    assert escape(t.module) == escaped_out
    assert t.module.say_hello("<blink>foo</blink>") == escaped_out
    assert escape(t.module.say_hello("<blink>foo</blink>")) == escaped_out

  def test_attr_filter(self):
    env = SandboxedEnvironment()
    tmpl = env.from_string('{{ 42|attr("__class__")|attr("__subclasses__")() }}')
    self.assert_raises(SecurityError, tmpl.render)


def suite():
  suite = unittest.TestSuite()
  suite.addTest(unittest.makeSuite(SandboxTestCase))
  return suite
