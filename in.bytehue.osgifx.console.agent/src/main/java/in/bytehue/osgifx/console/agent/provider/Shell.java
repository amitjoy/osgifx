package in.bytehue.osgifx.console.agent.provider;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;

/**
 * A redirector that redirects the input of a Gogo Command Processor
 */
public class Shell extends RedirectInput {
    CommandSession session;
    PrintStream    out;
    boolean        running;

    public void open(final CommandSession session) {
        this.session = session;
        out          = session.getConsole();
        prompt();
    }

    private void prompt() {
        CharSequence prompt;
        try {
            Object value = session.get("prompt");
            if (value instanceof CharSequence) {
                value = session.execute((CharSequence) value);
            }
            if (value != null) {
                prompt = session.format(value, Converter.LINE);
            } else {
                prompt = "> ";
            }
        } catch (final Exception e) {
            prompt = "> ";
        }
        out.print(prompt);
        out.flush();
    }

    @Override
    public void close() {

    }

    @Override
    public synchronized void add(final String s) throws IOException {
        if (running) {
            super.add(s);
        } else {
            running = true;
            try {
                final Object result = session.execute(s);
                if (result != null) {
                    out.println(session.format(result, Converter.INSPECT));
                }
            } catch (final Exception e) {
                e.printStackTrace(out);
            } finally {
                running = false;
            }
            out.flush();
            prompt();
        }
    }

}
