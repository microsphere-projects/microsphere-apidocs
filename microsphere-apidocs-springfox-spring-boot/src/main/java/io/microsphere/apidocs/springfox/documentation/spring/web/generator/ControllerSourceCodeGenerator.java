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

package io.microsphere.apidocs.springfox.documentation.spring.web.generator;

import org.springframework.stereotype.Controller;

/**
 * The Source Code Generator for Spring Web {@link Controller}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Controller
 * @since 1.0.0
 */
public interface ControllerSourceCodeGenerator {

    /**
     * Generate the source code for {@link Controller}
     *
     * @param interfaceClass     the class of interface
     * @param interfaceImplClass the implementation class of interface
     * @param injectedBeanName   the name of bean will be injected
     * @return the source code for {@link Controller}
     */
    String generate(Class<?> interfaceClass, Class<?> interfaceImplClass, String injectedBeanName);
}
