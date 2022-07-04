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
package com.osgifx.console.agent.redirector;

import static com.osgifx.console.agent.Agent.CONSOLE;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.osgifx.console.agent.provider.AgentServer;

/**
 * The Console Redirector redirects System.in, System.out, and System.err to the
 * supervisor. This is of course quite tricky. We have to replace System.*
 * fields and this can only be done if the security allows it. It also requires
 * that if we have multiple agents we need to share the redirecting.
 */
public class ConsoleRedirector implements Redirector {

    private static RedirectOutput    stdout;
    private static RedirectOutput    stderr;
    private static RedirectInput     stdin;
    private static List<AgentServer> agents = new CopyOnWriteArrayList<>();
    private final AgentServer        agent;

    /**
     * Constructor.
     *
     * @param agent the agent we're redirecting for
     */
    public ConsoleRedirector(final AgentServer agent) throws IOException {
        this.agent = agent;
        synchronized (agents) {
            if (!agents.contains(agent)) {
                agents.add(agent);
                if (agents.size() == 1) {
                    System.setOut(stdout = new RedirectOutput(agents, System.out, false));
                    System.setErr(stderr = new RedirectOutput(agents, System.err, true));
                    System.setIn(stdin = new RedirectInput(System.in));
                }
            }
        }
    }

    /**
     * Clean up
     */
    @Override
    public void close() throws IOException {
        synchronized (agents) {
            if (agents.remove(agent) && agents.isEmpty()) {
                System.setOut(stdout.getOut());
                System.setErr(stderr.getOut());
                System.setIn(stdin.getOrg());
            }
        }
    }

    @Override
    public int getPort() {
        return CONSOLE;
    }

    @Override
    public void stdin(final String s) throws IOException {
        stdin.add(s);
    }

    @Override
    public PrintStream getOut() {
        return stdout;
    }

}
