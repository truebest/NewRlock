package ru.start_car.newrlock.common.client;

import java.net.InetAddress;

import ru.start_car.newrlock.common.aids.EventHandler;
import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;
import ru.start_car.newrlock.common.messages.SerializableObject;
import ru.start_car.newrlock.common.network.ChannelData;

/**
 * Client side of data transmitting handler between client and server 
 */
public class ClientConnectionService {
	/**
	 * Callback if authentication is completed
	 */
	private class AuthenticationCompletedEvent implements EventHandler {
		@Override
		public void invoke(final Object arg) {
			if (!(Boolean)arg) { // authentication status
				stop();          // stop if authentication was failed
			}
			BaseClientSideChannel.raiseEvent(authenticationCompleted, arg);
		}
	}
	
	/**
	 * Callback if disconnected event happened
	 */
	private class ChannelClosedEvent implements EventHandler {
		@Override
		public void invoke(final Object arg) {
			if (!m_AutoReconnect) {
				stop();
			} else if (isRunning()) {
				BaseClientSideChannel.raiseEvent(reconnecting, null);
			}
		}
	}

	/**
	 * Callback if disconnected event for response on server command was happened
	 */
	private class ChannelClosedByServerEvent implements EventHandler {
		@Override
		public void invoke(final Object arg) {
			stop();
		}
	}
	
	/**
	 * Callback if data received event happened
	 */
	private class DataReceivedEvent implements EventHandler {
		@Override
		public void invoke(final Object arg) {
			ChannelData data = (ChannelData)arg;
			
			byte[] bb = data.getData();
			if (data.getIsCiphered()) {
				bb = m_Channel.decryptReceivedData(bb);
			}
			SerializableObject obj = SerializableObject.createInstance(bb);
			if (obj != null) {
				BaseClientSideChannel.raiseEvent(objectReceived, obj);
			}
		}
	}
	
	/**
	 * Channel to handle connection with the server
	 */
	private ClientChannelConnection m_Channel;
	/**
	 * Thread to run channel handling
	 */
	private Thread m_HandlerRunner;
	/**
	 * Event to close the thread
	 */
	private volatile boolean m_NeedClose;
	/**
	 * Value indicating if there is a need to reconnect to server on connection error or connection is dropped
	 */
	private boolean m_AutoReconnect;

	private EventHandler authenticationCompleted;
	private EventHandler disconnected;
	private EventHandler reconnecting;
	private EventHandler objectReceived;
	/**
	 * Callback when authentication process with server is completed
	 */
	public synchronized void setAuthenticationCompletedEventHandler(final EventHandler handler) {
		authenticationCompleted = handler;
	}
	/**
	 * Callback on disconnect from the server
	 */
	public synchronized void setDisconnectedEventHandler(final EventHandler handler) {
		disconnected = handler;
	}
	/**
	 * Callback on reconnecting to server is in action
	 * @param handler Handler for the event
	 */
	public synchronized void setReconnectingEventHandler(final EventHandler handler) {
		reconnecting = handler;
	}
	/**
	 * Callback on new data was received from server
	 * @param handler Handler for the event
	 */
	public synchronized void setObjectReceivedEventHandler(final EventHandler handler) {
		objectReceived = handler;
	}

	/**
	 * Start data transmitting with the server
	 * @param address Address of channel
	 * @param port Port of channel
	 * @param login User login
	 * @param password User password
	 * @param autoReconnect AutoReconnect on connecting error value
	 */
	public synchronized final void start(InetAddress address, int port, String login, char[] password, boolean autoReconnect) {
		if (m_Channel == null) {
			m_AutoReconnect = autoReconnect;
			m_Channel = new ClientChannelConnection(address, port, login, password);
			m_Channel.setAuthenticationCompletedEventHandler(new AuthenticationCompletedEvent());
			m_Channel.setChannelClosedEventHandler(new ChannelClosedEvent());
			m_Channel.setChannelClosedByServerEventHandler(new ChannelClosedByServerEvent());
			m_Channel.setDataReceivedEventHandler(new DataReceivedEvent());
			
			m_NeedClose = false;
			m_HandlerRunner = new Thread(new Runnable() {
				@Override
				public void run() {
					do {
						try {
							int timeout = m_Channel.execute();
							Thread.sleep(timeout);
						} catch(InterruptedException e) {
							return;
						} catch(Exception e) {
							PlatformTools.logError(Tools.getExceptionInfo(e));
							try {
								Thread.sleep(50);
							} catch(InterruptedException ex) {
								return;
							}
						}
					} while(!m_NeedClose);
				}
			});
			m_HandlerRunner.start();
		}
	}
	
	/**
	 * Stop communicating with the server
	 */
	public synchronized final void stop() {
		m_NeedClose = true;
		if (m_HandlerRunner != null) {
			try {
				m_HandlerRunner.interrupt();
				m_HandlerRunner.join(1000);
			} catch(Exception e) { }
			m_HandlerRunner = null;
		}
		if (m_Channel != null) {
			m_Channel.setAuthenticationCompletedEventHandler(null);
			m_Channel.setChannelClosedEventHandler(null);
			m_Channel.setChannelClosedByServerEventHandler(null);
			m_Channel.setDataReceivedEventHandler(null);
			m_Channel.close();
			m_Channel = null;
			BaseClientSideChannel.raiseEvent(disconnected, null);
		}
	}
	
	private synchronized boolean isRunning() {
		return m_HandlerRunner != null;
	}
	
	/**
	 * Send data to the server with encryption
	 * @param obj Object to send
	 */
	public synchronized final void SendCipheredData(final SerializableObject obj) {
		if (m_Channel != null) {
			byte[] bb = SerializableObject.instanceToBytes(obj);
			ChannelData data = new ChannelData();
			data.setIsCiphered(true);
			data.setIsAcknowledgmentRequired(true);
			data.setData(m_Channel.encryptDataToSend(bb));
	
			m_Channel.sendDataAsync(data);
		}
	}
}

