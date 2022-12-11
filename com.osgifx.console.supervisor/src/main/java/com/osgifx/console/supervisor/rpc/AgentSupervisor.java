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
package com.osgifx.console.supervisor.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSocketFactory;

import com.osgifx.console.agent.link.RemoteRPC;

public class AgentSupervisor<S, A> {

    private static final int     CONNECT_WAIT = 200;
    private A                    agent;
    private final CountDownLatch latch        = new CountDownLatch(1);
    protected volatile int       exitCode;
    private RemoteRPC<S, A>      remoteRPC;
    private final AtomicBoolean  quit         = new AtomicBoolean();
    protected String             host;
    protected int                port;
    protected int                timeout;

    protected void connect(final Class<A> agent,
                           final S supervisor,
                           final String host,
                           final int port) throws Exception {
        connect(agent, supervisor, host, port, -1, null, null);
    }

    protected void connect(final Class<A> agent,
                           final S supervisor,
                           final String host,
                           final int port,
                           final int timeout,
                           final String trustStore,
                           final String trustStorePassword) throws Exception {

        checkArgument(timeout > -1, "timeout cannot be less than -1");
        requireNonNull(supervisor, "'supervisor' cannot be null");
        requireNonNull(host, "'host' cannot be null");

        var retryTimeout = timeout;
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;

        while (true) {
            try {
                SSLSocketFactory sf = null;
                if (trustStore != null && trustStorePassword != null) {
                    System.setProperty("javax.net.ssl.trustStore", trustStore);
                    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
                    System.setProperty("javax.net.ssl.trustStoreType", "JKS");

                    sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                }
                final var socket = sf == null ? new Socket() : sf.createSocket();
                socket.connect(new InetSocketAddress(host, port), Math.max(timeout, 0));
                remoteRPC = new RemoteRPC<>(agent, supervisor, socket);
                this.setAgent(remoteRPC);
                remoteRPC.open();
                return;
            } catch (final ConnectException e) {
                clearSSLProperties();
                if (retryTimeout == 0) {
                    throw e;
                }
                if (retryTimeout > 0) {
                    retryTimeout = Math.max(retryTimeout - CONNECT_WAIT, 0);
                }
                Thread.sleep(CONNECT_WAIT);
            }
        }
    }

    public void setAgent(final RemoteRPC<S, A> link) {
        this.agent     = link.getRemote();
        this.remoteRPC = link;
    }

    public void close() throws IOException {
        clearSSLProperties();
        if (quit.getAndSet(true)) {
            return;
        }
        if (remoteRPC.isOpen()) {
            remoteRPC.close();
        }
        latch.countDown();
    }

    public int join() throws InterruptedException {
        latch.await();
        return exitCode;
    }

    protected void exit(final int exitCode) {
        this.exitCode = exitCode;
        try {
            close();
        } catch (final Exception e) {
            // ignore
        }
    }

    public A getAgent() {
        return agent;
    }

    public boolean isOpen() {
        return remoteRPC.isOpen();
    }

    private void clearSSLProperties() {
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
    }

}
