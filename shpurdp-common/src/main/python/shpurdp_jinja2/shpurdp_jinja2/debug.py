#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
shpurdp_jinja2.debug
~~~~~~~~~~~~

Implements the debug interface for Jinja.  This module does some pretty
ugly stuff with the Python traceback system in order to achieve tracebacks
with correct line numbers, locals and contents.

:copyright: (c) 2010 by the Jinja Team.
:license: BSD, see LICENSE for more details.
"""

import sys
import traceback
from shpurdp_jinja2.utils import CodeType, missing, internal_code
from shpurdp_jinja2.exceptions import TemplateSyntaxError


# how does the raise helper look like?
try:
  exec("raise TypeError, 'foo'")
except SyntaxError:
  raise_helper = "raise __jinja_exception__[1]"
except TypeError:
  raise_helper = "raise __jinja_exception__[0], __jinja_exception__[1]"


class TracebackFrameProxy(object):
  """Proxies a traceback frame."""

  def __init__(self, tb):
    self.tb = tb

  def _set_tb_next(self, next):
    if tb_set_next is not None:
      tb_set_next(self.tb, next and next.tb or None)
    self._tb_next = next

  def _get_tb_next(self):
    return self._tb_next

  tb_next = property(_get_tb_next, _set_tb_next)
  del _get_tb_next, _set_tb_next

  @property
  def is_jinja_frame(self):
    return "__jinja_template__" in self.tb.tb_frame.f_globals

  def __getattr__(self, name):
    return getattr(self.tb, name)


class ProcessedTraceback(object):
  """Holds a Jinja preprocessed traceback for priting or reraising."""

  def __init__(self, exc_type, exc_value, frames):
    assert frames, "no frames for this traceback?"
    self.exc_type = exc_type
    self.exc_value = exc_value
    self.frames = frames

  def chain_frames(self):
    """Chains the frames.  Requires ctypes or the debugsupport extension."""
    prev_tb = None
    for tb in self.frames:
      if prev_tb is not None:
        prev_tb.tb_next = tb
      prev_tb = tb
    prev_tb.tb_next = None

  def render_as_text(self, limit=None):
    """Return a string with the traceback."""
    lines = traceback.format_exception(
      self.exc_type, self.exc_value, self.frames[0], limit=limit
    )
    return "".join(lines).rstrip()

  def render_as_html(self, full=False):
    """Return a unicode string with the traceback as rendered HTML."""
    from shpurdp_jinja2.debugrenderer import render_traceback

    return "%s\n\n<!--\n%s\n-->" % (
      render_traceback(self, full=full),
      self.render_as_text().decode("utf-8", "replace"),
    )

  @property
  def is_template_syntax_error(self):
    """`True` if this is a template syntax error."""
    return isinstance(self.exc_value, TemplateSyntaxError)

  @property
  def exc_info(self):
    """Exception info tuple with a proxy around the frame objects."""
    return self.exc_type, self.exc_value, self.frames[0]

  @property
  def standard_exc_info(self):
    """Standard python exc_info for re-raising"""
    return self.exc_type, self.exc_value, self.frames[0].tb


def make_traceback(exc_info, source_hint=None):
  """Creates a processed traceback object from the exc_info."""
  exc_type, exc_value, tb = exc_info
  if isinstance(exc_value, TemplateSyntaxError):
    exc_info = translate_syntax_error(exc_value, source_hint)
    initial_skip = 0
  else:
    initial_skip = 1
  return translate_exception(exc_info, initial_skip)


def translate_syntax_error(error, source=None):
  """Rewrites a syntax error to please traceback systems."""
  error.source = source
  error.translated = True
  exc_info = (error.__class__, error, None)
  filename = error.filename
  if filename is None:
    filename = "<unknown>"
  return fake_exc_info(exc_info, filename, error.lineno)


def translate_exception(exc_info, initial_skip=0):
  """If passed an exc_info it will automatically rewrite the exceptions
  all the way down to the correct line numbers and frames.
  """
  tb = exc_info[2]
  frames = []

  # skip some internal frames if wanted
  for x in range(initial_skip):
    if tb is not None:
      tb = tb.tb_next
  initial_tb = tb

  while tb is not None:
    # skip frames decorated with @internalcode.  These are internal
    # calls we can't avoid and that are useless in template debugging
    # output.
    if tb.tb_frame.f_code in internal_code:
      tb = tb.tb_next
      continue

    # save a reference to the next frame if we override the current
    # one with a faked one.
    next = tb.tb_next

    # fake template exceptions
    template = tb.tb_frame.f_globals.get("__jinja_template__")
    if template is not None:
      lineno = template.get_corresponding_lineno(tb.tb_lineno)
      tb = fake_exc_info(exc_info[:2] + (tb,), template.filename, lineno)[2]

    frames.append(TracebackFrameProxy(tb))
    tb = next

  # if we don't have any exceptions in the frames left, we have to
  # reraise it unchanged.
  # XXX: can we backup here?  when could this happen?
  if not frames:
    raise exc_info[0](exc_info[1]).with_traceback(exc_info[2])

  traceback = ProcessedTraceback(exc_info[0], exc_info[1], frames)
  if tb_set_next is not None:
    traceback.chain_frames()
  return traceback


def fake_exc_info(exc_info, filename, lineno):
  """Helper for `translate_exception`."""
  exc_type, exc_value, tb = exc_info

  # figure the real context out
  if tb is not None:
    real_locals = tb.tb_frame.f_locals.copy()
    ctx = real_locals.get("context")
    if ctx:
      locals = ctx.get_all()
    else:
      locals = {}
    for name, value in real_locals.items():
      if name.startswith("l_") and value is not missing:
        locals[name[2:]] = value

    # if there is a local called __jinja_exception__, we get
    # rid of it to not break the debug functionality.
    locals.pop("__jinja_exception__", None)
  else:
    locals = {}

  # assamble fake globals we need
  globals = {
    "__name__": filename,
    "__file__": filename,
    "__jinja_exception__": exc_info[:2],
    # we don't want to keep the reference to the template around
    # to not cause circular dependencies, but we mark it as Jinja
    # frame for the ProcessedTraceback
    "__jinja_template__": None,
  }

  # and fake the exception
  code = compile("\n" * (lineno - 1) + raise_helper, filename, "exec")

  # if it's possible, change the name of the code.  This won't work
  # on some python environments such as google appengine
  try:
    if tb is None:
      location = "template"
    else:
      function = tb.tb_frame.f_code.co_name
      if function == "root":
        location = "top-level template code"
      elif function.startswith("block_"):
        location = f'block "{function[6:]}"'
      else:
        location = "template"
    code = CodeType(
      0,
      code.co_nlocals,
      code.co_stacksize,
      code.co_flags,
      code.co_code,
      code.co_consts,
      code.co_names,
      code.co_varnames,
      filename,
      location,
      code.co_firstlineno,
      code.co_lnotab,
      (),
      (),
    )
  except:
    pass

  # execute the code and catch the new traceback
  try:
    exec(code, globals, locals)
  except:
    exc_info = sys.exc_info()
    new_tb = exc_info[2].tb_next

  # return without this frame
  return exc_info[:2] + (new_tb,)


def _init_ugly_crap():
  """This function implements a few ugly things so that we can patch the
  traceback objects.  The function returned allows resetting `tb_next` on
  any python traceback object.
  """
  import ctypes
  from types import TracebackType

  # figure out side of _Py_ssize_t
  if hasattr(ctypes.pythonapi, "Py_InitModule4_64"):
    _Py_ssize_t = ctypes.c_int64
  else:
    _Py_ssize_t = ctypes.c_int

  # regular python
  class _PyObject(ctypes.Structure):
    pass

  _PyObject._fields_ = [
    ("ob_refcnt", _Py_ssize_t),
    ("ob_type", ctypes.POINTER(_PyObject)),
  ]

  # python with trace
  if hasattr(sys, "getobjects"):

    class _PyObject(ctypes.Structure):
      pass

    _PyObject._fields_ = [
      ("_ob_next", ctypes.POINTER(_PyObject)),
      ("_ob_prev", ctypes.POINTER(_PyObject)),
      ("ob_refcnt", _Py_ssize_t),
      ("ob_type", ctypes.POINTER(_PyObject)),
    ]

  class _Traceback(_PyObject):
    pass

  _Traceback._fields_ = [
    ("tb_next", ctypes.POINTER(_Traceback)),
    ("tb_frame", ctypes.POINTER(_PyObject)),
    ("tb_lasti", ctypes.c_int),
    ("tb_lineno", ctypes.c_int),
  ]

  def tb_set_next(tb, next):
    """Set the tb_next attribute of a traceback object."""
    if not (
      isinstance(tb, TracebackType)
      and (next is None or isinstance(next, TracebackType))
    ):
      raise TypeError("tb_set_next arguments must be traceback objects")
    obj = _Traceback.from_address(id(tb))
    if tb.tb_next is not None:
      old = _Traceback.from_address(id(tb.tb_next))
      old.ob_refcnt -= 1
    if next is None:
      obj.tb_next = ctypes.POINTER(_Traceback)()
    else:
      next = _Traceback.from_address(id(next))
      next.ob_refcnt += 1
      obj.tb_next = ctypes.pointer(next)

  return tb_set_next


# try to get a tb_set_next implementation
try:
  from shpurdp_jinja2._debugsupport import tb_set_next
except ImportError:
  try:
    tb_set_next = _init_ugly_crap()
  except:
    tb_set_next = None
del _init_ugly_crap
