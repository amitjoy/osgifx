/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.redirector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import com.osgifx.console.agent.provider.AgentServer;

/**
 * Handles the redirection of the output. Any text written to this PrintStream
 * is send to the supervisor. We are a bit careful here that we're breaking
 * recursive calls that can happen when there is shit happening deep down below.
 */
public final class RedirectOutput extends PrintStream {

    private static Timer                      timer        = new Timer();
    private final List<AgentServer>           agents;
    private final PrintStream                 out;
    private StringBuilder                     sb           = new StringBuilder();
    private final boolean                     err;
    private static final ThreadLocal<Boolean> onStack      = new ThreadLocal<>();
    private TimerTask                         active;
    private String                            lastOutput   = "";
    private final ReentrantLock               lock         = new ReentrantLock();
    private final ReentrantLock               redirectLock = new ReentrantLock();

    /**
     * If we do not have an original, we create a null stream because the
     * PrintStream requires this.
     */
    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(final int off) throws IOException {
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
        }
    }

    public RedirectOutput(final List<AgentServer> agents, PrintStream out, final boolean err) {
        super(out == null ? out = newNullOutputStream() : out);
        this.agents = agents;
        this.out    = out;
        this.err    = err;
    }

    private static PrintStream newNullOutputStream() {
        return new PrintStream(new NullOutputStream());
    }

    @Override
    public void write(final int b) {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(final byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        if ((off | len | b.length - (len + off) | off + len) < 0) {
            throw new IndexOutOfBoundsException();
        }

        out.write(b, off, len);
        if (onStack.get() == null) {
            onStack.set(true);
            try {
                sb.append(new String(b, off, len)); // default encoding!
                flushConditional();
            } catch (final Exception e) {
            } finally {
                onStack.remove();
            }
        } else {
            out.println("[recursive output] " + new String(b, off, len));
        }
    }

    private void flushConditional() {
        lock.lock();
        try {
            if (active != null) {
                return;
            }
            active = new TimerTask() {
                @Override
                public void run() {
                    redirectLock.lock();
                    try {
                        active = null;
                    } finally {
                        redirectLock.unlock();
                    }
                    flush();
                }
            };
            timer.schedule(active, 300);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flush() {
        final String output;
        lock.lock();
        try {
            if (sb.length() == 0) {
                return;
            }
            output = sb.toString();
            sb     = new StringBuilder();
        } finally {
            lock.unlock();
        }
        setLastOutput(output);
        for (final AgentServer agent : agents) {
            if (agent.quit) {
                continue;
            }
            try {
                if (err) {
                    agent.getSupervisor().stderr(output);
                } else {
                    agent.getSupervisor().stdout(output);
                }
            } catch (final InterruptedException ie) {
                return;
            } catch (final Exception ie) {
                try {
                    agent.close();
                } catch (final IOException e) {
                }
            }
        }
        super.flush();
    }

    @Override
    public void close() {
        super.close();
    }

    public PrintStream getOut() {
        return out;
    }

    public String getLastOutput() {
        return lastOutput;
    }

    private void setLastOutput(String out) {
        if (!"".equals(out) && out != null) {
            out = out.replaceAll("^>.*$", "");
            if (!"".equals(out) && !out.startsWith("true")) {
                lastOutput = out;
            }
        }
    }
}
