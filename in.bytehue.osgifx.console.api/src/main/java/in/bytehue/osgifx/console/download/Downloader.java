package in.bytehue.osgifx.console.download;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Downloader {

    void download(DownloadTask task);

    void shutdown();

    void cancelAll();
}
