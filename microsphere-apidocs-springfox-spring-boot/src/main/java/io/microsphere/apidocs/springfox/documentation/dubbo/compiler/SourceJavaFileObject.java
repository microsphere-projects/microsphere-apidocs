package io.microsphere.apidocs.springfox.documentation.dubbo.compiler;

import org.springframework.core.io.Resource;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;

import static javax.tools.JavaFileObject.Kind.SOURCE;
import static org.apache.dubbo.common.compiler.support.ClassUtils.JAVA_EXTENSION;
import static org.apache.dubbo.common.compiler.support.ClassUtils.toURI;

/**
 * {@link JavaFileObject} based on Spring {@link Resource}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class SourceJavaFileObject implements JavaFileObject {

    private final String className;

    private final String source;

    private final Charset charset;

    private final URI uri;

    private final Kind kind;

    public SourceJavaFileObject(String className, String sourceCode, Charset charset) {
        this.className = className;
        this.source = sourceCode;
        this.charset = charset;
        this.uri = toURI(className + JAVA_EXTENSION);
        this.kind = SOURCE;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
        return source;
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        return 0L;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(source.getBytes(charset));
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return this.className.endsWith(simpleName) && this.kind.equals(kind);
    }

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return null;
    }

    @Override
    public Kind getKind() {
        return kind;
    }
}