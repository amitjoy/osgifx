package in.bytehue.osgifx.console.download;

import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadTask {

    private URL                          url;
    private OutputStream                 outputStream;
    private final List<DownloadListener> listeners = new CopyOnWriteArrayList<>();

    private boolean paused;
    private boolean cancelled;
    private int     timeout = 15000;

    private Authentication authentication;

    public DownloadTask(final URL url, final OutputStream outputStream) {
        this.url          = url;
        this.outputStream = outputStream;
    }

    public DownloadTask(final URL url, final OutputStream outputStream, final DownloadListener listener) {
        this.url          = url;
        this.outputStream = outputStream;
        listeners.add(listener);
    }

    public URL getUrl() {
        return url;
    }

    public DownloadTask setUrl(final URL url) {
        this.url = url;
        return this;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public DownloadTask setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }

    public List<DownloadListener> getListeners() {
        return listeners;
    }

    public DownloadTask addListener(final DownloadListener listener) {
        listeners.add(listener);
        return this;
    }

    public DownloadTask removeListener(final DownloadListener listener) {
        listeners.remove(listener);
        return this;
    }

    public DownloadTask removeAllListener() {
        listeners.clear();
        return this;
    }

    public boolean isPaused() {
        return paused;
    }

    public DownloadTask setPaused(final boolean paused) {
        this.paused = paused;
        return this;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public DownloadTask setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public DownloadTask setTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public DownloadTask setAuthentication(final Authentication authentication) {
        this.authentication = authentication;
        return this;
    }
}