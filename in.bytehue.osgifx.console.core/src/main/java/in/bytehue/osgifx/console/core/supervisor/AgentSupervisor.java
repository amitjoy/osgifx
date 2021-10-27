package in.bytehue.osgifx.console.core.supervisor;

import static in.bytehue.osgifx.console.supervisor.Supervisor.CONNECTED_AGENT;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final Map<File, Info>          fileInfo    = new ConcurrentHashMap<>();
    private static final MultiMap<String, String> shaInfo     = new MultiMap<>();
    private static final int                      connectWait = 200;
    private static byte[]                         EMPTY       = {};
    private A                                     agent;
    private final CountDownLatch                  latch       = new CountDownLatch(1);
    protected volatile int                        exitCode;
    private Link<S, A>                            link;
    private final AtomicBoolean                   quit        = new AtomicBoolean(false);
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
            throw new IllegalArgumentException("timeout can not be less than -1");
        }

        int retryTimeout = timeout;
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;

        System.setProperty(CONNECTED_AGENT, host + ":" + port);

        while (true) {
            try {
                final Socket socket = new Socket();
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
                    retryTimeout = Math.max(retryTimeout - connectWait, 0);
                }
                Thread.sleep(connectWait);
            }
        }
    }

    public byte[] getFile(final String sha) throws Exception {
        List<String> copy;
        synchronized (shaInfo) {
            final List<String> list = shaInfo.get(sha);
            if (list == null) {
                return EMPTY;
            }

            copy = new ArrayList<>(list);
        }
        for (final String path : copy) {
            final File f = new File(path);
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

    public String addFile(File file) throws NoSuchAlgorithmException, Exception {
        file = file.getAbsoluteFile();
        Info info = fileInfo.get(file);
        if (info == null) {
            info = new Info();
            fileInfo.put(file, info);
            info.lastModified = -1;
        }

        synchronized (shaInfo) {
            if (info.lastModified != file.lastModified()) {
                final String sha = SHA1.digest(file).asHex();
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
