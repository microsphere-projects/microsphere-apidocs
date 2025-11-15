package io.microsphere.apidocs.springfox.documentation.spring.web.compiler;

import io.microsphere.logging.Logger;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link Writer} Adapter based on {@link Logger}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class LoggingWriterAdapter extends Writer {

    private final Logger logger;

    public LoggingWriterAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        String value = str.substring(off, len);
        logger.info(value);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        write(new String(cbuf, off, len));
    }

    @Override
    public void flush() throws IOException {
        // DO NOTHING
    }

    @Override
    public void close() throws IOException {
        // DO NOTHING
    }
}
