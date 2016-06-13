package ru.start_car.newrlock.common.messages;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import ru.start_car.newrlock.common.aids.IntPtr;
import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;

public abstract class SerializableObject {
	/**
	 * Array or Object has null value
	 */
	private static final int NULL_LENGTH = -1;

	/**
	 * Array has zero size
	 */
	private static final int EMPTY_LENGTH = 0;

	/**
	 * Encoder for string objects
	 */
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	protected SerializableObject() { }

	/**
	 * Serializable Constructor. Every child must override this constructor
	 * @param data Bytes of object data
	 * @param offset Start position of the object data
	 */
	protected SerializableObject(byte[] data, IntPtr offset) { }
	
	/**
	 * To Get Object bytes. Every child must implement serialization itself
	 * @return Byte array of the object data
	 */
	protected abstract byte[] getByteList();

	/**
	 * Return associated type from SerializableTypes
	 * @return Type of the object
	 */
	public abstract SerializableTypes getSerializableType();

	public static SerializableObject createInstance(byte[] data) {
		IntPtr offset = new IntPtr(0);
		return createInstance(data, offset);
	}

	public static SerializableObject createInstance(byte[] data, IntPtr offset)
	{
		// TODO: maybe in future make via Reflection to eliminate some troubles while coding
		// but for a little count of objects - this way is more fast

		IntPtr val = new IntPtr(0);
		if (getSIntVar(val, data, offset)) {
			SerializableTypes ser = SerializableTypes.fromInt(val.value);
			try {
				switch (ser){
					case Authentication:
						return new Authentication(data, offset);
					case DateTimeUtc:
						return new DateTimeUtc(data, offset);
					case TempObject:
						return new TempObject(data, offset);
					case Unknown:
						return null;
					default:
						PlatformTools.logInformation(Tools.getMethodName() + ": You must Write Code (in method SerializableObject.CreateInstance) to create object: \" + ser.toString() + \" int value: \" + val.toString()");
						break;
				}
			} catch (Exception e) {
				PlatformTools.logError(Tools.getExceptionInfo(e) + ": for type: " + ser.toString() + " int value: " + val.toString());
			}
		}
		return null;
	}

	/**
	 * Get object as byte array
	 * @param obj Object to get bytes from
	 * @return Byte array of object data
	 */
	public static byte[] instanceToBytes(SerializableObject obj) {
		if (obj != null) {
			int i = obj.getSerializableType().getValue();
			byte[] bt = toSIntVar(i);
			byte[] bo = obj.getByteList();
			ByteBuffer res = ByteBuffer.allocate(bt.length + bo.length);
			res.put(bt);
			res.put(bo);
			return res.array();
		}
		return toSIntVar(SerializableTypes.Unknown.getValue());
	}

	/**
	 * Convert Signed Integer to serialized byte array of SIntVar.
	 * @param value Value to convert
	 * @return One to five bytes representing value
	 */
	public static byte[] toSIntVar(int value) {
		byte[] bb = new byte[5];
		int pos = 0;
		
		byte b = 0;
		if (value < 0) {
			b = 0x40;
			value *= -1;
		}
		b |= (byte)(value & 0x3F); // first byte into 7 position put sign
		value >>>= 6;
		do {
			if (value != 0) {
				b |= 0x80;  // every 8 bit is mean to be continue
			}
			bb[pos++] = b;

			b = (byte)(value & 0x7F);
			value >>= 7;
		} while (value != 0 || b != 0);

		byte[] res = new byte[pos];
		System.arraycopy(bb, 0, res, 0, pos);
		return res;
	}

	/**
	 * Get integer value from serialized raw data
	 * @param value Variable to store received value
	 * @param rawData Bytes to get value from
	 * @param offset Start index to seek in the array
	 * @return True if value can be read
	 */
	public static boolean getSIntVar(IntPtr value, byte[] rawData, IntPtr offset) {
		if (value != null && rawData != null && offset != null) {
			int pos = offset.value;
			if (pos >= 0 && rawData.length > pos) {
				int b = rawData[pos];
				int sign = (b & 0x40) != 0 ? -1 : 1;
				int val = (b & 0x3F);
				int shift = 6;
				for (int i = pos + 1, j = 0; (i < rawData.length && j < 5) || (b & 0x80) == 0; i++, j++) {
					if ((b & 0x80) == 0) {
						value.value = val * sign;
						offset.value = i;
						return true;
					}
					b = rawData[i];
					val |= (b & 0x7F) << shift;
					shift += 7;
				}
			}
		}
		return false;
	}

	/**
	 * Get string as serialized byte array
	 * @param value String value to serialize
	 * @return String bytes that can be de-serialized
	 */
	public static byte[] getStringAsSerializedBytes(String value) {
		if (value == null) {
			return toSIntVar(NULL_LENGTH);
		}
		if (value.isEmpty()) {
			return toSIntVar(EMPTY_LENGTH);
		}
		byte[] bb = value.getBytes(UTF8_CHARSET);
		byte[] lengthOfArray = toSIntVar(bb.length);
		
		ByteBuffer res = ByteBuffer.allocate(lengthOfArray.length + bb.length);
		res.put(lengthOfArray);
		res.put(bb);
		
		return res.array();
	}

	/**
	 * Get string from serialized byte array
	 * @param rawData Bytes to get value from
	 * @param offset Start index to seek in the array
	 * @return Received string
	 */
	public static String getString(byte[] rawData, IntPtr offset) {
		IntPtr len = new IntPtr(0);
		if (getSIntVar(len, rawData, offset)) {
			int size = len.value;
			if (size == NULL_LENGTH) {
				return null;
			}
			if (size == EMPTY_LENGTH) {
				return "";
			}
			int pos = offset.value;
			offset.value += size;
			if ((rawData.length - offset.value) >= 0) {
				try {
					return new String(rawData, pos, size, UTF8_CHARSET);
				}
				catch (Exception e) { }
			}
		}
		return null;
	}

	/**
	 * Get serialized raw bytes from byte array (including size of array)
	 * @param value Bytes to get as serialized
	 * @return Bytes as serialized byte array
	 */
	public static byte[] getBytesAsSerializedBytes(byte[] value)
	{
		if (value == null) {
			return toSIntVar(NULL_LENGTH);
		}
		if (value.length == 0) {
			return toSIntVar(EMPTY_LENGTH);
		}
		int len = value.length;
		byte[] lengthOfArray = toSIntVar(len);
		ByteBuffer res = ByteBuffer.allocate(lengthOfArray.length + value.length);
		res.put(lengthOfArray);
		res.put(value);
		return res.array();
	}

	/**
	 * Get byte array from serialized raw data
	 * @param rawData Bytes to get value from
	 * @param offset Start index to seek in the array
	 * @return Byte array without service information
	 */
	public static byte[] getBytes(byte[] rawData, IntPtr offset) {
		IntPtr len = new IntPtr(0);
		if (getSIntVar(len, rawData, offset)) {
			int size = len.value;
			if (size == NULL_LENGTH) {
				return null;
			}
			if (size == EMPTY_LENGTH) {
				return new byte[0];
			}
			int pos = offset.value;
			offset.value += size;
			if ((rawData.length - offset.value) >= 0) {
				byte[] buf = new byte[size];
				ByteBuffer res = ByteBuffer.wrap(buf);
				res.put(rawData, pos, size);
				return res.array();
			}
		}
		return null;
	}
}
