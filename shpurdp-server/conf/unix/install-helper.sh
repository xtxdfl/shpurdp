#!/bin/bash
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
#                      SERVER INSTALL HELPER                     #
##################################################################

# WARNING. Please keep the script POSIX compliant and don't use bash extensions

ROOT_DIR_PATH="${RPM_INSTALL_PREFIX}"
ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'` # Customized folder, which shpurdp-server files are installed into ('/' or '' are default).
SHPURDP_UNIT="shpurdp-server"
ACTION=$1


OLD_PYLIB_PATH="${ROOT}/usr/lib/python2.6/site-packages"
OLD_PY_MODULES="shpurdp_commons;resource_management;shpurdp_jinja2;shpurdp_simplejson;shpurdp_server"

SHPURDP_SERVER_ROOT_DIR="${ROOT}/usr/lib/${SHPURDP_UNIT}"
SHPURDP_AGENT_ROOT_DIR="${ROOT}/usr/lib/shpurdp-agent"
SHPURDP_SERVER="${SHPURDP_SERVER_ROOT_DIR}/lib/shpurdp_server"

CA_CONFIG="${ROOT}/var/lib/${SHPURDP_UNIT}/keys/ca.config"
COMMON_DIR_SERVER="${ROOT}/usr/lib/${SHPURDP_UNIT}/lib/shpurdp_commons"
RESOURCE_MANAGEMENT_DIR_SERVER="${ROOT}/usr/lib/${SHPURDP_UNIT}/lib/resource_management"
JINJA_SERVER_DIR="${ROOT}/usr/lib/${SHPURDP_UNIT}/lib/shpurdp_jinja2"
SIMPLEJSON_SERVER_DIR="${ROOT}/usr/lib/${SHPURDP_UNIT}/lib/shpurdp_simplejson"
SHPURDP_PROPERTIES="${ROOT}/etc/${SHPURDP_UNIT}/conf/shpurdp.properties"
SHPURDP_ENV_RPMSAVE="${ROOT}/var/lib/${SHPURDP_UNIT}/shpurdp-env.sh.rpmsave" # this turns into shpurdp-env.sh during shpurdp-server start
SHPURDP_SERVER_KEYS_FOLDER="${ROOT}/var/lib/${SHPURDP_UNIT}/keys"
SHPURDP_SERVER_KEYS_DB_FOLDER="${ROOT}/var/lib/${SHPURDP_UNIT}/keys/db"
SHPURDP_SERVER_NEWCERTS_FOLDER="${ROOT}/var/lib/${SHPURDP_UNIT}/keys/db/newcerts"
CLEANUP_MODULES="resource_management;shpurdp_commons;shpurdp_server;shpurdp_ws4py;shpurdp_stomp;shpurdp_jinja2;shpurdp_simplejson"
SHPURDP_SERVER_VAR="${ROOT}/var/lib/${SHPURDP_UNIT}"
SHPURDP_HELPER="${ROOT}/var/lib/shpurdp-server/install-helper.sh.orig"

PYTHON_WRAPER_DIR="${ROOT}/usr/bin"
PYTHON_WRAPER_TARGET="${PYTHON_WRAPER_DIR}/shpurdp-python-wrap"

LOG_FILE=/dev/null

SHPURDP_SERVER_EXECUTABLE_LINK="${ROOT}/usr/sbin/shpurdp-server"
SHPURDP_SERVER_EXECUTABLE="${ROOT}/etc/init.d/shpurdp-server"

SHPURDP_CONFIGS_DIR="${ROOT}/etc/shpurdp-server/conf"
SHPURDP_CONFIGS_DIR_SAVE="${ROOT}/etc/shpurdp-server/conf.save"
SHPURDP_CONFIGS_DIR_SAVE_BACKUP="${ROOT}/etc/shpurdp-server/conf_$(date '+%d_%m_%y_%H_%M').save"
SHPURDP_LOG4J="${SHPURDP_CONFIGS_DIR}/log4j.properties"


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
  local lib_dir="${SHPURDP_SERVER_ROOT_DIR}/lib"

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

  find "${SHPURDP_SERVER_ROOT_DIR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done

  rm -rf ${SHPURDP_HELPER}
  find "${SHPURDP_SERVER_VAR}" -type d | tac | while read item; do
    echo "Removing empty dir ${item}..."
    rmdir --ignore-fail-on-non-empty ${item} 1>/dev/null 2>&1
  done
}

remove_autostart(){
   which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    chkconfig --list | grep shpurdp-server && chkconfig --del shpurdp-server
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    update-rc.d -f shpurdp-server remove
  fi
}

install_autostart(){
  local autostart_server_cmd=""
  which chkconfig > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    autostart_server_cmd="chkconfig --add shpurdp-server"
  fi
  which update-rc.d > /dev/null 2>&1
  if [ "$?" -eq 0 ] ; then
    autostart_server_cmd="update-rc.d shpurdp-server defaults"
  fi

  # if installed to customized root folder, skip shpurdp-server service actions,
  # as no file in /etc/init.d/shpurdp-server is present
  if [ ! "${ROOT}/" -ef "/" ]; then
    echo "Not adding shpurdp-server service to startup, as installed to customized root."
    echo "If you need this functionality run the commands below, which create shpurdp-server service and configure it to run at startup: "
    echo "sudo ln -s ${SHPURDP_SERVER_EXECUTABLE} /etc/init.d/shpurdp-server"
    echo "sudo ${autostart_server_cmd}"
  else
    ${autostart_server_cmd}
  fi
}

locate_python(){
  local python_binaries="/usr/bin/python3;/usr/bin/python3.9"

  echo ${python_binaries}| tr ';' '\n' | while read python_binary; do
    ${python_binary} -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (3,0)))" 1>>${LOG_FILE} 2>/dev/null

    if [ $? -eq 0 ]; then
      echo "${python_binary}"
      break
    fi
  done
}

do_install(){

  rm -f "${SHPURDP_SERVER_EXECUTABLE_LINK}"
  ln -s "${SHPURDP_SERVER_EXECUTABLE}" "${SHPURDP_SERVER_EXECUTABLE_LINK}"

  echo ${OLD_PY_MODULES} | tr ';' '\n' | while read item; do
   local old_path="${OLD_PYLIB_PATH}/${item}"
   if [ -d "${old_path}" ]; then
     echo "Removing old python module ${old_path}..."  1>>${LOG_FILE} 2>&1
     rm -rf ${old_path} 1>/dev/null 2>&1
   fi
  done

  # remove old python wrapper
  rm -f "${PYTHON_WRAPER_TARGET}"

  local shpurdp_python=$(locate_python)

  if [ -z "${shpurdp_python}" ]; then
    >&2 echo "Cannot detect Python for Shpurdp to use. Please manually set ${PYTHON_WRAPER_TARGET} link to point to correct Python binary"
  else
    mkdir -p "${PYTHON_WRAPER_DIR}"
    ln -s "${shpurdp_python}" "${PYTHON_WRAPER_TARGET}"
  fi

  sed -i "s|shpurdp.root.dir\s*=\s*/|shpurdp.root.dir=${ROOT_DIR_PATH}|g" "${SHPURDP_LOG4J}"
  sed -i "s|root_dir\s*=\s*/|root_dir = ${ROOT_DIR_PATH}|g" "${CA_CONFIG}"
  sed -i "s|^ROOT=\"/\"$|ROOT=\"${ROOT_DIR_PATH}\"|g" "${SHPURDP_SERVER_EXECUTABLE}"

  install_autostart |tee -a ${LOG_FILE}

  if [ -d "${SHPURDP_SERVER_KEYS_FOLDER}" ]; then
      chmod 700 "${SHPURDP_SERVER_KEYS_FOLDER}"
      if [ -d "${SHPURDP_SERVER_KEYS_DB_FOLDER}" ]; then
          chmod 700 "${SHPURDP_SERVER_KEYS_DB_FOLDER}"
          if [ -d "${SHPURDP_SERVER_NEWCERTS_FOLDER}" ]; then
              chmod 700 "${SHPURDP_SERVER_NEWCERTS_FOLDER}"
          fi
      fi
  fi

  if [ -f "${SHPURDP_ENV_RPMSAVE}" ]; then
    local python_path_line="export PYTHONPATH=${SHPURDP_SERVER_ROOT_DIR}/lib:\$\{PYTHONPATH\}"
    grep "^${python_path_line}\$" "${SHPURDP_ENV_RPMSAVE}" > /dev/null
    if [ $? -ne 0 ]; then
      echo -e "\n${python_path_line}" >> ${SHPURDP_ENV_RPMSAVE}
    fi
  fi
}

copy_helper(){
  local install_helper="${RPM_INSTALL_PREFIX}/var/lib/shpurdp-server/install-helper.sh"
  cp -f ${install_helper} ${SHPURDP_HELPER} 1>/dev/null 2>&1
}

do_remove(){
  ${SHPURDP_SERVER_EXECUTABLE} stop > /dev/null 2>&1

  if [ -d "${SHPURDP_CONFIGS_DIR_SAVE}" ]; then
    mv "${SHPURDP_CONFIGS_DIR_SAVE}" "${SHPURDP_CONFIGS_DIR_SAVE_BACKUP}"
  fi
  # part.1 Remove link created during install SHPURDP_ENV_RPMSAVE
  rm -f "${SHPURDP_SERVER_EXECUTABLE_LINK}"
  cp -rf "${SHPURDP_CONFIGS_DIR}" "${SHPURDP_CONFIGS_DIR_SAVE}"

  remove_autostart 1>>${LOG_FILE} 2>&1
  copy_helper
}


do_cleanup(){
  # do_cleanup is a function, which called after do_remove stage and is supposed to be save place to
  # remove obsolete files generated by application activity

  clean_pyc_files 1>>${LOG_FILE} 2>&1
  remove_shpurdp_unit_dir 1>>${LOG_FILE} 2>&1

  if [ ! -d "${SHPURDP_AGENT_ROOT_DIR}" ]; then
    echo "Removing ${PYTHON_WRAPER_TARGET} ..." 1>>${LOG_FILE} 2>&1
    rm -f ${PYTHON_WRAPER_TARGET} 1>>${LOG_FILE} 2>&1
  fi

  # part.2 Remove link created during install SHPURDP_ENV_RPMSAVE
  rm -rf "${SHPURDP_CONFIGS_DIR}" 1>>${LOG_FILE} 2>&1
}

do_backup(){
  # ToDo: find a way to move backup logic here from preinstall.sh and preinst scripts
  # ToDo: general problem is that still no files are installed on step, when backup is supposed to be done
  echo ""
}

do_upgrade(){
  # this function only gets called for rpm. Deb packages always call do_install directly.
  do_install
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
