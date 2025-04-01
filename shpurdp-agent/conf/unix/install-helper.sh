# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
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
# limitations under the License.


##################################################################
#                      AGENT INSTALL HELPER                      #
##################################################################

# WARNING. Please keep the script POSIX compliant and don't use bash extensions

SHPURDP_UNIT="shpurdp-agent"
ACTION=$1
SHPURDP_AGENT_ROOT_DIR="/usr/lib/${SHPURDP_UNIT}"
SHPURDP_SERVER_ROOT_DIR="/usr/lib/shpurdp-server"
COMMON_DIR="${SHPURDP_AGENT_ROOT_DIR}/lib/shpurdp_commons"
RESOURCE_MANAGEMENT_DIR="${SHPURDP_AGENT_ROOT_DIR}/lib/resource_management"
JINJA_DIR="${SHPURDP_AGENT_ROOT_DIR}/lib/shpurdp_jinja2"
SIMPLEJSON_DIR="${SHPURDP_AGENT_ROOT_DIR}/lib/shpurdp_simplejson"
OLD_OLD_COMMON_DIR="${SHPURDP_AGENT_ROOT_DIR}/lib/common_functions"
SHPURDP_AGENT="${SHPURDP_AGENT_ROOT_DIR}/lib/shpurdp_agent"
PYTHON_WRAPER_TARGET="/usr/bin/shpurdp-python-wrap"
SHPURDP_AGENT_VAR="/var/lib/${SHPURDP_UNIT}"
SHPURDP_AGENT_BINARY="/etc/init.d/${SHPURDP_UNIT}"
SHPURDP_AGENT_BINARY_SYMLINK="/usr/sbin/${SHPURDP_UNIT}"
SHPURDP_ENV_RPMSAVE="/var/lib/${SHPURDP_UNIT}/shpurdp-env.sh.rpmsave"
SHPURDP_HELPER="/var/lib/shpurdp-agent/install-helper.sh.orig"

LOG_FILE=/dev/null

CLEANUP_MODULES="resource_management;shpurdp_commons;shpurdp_agent;shpurdp_ws4py;shpurdp_stomp;shpurdp_jinja2;shpurdp_simplejson"

OLD_COMMON_DIR="/usr/lib/python2.6/site-packages/shpurdp_commons"
OLD_RESOURCE_MANAGEMENT_DIR="/usr/lib/python2.6/site-packages/resource_management"
OLD_JINJA_DIR="/usr/lib/python2.6/site-packages/shpurdp_jinja2"
OLD_SIMPLEJSON_DIR="/usr/lib/python2.6/site-packages/shpurdp_simplejson"
OLD_SHPURDP_AGENT_DIR="/usr/lib/python2.6/site-packages/shpurdp_agent"


resolve_log_file(){
 local log_dir=/var/log/${SHPURDP_UNIT}
 local log_file="${log_dir}/${SHPURDP_UNIT}-pkgmgr.log"

 if [ ! -d "${log_dir}" ]; then
   mkdir "${log_dir}" 1>/dev/null 2>&1
 fi

 if [ -d "${log_dir}" ]; then
   touch ${log_file} 1>/dev/null 2>&1
   if [ -f "${log_file}" ]; then
    LOG_FILE="${log_file}"
   fi
 fi

 echo "--> Install-helper custom action log started at $(date '+%d/%m/%y %H:%M') for '${ACTION}'" 1>>${LOG_FILE} 2>&1
}

clean_pyc_files(){
  # cleaning old *.pyc files
  local lib_dir="${SHPURDP_AGENT_ROOT_DIR}/lib"

  echo ${CLEANUP_MODULES} | tr ';' '\n' | while read item; do
    local item="${lib_dir}/${item}"
    echo "Cleaning pyc files from ${item}..."
    if [ -d "${item}" ]; then
      find ${item:?} -name *.pyc -exec rm {} \; 1>>${LOG_FILE} 2>&1
    else
      echo "Skipping ${item} pyc cleaning, as package not existing"
    fi
  done
}

remove_shpurdp_unit_dir(){
  # removing empty dirs, which left after cleaning pyc files

  find "${SHPURDP_AGENT_ROOT_DIR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done

  rm -rf ${SHPURDP_HELPER}
  find "${SHPURDP_AGENT_VAR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done
}

remove_autostart(){
  which chkconfig
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep shpurdp-server && chkconfig --del shpurdp-agent
  fi
  which update-rc.d
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f shpurdp-agent remove
  fi
}

install_autostart(){
  which chkconfig 1>>${LOG_FILE} 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --add shpurdp-agent
  fi
  which update-rc.d 1>>${LOG_FILE} 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d shpurdp-agent defaults
  fi
}

locate_python(){
  local python_binaries="/usr/bin/python;/usr/bin/python3;/usr/bin/python3.9"

  echo ${python_binaries}| tr ';' '\n' | while read python_binary; do
    ${python_binary} -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (3,0)))" 1>>${LOG_FILE} 2>/dev/null

    if [ $? -eq 0 ]; then
      echo "${python_binary}"
      break
    fi
  done
}

do_install(){
  if [ -d "/etc/shpurdp-agent/conf.save" ]; then
    cp -f /etc/shpurdp-agent/conf.save/* /etc/shpurdp-agent/conf
    mv /etc/shpurdp-agent/conf.save /etc/shpurdp-agent/conf_$(date '+%d_%m_%y_%H_%M').save
  fi

  # these symlinks (or directories) where created in shpurdp releases prior to shpurdp-2.6.2. Do clean up.   
  rm -rf "${OLD_COMMON_DIR}" "${OLD_RESOURCE_MANAGEMENT_DIR}" "${OLD_JINJA_DIR}" "${OLD_SIMPLEJSON_DIR}" "${OLD_OLD_COMMON_DIR}" "${OLD_SHPURDP_AGENT_DIR}"

  # setting up /usr/sbin/shpurdp-agent symlink
  rm -f "${SHPURDP_AGENT_BINARY_SYMLINK}"
  ln -s "${SHPURDP_AGENT_BINARY}" "${SHPURDP_AGENT_BINARY_SYMLINK}"

  # on nano Ubuntu, when umask=027 those folders are created without 'x' bit for 'others'.
  # which causes failures when hadoop users try to access tmp_dir
  chmod a+x ${SHPURDP_AGENT_VAR}

  chmod 1777 ${SHPURDP_AGENT_VAR}/tmp
  chmod 700 ${SHPURDP_AGENT_VAR}/keys
  chmod 700 ${SHPURDP_AGENT_VAR}/data

  install_autostart 1>>${LOG_FILE} 2>&1

  # remove old python wrapper
  rm -f "${PYTHON_WRAPER_TARGET}"

  local shpurdp_python=$(locate_python)
  local bak=/etc/shpurdp-agent/conf/shpurdp-agent.ini.old
  local orig=/etc/shpurdp-agent/conf/shpurdp-agent.ini
  local upgrade_agent_configs_script=/var/lib/shpurdp-agent/upgrade_agent_configs.py

  if [ -z "${shpurdp_python}" ] ; then
    >&2 echo "Cannot detect python for Shpurdp to use. Please manually set ${PYTHON_WRAPER_TARGET} link to point to correct python binary"
    >&2 echo "Cannot upgrade agent configs because python for Shpurdp is not configured. The old config file is saved as ${bak} . Execution of ${upgrade_agent_configs_script} was skipped."
  else
    ln -s "${shpurdp_python}" "${PYTHON_WRAPER_TARGET}"

    if [ -f ${bak} ]; then
      if [ -f "${upgrade_agent_configs_script}" ]; then
        ${upgrade_agent_configs_script}
      fi
      mv ${bak} ${bak}_$(date '+%d_%m_%y_%H_%M').save
    fi
  fi

  if [ -f "${SHPURDP_ENV_RPMSAVE}" ] ; then
    PYTHON_PATH_LINE="export PYTHONPATH=${SHPURDP_AGENT_ROOT_DIR}/lib:\$\{PYTHONPATH\}"
    grep "^${PYTHON_PATH_LINE}\$" "${SHPURDP_ENV_RPMSAVE}" >>${LOG_FILE}
    if [ $? -ne 0 ] ; then
      echo -e "\n${PYTHON_PATH_LINE}" 1>>${SHPURDP_ENV_RPMSAVE}
    fi
  fi
}

copy_helper(){
  cp -f /var/lib/shpurdp-agent/install-helper.sh ${SHPURDP_HELPER} 1>/dev/null 2>&1
}

do_remove(){
  /usr/sbin/shpurdp-agent stop 1>>${LOG_FILE} 2>&1

  rm -f "${SHPURDP_AGENT_BINARY_SYMLINK}" 1>>${LOG_FILE} 2>&1

  if [ -d "/etc/shpurdp-agent/conf.save" ]; then
    mv /etc/shpurdp-agent/conf.save /etc/shpurdp-agent/conf_$(date '+%d_%m_%y_%H_%M').save
  fi
  # first step / label: config_backup
  cp -rf /etc/shpurdp-agent/conf /etc/shpurdp-agent/conf.save

  remove_autostart 1>>${LOG_FILE} 2>&1
  copy_helper 1>>${LOG_FILE} 2>&1
}

do_cleanup(){
  # do_cleanup is a function, which called after do_remove stage and is supposed to be save place to
  # remove obsolete files generated by application activity

  clean_pyc_files 1>>${LOG_FILE} 2>&1

  # second step / label: config_backup
  rm -rf /etc/shpurdp-agent/conf

  if [ ! -d "${SHPURDP_SERVER_ROOT_DIR}" ]; then
    echo "Removing ${PYTHON_WRAPER_TARGET} ..." 1>>${LOG_FILE} 2>&1
    rm -f ${PYTHON_WRAPER_TARGET} 1>>${LOG_FILE} 2>&1
  fi

  remove_shpurdp_unit_dir 1>>${LOG_FILE} 2>&1
}

do_upgrade(){
  do_install
}

do_backup(){
  # ToDo: find a way to move backup logic here from preinstall.sh and preinst scripts
  # ToDo: general problem is that still no files are installed on step, when backup is supposed to be done
  echo ""
}

resolve_log_file

case "${ACTION}" in
  install)
    do_install
    ;;
  remove)
    do_remove
    ;;
  upgrade)
    do_upgrade
    ;;
  cleanup)
    do_cleanup
    ;;
  *)
    echo "Wrong command given"
    ;;
esac
