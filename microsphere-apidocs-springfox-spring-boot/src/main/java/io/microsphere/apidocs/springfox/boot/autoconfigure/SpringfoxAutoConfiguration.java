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


import io.microsphere.apidocs.springfox.boot.condition.ConditionalOnSpringFoxEnabled;
import io.microsphere.apidocs.springfox.documentation.dubbo.annotation.DubboDocumentation;
import io.microsphere.apidocs.springfox.documentation.dubbo.beans.factory.ApiServiceDocumentBeanDefinitionProcessor;
import io.microsphere.apidocs.springfox.documentation.dubbo.generator.DubboRestControllerSourceCodeGenerator;
import io.microsphere.apidocs.springfox.documentation.spring.web.generator.ControllerSourceCodeGenerator;
import io.swagger.models.Swagger;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;

import static springfox.documentation.builders.RequestHandlerSelectors.withClassAnnotation;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

/**
 * The Auto-Configuration class for Microsphere Springfox
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnSpringFoxEnabled
 * @since 1.0.0
 */
@Configuration
@ConditionalOnSpringFoxEnabled
@AutoConfigureBefore(value = {
        OpenApiAutoConfiguration.class, WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class
})
@Import(value = {
        SpringfoxAutoConfiguration.SwaggerConfiguration.class
})
public class SpringfoxAutoConfiguration {

    @ConditionalOnClass(Swagger.class)
    public static class SwaggerConfiguration {

        @ConditionalOnClass(DubboComponentScan.class)
        @Configuration(proxyBeanMethods = false)
        @Import(value = {DubboRestControllerSourceCodeGenerator.class})
        public static class DubboConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public Docket createDubboRestApi() {
                ApiSelectorBuilder asb = new Docket(SWAGGER_2)
                        .apiInfo(getApiInfo())
                        .select()
                        .apis(withClassAnnotation(DubboDocumentation.class))
                        .paths(PathSelectors.any());
                Docket docket = asb.build();
                docket.groupName("dubbo");
                return docket;
            }

            @Bean
            @ConditionalOnMissingBean
            public ApiServiceDocumentBeanDefinitionProcessor apiServiceDocumentBeanDefinitionRegistryPostProcessor(
                    ObjectProvider<ControllerSourceCodeGenerator> controllerSourceCodeGeneratorProvider) {
                return new ApiServiceDocumentBeanDefinitionProcessor(controllerSourceCodeGeneratorProvider);
            }

            private ApiInfo getApiInfo() {
                return new ApiInfoBuilder().title("Dubbo API")
                        .version(SpringfoxAutoConfiguration.class.getPackage().getImplementationVersion())
                        .build();
            }
        }
    }
}
