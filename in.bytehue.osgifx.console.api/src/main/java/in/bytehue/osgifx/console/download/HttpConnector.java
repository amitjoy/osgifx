package in.bytehue.osgifx.console.download;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpConnector {

    protected int BUFFER_SIZE                = 2048;
    protected int DEFAULT_STREAM_BUFFER_SIZE = 3072;
    protected int DEFAULT_CONNECT_TIMEOUT    = 13000;

    private int connectionTimeout = DEFAULT_CONNECT_TIMEOUT;

    private final Map<String, String> headers       = new HashMap<>();
    private String                    requestMethod = "GET";

    public HttpConnector() {
        HttpURLConnection.setFollowRedirects(true);
    }

    /**
     * @param url
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    protected URLConnection getConnection(final URL url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if ("http".equalsIgnoreCase(url.getProtocol()) || "ftp".equalsIgnoreCase(url.getProtocol())) {
            return getConnection(url, null);
        }
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            return getSecureConnection(url);
        }

        return null;
    }

    /**
     * @param url
     * @param proxy
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    protected URLConnection getConnection(final URL url, final Proxy proxy)
            throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if ("http".equalsIgnoreCase(url.getProtocol()) || "ftp".equalsIgnoreCase(url.getProtocol())) {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy == null ? Proxy.NO_PROXY : proxy);
            conn.setRequestMethod(requestMethod);
            setHeaders(conn);
            return conn;
        }
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            return getSecureConnection(url, proxy);
        }

        return null;
    }

    /**
     * @param url
     * @return
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    protected HttpsURLConnection getSecureConnection(final URL url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        return getSecureConnection(url, null);
    }

    /**
     * @param url
     * @param proxy
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    protected HttpsURLConnection getSecureConnection(final URL url, final Proxy proxy)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());

        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(proxy == null ? Proxy.NO_PROXY : proxy);
        conn.setRequestMethod(requestMethod);
        setHeaders(conn);

        conn.setSSLSocketFactory(context.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);

        conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        return conn;
    }

    /**
     * @param conn
     * @param data
     * @throws IOException
     */
    protected void doOutput(final URLConnection conn, final String data) throws IOException {
        final BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()), DEFAULT_STREAM_BUFFER_SIZE);

        wr.write(data);
        wr.flush();
        wr.close();
    }

    /**
     * @param conn
     * @return
     * @throws IOException
     */
    protected StringBuffer doInput(final URLConnection conn) throws IOException {
        final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()), DEFAULT_STREAM_BUFFER_SIZE);

        final StringBuffer buff = new StringBuffer();

        final char[] bb = new char[BUFFER_SIZE];
        int          nob;

        while ((nob = rd.read(bb)) != -1) {
            buff.append(new String(bb, 0, nob));
        }

        rd.close();

        return buff;
    }

    protected final static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private void setHeaders(final URLConnection uc) {
        final Iterator<String> itr = headers.keySet().iterator();
        while (itr.hasNext()) {
            final String key = itr.next();
            uc.addRequestProperty(key, headers.get(key));
        }
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void addHeader(final String key, final String value) {
        headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }
}