package ru.start_car.newrlock.common.messages;

import java.nio.ByteBuffer;

import ru.start_car.newrlock.common.aids.IntPtr;

public final class Authentication extends SerializableObject {
	/**
	 * Client id
	 */
	public String login;

	/**
	 * et or set Salt to hash password on first step of authentication and Get or set hash on second one
	 */
	public byte[] saltOrHash;

	/**
	 * Get or set public key (modulus) or Symmetric key
	 */
	public byte[] cryptoKey1;
	
	/**
	 * Get or set public key (exponent) or Symmetric key (IV)
	 */
	public byte[] cryptoKey2;

	public Authentication() { }

	public Authentication(byte[] data, IntPtr offset) {
		login = getString(data, offset);
		saltOrHash = getBytes(data, offset);
		cryptoKey1 = getBytes(data, offset);
		cryptoKey2 = getBytes(data, offset);
	}

	/**
	 * Serialize contents of object
	 */
	protected byte[] getByteList() {
		byte[] bl = getStringAsSerializedBytes(login);
		byte[] bs = getBytesAsSerializedBytes(saltOrHash);
		byte[] bc1 = getBytesAsSerializedBytes(cryptoKey1);
		byte[] bc2 = getBytesAsSerializedBytes(cryptoKey2);
		
		ByteBuffer res = ByteBuffer.allocate(bl.length + bs.length + bc1.length + bc2.length);
		res.put(bl);
		res.put(bs);
		res.put(bc1);
		res.put(bc2);
		return res.array();
	}

	public SerializableTypes getSerializableType() {
		return SerializableTypes.Authentication;
	}
}
