package noe.common;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Simple stream consumer for textual outputs.
 */
public class StreamConsumer extends Thread {
    private static final Logger log = LoggerFactory.getLogger(StreamConsumer.class);
    public static final String NEW_LINE = System.getProperty("line.separator");


    InputStream input;
    StringBuffer output;

    public StreamConsumer(InputStream is) {
        this(is, null);
    }

    public StreamConsumer(InputStream input, StringBuffer redirect) {
        this.input = input;
        this.output = redirect;
    }

    /**
     * creates readers to handle the text created by the external program
     */
    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (output != null) {
                    output.append(line);
                    output.append(NEW_LINE);
                }
            }
        } catch (IOException ioe) {
            log.warn("Exception detected when processing output", ioe);
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(isr);
        }
    }
}

