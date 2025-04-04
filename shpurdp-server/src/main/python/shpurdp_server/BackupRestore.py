#!/usr/bin/env shpurdp-python-wrap
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
import zipfile
import os
import logging
from shpurdp_server.shpurdpPath import ShpurdpPath

# Default values are hardcoded here
BACKUP_PROCESS = "backup"
RESTORE_PROCESS = "restore"
SUPPORTED_PROCESSES = [BACKUP_PROCESS, RESTORE_PROCESS]

# The list of files where the shpurdp server state is kept on the filesystem
SHPURDP_FILESYSTEM_STATE = [
  ShpurdpPath.get("/etc/shpurdp-server/conf"),
  ShpurdpPath.get("/var/lib/shpurdp-server/resources"),
  ShpurdpPath.get("/var/run/shpurdp-server/bootstrap/"),
  ShpurdpPath.get("/var/run/shpurdp-server/stack-recommendations"),
]

# What to use when no path/archive is specified
DEFAULT_ARCHIVE = ShpurdpPath.get("/var/lib/shpurdp-server/Shpurdp_State_Backup.zip")


# Responsible for managing the Backup/Restore process
class BackupRestore:
  def __init__(self, state_file_list, zipname, zip_folder_path):
    """
    Zip file creator
    :param state_file_list: the list of files where the Shpurdp State is kept on the filesystem
    :param zipname: the name of the archive to use
    :param zip_folder_path: the path of the archive
    :return:
    """
    self.state_file_list = state_file_list
    self.zipname = zipname
    self.zip_folder_path = zip_folder_path

  def perform_backup(self):
    """
    Used to perform the actual backup, by creating the zip archive
    :return:
    """
    try:
      print("Creating zip file...")
      # Use allowZip64=True to allow sizes greater than 4GB
      zipf = zipfile.ZipFile(self.zip_folder_path + self.zipname, "w", allowZip64=True)
      zipdir(zipf, self.state_file_list, self.zipname)
    except Exception as e:
      sys.exit("Could not create zip file. Details: " + str(e))

    print("Zip file created at " + self.zip_folder_path + self.zipname)

  def perform_restore(self):
    """
    Used to perform the restore process
    :return:
    """
    try:
      print("Extracting the archive " + self.zip_folder_path + self.zipname)
      unzip(self.zip_folder_path + self.zipname, "/")
    except Exception as e:
      sys.exit(
        "Could not extract the zipfile "
        + self.zip_folder_path
        + self.zipname
        + " Details: "
        + str(e)
      )


def unzip(source_filename, dest_dir):
  """
  Zip archive extractor
  :param source_filename: the absolute path of the file to unzip
  :param dest_dir: the destination of the zip content
  :return:
  """
  zf = zipfile.ZipFile(source_filename)
  try:
    zf.extractall(dest_dir)
  except Exception as e:
    print("A problem occurred while unzipping. Details: " + str(e))
    raise e
  finally:
    zf.close()


def zipdir(zipf, state_file_list, zipname):
  """
  Used to archive the specified directory
  :param zipf: the zipfile
  :param state_file_list: the file list to archive
  :param zipname: the name of the zip
  :return:
  """
  try:
    for path in state_file_list:
      for root, dirs, files in os.walk(path):
        for file in files:
          if not file == zipname:
            zipf.write(os.path.join(root, file))
  except Exception as e:
    print("A problem occurred while unzipping. Details: " + str(e))
    raise e
  finally:
    zipf.close()


def print_usage():
  """
  Usage instructions
  :return:
  """
  print(
    "Usage: python BackupRestore.py <processType> [zip-folder-path|zip-file-path]\n\n"
    + "    processType - backup : backs up the filesystem state of the Shpurdp server into a zip file\n"
    + "    processType - restore : restores the filesystem state of the Shpurdp server\n"
    + "    [zip-folder-path] used with backup specifies the path of the folder where the zip file to be created\n"
    + "    [zip-folder-path] used with restore specifies the path of the Shpurdp folder where the zip file to restore from is located\n"
  )


def validate_folders(folders):
  """
  Used to validate folder existence on the machine
  :param folders: folder list containing paths to validate
  :return:
  """
  for folder in folders:
    if not os.path.isdir(folder):
      sys.exit("Error while validating folders. Folder " + folder + " does not exist.")


def retrieve_path_and_zipname(archive_absolute_path):
  target = {"path": None, "zipname": None}
  try:
    elements = archive_absolute_path.split("/")
    if elements is not None and len(elements) > 0:
      target["zipname"] = elements[len(elements) - 1]
      target["path"] = archive_absolute_path.replace(elements[len(elements) - 1], "")
  except Exception as e:
    sys.exit(
      "Could not retrieve path and zipname from the absolute path "
      + archive_absolute_path
      + ". Please check arguments."
      + " Details: "
      + str(e)
    )

  return target


def main(argv=None):
  # Arg checks
  if len(argv) != 3 and len(argv) != 2:
    print_usage()
    sys.exit("Invalid usage.")
  else:
    process_type = argv[1]
    if not (SUPPORTED_PROCESSES.__contains__(process_type)):
      sys.exit("Unsupported process type: " + process_type)
    # if no archive is specified
    if len(argv) == 2:
      print("No path specified. Will use " + DEFAULT_ARCHIVE)
      location_data = retrieve_path_and_zipname(DEFAULT_ARCHIVE)
    else:
      location_data = retrieve_path_and_zipname(argv[2])

    validate_folders([location_data["path"]])
    zip_file_path = location_data["path"]
    shpurdp_backup_zip_filename = location_data["zipname"]

  backup_restore = BackupRestore(
    SHPURDP_FILESYSTEM_STATE, shpurdp_backup_zip_filename, zip_file_path
  )

  print(process_type.title() + " process initiated.")
  if process_type == BACKUP_PROCESS:
    validate_folders(SHPURDP_FILESYSTEM_STATE)
    backup_restore.perform_backup()
    print(BACKUP_PROCESS.title() + " complete.")
  if process_type == RESTORE_PROCESS:
    backup_restore.perform_restore()
    print(RESTORE_PROCESS.title() + " complete.")


if __name__ == "__main__":
  main(sys.argv)
