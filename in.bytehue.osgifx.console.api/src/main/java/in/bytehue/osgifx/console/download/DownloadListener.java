package in.bytehue.osgifx.console.download;

/**
 * Callback the be associted with the download operation
 */
public interface DownloadListener {

    /**
     * Callback to be executed when the download operation gets started
     *
     * @param fname the file name
     * @param fsize the file size
     */
    void onStart(String fname, int fsize);

    /**
     * Callback to be executed when part of the files get downloaded
     *
     * @param bytes the amount downloaded so far
     * @param totalDownloaded the total downloaded amount
     */
    void onUpdate(int bytes, int totalDownloaded);

    /**
     * Callback to be executed when the download operation is finished
     */
    void onComplete();

    /**
     * Callback when the download operation is cancelled
     */
    void onCancel();
}