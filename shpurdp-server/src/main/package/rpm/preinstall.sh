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

ROOT=`echo "${RPM_INSTALL_PREFIX}" | sed 's|/$||g'`


do_backups(){
  local etc_dir="${ROOT}/etc/shpurdp-server"
  local var_dir="${ROOT}/var/lib/shpurdp-server"
  local usr_dir="${ROOT}/usr/lib/shpurdp-server"
  local usr_backup_dir="${ROOT}/usr/lib/shpurdp-server-backups"

  local stacks_backup_folder="${var_dir}/resources/stacks_$(date '+%d_%m_%y_%H_%M').old"
  local common_service_backup_folder="${var_dir}/resources/common-services_$(date '+%d_%m_%y_%H_%M').old"

  #  backup configuration

  # data format:  "title:source:destination"; each new record on new line
  local backup_folders="configs:${etc_dir}/conf.save:${etc_dir}/conf_$(date '+%d_%m_%y_%H_%M').save
Shpurdp properties:${etc_dir}/conf/shpurdp.properties:${etc_dir}/conf/shpurdp.properties.rpmsave
Shpurdp properties:${var_dir}/shpurdp-env.sh:${var_dir}/shpurdp-env.sh.rpmsave
JAAS login file:${etc_dir}/conf/krb5JAASLogin.conf:${etc_dir}/conf/krb5JAASLogin.conf.rpmsave
stacks directory:${var_dir}/resources/stacks:${stacks_backup_folder}
common-services directory:${var_dir}/resources/common-services:${common_service_backup_folder}"

 echo "${backup_folders}"| while IFS=: read title source destination; do
   if [ -e "${source}" ]; then
     echo "Backing up ${title}: ${source} -> ${destination}"
     mv -f "${source}" "${destination}"
   fi
 done

  # backup mpacks

  local mpacks_folder="${ROOT}/var/lib/shpurdp-server/resources/mpacks"
  local mpacks_folder_old=${ROOT}/var/lib/shpurdp-server/resources/mpacks_$(date '+%d_%m_%y_%H_%M').old

  if [ -d "${mpacks_folder}" ]; then
    # Make a copy of mpacks folder
    if [ ! -d "${mpacks_folder_old}" ]; then
      echo "Backing up mpacks directory: ${mpacks_folder} -> ${mpacks_folder_old}"
      cp -R "${mpacks_folder}" "${mpacks_folder_old}"
    fi

    local symlink_update_folders="${stacks_backup_folder};${common_service_backup_folder}"

    echo ${symlink_update_folders}| tr ';' '\n'| while read item; do
      if [ -d "${item}" ]; then
        for link in $(find "${item}" -type l); do
          local target=`readlink ${link}`
          echo ${target}|grep -q "${mpacks_folder}/" 1>/dev/null 2>&1
          if [ $? -eq 0 ]; then
            local new_target="${target/$mpacks_folder/$mpacks_folder_old}"
            echo "Updating symlink ${link} -> ${new_target}"
            ln -snf ${new_target} ${link}
          fi
        done
      fi
    done
  fi

  # backup Shpurdp Views

  local shpurdp_views_folder="${var_dir}/resources/views"
  local shpurdp_views_backup_folder="${shpurdp_views_folder}/backups"

  if [ ! -d "${shpurdp_views_backup_folder}" ] && [ -d "${shpurdp_views_folder}" ]; then
    mkdir "${shpurdp_views_backup_folder}"
  fi

  ls ${shpurdp_views_folder}/*.jar > /dev/null 2>&1
  local jars_exist=$?
  if [ -d "${shpurdp_views_folder}" ] && [ -d "${shpurdp_views_backup_folder}" ] && [ ${jars_exist} -eq 0 ]; then
      echo "Backing up Shpurdp view jars: ${shpurdp_views_folder}/*.jar -> ${shpurdp_views_backup_folder}/"
      cp -u ${shpurdp_views_folder}/*.jar "${shpurdp_views_backup_folder}/" 1>/dev/null 2>&1
  fi

  # backup Shpurdp Server Jar

  local shpurdp_server_jar_files=${usr_dir}/shpurdp-server-*.jar
  local shpurdp_server_jar_files_backup_folder="${usr_backup_dir}"

  for f in ${shpurdp_server_jar_files}; do
      if [ -f "${f}" ]; then
          if [ ! -d "${shpurdp_server_jar_files_backup_folder}" ]; then
              mkdir -p "${shpurdp_server_jar_files_backup_folder}"
          fi
          echo "Backing up Shpurdp server jar: ${f} -> ${shpurdp_server_jar_files_backup_folder}/"
          mv -f "${f}" "${shpurdp_server_jar_files_backup_folder}/"
      fi
  done
}

do_backups
