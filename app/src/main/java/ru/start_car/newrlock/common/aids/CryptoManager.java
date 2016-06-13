package ru.start_car.newrlock.common.aids;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


// On error while use crypto library: "InvalidKeyException: Illegal key size"
// You will probably need to install the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files (available at Oracle).
// If you don't, the keysize is limited due to US export laws.
// http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html


//http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA

//http://goldenpackagebyanuj.blogspot.ru/2013/10/RSA-Encryption-Descryption-algorithm-in-java.html
//http://javadigest.wordpress.com/2012/08/26/rsa-encryption-example/

//http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#impl
//http://www.w3.org/TR/2002/REC-xmlenc-core-20021210/Overview.html#aes256-cbc
//http://docs.oracle.com/javase/tutorial/security/apisign/vstep2.html

/**
 * It is a Helper Class to prepare data to send across network with safety. It uses Public-key algorithm to encrypt Symmetric-key.
 * To initiate instance of an object of the class:
 * 1. CreatePublicKeyAndGetItPlain()
 * 2. SetOtherPublicKeyFromPlain()
 * 3. CreateOpenKeyAndGetItEncrypted()
 * 4. SetOtherOpenKeyFromDecrypted()
 * 5. EncryptData() / DecryptData()
 * If you need to reuse object call Reset() for it
 */
public final class CryptoManager {
	public static final class KeyInfo {
		/**
		 * Asymmetric Modulus or Symmetric Key
		 */
		public byte[] modulusOrKey;
		/**
		 * Asymmetric Exponent or Symmetric Initial Vector (IV)
		 */
		public byte[] exponentOrIV;
	}

	/**
	 * Public-key algorithm helper class for asymmetric cryptography
	 */
	private static final class AsymmetricData {
		public KeyPair keysCreated;
		public KeyInfo publicKeyReceived;
	}

	/**
	 * Symmetric-key algorithms helper class
	 */
	private static final class SymmetricData {
		public final KeyInfo keysCreated = new KeyInfo();
		public final KeyInfo keysReceived = new KeyInfo();
	}
	
	/**
	 * Public-key cryptography object
	 */
	private AsymmetricData m_AsymmetricData;

	/**
	 * Symmetric-key algorithms object
	 */
	private SymmetricData m_SymmetricData;

	/**
	 * Initialization process steps counter
	 */
	private int m_InitialStep;

	private static final String HASH_ALGORITHM = "SHA-256";

	private static final String ALGORITHM_ASYMMETRIC = "RSA";
	private static final String ALGORITHM_ASYMMETRIC_PARAM = "RSA/ECB/PKCS1Padding";
	private static final int ASYMMETRIC_KEY_SIZE = 1024;
	
	private static final String ALGORITHM_SYMMETRIC = "AES";
	private static final String ALGORITHM_SYMMETRIC_PARAM = "AES/CBC/ISO10126Padding";
	private static final int SYMMETRIC_KEY_SIZE = 256;

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	static {
		PRNGFixes.apply();
	}
	
	/**
	 * Create Hash from password and salt
	 * @param salt Salt bytes
	 * @param clearText Text chars
	 * @return Hash for given combination of salt and text
	 */
	public static byte[] hashPassword(final byte[] salt, final char[] clearText) {
		if (clearText != null) {
			byte[] bb = null;
			try {
				final String s = new String(clearText);
				bb = s.getBytes(UTF8_CHARSET);
				return hashData(salt, bb);
			} finally {
				if (bb != null) {
					Arrays.fill(bb, (byte) 0);
				}
			}
		}
		return null;
	}
	
	public static byte[] hashData(final byte[] salt, final byte[] rawData) {
		if (salt != null && rawData != null) {
			final byte[] bb = new byte[salt.length + rawData.length];
			System.arraycopy(salt, 0, bb, 0, salt.length);
			System.arraycopy(rawData, 0, bb, salt.length, rawData.length);
			try {
				final MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
				md.update(bb);
				return md.digest();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} finally {
				Arrays.fill(bb, (byte) 0);
			}
		}
		return null;
	}

	/**
	 * Generate byte array and fill it with encrypted random bytes
	 * @param size Size of array
	 * @return Encrypted array or null on error
	 */
	private static byte[] generateRandom(final int size) {
		final SecureRandom sr = new SecureRandom();//.getInstance("SHA1PRNG");
		final byte[] random = new byte[size];
		sr.nextBytes(random);
		return random;
	}

	public static byte[] generateSalt() {
		return generateRandom(6);
	}

	/**
	 * Generate random password from Base64 table
	 * @param passwordLength Length of password
	 * @return Password or null on error
	 */
	public static char[] generatePassword(final int passwordLength) {
		if (passwordLength < 4 || passwordLength > 100) {
			return null;
		}
		final byte[] rndValue = generateRandom(passwordLength);
		final char[] tmpRandom = PlatformTools.bytesToBase64Chars(rndValue);
		final char[] res = new char[passwordLength];
		System.arraycopy(tmpRandom, 0, res, 0, passwordLength);
		return res;
	}
	
	/**
	 * Reset to initial state
	 */
	public void reset() {
		m_InitialStep = 0;
		m_AsymmetricData = null;
		m_SymmetricData = null;
	}

	/**
	 * 1. Create own Public key
	 * @return Public key as byte array or null on error
	 */
	public KeyInfo createPublicKeyAndGetItPlain() {
		if (m_InitialStep == 0) {
			try {
				m_AsymmetricData = createAsymmetricData();
				m_InitialStep++;

				final KeyFactory fact = KeyFactory.getInstance(ALGORITHM_ASYMMETRIC);
				final RSAPublicKeySpec pub = fact.getKeySpec(m_AsymmetricData.keysCreated.getPublic(), RSAPublicKeySpec.class);

				final KeyInfo res = new KeyInfo();

				//The reason for the 00h valued byte at the start is because BigInteger.toByteArray() returns the signed representation.
				//http://stackoverflow.com/questions/8515691/getting-1-byte-extra-in-the-modulus-rsa-key-and-sometimes-for-exponents-also

				res.modulusOrKey = getModulusAsUnsigned(pub.getModulus());
				res.exponentOrIV = pub.getPublicExponent().toByteArray();
				return res;
			} catch (Exception e) {
				e.printStackTrace();
				reset();
			}
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on step 1");
		return null;
	}

	/**
	 * 2. Save other side Public key
	 * @param key Byte array of Key data
	 */
	public void setOtherPublicKeyFromPlain(final KeyInfo key) {
		if (m_InitialStep == 1 && key != null && key.modulusOrKey != null && key.modulusOrKey.length > 0 &&
			key.exponentOrIV != null && key.exponentOrIV.length > 0) {
			m_AsymmetricData.publicKeyReceived = key;
			m_InitialStep++;
			return;
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on step 2");
	}

	/**
	 * 3. Create own Symmetric key and get it encrypted with other side Public key
	 * @return Byte array of encrypted symmetric key
	 */
	public KeyInfo createOpenKeyAndGetItEncrypted() {
		if (m_InitialStep == 2) {
			try {
				m_SymmetricData = createSymmetricData();
				m_InitialStep++;
				KeyInfo key = new KeyInfo();
				key.modulusOrKey = encryptWithAsymmetric(m_SymmetricData.keysCreated.modulusOrKey);
				key.exponentOrIV = encryptWithAsymmetric(m_SymmetricData.keysCreated.exponentOrIV);
				return key;
			} catch (Exception e) {
				e.printStackTrace();
				reset();
			}
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on step 3");
		return null;
	}

	/**
	 * 4. Save other side Symmetric key
	 * @param encryptedData Pair of encrypted Key and IV
	 */
	public void setOtherOpenKeyFromEncrypted(final KeyInfo encryptedData) {
		if (m_InitialStep == 3) {
			if (encryptedData != null) {
				try {
					m_SymmetricData.keysReceived.modulusOrKey = decryptWithAsymmetric(encryptedData.modulusOrKey);
					m_SymmetricData.keysReceived.exponentOrIV = decryptWithAsymmetric(encryptedData.exponentOrIV);
					if (m_SymmetricData.keysReceived.modulusOrKey != null && m_SymmetricData.keysReceived.exponentOrIV != null) {
						m_InitialStep++;
						m_AsymmetricData = null;
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			reset();
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on step 4");
	}

	/**
	 * Encrypt data with own symmetric key
	 * @param plainData Bytes to be encrypted
	 * @param index Index in the array
	 * @param count Count of bytes to proceed
	 * @return Encrypted bytes
	 */
	public byte[] encryptData(final byte[] plainData, int index, int count) {
		if (m_InitialStep == 4 && plainData != null && index >= 0 && index < count && count <= plainData.length - index) {
			try {
				final Cipher encryptCipher = getCypher(m_SymmetricData.keysCreated.modulusOrKey, m_SymmetricData.keysCreated.exponentOrIV, Cipher.ENCRYPT_MODE);
				
				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				final CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, encryptCipher);
				cipherOutputStream.write(plainData, index, count);
				cipherOutputStream.flush();
				cipherOutputStream.close();
				return outputStream.toByteArray();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on encryptData");
		return null;
	}

	/**
	 * Decrypt data with symmetric key of other side
	 * @param encryptedData Byte array of encrypted data
	 * @param index Start index in the array
	 * @param count Count of bytes to proceed
	 * @return Decrypted data or null on error
	 */
	public byte[] decryptData(final byte[] encryptedData, int index, int count) {
		if (m_InitialStep == 4 && encryptedData != null && index >= 0 && index < count && count <= (encryptedData.length - index)) {
			try {
				final Cipher decryptCipher = getCypher(m_SymmetricData.keysReceived.modulusOrKey,
						m_SymmetricData.keysReceived.exponentOrIV,
						Cipher.DECRYPT_MODE);
				
				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				final ByteArrayInputStream inStream = new ByteArrayInputStream(encryptedData, index, count);
				final CipherInputStream cipherInputStream = new CipherInputStream(inStream, decryptCipher);
				final byte[] buf = new byte[ASYMMETRIC_KEY_SIZE];
				int bytesRead;
				while ((bytesRead = cipherInputStream.read(buf)) >= 0) {
					outputStream.write(buf, 0, bytesRead);
				}
				cipherInputStream.close();
		
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		PlatformTools.logError(Tools.getMethodName() + ": Error on decryptData");
		return null;
	}

	private static Cipher getCypher(final byte[] key, final byte[] iv, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM_SYMMETRIC);
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		
		final Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC_PARAM);
		cipher.init(mode, keySpec, ivSpec);
		return cipher;
	}
	
	/**
	 * Create asymmetric encoder helper
	 * @return Created helper class to work with Asymmetric algorithm
	 * @throws java.security.NoSuchAlgorithmException
	 */
	private AsymmetricData createAsymmetricData() throws NoSuchAlgorithmException/*, InvalidKeySpecException*/ {
		final AsymmetricData res = new AsymmetricData();
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM_ASYMMETRIC);
		keyGen.initialize(ASYMMETRIC_KEY_SIZE);
		res.keysCreated = keyGen.generateKeyPair();
		return res;
	}

	/**
	 * Create symmetric encoder helper
	 * @return Created helper class to work with Symmetric algorithm
	 * @throws javax.crypto.NoSuchPaddingException
	 * @throws java.security.NoSuchAlgorithmException
	 */
	private static SymmetricData createSymmetricData() throws NoSuchAlgorithmException, NoSuchPaddingException {
		final SymmetricData res = new SymmetricData();
		final Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC_PARAM);
		final byte[] iv = new byte[cipher.getBlockSize()];
		final SecureRandom sr = new SecureRandom();
		sr.nextBytes(iv);

		final KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_SYMMETRIC);
		keyGen.init(SYMMETRIC_KEY_SIZE);
	    final SecretKey aesKey = keyGen.generateKey();

	    res.keysCreated.modulusOrKey = aesKey.getEncoded();
	    res.keysCreated.exponentOrIV = iv;
		return res;
	}

	/**
	 * Encrypt data with Public key of other side
	 * @param plainData Bytes to encrypt
	 * @return Encrypted bytes
	 * @throws javax.crypto.NoSuchPaddingException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.InvalidKeyException
	 * @throws javax.crypto.BadPaddingException
	 * @throws javax.crypto.IllegalBlockSizeException
	 * @throws java.security.spec.InvalidKeySpecException
	 */
	private byte[] encryptWithAsymmetric(final byte[] plainData) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
		final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
				new BigInteger(1, m_AsymmetricData.publicKeyReceived.modulusOrKey),
				new BigInteger(1, m_AsymmetricData.publicKeyReceived.exponentOrIV));
		final KeyFactory fact = KeyFactory.getInstance(ALGORITHM_ASYMMETRIC);
		final PublicKey key = fact.generatePublic(keySpec);

		final Cipher cipher = Cipher.getInstance(ALGORITHM_ASYMMETRIC_PARAM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(plainData);
	}

	/**
	 * Decrypt data with own secret key (Asymmetric algorithm)
	 * @param encryptedData Bytes to decrypt
	 * @return Decrypted bytes
	 * @throws javax.crypto.NoSuchPaddingException
	 * @throws java.security.NoSuchAlgorithmException
	 * @throws java.security.InvalidKeyException
	 * @throws javax.crypto.BadPaddingException
	 * @throws javax.crypto.IllegalBlockSizeException
	 */
	private byte[] decryptWithAsymmetric(final byte[] encryptedData) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		final Cipher cipher = Cipher.getInstance(ALGORITHM_ASYMMETRIC_PARAM);
		cipher.init(Cipher.DECRYPT_MODE, m_AsymmetricData.keysCreated.getPrivate());
		return cipher.doFinal(encryptedData);
	}

	/**
	 * http://stackoverflow.com/questions/8515691/getting-1-byte-extra-in-the-modulus-rsa-key-and-sometimes-for-exponents-also
	 * @param i Value to convert
	 * @return Byte array of given value
	 */
	private static byte[] getModulusAsUnsigned(final BigInteger i) {
		final int len = ASYMMETRIC_KEY_SIZE / Byte.SIZE;
		final byte[] bb = i.toByteArray();
		if (bb.length == (len + 1) && bb[0] == (byte)0) {
			final byte[] res = new byte[len];
			System.arraycopy(bb, 1, res, 0, len);
			return res;
		}
		return bb;
	}
}
