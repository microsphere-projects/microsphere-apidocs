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

package io.microsphere.apidocs.springfox.documentation.spring.web.beans.factory.config;

import org.apache.dubbo.config.spring.util.GenericBeanPostProcessorAdapter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.RequestHandler;
import springfox.documentation.RequestHandlerKey;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spring.web.WebMvcRequestHandler;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;
import springfox.documentation.spring.wrapper.PatternsRequestCondition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static io.microsphere.reflect.FieldUtils.getFieldValue;
import static java.util.stream.Collectors.toList;
import static springfox.documentation.builders.BuilderDefaults.nullToEmptyList;
import static springfox.documentation.spi.service.contexts.Orderings.byPatternsCondition;

/**
 * {@link BeanPostProcessor} for {@link WebMvcRequestHandlerProvider} for Spring Framework 5.3+
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebMvcRequestHandlerProvider
 * @see GenericBeanPostProcessorAdapter
 * @see BeanPostProcessor
 * @since 1.0.0
 */
public class WebMvcRequestHandlerProviderBeanPostProcessor extends GenericBeanPostProcessorAdapter<RequestHandlerProvider> {

    @Override
    protected RequestHandlerProvider doPostProcessBeforeInitialization(RequestHandlerProvider bean, String beanName) throws BeansException {
        if (bean instanceof WebMvcRequestHandlerProvider) {
            return new WebMvcRequestHandlerProviderWrapper((WebMvcRequestHandlerProvider) bean);
        }
        return super.doPostProcessBeforeInitialization(bean, beanName);
    }

    static class WebMvcRequestHandlerProviderWrapper implements RequestHandlerProvider {

        private final List<RequestMappingInfoHandlerMapping> handlerMappings;

        private final HandlerMethodResolver methodResolver;

        private final String contextPath;


        WebMvcRequestHandlerProviderWrapper(WebMvcRequestHandlerProvider delegate) {
            this.handlerMappings = getFieldValue(delegate, "handlerMappings");
            this.methodResolver = getFieldValue(delegate, "methodResolver");
            this.contextPath = getFieldValue(delegate, "contextPath");
        }

        @Override
        public List<RequestHandler> requestHandlers() {
            return nullToEmptyList(handlerMappings).stream()
                    .filter(requestMappingInfoHandlerMapping ->
                            !("org.springframework.integration.http.inbound.IntegrationRequestMappingHandlerMapping"
                                    .equals(requestMappingInfoHandlerMapping.getClass()
                                            .getName())))
                    .map(toMappingEntries())
                    .flatMap((entries -> StreamSupport.stream(entries.spliterator(), false)))
                    .map(toRequestHandler())
                    .sorted(byPatternsCondition())
                    .collect(toList());
        }


        private Function<RequestMappingInfoHandlerMapping,
                Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>> toMappingEntries() {
            return input -> input.getHandlerMethods()
                    .entrySet();
        }

        private Function<Map.Entry<RequestMappingInfo, HandlerMethod>, RequestHandler> toRequestHandler() {
            return input -> new WebMvcRequestHandler(
                    contextPath,
                    methodResolver,
                    input.getKey(),
                    input.getValue()) {

                @Override
                public PatternsRequestCondition getPatternsCondition() {
                    springfox.documentation.spring.wrapper.RequestMappingInfo requestMappingInfo = getRequestMapping();
                    RequestMappingInfo requestMapping = (RequestMappingInfo) requestMappingInfo.getOriginalInfo();
                    org.springframework.web.servlet.mvc.condition.PatternsRequestCondition patternsRequestCondition = requestMapping.getPatternsCondition();
                    if (patternsRequestCondition == null) {
                        return new PatternsRequestCondition() {

                            @Override
                            public PatternsRequestCondition combine(PatternsRequestCondition other) {
                                return this;
                            }

                            @Override
                            public Set<String> getPatterns() {
                                return requestMapping.getPatternValues();
                            }

                        };
                    }
                    return super.getPatternsCondition();
                }

                @Override
                public RequestHandlerKey key() {
                    springfox.documentation.spring.wrapper.RequestMappingInfo requestMappingInfo = getRequestMapping();
                    RequestMappingInfo requestMapping = (RequestMappingInfo) requestMappingInfo.getOriginalInfo();
                    return new RequestHandlerKey(
                            requestMapping.getPatternValues(),
                            requestMapping.getMethodsCondition().getMethods(),
                            requestMapping.getConsumesCondition().getConsumableMediaTypes(),
                            requestMapping.getProducesCondition().getProducibleMediaTypes());
                }
            };
        }
    }
}
