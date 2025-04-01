#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Jinja2 Debug Interface
~~~~~~~~~~~~~~~~~~~~~~

Helper script for internal Jinja2 debugging.  Requires Werkzeug.

:copyright: Copyright 2010 by Armin Ronacher.
:license: BSD.
"""

import sys
import shpurdp_jinja2
from werkzeug import script

env = shpurdp_jinja2.Environment(
  extensions=[
    "shpurdp_jinja2.ext.i18n",
    "shpurdp_jinja2.ext.do",
    "shpurdp_jinja2.ext.loopcontrols",
  ]
)


def shell_init_func():
  def _compile(x):
    print(env.compile(x, raw=True))

  result = {"e": env, "c": _compile, "t": env.from_string, "p": env.parse}
  for key in shpurdp_jinja2.__all__:
    result[key] = getattr(shpurdp_jinja2, key)
  return result


def action_compile():
  print(env.compile(sys.stdin.read(), raw=True))


action_shell = script.make_shell(shell_init_func)


if __name__ == "__main__":
  script.run()
