#!/usr/bin/env python3
# coding: utf-8
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import subprocess, time, sys
import json
import datetime
from optparse import OptionParser

SKIP_TEST = "-DskipTests"
SHPURDP_AUTH_HEADERS = (
  "--header 'Authorization:Basic YWRtaW46YWRtaW4=' --header 'X-Requested-By: PIVOTAL'"
)
SHPURDP_BUILD_DOCKER_ROOT = "/tmp/shpurdp-build-docker"
NO_EXIT_SLEEP_TIME = 60
RETRY_MAX = 20


def git_deep_cleaning():
  proc = subprocess.Popen("git clean -xdf", shell=True, cwd="/tmp/shpurdp")
  return proc.wait()


def shpurdpUnitTest():
  proc = subprocess.Popen("mvn -fae clean install", shell=True, cwd="/tmp/shpurdp")
  return proc.wait()


def buildShpurdp(stack_distribution):
  stack_distribution_param = ""
  if stack_distribution is not None:
    stack_distribution_param = "-Dstack.distribution=" + stack_distribution
  proc = subprocess.Popen(
    "mvn -B clean install package rpm:rpm -Dmaven.clover.skip=true -Dfindbugs.skip=true "
    + SKIP_TEST
    + " "
    + stack_distribution_param
    + ' -Dpython.ver="python >= 2.6"',
    shell=True,
    cwd="/tmp/shpurdp",
  )
  return proc.wait()


def install_shpurdp_server():
  proc = subprocess.Popen(
    "sudo yum install -y shpurdp-server-*.x86_64.rpm",
    shell=True,
    cwd="/tmp/shpurdp/shpurdp-server/target/rpm/shpurdp-server/RPMS/x86_64",
  )
  return proc.wait()


def install_shpurdp_agent():
  proc = subprocess.Popen(
    "sudo yum install -y shpurdp-agent-*.x86_64.rpm",
    shell=True,
    cwd="/tmp/shpurdp/shpurdp-agent/target/rpm/shpurdp-agent/RPMS/x86_64",
  )
  return proc.wait()


def setup_shpurdp_server():
  proc = subprocess.Popen("echo -e '\n\n\n\n' | sudo shpurdp-server setup", shell=True)
  return proc.wait()


def start_shpurdp_server(debug=False):
  proc = subprocess.Popen(
    "sudo shpurdp-server start" + (" --debug" if debug else ""), shell=True
  )
  return proc.wait()


def start_dependant_services():
  retcode = 0
  proc = subprocess.Popen("sudo service sshd start", shell=True)
  retcode += proc.wait()
  proc = subprocess.Popen("sudo service ntpd start", shell=True)
  retcode += proc.wait()
  return retcode


def configure_shpurdp_agent():
  proc = subprocess.Popen("hostname -f", stdout=subprocess.PIPE, shell=True)
  hostname = proc.stdout.read().rstrip()
  proc = subprocess.Popen(
    "sudo sed -i 's/hostname=localhost/hostname="
    + hostname
    + "/g' /etc/shpurdp-agent/conf/shpurdp-agent.ini",
    shell=True,
  )
  return proc.wait()


def start_shpurdp_agent(wait_until_registered=True):
  retcode = 0
  proc = subprocess.Popen("service shpurdp-agent start", shell=True)
  retcode += proc.wait()
  if wait_until_registered:
    if not wait_until_shpurdp_agent_registered():
      print("ERROR: shpurdp-agent was not registered.")
      sys.exit(1)

  return retcode


def wait_until_shpurdp_agent_registered():
  """
  return True if shpurdp agent is found registered.
  return False if timeout
  """
  count = 0
  while count < RETRY_MAX:
    count += 1
    proc = subprocess.Popen(
      "curl " + "http://localhost:8080/api/v1/hosts " + SHPURDP_AUTH_HEADERS,
      stdout=subprocess.PIPE,
      shell=True,
    )
    hosts_result_string = proc.stdout.read()
    hosts_result_json = json.loads(hosts_result_string)
    if len(hosts_result_json["items"]) != 0:
      return True
    time.sleep(5)
  return False


def post_blueprint():
  proc = subprocess.Popen(
    "curl -X POST -D - "
    + "-d @single-node-HDP-2.1-blueprint1.json http://localhost:8080/api/v1/blueprints/myblueprint1 "
    + SHPURDP_AUTH_HEADERS,
    cwd=SHPURDP_BUILD_DOCKER_ROOT + "/blueprints",
    shell=True,
  )
  return proc.wait()


def create_cluster():
  proc = subprocess.Popen(
    "curl -X POST -D - "
    + "-d @single-node-hostmapping1.json http://localhost:8080/api/v1/clusters/mycluster1 "
    + SHPURDP_AUTH_HEADERS,
    cwd=SHPURDP_BUILD_DOCKER_ROOT + "/blueprints",
    shell=True,
  )
  return proc.wait()


# Loop to not to exit Docker container
def no_exit():
  print("")
  print("loop to not to exit docker container...")
  print("")
  while True:
    time.sleep(NO_EXIT_SLEEP_TIME)


class ParseResult:
  is_deep_clean = False
  is_rebuild = False
  stack_distribution = None
  is_test = False
  is_install_server = False
  is_install_agent = False
  is_deploy = False
  is_server_debug = False


def parse(argv):
  result = ParseResult()
  if len(argv) >= 2:
    parser = OptionParser()
    parser.add_option(
      "-c",
      "--clean",
      dest="is_deep_clean",
      action="store_true",
      default=False,
      help="if this option is set, git clean -xdf is executed for the shpurdp local git repo",
    )

    parser.add_option(
      "-b",
      "--rebuild",
      dest="is_rebuild",
      action="store_true",
      default=False,
      help="set this flag if you want to rebuild Shpurdp code",
    )

    parser.add_option(
      "-s",
      "--stack_distribution",
      dest="stack_distribution",
      help="set a stack distribution. [HDP|PHD|BIGTOP]. Make sure -b is also set when you set a stack distribution",
    )

    parser.add_option(
      "-d",
      "--server_debug",
      dest="is_server_debug",
      action="store_true",
      default=False,
      help="set a debug option for shpurdp-server",
    )

    (options, args) = parser.parse_args(argv[1:])
    if options.is_deep_clean:
      result.is_deep_clean = True
    if options.is_rebuild:
      result.is_rebuild = True
    if options.stack_distribution:
      result.stack_distribution = options.stack_distribution
    if options.is_server_debug:
      result.is_server_debug = True

  if argv[0] == "test":
    result.is_test = True

  if argv[0] == "server":
    result.is_install_server = True

  if argv[0] == "agent":
    result.is_install_server = True
    result.is_install_agent = True

  if argv[0] == "deploy":
    result.is_install_server = True
    result.is_install_agent = True
    result.is_deploy = True

  return result


if __name__ == "__main__":
  if len(sys.argv) == 1:
    print("specify one of test, server, agent or deploy")
    sys.exit(1)

  start = datetime.datetime.utcnow()

  # test: execute unit test
  # server: install shpurdp-server
  #    with or without rebuild
  # agent: install shpurdp-server and shpurdp-agent
  #    with or without rebuild
  # deploy: install shpurdp-server, shpurdp-agent and deploy Hadoop
  #    with or without rebuild

  parsed_args = parse(sys.argv[1:])

  if parsed_args.is_deep_clean:
    retcode = git_deep_cleaning()
    if retcode != 0:
      sys.exit(retcode)

  if parsed_args.is_test:
    retcode = shpurdpUnitTest()
    end = datetime.datetime.utcnow()
    print("")
    print("Duration: " + str((end - start).seconds) + " seconds")
    sys.exit(retcode)

  if parsed_args.is_rebuild:
    retcode = buildShpurdp(parsed_args.stack_distribution)
    if retcode != 0:
      sys.exit(retcode)

  if parsed_args.is_install_server:
    retcode = install_shpurdp_server()
    if retcode != 0:
      sys.exit(retcode)
    retcode = setup_shpurdp_server()
    if retcode != 0:
      sys.exit(retcode)
    retcode = start_shpurdp_server(parsed_args.is_server_debug)
    if retcode != 0:
      sys.exit(retcode)
    retcode = start_dependant_services()
    if retcode != 0:
      sys.exit(retcode)

  if parsed_args.is_install_agent:
    retcode = install_shpurdp_agent()
    if retcode != 0:
      sys.exit(retcode)
    retcode = configure_shpurdp_agent()
    if retcode != 0:
      sys.exit(retcode)
    retcode = start_shpurdp_agent()
    if retcode != 0:
      sys.exit(retcode)

  if parsed_args.is_deploy:
    retcode = post_blueprint()
    if retcode != 0:
      sys.exit(retcode)
    retcode = create_cluster()
    if retcode != 0:
      sys.exit(retcode)

  end = datetime.datetime.utcnow()

  print("")
  print("Duration: " + str((end - start).seconds) + " seconds")
  print("Parameters: " + str(sys.argv))
  no_exit()
