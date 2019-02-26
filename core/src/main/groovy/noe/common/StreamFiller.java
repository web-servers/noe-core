package noe.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple stream filler to pass text input.
 */
public class StreamFiller extends Thread {
    private static final Logger log = LoggerFactory.getLogger(StreamFiller.class);

    OutputStream output;
    InputStream input;

    public StreamFiller(OutputStream output, InputStream input) {
        this.output = output;
        this.input = input;
    }

    /**
     * streams given text input to output stream
     */
    public void run() {
        if (input != null) {
            byte[] buffer = new byte[1024];
            int read = 0;
            try {
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                input.close();
                output.close();
            } catch (IOException ioe) {
                log.warn("Exception detected when processing input/output", ioe);
            }
        }
    }
}

