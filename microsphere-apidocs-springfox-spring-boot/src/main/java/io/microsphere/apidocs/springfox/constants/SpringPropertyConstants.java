/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microsphere.apidocs.springfox.constants;

import io.microsphere.annotation.ConfigurationProperty;

/**
 * The constants for Spring Property
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface SpringPropertyConstants {

    /**
     * The property name prefix of Microsphere Springfox
     */
    String PROPERTY_NAME_PREFIX = "microsphere.springfox.";

    /**
     * The property name of Microsphere Springfox 'enabled' status
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = "true"
    )
    String SPRING_FOX_ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "enabled";
}
