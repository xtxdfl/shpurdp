
<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Delete Authentication Source
=====

[Back to Authentication Source Resources](authentication-source-resources.md)

**Summary**

Removes an existing authentication source resource identified by <code>:source_id</code> for a user
identified by <code>:user_name</code>. 
<p/><p/>
Only users with the <code>SHPURDP.MANAGE_USERS</code> privilege (currently, Shpurdp Administrators)
may perform this operation.

    DELETE /users/:user_name/source/:source_id

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr>
</table>
