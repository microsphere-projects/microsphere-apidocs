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

package io.microsphere.apidocs.springfox.boot.autoconfigure;


import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * {@link SpringfoxAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SpringfoxAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = SpringfoxAutoConfiguration.class,
        properties = {
                "spring.profiles.active=dubbo-provider"
        },
        webEnvironment = RANDOM_PORT
)
@EnableAutoConfiguration
class SpringfoxAutoConfigurationTest {

//    private static FixedHostPortGenericContainer zookeeper;
//
//    @BeforeAll
//    static void beforeAll() {
//        zookeeper = (FixedHostPortGenericContainer) new FixedHostPortGenericContainer("zookeeper")
//                .withFixedExposedPort(2181, 2181)
//                .waitingFor(forLogMessage("Started ServerConnector", 1));
//        zookeeper.start();
//    }
//
//    @AfterAll
//    static void afterAll() {
//        zookeeper.stop();
//    }

    @Test
    void test() {

    }
}