package in.bytehue.osgifx.console.download;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service API to be used to download file(s) from a specific URL
 */
@ProviderType
public interface Downloader {

    /**
     * Downloads the file associated from the specified task
     *
     * @param task the download task
     */
    void download(DownloadTask task);
}
