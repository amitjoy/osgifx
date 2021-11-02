package in.bytehue.osgifx.console.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import in.bytehue.osgifx.console.download.Authentication;
import in.bytehue.osgifx.console.download.DownloadListener;
import in.bytehue.osgifx.console.download.DownloadTask;
import in.bytehue.osgifx.console.download.Downloader;
import in.bytehue.osgifx.console.download.HttpConnector;

@Component
public final class DownloaderProvider extends HttpConnector implements Downloader {

    private static final int POOL_SIZE = 10;

    @Reference
    private LoggerFactory               factory;
    private FluentLogger                logger;
    private Proxy                       proxy;
    private BlockingQueue<DownloadTask> tasks;
    private ExecutorService             taskExecutor;

    @Activate
    void activate() {
        tasks        = new LinkedBlockingQueue<>();
        taskExecutor = Executors.newFixedThreadPool(POOL_SIZE, r -> new Thread(r, "downloader"));
        logger       = FluentLogger.of(factory.createLogger(getClass().getName()));

        taskExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final DirectDownload dl = new DirectDownload(tasks.take());
                    dl.download();
                } catch (final InterruptedException e) {
                    logger.atInfo().log("Stopping download thread");
                    Thread.currentThread().interrupt();
                    break;
                } catch (final Exception e) {
                    logger.atWarning().log(e.getMessage());
                }
            }
        });
    }

    @Deactivate
    void deactivate() {
        taskExecutor.shutdownNow();
    }

    protected class DirectDownload {

        private static final String CD_FNAME            = "fname=";
        private static final String CONTENT_DISPOSITION = "Content-Disposition";

        private volatile boolean cancel;
        private volatile boolean stop;

        private final DownloadTask task;

        public DirectDownload(final DownloadTask task) {
            this.task = task;
        }

        protected void download() throws IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {

            final HttpURLConnection conn = (HttpURLConnection) getConnection(task.getUrl(), proxy);

            if (task.getAuthentication() != null) {
                final Authentication auth       = task.getAuthentication();
                final String         authString = auth.getUsername() + ":" + auth.getPassword();
                conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encode(authString.getBytes()));
            }

            conn.setReadTimeout(task.getTimeout());
            conn.setDoOutput(true);
            conn.connect();

            final int fsize = conn.getContentLength();
            String    fname;

            final String cd = conn.getHeaderField(CONTENT_DISPOSITION);

            if (cd != null) {
                fname = cd.substring(cd.indexOf(CD_FNAME) + 1, cd.length() - 1);
            } else {
                final String url = task.getUrl().toString();
                fname = url.substring(url.lastIndexOf('/') + 1);
            }

            final InputStream            is        = conn.getInputStream();
            final OutputStream           os        = task.getOutputStream();
            final List<DownloadListener> listeners = task.getListeners();
            final byte[]                 buff      = new byte[BUFFER_SIZE];
            int                          res;

            for (final DownloadListener listener : listeners) {
                listener.onStart(fname, fsize);
            }

            int total = 0;
            while ((res = is.read(buff)) != -1) {
                os.write(buff, 0, res);
                total += res;
                for (final DownloadListener listener : listeners) {
                    listener.onUpdate(res, total);
                }
                synchronized (task) {
                    // cancel download
                    if (cancel || task.isCancelled()) {
                        close(is, os);
                        for (final DownloadListener listener : listeners) {
                            listener.onCancel();
                        }
                        throw new RuntimeException("Cancelled download");
                    }
                    // stop thread
                    if (stop) {
                        close(is, os);
                        for (final DownloadListener listener : listeners) {
                            listener.onCancel();
                        }
                        throw new InterruptedException("Shutdown");
                    }
                    // pause thread
                    while (task.isPaused()) {
                        try {
                            wait();
                        } catch (final Exception e) {
                        }
                    }
                }
            }
            for (final DownloadListener listener : listeners) {
                listener.onComplete();
            }
            close(is, os);
        }

        private void close(final InputStream is, final OutputStream os) {
            try {
                is.close();
                os.close();
            } catch (final IOException e) {
            }
        }

        public void cancel() {
            cancel = true;
        }

        public void shutdown() {
            stop = true;
        }
    }

    @Override
    public void download(final DownloadTask task) {
        tasks.add(task);
    }

}
