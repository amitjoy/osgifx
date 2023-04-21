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
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;

/**
 * A redirector that redirects the input of a Gogo Command Processor
 */
public final class Shell extends RedirectInput {

    CommandSession session;
    PrintStream    out;
    boolean        running;

    private final ReentrantLock lock = new ReentrantLock();

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
        // nothing to do
    }

    @Override
    public void add(final String s) throws IOException {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

}
