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

package io.microsphere.apidocs.springfox.documentation.dubbo.generator;

import io.microsphere.apidocs.springfox.documentation.dubbo.annotation.DubboDocumentation;
import io.microsphere.apidocs.springfox.documentation.dubbo.client.DubboClient;
import io.microsphere.apidocs.springfox.documentation.spring.web.generator.RestControllerSourceCodeGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static io.microsphere.spring.beans.factory.support.BeanRegistrar.registerBeanDefinition;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

/**
 * Dubbo {@link RestControllerSourceCodeGenerator}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class DubboRestControllerSourceCodeGenerator extends RestControllerSourceCodeGenerator
        implements EnvironmentAware, BeanDefinitionRegistryPostProcessor {

    private Environment environment;

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    protected void generateFields(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass, String injectedBeanName) {
        generateInjectedField(codeBuilder, interfaceClass, interfaceImplClass, injectedBeanName);
        generateInjectedField(codeBuilder, DubboClient.class, DubboClient.class);
    }

    @Override
    protected String getPackageName(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        String packageName = interfaceImplClass.getPackage().getName();
        return packageName + ".api.dubbo";
    }

    @Override
    protected void extendDeclaredAnnotations(List<Annotation> declaredAnnotations) {
        DubboDocumentation dubboDocumentation = synthesizeAnnotation(DubboDocumentation.class);
        declaredAnnotations.add(dubboDocumentation);
    }

    @Override
    protected String resolveRequestMappingPathOnControllerClass(String path, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        if (!path.endsWith(SLASH)) {
            path = path + SLASH;
        }
        String newPath = path + "api/dubbo";
        return newPath;
    }

    @Override
    protected String resolveControllerClassSimpleName(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        return interfaceImplClass.getSimpleName() + "DubboApi";
    }

    @Override
    protected void generateMethodBody(StringBuilder codeBuilder, Method method, Class<?> interfaceClass, Class<?> interfaceImplClass) {

        Class<?> returnType = method.getReturnType();
        if (!void.class.equals(returnType)) {
            codeBuilder.append("return ");
        }


        String interfaceClassName = getTypeName(interfaceClass);
        String beanFieldName = getFieldName(interfaceClass);
        String dubboClientFieldName = getFieldName(DubboClient.class);

        // dubboClient.getProxy(${interfaceClass},beanFieldName).${method.name}(...)

        generate(codeBuilder, dubboClientFieldName);
        generateDot(codeBuilder);
        generate(codeBuilder, "getProxy");
        generateStartParameter(codeBuilder);
        generate(codeBuilder, interfaceClassName);
        generate(codeBuilder, ".class");
        generateComma(codeBuilder);
        generate(codeBuilder, beanFieldName);
        generateEndParameter(codeBuilder);

        generateMethodInvocation(codeBuilder, method, interfaceImplClass);

        generateNewLine(codeBuilder);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        registerBeanDefinition(registry, DubboClient.class);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}