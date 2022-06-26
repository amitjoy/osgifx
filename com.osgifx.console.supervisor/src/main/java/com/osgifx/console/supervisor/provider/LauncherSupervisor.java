/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.supervisor.provider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;

@Component
public final class LauncherSupervisor extends AgentSupervisor<Supervisor, Agent> implements Supervisor {

    private Appendable stdout;
    private Appendable stderr;
    private int        shell = -100; // always invalid so we update it

    private final List<EventListener>    eventListeners    = new CopyOnWriteArrayList<>();
    private final List<LogEntryListener> logEntryListeners = new CopyOnWriteArrayList<>();

    @Override
    public boolean stdout(final String out) throws Exception {
        if (stdout != null) {
            stdout.append(out);
            return true;
        }
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        if (stderr != null) {
            stderr.append(out);
            return true;
        }
        return false;
    }

    public void setStdout(final Appendable out) throws Exception {
        stdout = out;
    }

    public void setStderr(final Appendable err) throws Exception {
        stderr = err;
    }

    public void setStdin(final InputStream in) throws Exception {
        final var isr = new InputStreamReader(in);

        final Thread stdin = new Thread("stdin") {
            @Override
            public void run() {
                final var sb = new StringBuilder();
                while (!isInterrupted()) {
                    try {
                        if (isr.ready()) {
                            final var read = isr.read();
                            if (read < 0) {
                                return;
                            }
                            sb.append((char) read);
                        } else if (sb.length() == 0) {
                            sleep(100);
                        } else {
                            getAgent().stdin(sb.toString());
                            sb.setLength(0);
                        }
                    } catch (final Exception e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
        };
        stdin.start();
    }

    public void setStreams(final Appendable out, final Appendable err) throws Exception {
        setStdout(out);
        setStderr(err);
        getAgent().redirect(shell);
    }

    public void connect(final String host, final int port) throws Exception {
        super.connect(Agent.class, this, host, port);
    }

    /**
     * The shell port to use.
     * <ul>
     * <li>&lt;0 – Attach to a local Gogo CommandSession
     * <li>0 – Use the standard console
     * <li>else – Open a stream to that port
     * </ul>
     *
     * @param shellPort
     */
    public void setShell(final int shellPort) {
        shell = shellPort;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void abort() throws Exception {
        if (isOpen()) {
            getAgent().abort();
        }
    }

    public void redirect(final int shell) throws Exception {
        if (this.shell != shell && isOpen()) {
            getAgent().redirect(shell);
            this.shell = shell;
        }
    }

    @Override
    public void onOSGiEvent(final XEventDTO event) {
        eventListeners.stream().filter(l -> matchTopic(event.topic, l.topics())).forEach(listener -> listener.onEvent(event));
    }

    @Override
    public void logged(final XLogEntryDTO logEvent) {
        logEntryListeners.forEach(listener -> listener.logged(logEvent));
    }

    @Override
    public void addOSGiEventListener(final EventListener eventListener) {
        if (eventListeners.contains(eventListener)) {
            return;
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeOSGiEventListener(final EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    @Override
    public void addOSGiLogListener(final LogEntryListener logEntryListener) {
        if (logEntryListeners.contains(logEntryListener)) {
            return;
        }
        logEntryListeners.add(logEntryListener);
    }

    @Override
    public void removeOSGiLogListener(final LogEntryListener logEntryListener) {
        logEntryListeners.remove(logEntryListener);
    }

    @Override
    public void connect(final String host, final int port, final int timeout) throws Exception {
        super.connect(Agent.class, this, host, port, timeout);
    }

    private static boolean matchTopic(final String receivedEventTopic, final Collection<String> listenerTopics) {
        if (listenerTopics.contains("*")) {
            return true;
        }
        for (final String topic : listenerTopics) {
            // positive match if only *
            if ("*".equals(topic)) {
                return true;
            }
            // positive match if it does contain * at the end and is a substring of the
            // received event topic
            if (topic.contains("*")) {
                final var prefix = topic.substring(0, topic.lastIndexOf('/'));
                if (receivedEventTopic.startsWith(prefix)) {
                    return true;
                }
            }
            // positive match if the it matches exactly the received event topic
            if (receivedEventTopic.equalsIgnoreCase(topic)) {
                return true;
            }
        }
        return false;
    }
}
