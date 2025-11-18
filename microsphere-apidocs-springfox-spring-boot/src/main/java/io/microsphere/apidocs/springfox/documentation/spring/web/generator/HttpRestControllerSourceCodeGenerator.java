package io.microsphere.apidocs.springfox.documentation.spring.web.generator;


import io.microsphere.apidocs.springfox.documentation.spring.web.annotation.HttpDocumentation;

import java.lang.annotation.Annotation;
import java.util.List;

import static io.microsphere.net.URLUtils.buildURI;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

/**
 * HTTP {@link RestControllerSourceCodeGenerator}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class HttpRestControllerSourceCodeGenerator extends RestControllerSourceCodeGenerator {

    @Override
    protected String getPackageName(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        String packageName = interfaceImplClass.getPackage().getName();
        return packageName + ".api.http";
    }

    @Override
    protected void extendDeclaredAnnotations(List<Annotation> declaredAnnotations) {
        HttpDocumentation httpSwagger = synthesizeAnnotation(HttpDocumentation.class);
        declaredAnnotations.add(httpSwagger);
    }

    @Override
    protected String resolveRequestMappingPathOnControllerClass(String path, Class<?> interfaceClass, Class<?> interfaceImplClass) {
        return buildURI(path, "api", "http");
    }

    @Override
    protected String resolveControllerClassSimpleName(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        return interfaceImplClass.getSimpleName() + "HttpApi";
    }
}
