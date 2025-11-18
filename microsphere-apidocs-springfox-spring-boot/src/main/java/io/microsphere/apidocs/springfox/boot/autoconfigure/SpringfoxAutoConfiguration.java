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
import io.microsphere.apidocs.springfox.documentation.spring.web.annotation.HttpDocumentation;
import io.microsphere.apidocs.springfox.documentation.spring.web.beans.factory.config.WebMvcRequestHandlerProviderBeanPostProcessor;
import io.microsphere.apidocs.springfox.documentation.spring.web.generator.ControllerSourceCodeGenerator;
import io.microsphere.apidocs.springfox.documentation.spring.web.generator.HttpRestControllerSourceCodeGenerator;
import io.swagger.models.Swagger;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;
import springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.function.Consumer;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;
import static springfox.documentation.builders.PathSelectors.any;
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
    @Import(value = {
            SwaggerConfiguration.DubboConfiguration.class,
            HttpRestControllerSourceCodeGenerator.class,
            DubboRestControllerSourceCodeGenerator.class
    })
    public static class SwaggerConfiguration {

        @Bean
        @ConditionalOnWebApplication(type = SERVLET)
        @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition")
        public WebMvcRequestHandlerProviderBeanPostProcessor webMvcRequestHandlerProviderBeanPostProcessor() {
            return new WebMvcRequestHandlerProviderBeanPostProcessor();
        }

        @Bean
        @ConditionalOnMissingBean
        public ApiServiceDocumentBeanDefinitionProcessor apiServiceDocumentBeanDefinitionProcessor(
                ObjectProvider<ControllerSourceCodeGenerator> controllerSourceCodeGeneratorProvider) {
            return new ApiServiceDocumentBeanDefinitionProcessor(controllerSourceCodeGeneratorProvider);
        }

        @Bean
        public Docket springRestApi() {
            ApiSelectorBuilder asb = new Docket(SWAGGER_2)
                    .apiInfo(apiInfo(builder -> builder.title("Spring REST")))
                    .select()
                    .apis(withClassAnnotation(RestController.class)
                            .and(withClassAnnotation(DubboDocumentation.class).negate())
                            .and(withClassAnnotation(HttpDocumentation.class).negate()))
                    .paths(any());
            Docket docket = asb.build();
            docket.groupName("default");
            return docket;
        }

        @ConditionalOnClass(DubboComponentScan.class)
        static class DubboConfiguration {

            @Bean
            public Docket dubboRPCApi() {
                ApiSelectorBuilder asb = new Docket(SWAGGER_2)
                        .apiInfo(apiInfo(builder -> builder.title("Dubbo RPC")))
                        .select()
                        .apis(withClassAnnotation(DubboDocumentation.class))
                        .paths(any());
                Docket docket = asb.build();
                docket.groupName("dubbo-rpc");
                return docket;
            }

            @Bean
            public Docket dubboHTTPApi() {
                ApiSelectorBuilder asb = new Docket(SWAGGER_2)
                        .apiInfo(apiInfo(builder -> builder.title("Dubbo HTTP")))
                        .select()
                        .apis(withClassAnnotation(HttpDocumentation.class))
                        .paths(any());
                Docket docket = asb.build();
                docket.groupName("dubbo-http");
                return docket;
            }
        }
    }

    static ApiInfo apiInfo(Consumer<ApiInfoBuilder> apiInfoBuilderConsumer) {
        ApiInfoBuilder apiInfoBuilder = new ApiInfoBuilder();
        apiInfoBuilderConsumer.accept(apiInfoBuilder);
        return apiInfoBuilder.build();
    }
}