#!/bin/bash
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
# limitations under the License.
# Exit immediately if a command exits with a non-zero status
set -e
# Set Shpurdp passphrase
SHPURDP_PASSPHRASE="DEV"

# Set JVM arguments for Shpurdp
SHPURDP_JVM_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED "
SHPURDP_JVM_ARGS+="--add-opens java.base/java.util.regex=ALL-UNNAMED "
SHPURDP_JVM_ARGS+="--add-opens java.base/java.util=ALL-UNNAMED "
SHPURDP_JVM_ARGS+="--add-opens java.base/java.lang.reflect=ALL-UNNAMED "
SHPURDP_JVM_ARGS+="-Xms512m -Xmx2048m "
SHPURDP_JVM_ARGS+="-Djava.security.auth.login.config=$ROOT/etc/shpurdp-server/conf/krb5JAASLogin.conf "
SHPURDP_JVM_ARGS+="-Djava.security.krb5.conf=/etc/krb5.conf "
SHPURDP_JVM_ARGS+="-Djavax.security.auth.useSubjectCredsOnly=false "
SHPURDP_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.protocol=\"plain ssl\" "
SHPURDP_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.maxsize=20 "
SHPURDP_JVM_ARGS+="-Dcom.sun.jndi.ldap.connect.pool.timeout=300000"
export SHPURDP_JVM_ARGS

# Update PATH to include Shpurdp server directory
export PATH="$PATH:$ROOT/var/lib/shpurdp-server"

# Set Python path for Shpurdp server
export PYTHONPATH="/usr/lib/shpurdp-server/lib:$PYTHONPATH"

# Additional server classpath can be set using SERVER_CLASSPATH
# Uncomment the following line to add additional directories or jars
# export SERVER_CLASSPATH=/etc/hadoop/conf/secure