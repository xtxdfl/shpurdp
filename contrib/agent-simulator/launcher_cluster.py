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

import time
import sys
from cluster import Cluster
from config import Config
from data import Data
import subprocess


def request_cluster(argv):
    """
    only request cluster on GCE, and output all configuration information
    :param argv: sys.argv
    :return: None
    """
    if len(argv) < 7:
        print_help()
        exit(1)

    cluster_name = argv[2]
    shpurdp_agent_vm_num = int(argv[3])
    docker_num = int(argv[4])
    service_server_num = int(argv[5])
    with_shpurdp_server = False
    shpurdp_server_num = int(argv[6])
    if shpurdp_server_num > 0:
        with_shpurdp_server = True

    cluster = Cluster()
    cluster.request_gce_cluster(shpurdp_agent_vm_num, docker_num, service_server_num,
                                with_shpurdp_server, cluster_name)

    time_to_wait = Config.ATTRIBUTES["gce_boot_time"]
    print "wait ", str(time_to_wait), " seconds for the cluster to boot ... ..."
    time.sleep(int(time_to_wait))

    data = Data()
    data.add_new_cluster(cluster)

    print "complete"


def up_cluster(argv):
    """
    run all Shpurdp-agents in Docker container and VMs,
    run Shpurdp-server if there is according to the configuration file
    :param argv: sys.argv
    :return: None
    """
    if len(argv) < 3:
        print_help()
        exit(1)

    cluster_name = argv[2]
    cluster = Cluster.load_from_json(cluster_name)

    if cluster is None:
        print cluster_name, " cluster not found"
        exit(1)

    if cluster.state != Cluster.STATE_FREE:
        print cluster_name, " cluster is already running"
        exit(1)

    shpurdp_server = cluster.get_shpurdp_server_vm()
    if shpurdp_server is None:
        print "Unable to run cluster", cluster_name,\
            " no Shpurdp-server in this cluster, you can only merge this cluster into another one"
        exit(1)

    print "Configuring cluster"
    print "Check output folder: ", Config.ATTRIBUTES["output_folder"]

    cluster.run_cluster(shpurdp_server.weave_internal_ip, shpurdp_server.external_ip)
    data = Data()
    data.set_cluster_state(cluster_name, Cluster.STATE_RUNNING)

    # reset terminal. The SSH subprocess call of the program cause the terminal display to be abnormal.
    # This is an unsolved minor issue.
    subprocess.call(["reset"])

    print "Complete"


def merge_cluster(argv):
    """
    Merge the cluster to another running cluster
    :param argv: sys.argv
    :return: None
    """
    if len(argv) < 4:
        print_help()
        exit(1)

    merged_cluster_name = argv[2]
    merged_cluster = Cluster.load_from_json(merged_cluster_name)
    if merged_cluster is None:
        print merged_cluster_name, " cluster not found"
        exit(1)

    if merged_cluster.state != Cluster.STATE_FREE:
        print merged_cluster_name, " cluster is already running"
        exit(1)

    weave_ip = ""
    external_ip = ""
    extended_cluster_name = ""
    if len(argv) == 4:
        extended_cluster_name = argv[3]
        extended_cluster = Cluster.load_from_json(extended_cluster_name)
        if extended_cluster is None:
            print extended_cluster_name, " cluster not found"
            exit(1)

        if extended_cluster.state != Cluster.STATE_RUNNING:
            if extended_cluster.state == Cluster.STATE_FREE:
                print extended_cluster_name, " cluster is not running, can't be extended"
            elif extended_cluster.state.startswith(Cluster.STATE_MERGE):
                print extended_cluster_name, " cluster is merged to another cluster, can't be extended"
            exit(1)

        shpurdp_server = extended_cluster.get_shpurdp_server_vm()
        weave_ip = shpurdp_server.weave_internal_ip
        external_ip = shpurdp_server.external_ip

    elif len(argv) == 5:
        weave_ip = argv[3]
        external_ip = argv[4]

    else:
        print_help()
        exit(1)

    if merged_cluster.get_shpurdp_server_vm() is not None:
        print merged_cluster, " cluster has one VM to install Shpurdp-server, which will NOT be merged"

    print "Configuring cluster"
    print "Check output folder: ", Config.ATTRIBUTES["output_folder"]
    merged_cluster.run_cluster(weave_ip, external_ip)

    data = Data()
    data.set_cluster_state(merged_cluster_name, "{0} to {1}".format(Cluster.STATE_MERGE, extended_cluster_name))

    # reset terminal. The SSH subprocess call of the program cause the terminal display to be abnormal.
    # This is an unsolved minor issue.
    subprocess.call(["reset"])
    print "Complete"


def list_cluster():
    """
    list the cluster creation history
    :return: None
    """
    data = Data()
    data.print_cluster_summary_list()


def show_cluster(argv):
    """
    show detail information about a cluster
    :param argv: sys.argv
    :return: None
    """
    if len(argv) < 3:
        print_help()
        exit(1)
    cluster_name = argv[2]
    cluster = Cluster.load_from_json(cluster_name)

    if cluster is None:
        print cluster_name, " cluster not found"
        exit(1)

    cluster.print_description()


def print_help():
    """
    print help information
    :return: None
    """
    print "usage:"
    print

    print "request", "  ", "--request a cluster from GCE, generate the configuration for the cluster"
    print "\t\t", "<the name of the cluster>"
    print "\t\t", "<number of VMs>"
    print "\t\t", "<number of dockers each VM>"
    print "\t\t", "<number of service servers>, directly install Shpurdp-Agent, not inside Dockers"
    print "\t\t", "<number of shpurdp-server>, either 0 or 1"
    print

    print "up", "  ", "--run all Shpurdp-agents and Shpurdp-server of the cluster"
    print "\t\t", "<the name of the cluster>"
    print

    print "merge", "  ", "--run one cluster, and add to another cluster"
    print "\t\t", "<the name of the cluster to be merged>"
    print "\t\t", "<the name of the cluster to be extended>"
    print

    print "merge", "  ", "--run one cluster, and add to another cluster"
    print "\t\t", "<the name of the cluster to be merged>"
    print "\t\t", "<Weave IP of the Shpurdp-server>"
    print "\t\t", "<External IP of the Shpurdp-server>"
    print

    print "list", "  ", "--list all the cluster"
    print

    print "show", "  ", "--show cluster information"
    print "\t\t", "<the name of the cluster>"
    print

    print "help", "  ", "help info"
    print


def main(argv):
    # the first argument is the python file name
    if len(argv) < 2:
        print_help()
        exit(1)

    command = argv[1]

    if command == "request":
        request_cluster(argv)

    elif command == "up":
        up_cluster(argv)

    elif command == "merge":
        merge_cluster(argv)

    elif command == "list":
        list_cluster()

    elif command == "show":
        show_cluster(argv)

    elif command == "help":
        print_help()

    else:
        print_help()


if __name__ == "__main__":
    Config.load()
    main(sys.argv)
