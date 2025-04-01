#!/usr/bin/env python3
from shpurdp_jinja2 import Environment

print(
  Environment(extensions=["shpurdp_jinja2.i18n.TransExtension"])
  .from_string("""\
{% trans %}Hello {{ user }}!{% endtrans %}
{% trans count=users|count %}{{ count }} user{% pluralize %}{{ count }} users{% endtrans %}
""")
  .render(user="someone")
)
