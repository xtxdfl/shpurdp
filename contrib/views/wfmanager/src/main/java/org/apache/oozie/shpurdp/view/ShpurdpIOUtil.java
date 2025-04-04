/**
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
package org.apache.oozie.shpurdp.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.shpurdp.view.URLStreamProvider;
import org.apache.shpurdp.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShpurdpIOUtil {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(ShpurdpIOUtil.class);
	private ViewContext viewContext;

	public ShpurdpIOUtil(ViewContext viewContext) {
		super();
		this.viewContext = viewContext;
	}

	public InputStream readFromUrl(String urlToRead, String method,
			String body, Map<String, String> newHeaders) {
		URLStreamProvider streamProvider = viewContext.getURLStreamProvider();
		InputStream stream = null;
		try {
			if (isSecurityEnabled()) {
				stream = streamProvider.readAsCurrent(urlToRead, method, body,
						newHeaders);

			} else {
				stream = streamProvider.readFrom(urlToRead, method, body,
						newHeaders);
			}
		} catch (IOException e) {
			LOGGER.error("error talking to oozie", e);
			throw new RuntimeException(e);
		}
		return stream;
	}

	private boolean isSecurityEnabled() {
		String securityEnbaled = viewContext.getProperties()
					             .get("hadoop.security.authentication");
		return !"simple".equalsIgnoreCase(securityEnbaled);
	}
}
