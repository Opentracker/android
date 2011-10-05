package net.opentracker.android.test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TestUpload {

    // http://groups.google.com/group/android-developers/browse_thread/thread/7fe20f9af396b520
    // http://stackoverflow.com/questions/5474916/multipartentity-not-creating-good-request
    public void upload() {
        // WORKS WITH JPEG FILE -- String existingFileName =
        // "/sdcard/dcim/ Camera/1225231027592.jpg";
        String existingFileName = "/sdcard/Music/kryptonite.mp3"; // DOES NOT
        // WORK WITH
        // MP3 FILE
        File uploadFile = new File(existingFileName);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(uploadFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL connectURL = new URL("http://www.mysite.com/uploads.php");
            // connectURL is a URL object
            HttpURLConnection conn =
                    (HttpURLConnection) connectURL.openConnection();
            // allow inputs
            conn.setDoInput(true);
            // allow outputs
            conn.setDoOutput(true);
            // don't use a cached copy
            conn.setUseCaches(false);
            // use a post method
            conn.setRequestMethod("POST");
            // set post headers
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);
            // open data output stream
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos
                    .writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
                            + existingFileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            // create a buffer of maximum size
            int bytesAvailable = fileInputStream.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            // read file and write it into form...
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
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
            InputStream is = conn.getInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            // String s = b.toString();
            dos.close();
        } catch (MalformedURLException ex) {
            // Log.e(Tag, "error: " + ex.getMessage(), ex);
        } catch (IOException ioe) {
            // Log.e(Tag, "error: " + ioe.getMessage(), ioe);
        }
    }
}
