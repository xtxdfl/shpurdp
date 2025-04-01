/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shpurdp.server.events;

/**
 * Event signaling the creation or changing of an Shpurdp configuration entry.
 */
public class ShpurdpConfigurationChangedEvent extends ShpurdpEvent {

  private final String categoryName;

  /**
   * Constructor.
   *
   */
  public ShpurdpConfigurationChangedEvent(String categoryName) {
    super(ShpurdpEventType.SHPURDP_CONFIGURATION_CHANGED);
    this.categoryName = categoryName;
  }

  public String getCategoryName() {
    return categoryName;
  }
}
