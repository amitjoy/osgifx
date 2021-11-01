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
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import in.bytehue.osgifx.console.download.Authentication;
import in.bytehue.osgifx.console.download.DownloadListener;
import in.bytehue.osgifx.console.download.DownloadTask;
import in.bytehue.osgifx.console.download.Downloader;
import in.bytehue.osgifx.console.download.HttpConnector;

@Component
public final class DownloaderProvider extends HttpConnector implements Downloader {

    @Reference
    private LoggerFactory factory;

    private FluentLogger logger;

    private final int poolSize   = 3;
    private final int bufferSize = 2048;

    private DirectDownloadThread[]            dts;
    private Proxy                             proxy;
    private final BlockingQueue<DownloadTask> tasks = new LinkedBlockingQueue<>();

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    protected class DirectDownloadThread extends Thread {
        private static final String CD_FNAME            = "fname=";
        private static final String CONTENT_DISPOSITION = "Content-Disposition";

        private boolean cancel = false;
        private boolean stop   = false;

        private final BlockingQueue<DownloadTask> tasks;

        public DirectDownloadThread(final BlockingQueue<DownloadTask> tasks) {
            this.tasks = tasks;
        }

        protected void download(final DownloadTask dt)
                throws IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {

            final HttpURLConnection conn = (HttpURLConnection) getConnection(dt.getUrl(), proxy);

            if (dt.getAuthentication() != null) {
                final Authentication auth       = dt.getAuthentication();
                final String         authString = auth.getUsername() + ":" + auth.getPassword();
                conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encode(authString.getBytes()));
            }

            conn.setReadTimeout(dt.getTimeout());
            conn.setDoOutput(true);
            conn.connect();

            final int fsize = conn.getContentLength();
            String    fname;

            final String cd = conn.getHeaderField(CONTENT_DISPOSITION);

            if (cd != null) {
                fname = cd.substring(cd.indexOf(CD_FNAME) + 1, cd.length() - 1);
            } else {
                final String url = dt.getUrl().toString();
                fname = url.substring(url.lastIndexOf('/') + 1);
            }

            final InputStream is = conn.getInputStream();

            final OutputStream           os        = dt.getOutputStream();
            final List<DownloadListener> listeners = dt.getListeners();

            final byte[] buff = new byte[bufferSize];
            int          res;

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

                synchronized (dt) {
                    // cancel download
                    if (cancel || dt.isCancelled()) {
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
                    while (dt.isPaused()) {
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

        @Override
        public void run() {
            while (true) {
                try {
                    download(tasks.take());
                } catch (final InterruptedException e) {
                    logger.atInfo().log("Stopping download thread");
                    break;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
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
    public void download(final DownloadTask dt) {
        tasks.add(dt);
    }

    public void run() {
        logger.atInfo().log("Initializing downloader...");
        dts = new DirectDownloadThread[poolSize];
        for (int i = 0; i < dts.length; i++) {
            dts[i] = new DirectDownloadThread(tasks);
            dts[i].start();
        }
        logger.atInfo().log("Downloader started, waiting for tasks.");
    }

    @Override
    public void cancelAll() {
        for (final DirectDownloadThread dt : dts) {
            if (dt != null) {
                dt.cancel();
            }
        }
    }

}
