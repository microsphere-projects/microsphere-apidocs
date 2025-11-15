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

package io.microsphere.apidocs.springfox.documentation.dubbo.client;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.utils.PathUtils.normalize;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.util.ClassUtils.getUserClass;
import static org.springframework.util.StringUtils.hasText;

/**
 * Dubbo Client
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class DubboClient implements EnvironmentAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DubboClient.class);

    private static final String DUBBO_PORT_PROPERTY_NAME = "dubbo.protocol.port";

    private List<ReferenceConfig> referenceConfigs = new LinkedList<>();

    private Map<Class<?>, Object> proxiesCache = new HashMap<>();

    private ClassLoader classLoader;

    private String referenceBaseURL;

    public <T> T getProxy(Class<T> interfaceClass, Object dubboServiceBean) {
        Object proxy = proxiesCache.computeIfAbsent(interfaceClass, type -> buildProxy(interfaceClass, dubboServiceBean));
        return interfaceClass.cast(proxy);
    }

    private Object buildProxy(Class<?> interfaceClass, Object dubboServiceBean) {
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-client");

        Class<?> dubboServiceBeanClass = dubboServiceBean.getClass();
        Class<?> interfaceImplClass = getUserClass(dubboServiceBeanClass);
        Map<String, Object> dubboServiceAttributes = getDubboServiceAttributes(interfaceImplClass);
        String path = (String) dubboServiceAttributes.get(PATH_KEY);
        String group = (String) dubboServiceAttributes.get(GROUP_KEY);
        String version = (String) dubboServiceAttributes.getOrDefault(VERSION_KEY, "1.0.0");

        String url = buildReferenceURL(path);
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setInterface(interfaceClass);
        referenceConfig.setUrl(url);
        if (hasText(group)) {
            referenceConfig.setGroup(group);
        }
        if (hasText(version)) {
            referenceConfig.setVersion(version);
        }
        // Add ReferenceConfig
        referenceConfigs.add(referenceConfig);

        // Get the proxy
        Object proxy = referenceConfig.get();

        logger.info("The Dubbo proxy[interface : '{}' , implementation : '{}' ] was built , ReferenceConfig : {}", interfaceClass.getName(),
                dubboServiceBeanClass.getName(), referenceConfig);
        return proxy;
    }

    private Map<String, Object> getDubboServiceAttributes(Class<?> interfaceImplClass) {
        Annotation annotation = interfaceImplClass.getAnnotation(DubboService.class);
        if (annotation == null) {
            annotation = interfaceImplClass.getAnnotation(Service.class);
        }
        if (annotation == null) {
            annotation = interfaceImplClass.getAnnotation(com.alibaba.dubbo.config.annotation.Service.class);
        }
        if (annotation == null) {
            return emptyMap();
        }
        logger.info("The Dubbo Service annotation '@{}' was found in the implementation class[type : '{}']",
                annotation.annotationType().getSimpleName(), interfaceImplClass.getName());
        Map<String, Object> annotationAttributes = getAnnotationAttributes(annotation);
        return annotationAttributes;
    }

    private String buildReferenceBaseURL(Environment environment) {
        int port = environment.getProperty(DUBBO_PORT_PROPERTY_NAME, int.class);
        return "dubbo://127.0.0.1:" + port;
    }

    private String buildReferenceURL(String path) {
        return this.referenceBaseURL + normalize(path);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.referenceBaseURL = buildReferenceBaseURL(environment);
    }

    @Override
    public void destroy() throws Exception {
        referenceConfigs.forEach(ReferenceConfig::destroy);
        referenceConfigs.clear();
        proxiesCache.clear();
        logger.info("DubboClient is destroyed");
    }
}
