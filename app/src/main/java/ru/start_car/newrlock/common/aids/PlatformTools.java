package ru.start_car.newrlock.common.aids;

//import java.util.Base64;
import android.util.Base64;
import android.util.Log;

public final class PlatformTools {
	/**
	 * Filter sign to add to LogCat
	 */
	private static final String MY_LOG = "CHANNEL_LOG";

	public static void logError(String s) {
		Log.e(MY_LOG, s);
	}

	public static void logWarning(String s) {
		Log.w(MY_LOG, s);
	}

	public static void logInformation(String s) {
		Log.i(MY_LOG, s);
	}

	public static char[] bytesToBase64Chars(byte[] value) {
		return Base64.encodeToString(value, Base64.DEFAULT).toCharArray();
	}

/*	public static char[] bytesToBase64Chars(byte[] value) {
		return Base64.getEncoder().encodeToString(value).toCharArray();
	}*/

	/**
	 * Change for non debug release to false
	 */
	public static final boolean isDebug = true;
}