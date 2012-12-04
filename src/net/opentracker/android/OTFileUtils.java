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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import android.content.Context;

/**
 * OTFileUtils provides methods to manipulate files on an Android device for
 * Opentracker's OTLogging/ analytics engines.
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: OTFileUtils.java 14175 2012-03-08 15:59:48Z eddie $
 */
public class OTFileUtils {

    private static final String LINE_SEPARATOR =
            System.getProperty("line.separator");

    private static final String TAG = OTFileUtils.class.getName();

    /*
     * OTUpload has file to be uploaded
     */
    public final static String UPLOAD_PATH = "/OTUpload/";

    private final Context appContext;

    /**
     * Holds session state variables otui and ots
     */
    private final String sessionStateDirectory = "/OTState/";

    /**
     * Constructs an OTFileUtils with the Context used for saving data locally.
     * 
     * @param appContext
     */
    public OTFileUtils(Context appContext) {
        this.appContext = appContext;
    }

    /**
     * Method to append a string to a file
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @param fileName
     *            The file name to append to
     * @param writeString
     * @throws IOException
     */
    public void appendToFile(String pathName, String fileName,
            String writeString) throws IOException {
        LogWrapper.v(TAG, "appendToFile()");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;

        File file = new File(internalPath + fileName);
        if (file != null) {
            try {
                BufferedWriter writer =
                        new BufferedWriter(new FileWriter(file, true));
                writer.write(writeString);
                writer.newLine(); // Write system dependent end of line.
                writer.close();
            } catch (IOException e) {
                LogWrapper.w(TAG, "Write has failed: " + e.getMessage());
                throw new IOException();
            }
            return;// success
        }
        throw new IOException("Cannot write to file: " + fileName);
    }

    /**
     * Method to compress a file with a GZIPOutputStream
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @param fileName
     *            The file name to compress
     * @throws IOException
     */
    public void compressFile(String pathName, String fileName)
            throws IOException {
        LogWrapper.v(TAG, "compressFile()");
        // String localPath = path.replace("OTTest", "OTDir");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;
        try {
            File file = new File(internalPath + fileName);

            LogWrapper.v(TAG, "Entering method to zip the file: " + file);
            String gzipFile = file + ".gz";
            String gzipFileName = fileName + ".gz";

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(gzipFile);
            } catch (FileNotFoundException fnfe) {
                // No file, don't need to do anything
                LogWrapper.w(TAG, "File not found, not doing anything.");
                return;
            }

            LogWrapper.v(TAG, "Creating gzip file named: " + gzipFile);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);

            LogWrapper.v(TAG, "Opening stream");
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);

            LogWrapper.v(TAG, "Transferring file from" + file + " to "
                    + gzipFile);
            byte[] buffer = new byte[1024];
            int i;
            while ((i = in.read(buffer)) >= 0) {
                gzos.write(buffer, 0, i);
            }
            in.close();
            gzos.close();

            File fileGz = new File(internalPath + gzipFileName);
            LogWrapper.v(TAG, "File is in now gzip format, size:"
                    + fileGz.length() + "[bytes] from  " + file.length()
                    + "[bytes]");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String[] listFiles(String pathName) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "listFiles()");

        String internalPath = appContext.getFilesDir() + pathName;

        File dir = new File(internalPath);

        String[] children = dir.list();

        /*
         * if (children == null) { // Either dir does not exist or is not a
         * directory } else { for (int i = 0; i < children.length; i++) { // Get
         * filename of file or directory LogWrapper.e(TAG, "file: " +
         * children[i]); try { LogWrapper.e(TAG, "length: " +
         * getFileSize(pathName, children[i])); } catch (Exception e) { } } }
         */

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        if (dir == null)
            return null;
        else
            return children;

    }

    /**
     * Method to empty the contents of a file (writes zero bytes cf unix 'cat
     * /dev/null > emptyFile') in the sessionStateDirectory "/OTState/", if it
     * doesn't exist, then the file is created with the makeFile method.
     * 
     * 
     * @param fileName
     *            The file name to empty
     * @throws IOException
     */
    public void emptyFile(String fileName) throws IOException {
        LogWrapper.v(TAG, "emptyFile()");
        emptyFile(sessionStateDirectory, fileName);
    }

    /**
     * Method to empty the contents of a file (writes zero bytes cf unix 'cat
     * /dev/null > emptyFile') ,if it doesn't exist, then the file is created
     * with the makeFile method.
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @param fileName
     *            The file name to empty
     * @throws IOException
     */
    public void emptyFile(String pathName, String fileName) throws IOException {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "emptyFile()");
        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            return;
        }

        String internalPath = appContext.getFilesDir() + pathName;

        FileOutputStream erasor;
        try {
            erasor = new FileOutputStream(internalPath + fileName);
            erasor.write(new byte[0]);
            erasor.close();
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            makeFile(pathName, fileName);
            LogWrapper.w(TAG, "FileNotFoundException: " + e);
        } catch (IOException e) {
            // e.printStackTrace();
            makeFile(pathName, fileName);
            LogWrapper.w(TAG, "IOException: " + e);
        }

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");
    }

    public String getInternalPath(String pathName) {
        return appContext.getFilesDir() + pathName;
    }

    /**
     * Get the session state data; otui and ots data inside the
     * sessionStateDirectory as a hashmap: {otui: value, ots: value}.
     * 
     * @throws IOException
     */
    public HashMap<String, String> getSessionStateDataPairs()
            throws IOException {
        LogWrapper.v(TAG, "getSessionStateDataPairs()");

        File dir = new File(appContext.getFilesDir() + sessionStateDirectory);
        String[] children = dir.list();
        HashMap<String, String> fileNameDataPair =
                new HashMap<String, String>();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < children.length; i++) {
                // Get filename of file or directory
                String filename = children[i];
                String fileData = readFile(sessionStateDirectory, filename);
                fileNameDataPair.put(filename, fileData);
            }
        }
        LogWrapper.v(TAG, "getSessionStateDataPairs(): " + fileNameDataPair);

        return fileNameDataPair;
    }

    /**
     * Makes an empty file in the default sessionStateDirectory directory
     * ("/OTState/").
     * 
     * @param fileName
     *            The file name to make
     * @throws IOException
     *             If a file could not be created
     */
    public void makeFile(String fileName) throws IOException {
        LogWrapper.v(TAG, "makeFile()");
        makeFile(sessionStateDirectory, fileName);
    }

    /**
     * Makes an empty file in the path name given directory and creates this
     * directory if needed.
     * 
     * @param fileName
     *            The file name to make
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @throws IOException
     *             If a file could not be created
     */
    public void makeFile(String pathName, String fileName) throws IOException {
        LogWrapper.v(TAG, "makeFile(String pathName, String fileName)");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        // .getAbsolutePath() gives same results as getFilesDir() (?)
        String internalPath =
                appContext.getFilesDir().getAbsolutePath() + pathName;

        // LogWrapper.d(TAG, "got appContext.getFilesDir(): " +
        // appContext.getFilesDir());
        // LogWrapper.d(TAG, "got appContext.getFilesDir().getAbsolutePath(): "
        // + appContext.getFilesDir().getAbsolutePath());

        File file = new File(internalPath + fileName);
        if (file.exists()) {
            return;
        }

        // Otherwise, create any necessary directories, and the file itself.
        try {
            new File(internalPath).mkdirs();

            if (file.createNewFile()) {
                return;
            }
        } catch (IOException e) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException();
        }

    }

    /**
     * Method to read a file and get its contents as a string in the default
     * sessionStateDirectory directory ("/OTState/").
     * 
     * @param fileName
     *            The file name to read
     * @throws IOException
     */
    public String readFile(String fileName) throws IOException {
        LogWrapper.v(TAG, "readFile(String fileName)");
        return readFile(sessionStateDirectory, fileName);
    }

    /**
     * Method to read a file and get its contents as a string.
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * 
     * @param fileName
     *            The file name to read
     * @return
     * @throws IOException
     */
    public String readFile(String pathName, String fileName) throws IOException {
        LogWrapper.v(TAG, "readFile(String pathName, String fileName)");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;
        File file = new File(internalPath + fileName);

        try {
            // http://www.devdaily.com/java/java-bufferedreader-readline-string-examples

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            new FileInputStream(file), "UTF-8"), 2 * 1024);
            String line = null;
            StringBuffer content = new StringBuffer();

            // use the readLine method of the BufferedReader to read one line at
            // a time. the readLine method returns null when there is nothing
            // else to read.
            while ((line = reader.readLine()) != null) {
                content.append(line + LINE_SEPARATOR);
            }

            String result = content.toString().replaceAll("\0", "").trim();
            reader.close();

            LogWrapper.v(TAG, "The following data has been read from "
                    + internalPath + fileName + ": " + result);

            return result;

        } catch (FileNotFoundException e) {
            LogWrapper.w(TAG, "File not found: " + e.getMessage());
            throw new FileNotFoundException();
        } catch (IOException e) {
            LogWrapper.w(TAG, "IO Exception: " + e.getMessage());
            throw new IOException();
        }
    }

    public long getFileSize(String fileName) throws IOException {
        return getFileSize(sessionStateDirectory, fileName);
    }

    /**
     * Method to get the size of the file line is in the array.
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * 
     * @param fileName
     *            The file name to read
     * @return
     * @throws IOException
     */
    public long getFileSize(String pathName, String fileName)
            throws IOException {

        long t0 = System.currentTimeMillis();

        LogWrapper.v(TAG, "getFileSize(String pathName, String fileName)");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;
        File file = new File(internalPath + fileName);

        // Get the number of bytes in the file
        long length = file.length();

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        return length;
    }

    /**
     * Resets all the files that hold the data session/ user data as with a
     * clean install
     * 
     * @throws IOException
     * 
     */
    public void resetAll() throws IOException {
        emptyFile("otpe");
        emptyFile("otui");
        emptyFile("otui");
    }

    /**
     * Method to read a file and get its contents as a string array, where each
     * line is in the array.
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * 
     * @param fileName
     *            The file name to read
     * @return
     * @throws IOException
     */
    public String[] readFileLines(String pathName, String fileName)
            throws IOException {
        LogWrapper.v(TAG, "readFileLines(String pathName, String fileName)");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;
        File file = new File(internalPath + fileName);

        ArrayList<String> fileLines = new ArrayList<String>();
        try {
            // If it did exist, the file contains the ID.
            final int bufferSize = 100;
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            new FileInputStream(file), "UTF-8"), bufferSize);
            String line = null;
            while ((line = reader.readLine()) != null) {
                fileLines.add(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            LogWrapper.w(TAG, "File not found: " + e.getMessage());
            throw new FileNotFoundException();
        } catch (IOException e) {
            LogWrapper.w(TAG, "IO Exception: " + e.getMessage());
            throw new IOException();
        }
        // LogWrapper.i(TAG, "line:" + fileLines);
        String[] arrayLines = new String[fileLines.size()];
        for (int i = 0; i < fileLines.size(); i++) {
            arrayLines[i] = fileLines.get(i);
        }
        return arrayLines;
    }

    /**
     * Method to remove a file.
     * 
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @param fileName
     *            The file name to remove
     * @throws IOException
     */
    public void removeFile(String pathName, String fileName) throws IOException {
        LogWrapper.v(TAG, "removeFile()");

        if (pathName == null || fileName == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        String internalPath = appContext.getFilesDir() + pathName;
        File file = new File(internalPath + fileName);

        // remove file
        file.delete();
        LogWrapper.v(TAG, "removed file:" + fileName);

    }

    /**
     * Writes the writeString to the fileName given in the default
     * sessionStateDirectory directory ("/OTState/").
     * 
     * @param fileName
     *            The file name write to
     * @param writeString
     *            The string to write
     * @throws IOException
     *             If a file could not be written, or any parameters are null
     */
    public void writeFile(String fileName, String writeString)
            throws IOException {
        LogWrapper.v(TAG, "writeFile(String fileName, String writeString)");
        writeFile(sessionStateDirectory, fileName, writeString);
    }

    /**
     * Writes the writeString to the fileName given in the pathName
     * 
     * @param fileName
     *            The file name to write to
     * @param pathName
     *            The path to use relative to the apps context, eg. "/OTUpload/"
     * @param writeString
     *            The string to write
     * @throws IOException
     *             If a file could not be written, or any parameters are null
     */
    public void writeFile(String pathName, String fileName, String writeString)
            throws IOException {
        LogWrapper
                .v(TAG,
                        "writeFile(String pathName, String fileName, String writeString)");

        if (pathName == null || fileName == null || writeString == null) {
            LogWrapper.w(TAG, "Unable to make, get or create file: " + fileName
                    + " with pathName: " + pathName);
            throw new IOException("pathName and/ or fileName is null");
        }

        // TODO .getAbsolutePath()
        String internalPath = appContext.getFilesDir() + pathName;

        File file = new File(internalPath + fileName);
        if (file != null) {
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(writeString);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                LogWrapper.w(TAG, "Write is failed: " + e.getMessage());
                throw new IOException();
            }
            return;
        }
        throw new IOException("Cannot write to file: " + fileName);
    }

}
