package in.bytehue.osgifx.console.agent.provider;

import java.io.IOException;
import java.io.PrintStream;

/**
 * This is a null redirector. That is, it just does nothing.
 */
public class NullRedirector implements Redirector {

    @Override
    public void close() throws IOException {
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public void stdin(final String s) {
    }

    @Override
    public PrintStream getOut() throws Exception {
        return System.out;
    }

}
