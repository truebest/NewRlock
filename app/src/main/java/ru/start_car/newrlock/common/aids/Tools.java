package ru.start_car.newrlock.common.aids;

import java.nio.charset.Charset;

/**
 * http://introcs.cs.princeton.edu/java/51data/CRC16CCITT.java.html
 */
public final class Tools {
	/**
	 * Method to check some code
	 */
	public static void test() {

		// comment this line to start test
		if (true) return;



		final Charset UTF8_CHARSET = Charset.forName("UTF-8");
		try {
			CryptoManager cln = new CryptoManager();
			CryptoManager.KeyInfo clnKey = cln.createPublicKeyAndGetItPlain();

			CryptoManager srv = new CryptoManager();
			CryptoManager.KeyInfo srvKey = srv.createPublicKeyAndGetItPlain();
			srv.setOtherPublicKeyFromPlain(clnKey);

			cln.setOtherPublicKeyFromPlain(srvKey);
			CryptoManager.KeyInfo clnOpenKey = cln.createOpenKeyAndGetItEncrypted();

			CryptoManager.KeyInfo srvOpenKey = srv.createOpenKeyAndGetItEncrypted();
			srv.setOtherOpenKeyFromEncrypted(clnOpenKey);

			cln.setOtherOpenKeyFromEncrypted(srvOpenKey);
			String clnStr = "Привет от Клиента";
			byte[] clnPlainBytes = clnStr.getBytes(UTF8_CHARSET);
			byte[] clnBytes = cln.encryptData(clnPlainBytes, 0, clnPlainBytes.length);

			byte[] srvPlainBytesFromCln = srv.decryptData(clnBytes, 0, clnBytes.length);
			String srvStrFromClient = new String(srvPlainBytesFromCln, 0, srvPlainBytesFromCln.length, UTF8_CHARSET);

			String srvStr = "Привет от Server";
			byte[] srvPlainBytes = srvStr.getBytes(UTF8_CHARSET);
			byte[] srvBytes = srv.encryptData(srvPlainBytes, 0, srvPlainBytes.length);

			byte[] clnPlainBytesFromSrv = cln.decryptData(srvBytes, 0, srvBytes.length);
			String clnStrFromServer = new String(clnPlainBytesFromSrv, 0, clnPlainBytesFromSrv.length, UTF8_CHARSET);
		} catch (Exception e) {
			e.printStackTrace();
		}



		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// TEST BLOCK
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		/*TempObject o = new TempObject();
		o.intVal = -129;
		o.dateVal = new Date(2014, 3, 15, 5, 4, 28);
		o.textVal = "привет_123";
		byte[] bb = SerializableObject.instanceToBytes(o);

		Object oo = SerializableObject.createInstance(bb);
		TempObject o1 = (TempObject)oo;
		o1.intVal = 3;*/
	}


	private static final int CLIENT_CODE_STACK_INDEX;

	static {
		// Finds out the index of "this code" in the returned stack trace - funny but it differs in JDK 1.5 and 1.6
		int i = 0;
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			if (ste.getClassName().equals(Tools.class.getName())) {
				break;
			}
			i++;
		}
		CLIENT_CODE_STACK_INDEX = i + 2;
	}

	/**
	 * Get name of method from with function is called
	 * @return Full method name or empty string
	 */
	public static String getMethodName() {
		return getMethodName(CLIENT_CODE_STACK_INDEX);
	}

	public static String getMethodName(final int idx) {
		StackTraceElement[] steSet = Thread.currentThread().getStackTrace();
		if (steSet.length > idx){
			StackTraceElement ste = steSet[idx];
			return ste.getClassName() + "." + ste.getMethodName();
		}
		return "";
	}

	public static String getExceptionInfo(Exception e) {
		return getMethodName(CLIENT_CODE_STACK_INDEX) + ": " + (e != null ? e.getMessage() + " / " + e.toString() : "?");
	}

	public static int Crc16(final byte[] buffer, int pos, int len) {
		int crc = 0xFFFF;          // initial value
		if (buffer != null && pos >= 0 && (pos + len) <= buffer.length) {
			final int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 
			while (len-- > 0) {
				for (int i = 0; i < 8; i++) {
					boolean bit = ((buffer[pos] >> (7-i) & 1) == 1);
					boolean c15 = ((crc >> 15 & 1) == 1);
					crc <<= 1;
					if (c15 ^ bit) crc ^= polynomial;
				}
				pos++;
			}
		}
		else {
			PlatformTools.logError("crc16 error data");
		}
		return crc & 0xffff;
	}
	
	public static String byteArrayToHex(byte[] a) {
	   StringBuilder sb = new StringBuilder();
	   for(byte b: a)
	      sb.append(String.format("%02x", b & 0xFF));
	   return sb.toString();
	}
}
