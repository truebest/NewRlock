package ru.start_car.newrlock.common.client;

import java.net.InetAddress;
import java.util.Arrays;

import ru.start_car.newrlock.common.aids.CryptoManager;
import ru.start_car.newrlock.common.aids.EventHandler;
import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;
import ru.start_car.newrlock.common.messages.Authentication;
import ru.start_car.newrlock.common.messages.SerializableObject;
import ru.start_car.newrlock.common.network.ChannelData;
import ru.start_car.newrlock.common.network.ChannelException;
import ru.start_car.newrlock.common.network.WaitAcknowledgment;

/**
 * Client side handler for channel
 */
public final class ClientChannelConnection extends ClientChannelUdp {
	/**
	 * Information needed to connect to server
	 */
	private class AuthenticationInfo {
		/**
		 * Login to connect to server
		 */
		public final String login;
		/**
		 * Password to connect to server.
		 */
		public final char[] password;
		/**
		 * Client side Salt generated on every session
		 */
		public byte[] clientSalt;
		/**
		 * Hash that that must be received from server
		 */
		public byte[] serverHash;

		public AuthenticationInfo(String login, char[] password) {
			this.login = login;
			this.password = password;
		}
	}
	private final AuthenticationInfo m_AuthenticationInfo;

	private enum AuthenticationState { initial, waitAuthPacket1, waitAuthPacket2, authenticationCompleted }
	/**
	 * Current authentication step
	 */
	private AuthenticationState m_AuthenticationState;
	
	/**
	 * Last time where channel was active (in Milliseconds)
	 */
	private long m_LastChannelEvent;

	/**
	 * Secret data manager
	 */
	private final CryptoManager m_CryptoManager = new CryptoManager();

	private EventHandler authenticationCompleted;
	/**
	 * Raise on authentication is completed
	 */
	public final synchronized void setAuthenticationCompletedEventHandler(final EventHandler handler) {
		authenticationCompleted = handler;
	}

	public ClientChannelConnection(InetAddress address, int port, String login, char[] password) {
		super(address, port);
		m_AuthenticationInfo = new AuthenticationInfo(login, password);
	}

	@Override
	protected void afterExecute() throws ChannelException {
		if (m_AuthenticationState != AuthenticationState.authenticationCompleted &&
		   (System.currentTimeMillis() - m_LastChannelEvent) > (WaitAcknowledgment.WAIT_ACK_MSEC * 4)) {
			throw new ChannelException("Authentication timeout");
		}
	}

	@Override
	protected void createChannel() throws ChannelException {
		super.createChannel();

		m_AuthenticationInfo.clientSalt = CryptoManager.generateSalt();

		Authentication info = new Authentication();
		info.login = m_AuthenticationInfo.login;
		info.saltOrHash = m_AuthenticationInfo.clientSalt; // client side Salt
		CryptoManager.KeyInfo key = m_CryptoManager.createPublicKeyAndGetItPlain();
		info.cryptoKey1 = key.modulusOrKey;
		info.cryptoKey2 = key.exponentOrIV;

		ChannelData send = new ChannelData();
		send.setIsAcknowledgmentRequired(true);
		send.setData(SerializableObject.instanceToBytes(info));
		sendDataAsync(send);

		m_AuthenticationState = AuthenticationState.waitAuthPacket1;
		m_LastChannelEvent = System.currentTimeMillis();
	}

	@Override
	protected void closeChannel() {
		super.closeChannel();
		m_CryptoManager.reset();
		m_AuthenticationState = AuthenticationState.initial;
	}

	@Override
	protected void handleReceivedData(ChannelData data) throws ChannelException {
		m_LastChannelEvent = System.currentTimeMillis();
		if (m_AuthenticationState == AuthenticationState.authenticationCompleted) {
			super.handleReceivedData(data);
		} else {
			processAuthentication(data);
		}
	}

	private void processAuthentication(final ChannelData data) /*throws ChannelException*/ {
		Authentication info = (Authentication)SerializableObject.createInstance(data.getData());
		if (info != null) {
			Authentication infoToSend;
			ChannelData send;
			boolean isOk = true;
			if (m_AuthenticationState == AuthenticationState.waitAuthPacket1) {
				if (info.cryptoKey1 == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.cryptoKey1 == null");
					isOk = false;
				}
				if (info.cryptoKey2 == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.cryptoKey2 == null");
					isOk = false;
				}
				if (info.saltOrHash == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.saltOrHash == null");
					isOk = false;
				}

				if (isOk) {
					m_AuthenticationInfo.serverHash = CryptoManager.hashPassword(info.saltOrHash, m_AuthenticationInfo.password); // generate hash stored in server DB
	
					// To server send Temporary Hash (Stored In Server) + Client Salt and Client Symmetric Key
					infoToSend = new Authentication();
					infoToSend.saltOrHash = CryptoManager.hashData(m_AuthenticationInfo.clientSalt, m_AuthenticationInfo.serverHash); // client hash to transmit to server
	
					CryptoManager.KeyInfo key = new CryptoManager.KeyInfo();
					key.modulusOrKey = info.cryptoKey1;
					key.exponentOrIV = info.cryptoKey2;
	
					m_CryptoManager.setOtherPublicKeyFromPlain(key);
					CryptoManager.KeyInfo keys = m_CryptoManager.createOpenKeyAndGetItEncrypted(); 
					infoToSend.cryptoKey1 = keys.modulusOrKey;
					infoToSend.cryptoKey2 = keys.exponentOrIV;
	
					send = new ChannelData();
					send.setIsAcknowledgmentRequired(true);
					send.setData(SerializableObject.instanceToBytes(infoToSend));
					sendDataAsync(send);
	
					m_AuthenticationState = AuthenticationState.waitAuthPacket2;
					return;
				}
			}
			if (m_AuthenticationState == AuthenticationState.waitAuthPacket2) {
				// Get from server Server Symmetric Key and Hash Stored In Server
				if (info.cryptoKey1 == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.cryptoKey1 == null");
					isOk = false;
				}
				if (info.cryptoKey2 == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.cryptoKey2 == null");
					isOk = false;
				}
				if (info.saltOrHash == null) {
					PlatformTools.logWarning(Tools.getMethodName() + ": info.saltOrHash == null");
					isOk = false;
				}

				if (isOk) {
					CryptoManager.KeyInfo keys = new CryptoManager.KeyInfo();
					keys.modulusOrKey = info.cryptoKey1;
					keys.exponentOrIV = info.cryptoKey2;
					m_CryptoManager.setOtherOpenKeyFromEncrypted(keys);
					byte[] hash = m_CryptoManager.decryptData(info.saltOrHash, 0, info.saltOrHash.length);
					// compare server hash and client hash
					if (Arrays.equals(hash, m_AuthenticationInfo.serverHash)) {
						m_AuthenticationState = AuthenticationState.authenticationCompleted;
						raiseEventAsync(authenticationCompleted, true);

						PlatformTools.logInformation(Tools.getMethodName() + ": A connection with server is established");
						return;
					}
				}
			}
		}
		raiseEventAsync(authenticationCompleted, false);
		//throw new ChannelException("On authentication process received bad data");
	}

	/**
	 * Encrypt data before to send to server
	 * @param data Data to encrypt
	 * @return Encrypted data
	 */
	public final byte[] encryptDataToSend(final byte[] data) {
		return data != null ? m_CryptoManager.encryptData(data, 0, data.length) : null;
	}

	/**
	 * Decrypt received data from server
	 * @param data Data to decrypt
	 * @return Decrypted data
	 */
	public final byte[] decryptReceivedData(final byte[] data) {
		return data != null ? m_CryptoManager.decryptData(data, 0, data.length) : null;
	}
}
