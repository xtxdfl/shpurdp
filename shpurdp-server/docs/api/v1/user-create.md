
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

Create User
=====

[Back to User Resources](user-resources.md)

**Summary**

Create a new user resource identified by <code>:user_name</code>. 
<p/><p/>
Only users with the <code>SHPURDP.MANAGE_USERS</code> privilege (currently, Shpurdp Administrators)
may perform this operation.

    POST /users/:user_name

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
    <td>The authenticated user does not have authorization to create/store user persisted data.</td>  
  </tr>
</table>


**Examples*
    
Create a user with a username of "jdoe".
    
    POST /users/jdoe
    
    {
      "Users": {
        "local_user_name": "jdoe",
        "display_name": "Jane Doe",
        "admin" : false
      }
    }

    201 Created
    

Create multiple users.
    
    POST /users
    
    [
      {
        "Users": {
          "user_name": "UserA",
          "admin": "true"
        }
      },
      {
        "Users": {
          "user_name": "userb",
          "active": "false"
        }
      },
      {
        "Users": {
          "user_name": "userc",
          "local_user_name": "UserC"
        }
      },
      {
        "Users": {
          "user_name": "userd",
          "local_user_name": "userD",
          "display_name": "User D"
        }
      },
      {
        "Users": {
          "user_name": "usere",
          "password": "hadoop"
        }
      }
    ]
    
    201 Created
        