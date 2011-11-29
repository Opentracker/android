package net.opentracker.android;

import android.util.Log;

/**
 * 
 * Wrapper for Androids logging mechanism.
 * 
 * While researching android's logging mechanics, logging seems to be hard to
 * turn off. This class is intended to turn it on or off easily.
 * 
 * Verbose should never be compiled into an application except during
 * development. Debug logs are compiled in but stripped at runtime. Error,
 * warning and info logs are always kept.
 * 
 * As per
 * http://stackoverflow.com/questions/2018263/android-logging/2019002#2019002
 * 
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: LogWrapper.java 13603 2011-11-29 11:55:42Z eddie $
 */
public class LogWrapper {

    private static final int VERBOSE = 1;

    private static final int DEBUG = 2;

    private static final int INFO = 3;

    private static final int WARN = 4;

    private static final int ERROR = 5;

    /*
     * Common way is make a int named LOG_LEVEL, and you can define it's debug
     * level based on LOG_LEVEL .
     * 
     * Later, you can just change the LOG_LEVEL for all debug output level.
     * 
     * By default this should be INFO or above.
     * 
     * Lower levels will give more details but will also clutter up the LogCat
     * output.
     */
    private static final int LOG_LEVEL = INFO;

    @SuppressWarnings("all")
    public static void v(String tag, Object msg) {
        if (VERBOSE >= LOG_LEVEL)
            Log.v(tag, msg.toString());
    }

    @SuppressWarnings("all")
    public static void d(String tag, Object msg) {
        if (DEBUG >= LOG_LEVEL)
            Log.d(tag, msg.toString());
    }

    @SuppressWarnings("all")
    public static void i(String tag, Object msg) {
        if (INFO >= LOG_LEVEL) {
            Log.i(tag, msg.toString());
        }
    }

    @SuppressWarnings("all")
    public static void w(String tag, Object msg) {
        if (WARN >= LOG_LEVEL) {
            Log.w(tag, msg.toString());
        }
    }

    @SuppressWarnings("all")
    public static void e(String tag, Object msg) {
        if (ERROR >= LOG_LEVEL) {
            Log.e(tag, msg.toString());
        }
    }

}