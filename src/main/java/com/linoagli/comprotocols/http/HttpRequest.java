/**
 * comm-protocols Project.
 * com.linoagli.comprotocols.http
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols.http;

import com.linoagli.comprotocols.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple HTTP request implementation class.
 */
public class HttpRequest {
    /**
     * The supported HTTP request methods
     */
    public enum Method { GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD, TRACE }

    public static final String CHARSET_UTF_8 = "UTF-8";
    public static final String CHARSET_US_ASCII = "US-ASCII";
    public static final String CHARSET_ISO_88591 = "ISO-8859-1";

    public static final String MIME_TYPE_WEB_FORM = "application/x-www-form-urlencoded";
    public static final String MIME_TYPE_JSON = "application/json";

    private Callback callback;

    private String contentType;
    private String charset;
    private String authorization;
    private int timeOutDelay;
    private Method method;
    private String url;
    private String params;
    private boolean wasSuccessful;
    private int responseCode;
    private String responseString;

    public HttpRequest() {
        contentType = MIME_TYPE_WEB_FORM;
        charset = CHARSET_UTF_8;
        authorization = null;
        timeOutDelay = 15000;
    }

    /**
     * Sets the content type for this HTTP request instance.
     *
     * @param contentType the content type
     */
    public void setContentType(String contentType) {
        if (Utils.isStringEmpty(contentType)) return;

        this.contentType = contentType;
    }

    /**
     * Sets the charset for this HTTP request instance.
     *
     * @param charset the charset
     */
    public void setCharset(String charset) {
        if (Utils.isStringEmpty(charset)) return;

        this.charset = charset;
    }

    /**
     * Sets <i>Authorization</i> header for this HTTP request instance.
     *
     * @param authorization the authorization value
     * @param isBasic whether or not this authorization is basic
     * @param shouldEncode whether or not to encode the value using Base64
     */
    public void setAuthorization(String authorization, boolean isBasic, boolean shouldEncode) {
        if (Utils.isStringEmpty(authorization)) return;

        this.authorization = (isBasic) ? "Basic " : "";
        this.authorization += (shouldEncode) ? Utils.encodeBase64(authorization) : authorization;
    }

    /**
     * Set's the request's connection and read time out delay before a time out exception is raised internally causing
     * the request to fail.
     *
     * @param timeOutDelay the time out delay in milliseconds
     */
    public void setTimeOutDelay(int timeOutDelay) {
        this.timeOutDelay = timeOutDelay;
    }

    /**
     * @return the url that was used to submit the HTTP request.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return whether or not the request was successful.
     */
    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    /**
     * @return the HTTP request's server response code or -1 if the request failed at the library code level.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @return the HTTP request's server response string or an error message if the request failed at the library code level.
     */
    public String getResponseString() {
        return responseString;
    }

    /**
     * Posts a new <i>synchronous</i> HTTP request.
     *
     * @param method the request method (see {@link Method})
     * @param url the request url or api endpoint
     * @param params the request parameters. (<b>Recommended</b>: use {@link ParamsBuilder})
     * @return this {@link HttpRequest} instance.
     */
    public HttpRequest post(Method method, String url, String params) {
        return postRequest(method, url, params, false, null);
    }

    /**
     * Posts a new <i>asynchronous</i> HTTP request.
     *
     * @param method the request method (see {@link Method})
     * @param url the request url or api endpoint
     * @param params the request parameters. (<b>Recommended</b>: use {@link ParamsBuilder})
     * @param callback an instance of {@link HttpRequest.Callback}
     */
    public void postAsync(Method method, String url, String params, Callback callback) {
        postRequest(method, url, params, true, callback);
    }

    private HttpRequest postRequest(Method method, String url, String params, boolean async, Callback callback) {
        this.method = method;
        this.url = url;
        this.params = params;
        this.callback = callback;

        this.wasSuccessful = false;
        this.responseCode = -1;
        this.responseString = null;

        if (async) {
            new Thread(this::doRequest).start();
        } else {
            doRequest();
        }

        return this;
    }

    private void doRequest() {
        try {
            HttpURLConnection connection = generateConnection();

            // Retrieving string response code
            responseCode = connection.getResponseCode();
            wasSuccessful = (responseCode / 100) == 2;

            // Retrieving the response string
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                try {

                    InputStream is = (wasSuccessful) ? connection.getInputStream() : connection.getErrorStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int data = 0;
                    while ((data = bis.read()) != -1) baos.write(data);

                    is.close();
                    responseString = new String(baos.toByteArray());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Cleaning up
            connection.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();

            wasSuccessful = false;
            responseCode = -1;
            responseString = e.getMessage();
        }

        if (callback != null) callback.onRequestComplete(this);
    }

    private HttpURLConnection generateConnection() throws Exception {
        // Creating proper request string and java URL instance
        boolean isGetRequest = method == Method.GET;
        boolean hasParams = params != null && !params.trim().isEmpty();

        String request = this.url;

        if (isGetRequest && hasParams) request += "?" + params;

        URL url = new URL(request);

        // Setting up the http connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method.name());
        connection.setDoInput(true);
        connection.setDoOutput(!isGetRequest && hasParams);
        connection.setUseCaches(false);

        // Setting timeout delays
        connection.setConnectTimeout(timeOutDelay);
        connection.setReadTimeout(timeOutDelay);

        // Setting the headers
        connection.setRequestProperty("Content-Type", contentType + "; charset=" + charset);

        if (authorization != null && !authorization.isEmpty()) {
            connection.setRequestProperty("Authorization", authorization);
        }

//        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");   // TODO url special properties support

        // Establishing connection
        connection.connect();

        // Writing the url parameters to the output stream if necessary
        if (connection.getDoOutput()) {
            OutputStream os = connection.getOutputStream();
            os.write(params.getBytes(charset));
            os.flush();
            os.close();
        }

        return connection;
    }

    /**
     * This is a helper class used to construct the HTTP request parameters
     */
    public static class ParamsBuilder {
        private List<String> params = new ArrayList<String>();

        /**
         * Adds a request parameter.
         *
         * @param key the parameter key
         * @param value the parameter value
         * @param shouldEncodeValue whether or not to encode the parameter value
         * @return the instance of {@link ParamsBuilder}
         */
        public ParamsBuilder add(String key, String value, boolean shouldEncodeValue) {
            String item = null;

            try {
                item = key + "=" + URLEncoder.encode(value, CHARSET_UTF_8);
                params.add(item);
            }
            catch (Exception e) {
                System.err.println("Something bad happened while trying to add param " + key + "=" + value + ": " + e.getMessage());
            }

            return this;
        }

        /**
         * @return the proper string representation of the parameter list
         */
        public String toString() {
            return Utils.listItemsToString(params, "&");
        }
    }

    /**
     * The HTTP request callback interface.
     */
    public interface Callback {
        /**
         * Notifies the object implementing this interface that the request was successful.
         *
         * @param request the {@link HttpRequest} instance that fired this callback. The request's results can be
         *                retrieved here (ie: success, response code, string, etc...)
         */
        public void onRequestComplete(HttpRequest request);
    }
}
