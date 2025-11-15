package io.microsphere.apidocs.springfox.documentation.spring.web.compiler;

import io.microsphere.logging.Logger;
import org.apache.dubbo.common.compiler.support.AbstractCompiler;
import org.springframework.util.FileSystemUtils;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.util.StringUtils.split;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;

/**
 * JDK {@link Compiler} is compatible with FAT-JAR or normal File System
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class JdkCompiler extends AbstractCompiler {

    private static final Logger logger = getLogger(JdkCompiler.class);

    private static final Writer out = new LoggingWriterAdapter(logger);

    private static final String JAVAC_OPTIONS = "-source 1.8 -target 1.8 -g -parameters -proc:none -Xlint:unchecked -nowarn -Xlint:deprecation";

    private static final List<String> OPTIONS = ofList(split(JAVAC_OPTIONS, ' '));

    private static final Class[] parameterTypes = new Class[]{String.class, byte[].class, int.class, int.class};

    private static final Method defineClassMethod = findMethod(ClassLoader.class, "defineClass", parameterTypes);

    private static final List<File> fileSystemClassPathFiles = findFileSystemClassPathFiles();

    static {
        // Fast-fail check
        defineClassMethod.setAccessible(true);
    }

    private final ClassLoader classLoader;

    private final JavaCompiler compiler;

    private final Locale locale;

    private final Charset charset;

    private Path targetPath;

    private File targetDirectory;

    private File outputDirectory;

    private List<File> classPathFiles;

    /**
     * Batch
     */
    private boolean batchEnabled;

    private Map<String, String> batchSourceCode;

    public JdkCompiler(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.locale = Locale.getDefault();
        this.charset = StandardCharsets.UTF_8;
    }

    public Class<?> compile(String source) throws Throwable {
        return compile(source, this.classLoader);
    }

    public void init() throws Exception {
        this.targetPath = makeTargetPath();
        this.targetDirectory = targetPath.toFile();
        this.outputDirectory = makeOutputDirectory();
        this.classPathFiles = resolveClassPathFiles();
        logger.info("The compiler is initialized , the target path : '{}'", targetPath);
    }

    public void enableBatch() {
        this.batchEnabled = true;
        this.batchSourceCode = new LinkedHashMap<>();
    }

    public void disableBatch() {
        this.batchEnabled = false;
        this.clearBatchData();
    }

    public boolean isBatchEnabled() {
        return this.batchEnabled;
    }

    private void clearBatchData() {
        this.clearBatchSourceCode();
    }

    private void clearBatchSourceCode() {
        if (this.batchSourceCode != null) {
            this.batchSourceCode.clear();
            this.batchSourceCode = null;
        }
    }

    private void addBatchSourceCode(String className, String sourceCode) {
        if (this.batchSourceCode != null) {
            this.batchSourceCode.put(className, sourceCode);
        }
    }

    @Override
    protected Class<?> doCompile(ClassLoader classLoader, String className, String sourceCode) throws Throwable {

        if (isBatchEnabled()) {
            addBatchSourceCode(className, sourceCode);
            return null;
        }

        Map<String, String> sourceCodeMap = singletonMap(className, sourceCode);

        List<Class<?>> compiledClasses = compile(sourceCodeMap);

        return compiledClasses.isEmpty() ? null : compiledClasses.get(0);
    }

    private List<Class<?>> compile(Map<String, String> sourceCodeMap) throws Throwable {
        long startTime = System.currentTimeMillis();
        assertInitialStatus();
        int size = sourceCodeMap.size();
        List<Class<?>> compiledClasses = emptyList();
        if (size > 0) {
            Map<String, File> classFiles = new HashMap<>(size);
            DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
            try (StandardJavaFileManager javaFileManager = compiler.getStandardFileManager(diagnosticCollector, locale, charset)) {
                File outputDirectory = this.outputDirectory;
                List<File> classpathFiles = this.classPathFiles;
                javaFileManager.setLocation(StandardLocation.CLASS_OUTPUT, singleton(outputDirectory));
                javaFileManager.setLocation(StandardLocation.CLASS_PATH, classpathFiles);
                List<JavaFileObject> compilationUnits = new ArrayList<>(size);

                for (Map.Entry<String, String> entry : sourceCodeMap.entrySet()) {
                    String className = entry.getKey();
                    String sourceCode = entry.getValue();
                    compilationUnits.add(new SourceJavaFileObject(className, sourceCode, charset));

                    File classFile = new File(outputDirectory, className.replace('.', '/').concat(".class"));
                    classFiles.put(className, classFile);
                }

                JavaCompiler.CompilationTask task = compiler.getTask(out, javaFileManager, diagnosticCollector, OPTIONS, null, compilationUnits);
                Boolean result = task.call();

                logger.info("The result of the compilation ：{} , cost : {} ms , path : '{}' , classes : {} , diagnostics : {}", result,
                        System.currentTimeMillis() - startTime, outputDirectory, classFiles.keySet(), diagnosticCollector.getDiagnostics());

            }

            compiledClasses = new ArrayList<>(size);
            for (Map.Entry<String, File> entry : classFiles.entrySet()) {
                String className = entry.getKey();
                File classFile = entry.getValue();
                if (classFile.exists()) {
                    compiledClasses.add(loadClass(className, classFile));
                }
            }

            compiledClasses = unmodifiableList(compiledClasses);
        }
        return compiledClasses;
    }

    public List<Class<?>> batchCompile() throws Throwable {
        List<Class<?>> compiledClasses = emptyList();
        if (isBatchEnabled()) {
            try {
                compiledClasses = compile(this.batchSourceCode);
                logger.info("The batch compilation is completed");
            } finally {
                this.clearBatchData();
            }
        } else {
            logger.warn("Batch compilation is disabled");
        }
        return compiledClasses;
    }

    public void destroy() {
        logger.info("The compiler is about to destroy");
        disableBatch();
        removeTargetDirectory();
    }

    private void removeTargetDirectory() {
        if (this.targetDirectory != null) {
            FileSystemUtils.deleteRecursively(targetDirectory);
            logger.info("The target directory[ path : '{}'] was removed", targetPath);
        }
    }

    private void assertInitialStatus() {
        if (targetPath == null) {
            throw new IllegalStateException("'targetPath' is null, please invoke 'init()' method before compile");
        }
    }

    private File makeOutputDirectory() throws IOException {
        Path outputPath = targetPath.resolve("out");
        Files.createDirectory(outputPath);
        File outputDirectory = outputPath.toFile();
        return outputDirectory;
    }

    private Class<?> loadClass(String className, File classFile) throws Throwable {
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        Class<?> compiledClass = (Class) defineClassMethod.invoke(classLoader, className, bytes, 0, bytes.length);
        return compiledClass;
    }

    private Path makeTargetPath() throws IOException {
        Path targetPath = Files.createTempDirectory("target");
        return targetPath;
    }

    private List<File> resolveClassPathFiles() throws IOException, URISyntaxException {
        final List<File> classPathFiles;
        JarFile fatJarFile = findFatJarFile();
        if (fatJarFile == null) { // If Non-Fat JAR runtime
            classPathFiles = resolveFileSystemClassPathFiles();
        } else {
            classPathFiles = resolveFatJarClassPathFiles(fatJarFile);
        }
        logger.debug("Resolved class path files : {}", classPathFiles);
        return classPathFiles;
    }

    private JarFile findFatJarFile() throws IOException {
        URL bootInfResource = classLoader.getResource("BOOT-INF/");
        if (bootInfResource != null) {
            String path = bootInfResource.toString();
            int beginIndex = path.indexOf("!/");
            String jarFilePath = path.substring(9, beginIndex);
            return new JarFile(jarFilePath);
        }
        return null;
    }

    private List<File> resolveFileSystemClassPathFiles() throws URISyntaxException {
        List<File> classPathFiles = this.fileSystemClassPathFiles;

        if (classPathFiles.isEmpty()) {
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                URL[] urls = urlClassLoader.getURLs();
                classPathFiles = new ArrayList<>(urls.length);
                for (URL url : urls) {
                    Path path = Paths.get(url.toURI());
                    if (Files.isRegularFile(path) || Files.isDirectory(path)) {
                        classPathFiles.add(path.toFile());
                    }
                }
            }
        }

        return classPathFiles;
    }

    private List<File> resolveFatJarClassPathFiles(JarFile fatJarFile) throws IOException {
        List<File> classPathFiles = new LinkedList<>();
        resolveFatJarClassPathFiles(classPathFiles, fatJarFile, "BOOT-INF/classes/", true);
        resolveFatJarClassPathFiles(classPathFiles, fatJarFile, "BOOT-INF/lib/", false);
        return classPathFiles;
    }

    private void resolveFatJarClassPathFiles(List<File> classPathFiles, JarFile jarFile, String basePath, boolean directory) throws IOException {
        jarFile.stream().forEach(jarEntry -> {
            String entryName = jarEntry.getName();
            if (entryName.startsWith(basePath)) {
                File targetFile = new File(targetDirectory, entryName);
                targetFile.getParentFile().mkdirs();
                if (!jarEntry.isDirectory()) {
                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        Files.copy(inputStream, targetFile.toPath());
                        if (!directory) {
                            classPathFiles.add(targetFile);
                        }
                    } catch (IOException e) {
                        logger.error("JarEntry[name : '{}'] read error", entryName, e);
                    }
                }
            }
        });
        if (directory) {
            classPathFiles.add(new File(targetDirectory, basePath));
        }
    }

    private static List<File> findFileSystemClassPathFiles() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<File> classPathFiles = new LinkedList<>();
        // Bootstrap Class Path
        if (runtimeMXBean.isBootClassPathSupported()) {
            addClassPathFiles(classPathFiles, runtimeMXBean.getBootClassPath());
        }
        // App Class Path
        addClassPathFiles(classPathFiles, runtimeMXBean.getClassPath());
        return classPathFiles;
    }

    private static void addClassPathFiles(List<File> classPathFiles, String classPath) {
        String[] classPaths = split(classPath, File.pathSeparatorChar);
        if (classPaths != null) {
            Stream.of(classPaths).map(File::new).filter(File::exists).forEach(classPathFiles::add);
        }
    }
}
