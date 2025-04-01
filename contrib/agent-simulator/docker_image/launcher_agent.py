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


import sys
import subprocess


def replace_conf(server_ip):
    """
    replace the server host IP in the Shpurdp-agent configuration file
    :param server_ip: internal Weave IP address of Shpurdp-server
    :return: None
    """
    lines = []
    with open("/etc/shpurdp-agent/conf/shpurdp-agent.ini") as f:
        lines = f.readlines()

    with open("/etc/shpurdp-agent/conf/shpurdp-agent.ini", "w+") as f:
        for line in lines:
            line = line.replace("hostname=localhost", "hostname=" + server_ip)
            f.write(line)


def run_ssh():
    """
    run SSH service on this Docker Container
    :return: None
    """
    subprocess.call("/run_ssh.sh")


def run_shpurdp_agent():
    """
    command line to run Shpurdp-agent
    :return: None
    """
    subprocess.call("/shpurdp_agent_start.sh")


def set_weave_ip(weave_ip):
    """
    set the IP and hostname mapping for this Container
    Docker will assign an IP to each Container, and map it to hostname, which is not we want
    We want our Weave IP to be mapped to hostname
    :param weave_ip:
    :return: None
    """
    with open("/etc/hosts") as etc_hosts:
        all_resolution = etc_hosts.readlines()

    with open("/etc/hosts", "w") as etc_hosts:
        for index in range(len(all_resolution)):
            if index == 0:
                token = all_resolution[index].split()
                etc_hosts.write(f"{weave_ip} {token[1]} {token[2]}\n")
            else:
                etc_hosts.write(all_resolution[index])


def main():
    shpurdp_server_ip = sys.argv[1]
    my_weave_ip = sys.argv[2]
    replace_conf(shpurdp_server_ip)
    set_weave_ip(my_weave_ip)
    run_shpurdp_agent()
    run_ssh()


if __name__ == "__main__":
    main()
