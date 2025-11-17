package io.microsphere.apidocs.springfox.documentation.dubbo.beans.factory;

import io.microsphere.apidocs.springfox.documentation.dubbo.compiler.JdkCompiler;
import io.microsphere.apidocs.springfox.documentation.spring.web.generator.ControllerSourceCodeGenerator;
import io.microsphere.logging.Logger;
import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.spring.beans.factory.support.BeanRegistrar.registerBeanDefinition;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * {@link BeanDefinitionRegistryPostProcessor} for API Service Swagger Document
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class ApiServiceDocumentBeanDefinitionProcessor
        implements BeanDefinitionRegistryPostProcessor, BeanFactoryPostProcessor, BeanClassLoaderAware, InitializingBean, DisposableBean {

    private static final Logger logger = getLogger(ApiServiceDocumentBeanDefinitionProcessor.class);

    private static final String SERVICE_BEAN_CLASS_NAME = ServiceBean.class.getName();

    private BeanDefinitionRegistry registry;

    private ClassLoader classLoader;

    private JdkCompiler jdkCompiler;

    private final ObjectProvider<ControllerSourceCodeGenerator> controllerSourceCodeGeneratorProvider;

    public ApiServiceDocumentBeanDefinitionProcessor(ObjectProvider<ControllerSourceCodeGenerator> controllerSourceCodeGeneratorProvider) {
        this.controllerSourceCodeGeneratorProvider = controllerSourceCodeGeneratorProvider;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        findDubboServiceBeanDefinitions().forEach(this::processDubboServiceBeanDefinition);
        registerGeneratedBeanDefinitions();
    }

    private void processDubboServiceBeanDefinition(BeanDefinition dubboServiceBeanDefinition) {
        MutablePropertyValues propertyValues = dubboServiceBeanDefinition.getPropertyValues();
        Class<?> dubboInterfaceClass = resolveDubboInterfaceClass(propertyValues);
        String dubboProviderBeanName = resolveDubboProviderBeanName(propertyValues);
        Class<?> dubboProviderClass = resolveDubboProviderClass(dubboProviderBeanName);
//        if (findAnnotation(dubboProviderClass, RestController.class) == null) {
//            logger.debug("Dubbo Service [interface : '{}' , provider : '{}'] is not a @RestController Bean", dubboInterfaceClass.getName(),
//                    dubboProviderClass.getName());
//            return;
//        }
        controllerSourceCodeGeneratorProvider.forEach(generator -> {
            try {
                String sourceCode = generator.generate(dubboInterfaceClass, dubboProviderClass, dubboProviderBeanName);
                jdkCompiler.compile(sourceCode);
            } catch (Throwable e) {
                // Batch mode will not throw any exception or error
                logger.error("Generating or compiling code[interface : '{}' , implementation : '{}']  is failed", dubboInterfaceClass.getName(),
                        dubboProviderClass.getName(), e);
            }
        });
    }

    private void registerGeneratedBeanDefinitions() {
        try {
            jdkCompiler.batchCompile().forEach(this::registerGeneratedBeanDefinition);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void registerGeneratedBeanDefinition(Class<?> generatedClass) {
        registerBeanDefinition(registry, generatedClass);
    }

    private Class<?> resolveDubboInterfaceClass(MutablePropertyValues propertyValues) {
        String dubboInterfaceClassName = (String) propertyValues.get("interface");
        return resolveClassName(dubboInterfaceClassName, classLoader);
    }

    private String resolveDubboProviderBeanName(MutablePropertyValues propertyValues) {
        RuntimeBeanReference reference = (RuntimeBeanReference) propertyValues.get("ref");
        String dubboProviderBeanName = reference.getBeanName();
        return dubboProviderBeanName;
    }

    private Class<?> resolveDubboProviderClass(String dubboProviderBeanName) {
        BeanDefinition dubboServiceBeanDefinition = registry.getBeanDefinition(dubboProviderBeanName);
        String dubboProviderBeanClassName = dubboServiceBeanDefinition.getBeanClassName();
        return resolveClassName(dubboProviderBeanClassName, classLoader);
    }

    protected List<BeanDefinition> findDubboServiceBeanDefinitions() {
        return Stream.of(registry.getBeanDefinitionNames()).map(registry::getBeanDefinition).filter(this::isDubboServiceBeanDefinition)
                .collect(Collectors.toList());
    }

    private boolean isDubboServiceBeanDefinition(BeanDefinition beanDefinition) {
        return SERVICE_BEAN_CLASS_NAME.equals(beanDefinition.getBeanClassName());
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.jdkCompiler = new JdkCompiler(classLoader);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.jdkCompiler.init();
        this.jdkCompiler.enableBatch();
    }

    @Override
    public void destroy() throws Exception {
        this.jdkCompiler.destroy();
    }
}
