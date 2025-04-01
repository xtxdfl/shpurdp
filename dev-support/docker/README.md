<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->


how to build
--------------------

```
docker build -t shpurdp/build ./dev-support/docker/docker
```

how to run
--------------------

```
# bash
docker run --privileged -t -i -p 80:80 -p 5005:5005 -p 8080:8080 -h node1.mydomain.com --name shpurdp1 -v ${SHPURDP_SRC:-$(pwd)}:/tmp/shpurdp shpurdp/build bash
# where 5005 is java debug port and 8080 is the default http port, if no --privileged shpurdp-server start fails due to access to /proc/??/exe
# -t is required otherwise, sudo commands do not run

# build, install shpurdp and deploy hadoop in container
cd {shpurdp src}
docker rm shpurdp1
docker run --privileged -t -p 80:80 -p 5005:5005 -p 8080:8080 -h node1.mydomain.com --name shpurdp1 -v ${SHPURDP_SRC:-$(pwd)}:/tmp/shpurdp shpurdp/build /tmp/shpurdp-build-docker/bin/shpurdpbuild.py [test|server|agent|deploy] [-b] [-s [HDP|BIGTOP|PHD]] [-d] [-c]
where
test: mvn test
server: install and run shpurdp-server
agent: install and run shpurdp-server and shpurdp-agent
deploy: install and run shpurdp-server and shpurdp-agent, and deploy a hadoop
-b option to rebuild shpurdp
-d option to start shpurdp-server with --debug option
-c option to clean local git repo. "git clean -xdf"
```

how to run unit test
--------------------
```
cd dev-support/docker/docker
python -m bin.test.shpurdpbuild_test

```

