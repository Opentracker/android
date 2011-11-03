/*
 *  Copyright (C) 2011 Opentracker.net 
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 * 
 *  The full license is located at the root of this distribution
 *  in the LICENSE file.
 *
 *  Please report bugs to support@opentracker.net
 *
 */
package net.opentracker.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * OTSend provides methods for sending key value pairs to Opentracker's logging/
 * analytics engines via HTTP POST requests, and uploading (compressed) files.
 * 
 * For the HTTP POST requests to work on android you must declare Internet
 * permissions in your apps manifest by adding the following line to
 * AndroidManifest.xml. This allows your application to use any Internet
 * connections.
 * 
 * <uses-permission android:name="android.permission.INTERNET" />
 */
public class OTSend {

    private static final String DEFAULT_LOG_URL = "http://log.opentracker.net/";

    private static final String TAG = OTSend.class.getName();

    private static final String DEFAULT_UPLOAD_SERVER =
            "http://upload.opentracker.net/upload/upload.jsp";

    private static HttpURLConnection conn = null;

    private static DataOutputStream dos = null;

    private static DataInputStream inStream = null;

    private static final String lineEnd = "\r\n";

    private static final String twoHyphens = "--";

    private static final String boundary = "*****";

    private static int bytesRead, bytesAvailable, bufferSize;

    private static byte[] buffer;

    private static final int maxBufferSize = 1 * 1024 * 1024;

    private OTSend() {
    }

    /**
     * Method used for uploading a file to the default upload server
     * 
     * 
     * @param pathName
     *            The path to use taking the apps context into account
     * @param fileName
     *            The file name to append to
     */
    public static void uploadFile(String pathName, String fileName) {
        Log.v(TAG, "uploadFile()");
        uploadFile(DEFAULT_UPLOAD_SERVER, pathName, fileName);
    }

    /**
     * Method used for uploading a file to a server;
     * 
     * @param uploadServer
     *            The server to upload the file to
     * @param pathName
     *            The path to use taking the apps context into account
     * @param fileName
     *            The file name to append to
     */
    public static void uploadFile(String uploadServer, String pathName,
            String fileName) {
        Log.v(TAG, "uploadFile()");

        String randomFileName = UUID.randomUUID() + ".gz";
        try {
            // ------------------ CLIENT REQUEST
            FileInputStream fileInputStream =
                    new FileInputStream(new File(pathName + fileName));

            // Open a URL connection to the Servlet
            URL url = new URL(uploadServer);

            // Open a HTTP connection to the URLs
            conn = (HttpURLConnection) url.openConnection();

            // Allow Inputs
            conn.setDoInput(true);

            // Allow Outputs
            conn.setDoOutput(true);

            // Don't use a cached copy.
            conn.setUseCaches(false);

            // Use a post method.
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Connection", "Keep-Alive");

            conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"upload\";"
                    + " filename=\"" + randomFileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // close streams
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            Log.e(TAG, "From ServletCom CLIENT REQUEST: " + ex);

        } catch (IOException ioe) {
            Log.e(TAG, "From ServletCom CLIENT REQUEST: " + ioe);
        }

        // ------------------ read the SERVER RESPONSE
        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;
            while ((str = inStream.readLine()) != null) {
                Log.v(TAG, "Server response: " + str);
            }
            inStream.close();

        } catch (IOException ioex) {
            Log.e(TAG, "Server response: " + ioex);
        }

    }

    /**
     * Sends the key value pairs to Opentracker's logging/ analytics engines via
     * HTTP POST requests.
     * 
     * Based on sending key value pairs documentated at:
     * http://api.opentracker.net/api/inserts/insert_event.jsp
     * 
     * @param keyValuePairs
     *            the key value pairs (plain text utf-8 strings) to send to the
     *            logging service.
     * 
     * @return the response as string, null if an exception is caught
     */
    public static String send(HashMap<String, String> keyValuePairs) {
        Log.v(TAG, "send()");

        // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
        // http://hc.apache.org/httpclient-3.x/tutorial.html
        HttpClient client = new DefaultHttpClient();

        HttpPost post = new HttpPost(DEFAULT_LOG_URL);

        Iterator<Entry<String, String>> it =
                keyValuePairs.entrySet().iterator();

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        while (it.hasNext()) {

            Map.Entry<String, String> pair =
                    (Map.Entry<String, String>) it.next();
            pairs.add(new BasicNameValuePair(pair.getKey(), pair.getValue()));
            Log.v(TAG, pair.getKey() + " = " + pair.getValue());

        }

        String responseText = null;
        try {

            post.setEntity(new UrlEncodedFormEntity(pairs));
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // http://hc.apache.org/httpclient-3.x/tutorial.html
            // It is vital that the response body is always read regardless of
            // the status returned by the server.
            responseText = getResponseBody(entity);

            Log.v(TAG, "Success url:" + post.toString());
            return responseText;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        }
        return responseText;
    }

    /**
     * Sends a post request with the url via HTTP POST requests.
     * 
     * @return the response as string, null if an exception is caught
     */
    private static String send(String url) {
        Log.v(TAG, "send()");

        HttpClient client = new DefaultHttpClient();
        Log.i(TAG, "url being read:" + url);
        HttpPost post = new HttpPost(url);
        // post.setEntity(url);
        String responseText = null;
        try {
            HttpResponse response = client.execute(post);
            HttpEntity entity = null;
            entity = response.getEntity();
            responseText = getResponseBody(entity);
            Log.v(TAG, "the url:" + post.toString());
            Log.v(TAG, "Response after sending data:" + responseText);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e);
        }
        return responseText;
    }

    /**
     * getResponseBody function gives out the HTTP POST data from the given
     * httpResponse output: data from the http as string input : httpEntity type
     * 
     * based on:
     * http://thinkandroid.wordpress.com/2009/12/30/getting-response-body
     * -of-httpresponse/
     */
    private static String getResponseBody(final HttpEntity entity)
            throws IOException, ParseException {
        Log.v(TAG, "getResponseBody()");

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream instream = entity.getContent();
        if (instream == null) {
            return "";
        }
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "HTTP entity too large to be buffered in memory");
        }
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }
        Reader reader = new InputStreamReader(instream, charset);
        StringBuilder buffer = new StringBuilder();
        try {
            char[] tmp = new char[1024];
            int l;
            while ((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

}
