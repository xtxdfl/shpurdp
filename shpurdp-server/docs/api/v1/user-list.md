
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

List Users
=====

[Back to User Resources](user-resources.md)

**Summary**

Returns a collection of the existing Users.

    GET /users

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>

**Example**

Get the collection of all currently stored users.

    GET /users

    200 OK
    {
      "href" : "http://your.shpurdp.server/api/v1/users",
      "items" : [
        {
          "href" : "http://your.shpurdp.server/api/v1/users/admin",
          "Users" : {
            "user_name" : "admin"
          }
        },
        {
          "href" : "http://your.shpurdp.server/api/v1/users/jdoe",
          "Users" : {
            "user_name" : "jdoe"
          }
        },
        {
          "href" : "http://your.shpurdp.server/api/v1/users/jsmith",
          "Users" : {
            "user_name" : "jsmith"
          }
        },
        {
          "href" : "http://your.shpurdp.server/api/v1/users/jqpublic",
          "Users" : {
            "user_nane" : "jqpublic"
          }
        }
      ]
    }