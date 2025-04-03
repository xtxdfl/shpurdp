%define __python 3
%define __jar_repack 0
Name: shpurdp-agent
Version: 3.0.0.0
Release: SNAPSHOT
Summary: Shpurdp Agent
License: 2012, Apache Software Foundation
Vendor: Apache Software Foundation
URL: https://www.apache.org
Group: Development
Packager: Apache Software Foundation
Requires: openssl,
Requires: python3-rpm,
Requires: zlib,
Requires: net-tools,
Requires: python3-distro
autoprov: yes
autoreq: no
BuildRoot: /root/shpurdp/shpurdp-agent/target/rpm/shpurdp-agent/buildroot

%description
Maven Recipe: RPM Package.

%install

if [ -d $RPM_BUILD_ROOT ];
then
  mv /root/shpurdp/shpurdp-agent/target/rpm/shpurdp-agent/tmp-buildroot/* $RPM_BUILD_ROOT
else
  mv /root/shpurdp/shpurdp-agent/target/rpm/shpurdp-agent/tmp-buildroot $RPM_BUILD_ROOT
fi
chmod -R +w $RPM_BUILD_ROOT

%files

%attr(-,root,root)  "/etc/init.d/shpurdp-agent"
%attr(-,root,root)  "/etc/shpurdp-agent/conf/logging.conf.sample"
%attr(-,root,root)  "/etc/shpurdp-agent/conf/shpurdp-agent.ini"
%attr(-,root,root)  "/etc/init/shpurdp-agent.conf"
%attr(755,root,root) "/usr/lib/shpurdp-agent"
%attr(755,root,root) "/var/lib/shpurdp-agent"
%attr(644,root,root)  "/var/lib/shpurdp-agent/tools/zkmigrator.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/tools/jcepolicyinfo.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/guava-32.1.3-jre.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-io-2.8.0.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-lang-2.6.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-cli-1.3.1.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-configuration-1.6.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/hadoop-auth-2.7.3.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/slf4j-api-2.0.0.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-collections4-4.4.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/commons-logging-1.1.1.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/htrace-core-3.1.0-incubating.jar"
%attr(644,root,root)  "/var/lib/shpurdp-agent/cred/lib/hadoop-common-2.7.3.jar"
%attr(-,root,root) "/var/log/shpurdp-agent"
%attr(-,root,root) "/var/run/shpurdp-agent"

%pre
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License


do_backups(){
  local etc_dir="/etc/shpurdp-agent"
  local var_dir="/var/lib/shpurdp-agent"
  local sudoers_dir="/etc/sudoers.d"

  # format: title note source target
  local backup_folders="stack folders::${var_dir}/cache/stacks:${var_dir}/cache/stacks_$(date '+%d_%m_%y_%H_%M').old
common services folder::${var_dir}/cache/common-services:${var_dir}/cache/common-services_$(date '+%d_%m_%y_%H_%M').old
shpurdp-agent.ini::${etc_dir}/conf/shpurdp-agent.ini:${etc_dir}/conf/shpurdp-agent.ini.old
sudoers:Please restore the file if you were using it for shpurdp-agent non-root functionality:${sudoers_dir}/shpurdp-agent:${sudoers_dir}/shpurdp-agent.bak"

  echo "${backup_folders}" | while IFS=: read title notes source target; do
    if [ -e "${source}" ]; then
      echo -n "Moving ${title}: ${source} -> ${target}"

      if [ ! -z ${notes} ]; then
        echo ", ${notes}"
      else
        echo ""
      fi

      mv -f "${source}" "${target}"
    fi
  done
}

do_backups

exit 0

%post
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# Warning: don't add changes to this script directly, please add changes to install-helper.sh.

case "$1" in
  1) # Action install
    if [ -f "/var/lib/shpurdp-agent/install-helper.sh" ]; then
        /var/lib/shpurdp-agent/install-helper.sh install
    fi
  ;;
  2) # Action upgrade
    if [ -f "/var/lib/shpurdp-agent/install-helper.sh" ]; then
        /var/lib/shpurdp-agent/install-helper.sh upgrade
    fi
  ;;
esac

exit 0

%preun
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# WARNING: This script is performed not only on uninstall, but also
# during package update. See http://www.ibm.com/developerworks/library/l-rpm2/
# for details

if [ "$1" -eq 0 ]; then  # Action is uninstall
    if [ -f "/var/lib/shpurdp-agent/install-helper.sh" ]; then
      /var/lib/shpurdp-agent/install-helper.sh remove
    fi
fi

exit 0

%postun
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License


if [ "$1" -eq 0 ]; then  # Action is uninstall
    if [ -f "/var/lib/shpurdp-agent/install-helper.sh.orig" ]; then
      /var/lib/shpurdp-agent/install-helper.sh.orig cleanup
      rm -f /var/lib/shpurdp-agent/install-helper.sh.orig 1>/dev/null 2>&1
    fi
fi

exit 0

%posttrans
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License


SHPURDP_AGENT_BINARY="/etc/init.d/shpurdp-agent"
SHPURDP_AGENT_BINARY_SYMLINK="/usr/sbin/shpurdp-agent"

# setting shpurdp-agent binary symlink
if [ ! -f "${SHPURDP_AGENT_BINARY_SYMLINK}" ]; then
  ln -s "${SHPURDP_AGENT_BINARY}" "${SHPURDP_AGENT_BINARY_SYMLINK}"
fi

exit 0
