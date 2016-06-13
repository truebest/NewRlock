package ru.start_car.newrlock.common.client;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.start_car.newrlock.common.aids.EventHandler;
import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;
import ru.start_car.newrlock.common.network.ChannelData;
import ru.start_car.newrlock.common.network.ChannelException;
import ru.start_car.newrlock.common.network.WaitAcknowledgment;

/**
 * Abstract client side actions for exchanging data through channel
 */
public abstract class BaseClientSideChannel {
	/**
	 * A reason the connection was closed
	 */
	private enum CloseConnectionReason { None, ReconnectNeeded, ClosedByServer }
	
	/**
	 * Executor to handle callback actions
	 */
	private static final ExecutorService s_Executor =
		//Executors.newCachedThreadPool ();
		Executors.newSingleThreadExecutor ();

	/**
	 * Disposed sign
	 */
	private boolean m_Disposed;
	/**
	 * Locker for multithreaded access to channel object
	 */
	private final Object m_Locker = new Object();
	/**
	 * Collection of data to send to server
	 */
	private final PriorityQueue<ChannelData> m_DataToSend = new PriorityQueue<ChannelData>();

	/**
	 * Last DataChannel received Id (to skip handling repeated packets)
	 */
	private int m_LastReceivedPacketId = Integer.MIN_VALUE;

	/**
	 * Wait acknowledgment data. Id of sent data and the data to repeat sending on no acknowledgment
	 */
	private WaitAcknowledgment m_WaitAcknowledgment;

	/**
	 * Reason for connection was closed
	 */
	private CloseConnectionReason m_CloseConnectionReason;
			
	private EventHandler channelClosed;
	private EventHandler channelClosedByServer;
	private EventHandler dataReceived;
	private EventHandler acknowledgmentReceived;
	private EventHandler acknowledgmentError;
	/**
	 * Raise on available channel was closed
	 */
	public final synchronized void setChannelClosedEventHandler(EventHandler handler) {
		channelClosed = handler;
	}
	/**
	 * Raise on available channel was closed by server command
	 */
	public final synchronized void setChannelClosedByServerEventHandler(EventHandler handler) {
		channelClosedByServer = handler;
	}
	/**
	 * Raise on data is received (Asynchronous event in separate thread)
	 */
	public final synchronized void setDataReceivedEventHandler(EventHandler handler) {
		dataReceived = handler;
	}
	/**
	 * Raise on acknowledgment was received (Asynchronous event in separate thread)
	 */
	public final synchronized void setAcknowledgmentReceivedEventHandler(EventHandler handler) {
		acknowledgmentReceived = handler;
	}
	/**
	 * Raise on acknowledgment was not received (Asynchronous event in separate thread)
	 */
	public final synchronized void setAcknowledgmentErrorEventHandler(EventHandler handler) {
		acknowledgmentError = handler;
	}
	
	/**
	 * Check if channel is available
	 * @return True if channel is available
	 */
	protected abstract boolean getIsChannelAvailable();

	public void close() {
		synchronized (m_Locker) {
			if (!m_Disposed) {
				closeChannel();
				m_Disposed = true;
			}
		}
	}
	
	/**
	 * Send/receive bytes throw Data Channel
	 */
	public final int execute() {
		int timeout = 100;
		try {
			synchronized (m_Locker) {
				if (!m_Disposed) {
					if (!getIsChannelAvailable()) {
						createChannel();
					}
					ChannelData data = makeReceiveData();
					if (data != null && checkIsDataFromCorrectServer(data)) {
						if (data.getIsService()) {
							handleServiceMessage(data);
						} else {
							if (data.getIsAcknowledgmentRequired()) {
								makeSendData(ChannelData.createAcknowledgmentFor(data));
							}
							
							int packetId = data.getPacketId();
							// process only new data. Id is looping counter from (Integer.MIN_VALUE + 1) to (Integer.MAX_VALUE)
							// so 5000 - to define that the difference between values is big enough
							if ((packetId > m_LastReceivedPacketId) ||
								(packetId < 0 && m_LastReceivedPacketId > 0 &&
								(int)((long)m_LastReceivedPacketId - (long)packetId) > 5000)) {
								m_LastReceivedPacketId = packetId;
								handleReceivedData(data);
							}
						}
						timeout = 0;
					}
					if (sendAvailableData()) {
						timeout = 0;
					}
					afterExecute();
				}
			}
		} catch (ChannelException e) {
			//PlatformTools.logError("BaseClientSideChannel.execute(ChannelException): " + e.getMessage() + " --- " + e.toString());
			PlatformTools.logError(Tools.getExceptionInfo(e));
			synchronized (m_Locker) {
				closeChannel();
			}
			timeout = 3000;
		} catch (Exception e) {
			//PlatformTools.logError("BaseClientChannel.execute(Exception): " + e.getMessage() + " --- " + e.toString());
			PlatformTools.logError(Tools.getExceptionInfo(e));
			timeout = 500;
		}
		return timeout;
	}

	protected abstract void afterExecute() throws ChannelException;

	/**
	 * Check received data is came from valid server from expected EndPoint
	 * @param data Data to check
	 * @return True if success
	 */
	protected abstract boolean checkIsDataFromCorrectServer(ChannelData data);

	/**
	 * Create Channel and set it's options
	 */
	protected abstract void createChannel() throws ChannelException;

	/**
	 * Close Channel if it exists
	 */
	protected void closeChannel() {
		PlatformTools.logInformation(Tools.getMethodName());

		if (getIsChannelAvailable()) {
			raiseEventAsync(m_CloseConnectionReason == CloseConnectionReason.ClosedByServer ? channelClosedByServer : channelClosed, null);
		}
		m_WaitAcknowledgment = null;
		m_CloseConnectionReason = CloseConnectionReason.None;
		m_LastReceivedPacketId = Integer.MIN_VALUE;
	}

	/**
	 * If there are available data in Channel then receive it (physical receiving)
	 * @return Data that were received and extra information or null if no data
	 */
	protected abstract ChannelData makeReceiveData() throws ChannelException;

	/**
	 * Need handle sending of data (physical sending)
	 * @param data Data to send
	 */
	protected abstract void makeSendData(ChannelData data) throws ChannelException;

	/**
	 * Handle data received via channel
	 * @param data Data to handle
	 */
	protected void handleReceivedData(ChannelData data) throws ChannelException {
		raiseEventAsync(dataReceived, data);
	}
			
	protected void handleAcknowledgmentReceived(ChannelData data) {
		raiseEventAsync(acknowledgmentReceived, data);
	}

	protected void handleAcknowledgmentError(ChannelData data) {
		raiseEventAsync(acknowledgmentError, data);
	}

	private void handleServiceMessage(ChannelData data) throws ChannelException
	{
		switch(data.getServiceMessageType()) {
		case Acknowledgment:
			if (m_WaitAcknowledgment != null && m_WaitAcknowledgment.isIdEqual(data.getPacketId())) {
				m_WaitAcknowledgment = null;
				handleAcknowledgmentReceived(data);
			}
			break;
			
		case CloseConnection:
			m_CloseConnectionReason = CloseConnectionReason.ClosedByServer;
			throw new ChannelException("CloseConnection");
			
		case ReconnectNeeded:
			m_CloseConnectionReason = CloseConnectionReason.ReconnectNeeded;
			throw new ChannelException("ReconnectNeeded");

		default:
			break;
		}
	}

	/**
	 * Send data to the other side of Data Channel. Don't mind of sending result. Only physical attempt to send. Logic of error handling in other place.
	 * @return True - data were sent
	 */
	private boolean sendAvailableData() throws ChannelException {
		if (m_WaitAcknowledgment == null) {
			ChannelData data;
			synchronized (m_DataToSend) {
				data = m_DataToSend.poll();
			}
			if (data != null) {
				makeSendData(data);
				if (!data.getIsService() && data.getIsAcknowledgmentRequired()) {
					m_WaitAcknowledgment = new WaitAcknowledgment(data);
				}
				return true;
			}
		} else {
			if (m_WaitAcknowledgment.getHaveTries()) {
				if (m_WaitAcknowledgment.checkConditionsToResend()) {
					PlatformTools.logError(Tools.getMethodName() + ": Acknowledgment is not received. Repeat to send data");

					makeSendData(m_WaitAcknowledgment.getData());
					return true;
				}
			} else { // connection is bad - no answer
				PlatformTools.logError(Tools.getMethodName() + ": Acknowledgment is not received after sending 3 data packet. Maybe connection is unavailable. Reset it");

				handleAcknowledgmentError(m_WaitAcknowledgment.getData());
				throw new ChannelException("Acknowledgement was not received");
			}
		}
		return false;
	}

	/**
	 * Send data to Channel asynchronous
	 * @param data Store data to send it later
	 */
	public final void sendDataAsync(ChannelData data) {
		PlatformTools.logInformation(Tools.getMethodName());

		if (data != null) {
			synchronized (m_DataToSend) {
				m_DataToSend.offer(data);
			}
		}
	}

	protected static void raiseEventAsync(final EventHandler handler, final Object arg) {
		if (handler != null) {
			if (PlatformTools.isDebug) {
				PlatformTools.logInformation(Tools.getMethodName() + ": event is " + handler.getClass().getSimpleName());
			}
			s_Executor.execute(new Runnable() {
				@Override
				public void run () {
					try {
						handler.invoke(arg);
					} catch (Exception e) {
						PlatformTools.logError(Tools.getExceptionInfo(e));
					}
				}
			});
		}
	}

	public static void raiseEvent(final EventHandler handler, final Object arg) {
		if (handler != null) {
			try {
				if (PlatformTools.isDebug) {
					PlatformTools.logInformation(Tools.getMethodName() + ": event is " + handler.getClass().getSimpleName());
				}
				handler.invoke(arg);
			} catch (Exception e) {
				PlatformTools.logError(Tools.getExceptionInfo(e));
			}
		}
	}
}
