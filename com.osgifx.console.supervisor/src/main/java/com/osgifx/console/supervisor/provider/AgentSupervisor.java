/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;

import aQute.bnd.util.dto.DTO;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.remote.util.Link;

/**
 * This is a base class that provides the basic functionality of a supervisor.
 * In general an actual supervisor extends this class to provide the
 * functionality to use on the client side.
 *
 * @param <S> The supervisor type
 * @param <A> The agent type
 */
public class AgentSupervisor<S, A> {

	private static final Map<File, Info>          fileInfo     = new ConcurrentHashMap<>();
	private static final MultiMap<String, String> shaInfo      = new MultiMap<>();
	private static final int                      CONNECT_WAIT = 200;
	private static final byte[]                   EMPTY        = {};
	private A                                     agent;
	private final CountDownLatch                  latch        = new CountDownLatch(1);
	protected volatile int                        exitCode;
	private Link<S, A>                            link;
	private final AtomicBoolean                   quit         = new AtomicBoolean(false);
	protected String                              host;
	protected int                                 port;
	protected int                                 timeout;

	static class Info extends DTO {
		public String sha;
		public long   lastModified;
	}

	protected void connect(final Class<A> agent, final S supervisor, final String host, final int port) throws Exception {
		connect(agent, supervisor, host, port, -1);
	}

	protected void connect(final Class<A> agent, final S supervisor, final String host, final int port, final int timeout)
	        throws Exception {
		if (timeout < -1) {
			throw new IllegalArgumentException("timeout cannot be less than -1");
		}

		var retryTimeout = timeout;
		this.host    = host;
		this.port    = port;
		this.timeout = timeout;

		while (true) {
			try {
				final var socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), Math.max(timeout, 0));
				link = new Link<>(agent, supervisor, socket);
				this.setAgent(link);
				link.open();
				return;
			} catch (final ConnectException e) {
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

	public byte[] getFile(final String sha) throws Exception {
		List<String> copy;
		synchronized (shaInfo) {
			final var list = shaInfo.get(sha);
			if (list == null) {
				return EMPTY;
			}

			copy = Lists.newArrayList(list);
		}
		for (final String path : copy) {
			final var f = new File(path);
			if (f.isFile()) {
				return IO.read(f);
			}
		}
		return EMPTY;
	}

	public void setAgent(final Link<S, A> link) {
		this.agent = link.getRemote();
		this.link  = link;
	}

	public void close() throws IOException {
		if (quit.getAndSet(true)) {
			return;
		}

		if (link.isOpen()) {
			link.close();
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

	public String addFile(File file) throws Exception {
		file = file.getAbsoluteFile();
		final var info = fileInfo.computeIfAbsent(file, f -> {
			final var i = new Info();
			i.lastModified = -1;
			return i;
		});
		synchronized (shaInfo) {
			if (info.lastModified != file.lastModified()) {
				final var sha = SHA1.digest(file).asHex();
				if (info.sha != null && !sha.equals(info.sha)) {
					shaInfo.removeValue(info.sha, file.getAbsolutePath());
				}
				info.sha          = sha;
				info.lastModified = file.lastModified();
				shaInfo.add(sha, file.getAbsolutePath());
			}
			return info.sha;
		}
	}

	public boolean isOpen() {
		return link.isOpen();
	}

}
