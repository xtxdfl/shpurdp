/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.state.kerberos;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.shpurdp.server.ShpurdpException;

import com.google.inject.Singleton;

/**
 * KerberosDescriptorFactory is a factory class used to create KerberosDescriptor instances using
 * various sources of data.
 */
@Singleton
public class KerberosDescriptorFactory extends AbstractKerberosDescriptorFactory {

  /**
   * Given a file containing JSON-formatted text, attempts to create a KerberosDescriptor
   *
   * @param file a File pointing to the file containing JSON-formatted text
   * @return a newly created KerberosDescriptor
   * @throws java.io.FileNotFoundException            if the specified File does not point to a valid file
   * @throws java.io.IOException                      if the specified File is not a readable file
   * @throws org.apache.shpurdp.server.ShpurdpException if the specified File does not contain valid JSON data
   */
  public KerberosDescriptor createInstance(File file) throws IOException {
    try {
      return new KerberosDescriptor(parseFile(file));
    } catch (ShpurdpException e) {
      throw new ShpurdpException(String.format("An error occurred processing the JSON-formatted file: %s", file.getAbsolutePath()), e);
    }
  }

  /**
   * Given a String containing JSON-formatted text, attempts to create a KerberosDescriptor
   *
   * @param json a File pointing to the file containing JSON-formatted text
   * @return a newly created KerberosDescriptor
   * @throws ShpurdpException if an error occurs while processing the JSON-formatted String
   */
  public KerberosDescriptor createInstance(String json) throws ShpurdpException {
    try {
      return new KerberosDescriptor(parseJSON(json));
    } catch (ShpurdpException e) {
      throw new ShpurdpException("An error occurred processing the JSON-formatted string", e);
    }
  }

  /**
   * Creates a new KerberosDescriptor
   * <p/>
   * See {@link org.apache.shpurdp.server.state.kerberos.KerberosDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param map a Map of values use to populate the data for the new instance
   * @see org.apache.shpurdp.server.state.kerberos.KerberosDescriptor
   */
  public KerberosDescriptor createInstance(Map<?, ?> map) {
    return new KerberosDescriptor(map);
  }

}
