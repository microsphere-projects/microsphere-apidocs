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

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.beans.Introspector.decapitalize;
import static java.lang.System.lineSeparator;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

/**
 * Abstract Delegating {@link ControllerSourceCodeGenerator} class for {@link RestController}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public abstract class RestControllerSourceCodeGenerator implements ControllerSourceCodeGenerator {

    protected static final String LINE_SEPARATOR = lineSeparator();

    protected static final String SEPARATOR = ";";

    protected static final String COMMA = ",";

    protected static final String SPACE = " ";

    protected static final String DOT = ".";

    protected static final String START_BODY = "{";

    protected static final String END_BODY = "}";

    protected static final String START_PARAMETER = "(";

    protected static final String END_PARAMETER = ")";

    protected static final String DOUBLE_QUOTE = "\"";

    protected static final String SLASH = "/";

    protected static final Class<RequestMapping> REQUEST_MAPPING_CLASS = RequestMapping.class;

    protected final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public final String generate(Class<?> interfaceClass, Class<?> interfaceImplClass, String injectedBeanName) {

        StringBuilder codeBuilder = new StringBuilder();

        interfaceImplClass = ClassUtils.getUserClass(interfaceImplClass);

        // package ...
        generatePackage(codeBuilder, interfaceClass, interfaceImplClass);

        // annotate on the declared class
        generateDeclaredAnnotations(codeBuilder, interfaceClass, interfaceImplClass);

        // declare the class
        generateDeclaredClass(codeBuilder, interfaceClass, interfaceImplClass);

        // start class body
        generateStartBody(codeBuilder);
        generateNewLine(codeBuilder);

        // generate fields
        generateFields(codeBuilder, interfaceClass, interfaceImplClass, injectedBeanName);

        // declare methods
        generateMethods(codeBuilder, interfaceClass, interfaceImplClass);

        // end class body
        generateEndBody(codeBuilder);
        return codeBuilder.toString();
    }

    protected void generatePackage(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        final String packageName = getPackageName(interfaceImplClass, interfaceImplClass);
        codeBuilder.append("package ").append(packageName);
        generateSeparator(codeBuilder);
        generateNewLine(codeBuilder);
    }

    protected void generateDeclaredAnnotations(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        List<Annotation> declaredAnnotations = new LinkedList<>();
        extendDeclaredAnnotations(declaredAnnotations);
        declaredAnnotations.forEach(annotation -> {
            generateAnnotation(codeBuilder, annotation, LINE_SEPARATOR);
        });
        generateDeclaredAnnotation(codeBuilder, RestController.class);
        generateRequestMappingAnnotation(codeBuilder, interfaceClass, interfaceImplClass);
    }

    protected final void generateImportClass(StringBuilder codeBuilder, Class<?> importClass) {
        if (importClass == null) {
            return;
        }
        String importClassName = getTypeName(importClass);
        generateImportClass(codeBuilder, importClassName);
    }

    protected final void generateImportClass(StringBuilder codeBuilder, String importClassName) {
        codeBuilder.append("import ").append(importClassName);
        generateSeparator(codeBuilder);
        generateNewLine(codeBuilder);
    }

    protected final void generateDeclaredAnnotation(StringBuilder codeBuilder, Class<? extends Annotation> annotationClass, Object... attributes) {
        generateAnnotation(codeBuilder, annotationClass);
        generateAnnotationAttributes(codeBuilder, attributes);
        generateNewLine(codeBuilder);
    }

    protected void generateDeclaredClass(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        String classSimpleName = resolveControllerClassSimpleName(interfaceClass, interfaceImplClass);
        generateDeclaredClass(codeBuilder, classSimpleName);
    }

    protected void generateFields(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass, String injectedBeanName) {
        // inject the target bean by type
        generateInjectedField(codeBuilder, interfaceClass, interfaceImplClass, injectedBeanName);
    }

    protected void generateDeclaredClass(StringBuilder codeBuilder, String classSimpleName) {
        codeBuilder.append("public class ").append(classSimpleName);
    }

    protected void generateInjectedField(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass, String injectedBeanName) {
        Annotation injectedAnnotation = getInjectedAnnotation(interfaceClass, interfaceImplClass);
        if (injectedAnnotation != null) {
            if (StringUtils.hasText(injectedBeanName)) {
                generateDeclaredAnnotation(codeBuilder, Qualifier.class, injectedBeanName);
            }
            generateAnnotation(codeBuilder, injectedAnnotation, LINE_SEPARATOR);

            String interfaceClassName = getTypeName(interfaceClass);
            String fieldName = getFieldName(interfaceClass);
            codeBuilder.append("private ").append(interfaceClassName).append(" ").append(fieldName);
            generateSeparator(codeBuilder);
            generateNewLine(codeBuilder);
        }
    }

    protected void generateInjectedField(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        generateInjectedField(codeBuilder, interfaceClass, interfaceImplClass, null);
    }

    protected Annotation getInjectedAnnotation(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        return synthesizeAnnotation(Autowired.class);
    }

    protected void generateMethods(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        Stream.of(interfaceClass.getMethods()).sorted(this::sort).forEach(method -> {
            // generate @RequestMapping
            generateRequestMappingMethod(codeBuilder, method, interfaceClass, interfaceImplClass);
        });
    }

    protected int sort(Method oneMethod, Method anotherMethod) {
        return oneMethod.getName().compareTo(anotherMethod.getName());
    }

    protected final void generateRequestMappingMethod(StringBuilder codeBuilder, Method method, Class<?> interfaceClass,
                                                      Class<?> interfaceImplClass) {
        RequestMapping requestMapping = getMergedAnnotation(method, REQUEST_MAPPING_CLASS);
        if (requestMapping != null) {
            generateRequestMappingMethod(codeBuilder, method, requestMapping, interfaceClass, interfaceImplClass);
        }
    }

    final void generateRequestMappingMethod(StringBuilder codeBuilder, Method method, RequestMapping requestMapping, Class<?> interfaceClass,
                                            Class<?> interfaceImplClass) {

        // Filter the annotation attributes with default-value
        Map<String, Object> requestMappingAttributes = getAnnotationAttributes(requestMapping, true);

        // annotate @RequestMapping
        generateAnnotation(codeBuilder, REQUEST_MAPPING_CLASS, requestMappingAttributes);

        // annotate @ApiOperation if present
        generateAnnotation(codeBuilder, method, ApiOperation.class);

        // access modifiers
        generateAccessModifiers(codeBuilder, method);

        // type parameters
        generateTypeParameters(codeBuilder, method);

        // return type
        generateReturnType(codeBuilder, method);

        // method name
        generateMethodName(codeBuilder, method);

        // method parameters
        generateMethodParameters(codeBuilder, method, interfaceImplClass);

        // method throws
        generateMethodThrows(codeBuilder, method);

        // start method body
        generateStartBody(codeBuilder);
        generateNewLine(codeBuilder);

        // generate invoke code
        generateMethodBody(codeBuilder, method, interfaceClass, interfaceImplClass);

        // end method body
        generateEndBody(codeBuilder);
        generateNewLine(codeBuilder);
    }

    final void generateAccessModifiers(StringBuilder codeBuilder, Method method) {
        codeBuilder.append("public ");
    }

    final void generateTypeParameters(StringBuilder codeBuilder, Method method) {
        TypeVariable<?>[] typeParameters = method.getTypeParameters();
        if (typeParameters.length > 0) {
            boolean first = true;
            codeBuilder.append('<');
            for (TypeVariable<?> typeParameter : typeParameters) {
                if (!first)
                    generateComma(codeBuilder);
                codeBuilder.append(typeParameter.toString());
                first = false;
            }
            codeBuilder.append("> ");
        }
    }

    final void generateReturnType(StringBuilder codeBuilder, Method method) {
        Type returnType = method.getGenericReturnType();
        generateType(codeBuilder, returnType);
        generateSpace(codeBuilder);
    }

    final void generateMethodName(StringBuilder codeBuilder, Method method) {
        String methodName = method.getName();
        generate(codeBuilder, methodName);
    }

    final void generateMethodParameters(StringBuilder codeBuilder, Method method, Class<?> interfaceImplClass) {
        Method overriddenMethod = getOverriddenMethod(method, interfaceImplClass);
        generateMethodParameters(codeBuilder, method, overriddenMethod);
    }

    protected final Method getOverriddenMethod(Method method, Class<?> interfaceImplClass) {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Method overriddenMethod = ReflectionUtils.findMethod(interfaceImplClass, methodName, parameterTypes);
        return overriddenMethod;
    }

    protected final void generateMethodParameters(StringBuilder codeBuilder, Method method, Method overriddenMethod) {
        int count = method.getParameterCount();
        generateStartParameter(codeBuilder);
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(overriddenMethod);

        Parameter[] overriddenParameters = overriddenMethod.getParameters();
        String[] overriddenParameterNames = parameterNameDiscoverer.getParameterNames(overriddenMethod);
        for (int i = 0; i < count; i++) {
            Parameter parameter = parameters[i];
            String parameterName = parameterNames[i];
            Parameter overriddenParameter = overriddenParameters[i];
            String overriddenParameterName = overriddenParameterNames[i];
            generateParameter(codeBuilder, parameter, parameterName, overriddenParameter, overriddenParameterName);
            if (i < count - 1) {
                generateComma(codeBuilder);
            }
        }
        generateEndParameter(codeBuilder);
    }

    final void generateParameter(StringBuilder codeBuilder, Parameter parameter, String parameterName, Parameter overriddenParameter,
                                 String overriddenParameterName) {
        // declare annotations
        Annotation[] annotations = findAnnotations(parameter, overriddenParameter);
        for (Annotation annotation : annotations) {
            generateAnnotation(codeBuilder, annotation, SPACE);
        }

        // declare parameter type
        generateParameterType(codeBuilder, overriddenParameter.getParameterizedType());
        // parameter type
        generateParameterName(codeBuilder, overriddenParameterName);
    }

    private Annotation[] findAnnotations(Parameter parameter, Parameter overriddenParameter) {
        Annotation[] declaredAnnotations = parameter.getAnnotations();
        Annotation[] overriddenAnnotations = overriddenParameter.getAnnotations();
        Map<Class, Annotation> effectiveAnnotations = new LinkedHashMap<>();
        addAnnotations(effectiveAnnotations, declaredAnnotations);
        addAnnotations(effectiveAnnotations, overriddenAnnotations);
        return effectiveAnnotations.values().toArray(new Annotation[0]);
    }

    private void addAnnotations(Map<Class, Annotation> effectiveAnnotations, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            effectiveAnnotations.put(annotation.annotationType(), annotation);
        }
    }

    final void generateParameterType(StringBuilder codeBuilder, Type type) {
        generateType(codeBuilder, type);
        generateSpace(codeBuilder);
    }

    final void generateParameterName(StringBuilder codeBuilder, String parameterName) {
        generate(codeBuilder, parameterName);
    }

    final void generateMethodThrows(StringBuilder codeBuilder, Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        int length = exceptionTypes.length;
        if (length > 0) {
            codeBuilder.append("throws ");
            for (int i = 0; i < length; i++) {
                Class<?> exceptionType = exceptionTypes[i];
                generateType(codeBuilder, exceptionType);
                if (i < length - 1) {
                    generateComma(codeBuilder);
                }
            }
        }
    }

    protected void generateMethodBody(StringBuilder codeBuilder, Method method, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        String fieldName = getFieldName(interfaceClass);
        Class<?> returnType = method.getReturnType();
        if (!void.class.equals(returnType)) {
            codeBuilder.append("return ");
        }
        codeBuilder.append(fieldName);
        generateMethodInvocation(codeBuilder, method, interfaceImplClass);
        generateNewLine(codeBuilder);
    }

    protected final void generateMethodInvocation(StringBuilder codeBuilder, Method method, Class<?> interfaceImplClass) {
        String methodName = method.getName();
        Method overriddenMethod = getOverriddenMethod(method, interfaceImplClass);

        generateDot(codeBuilder);
        generate(codeBuilder, methodName);
        generateStartParameter(codeBuilder);
        String parametersValue = getMethodParametersValue(overriddenMethod);
        generate(codeBuilder, parametersValue);
        generateEndParameter(codeBuilder);
        generateSeparator(codeBuilder);
    }

    protected final String getMethodParametersValue(Method method) {
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        String parametersValue = arrayToCommaDelimitedString(parameterNames);
        return parametersValue;
    }

    final void generateRequestMappingAnnotation(StringBuilder codeBuilder, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        Class<RequestMapping> annotationClass = REQUEST_MAPPING_CLASS;
        RequestMapping requestMapping = interfaceClass.getAnnotation(annotationClass);
        if (requestMapping == null) {
            requestMapping = interfaceImplClass.getAnnotation(annotationClass);
        }
        if (requestMapping != null) {
            Map<String, Object> annotationAttributes = getAnnotationAttributes(requestMapping, true);
            // remove "path" as alias
            String[] path = (String[]) annotationAttributes.remove("path");
            String pathValue = ObjectUtils.isEmpty(path) ? SLASH : path[0];
            String pathOnControllerClass = resolveRequestMappingPathOnControllerClass(pathValue, interfaceClass, interfaceImplClass);
            // reset the "value" attribute as new path
            annotationAttributes.put("value", pathOnControllerClass);
            generateAnnotation(codeBuilder, annotationClass, annotationAttributes);
        } else { // otherwise generate default request mapping
            generateDeclaredAnnotation(codeBuilder, annotationClass,
                    resolveRequestMappingPathOnControllerClass(SLASH, interfaceClass, interfaceImplClass));
        }
    }

    final void generateAnnotation(StringBuilder codeBuilder, AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        Annotation annotation = annotatedElement.getAnnotation(annotationClass);
        generateAnnotation(codeBuilder, annotation, LINE_SEPARATOR);
    }

    final void generateAnnotation(StringBuilder codeBuilder, Class<? extends Annotation> annotationClass, Map<String, Object> annotationAttributes) {
        generateAnnotation(codeBuilder, annotationClass);
        generateAnnotationAttributes(codeBuilder, annotationAttributes);
        generateNewLine(codeBuilder);
    }

    final void generateAnnotation(StringBuilder codeBuilder, Annotation annotation, String separator) {
        if (annotation != null) {
            generateAnnotation(codeBuilder, annotation.annotationType());
            generateAnnotationAttributes(codeBuilder, annotation);
            generate(codeBuilder, separator);
        }
    }

    final void generateAnnotation(StringBuilder codeBuilder, Class<? extends Annotation> annotationClass) {
        String annotationClassName = getTypeName(annotationClass);
        codeBuilder.append("@").append(annotationClassName);
    }

    final void generateAnnotationAttributes(StringBuilder codeBuilder, Object... attributes) {

        int length = attributes == null ? 0 : attributes.length;

        if (length == 0) {
            return;
        }

        int times = length / 2;
        Map<String, Object> annotationAttributes = new LinkedHashMap<>(times + 1);

        if (length == 1) {
            annotationAttributes.put("value", attributes[0]);
        } else {
            for (int i = 0; i < times * 2; ) {
                String name = String.valueOf(attributes[i++]);
                Object value = attributes[i++];
                annotationAttributes.put(name, value);
            }
        }

        generateAnnotationAttributes(codeBuilder, annotationAttributes);
    }

    final void generateAnnotationAttributes(StringBuilder codeBuilder, Annotation annotation) {

        if (annotation == null) {
            return;
        }

        Map<String, Object> annotationAttributes = getAnnotationAttributes(annotation, true);

        generateAnnotationAttributes(codeBuilder, annotationAttributes);
    }

    final void generateAnnotationAttributes(StringBuilder codeBuilder, Map<String, Object> annotationAttributes) {

        int size = annotationAttributes.size();

        if (size < 1) {
            return;
        }

        // sort
        Map<String, Object> attributes = new TreeMap<>(annotationAttributes);

        generateStartParameter(codeBuilder);

        int index = 0;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            generateAnnotationAttribute(codeBuilder, entry.getKey(), entry.getValue());
            if (index++ < size - 1) {
                generateComma(codeBuilder);
            }
        }

        generateEndParameter(codeBuilder);
    }

    final void generateAnnotationAttribute(StringBuilder codeBuilder, String name, Object value) {
        String attributeValue = resolveAnnotationAttribute(value);
        codeBuilder.append(name).append("=").append(attributeValue);
    }

    private String resolveAnnotationAttribute(Object value) {
        if (value instanceof Class) {
            return resolveAnnotationClassAttribute((Class) value);
        } else if (value instanceof String) {
            return resolveAnnotationStringAttribute((String) value);
        } else if (value instanceof Enum) {
            return resolveAnnotationEnumAttribute((Enum) value);
        } else if (value instanceof Annotation) {
            return resolveAnnotationAttribute((Annotation) value);
        } else if (value.getClass().isArray()) {
            return resolveAnnotationArrayAttribute(value);
        }
        return String.valueOf(value);
    }

    private String resolveAnnotationClassAttribute(Class<?> value) {
        return getTypeName(value) + ".class";
    }

    private String resolveAnnotationStringAttribute(String value) {
        return "\"" + value + "\"";
    }

    private String resolveAnnotationEnumAttribute(Enum value) {
        String className = getTypeName(value.getClass());
        String name = value.name();
        return className + "." + name;
    }

    private String resolveAnnotationAttribute(Annotation value) {
        StringBuilder codeBuilder = new StringBuilder();
        generateAnnotation(codeBuilder, value.annotationType());
        generateAnnotationAttributes(codeBuilder, value);
        return codeBuilder.toString();
    }

    private String resolveAnnotationArrayAttribute(Object value) {
        int length = Array.getLength(value);
        StringBuilder codeBuilder = new StringBuilder();

        generateStartBody(codeBuilder);

        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            codeBuilder.append(resolveAnnotationAttribute(element));
            if (i < length - 1) {
                generateComma(codeBuilder);
            }
        }

        generateEndBody(codeBuilder);

        return codeBuilder.toString();
    }

    protected final void generateStartBody(StringBuilder codeBuilder) {
        generateSpace(codeBuilder);
        codeBuilder.append(START_BODY);
    }

    protected final void generateEndBody(StringBuilder codeBuilder) {
        codeBuilder.append(END_BODY);
    }

    protected final void generateStartParameter(StringBuilder codeBuilder) {
        codeBuilder.append(START_PARAMETER);
    }

    protected final void generateEndParameter(StringBuilder codeBuilder) {
        codeBuilder.append(END_PARAMETER);
    }

    protected final void generateSeparator(StringBuilder codeBuilder) {
        codeBuilder.append(SEPARATOR);
    }

    protected final void generateNewLine(StringBuilder codeBuilder) {
        generate(codeBuilder, LINE_SEPARATOR);
    }

    protected final void generateSpace(StringBuilder codeBuilder) {
        generate(codeBuilder, SPACE);
    }

    protected final void generateComma(StringBuilder codeBuilder) {
        generate(codeBuilder, COMMA);
    }

    protected final void generateType(StringBuilder codeBuilder, Type type) {
        generate(codeBuilder, getTypeName(type));
    }

    protected final void generateDoubleQuote(StringBuilder coderBuilder) {
        generate(coderBuilder, DOUBLE_QUOTE);
    }

    protected final void generateDot(StringBuilder coderBuilder) {
        generate(coderBuilder, DOT);
    }

    protected final void generate(StringBuilder codeBuilder, String content) {
        codeBuilder.append(content);
    }

    protected String getFieldName(Class<?> type) {
        String classSimpleName = type.getSimpleName();
        return getFieldName(classSimpleName);
    }

    protected String getFieldName(String classSimpleName) {
        String fieldName = decapitalize(classSimpleName);
        return fieldName;
    }

    protected String getTypeName(Type type) {
        return type.getTypeName().replace('$', '.');
    }

    protected abstract String getPackageName(Class<?> interfaceClass, Class<?> interfaceImplClass);

    protected abstract void extendDeclaredAnnotations(List<Annotation> declaredAnnotations);

    protected abstract String resolveRequestMappingPathOnControllerClass(String path, Class<?> interfaceClass, Class<?> interfaceImplClass);

    protected abstract String resolveControllerClassSimpleName(Class<?> interfaceClass, Class<?> interfaceImplClass);

}