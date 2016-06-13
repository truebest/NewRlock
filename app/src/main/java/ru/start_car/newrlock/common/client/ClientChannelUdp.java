package ru.start_car.newrlock.common.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;
import ru.start_car.newrlock.common.network.ChannelData;
import ru.start_car.newrlock.common.network.ChannelException;
import ru.start_car.newrlock.common.network.IPEndPoint;

/*import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;*/

/**
 * Channel to manage connections via Udp protocol
 */
public abstract class ClientChannelUdp extends BaseClientSideChannel {
	/**
	 * Maximum size of receive buffer
	 */
	private static final int s_MaximumBufferSize = 1440;
	/**
	 * Socket object
	 */
	private DatagramSocket m_WorkSocket;
	/**
	 * Listening address or address to send data (server or client)
	 */
	private final IPEndPoint m_WorkEndPoint;
	/**
	 * Packet to receive data
	 */
	private final byte[] m_ReceiveBuffer = new byte[s_MaximumBufferSize];
	/**
	 * List of addresses data can be received from
	 */
	//private static final ArrayList<InetAddress> s_CurrentAddresses;
	
	@Override
	protected boolean getIsChannelAvailable() {
		return m_WorkSocket != null;
	}

	/*static {
		s_CurrentAddresses = getAddresses();
		if (Debug.isDebuggerConnected()) {
			for (InetAddress inetAddress : s_CurrentAddresses) {
				Log.i(MY_LOG, inetAddress.toString());
			}
		}
	}*/

	/*/**
	 * Get list of local machine IP addresses
	 * @param family
	 * @return
	 */
	/*private static final ArrayList<InetAddress> getAddresses() {
		ArrayList<InetAddress> res = new ArrayList<InetAddress>();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					res.add(inetAddress);
				}
			}
		} catch(SocketException e) {
			Log.e(MY_LOG, "ClientChannelUdp.getAddresses: " + e.getMessage());
		}
		return res;
	}*/

	/**
	 * Create instance of connecting Socket
	 * @param address Address for this instance
	 * @param port Port for the instance
	 */
	public ClientChannelUdp(InetAddress address, int port) {
		m_WorkEndPoint = new IPEndPoint(address, port);
	}

	//@SuppressWarnings("unused")
	@Override
	protected boolean checkIsDataFromCorrectServer(ChannelData data) {
		// TODO: maybe checkIsInLocalAddresses is not needed
		Object ep = data.getEndPoint();
		return m_WorkEndPoint.equals(ep) /*|| (checkIsInLocalAddresses(ep))*/;
	}

	/*/**
	 * Check if given end point address equals to one of local ip addresses. To check if program starts from local PC (server and client)
	 * @param endPoint
	 * @return
	 */
	/*private final boolean checkIsInLocalAddresses(Object endPoint) {
		IPEndPoint ep = IPEndPoint.toIPEndPoint(endPoint);
		if (ep != null) {
			Log.i(MY_LOG, "ClientChannelUdp.checkIsInLocalAddresses endpoint is " + ep.toString());
			InetAddress addr = ep.address;
			return addr != null && existsInAddresses(addr);
		}
		return false;
	}

	private static final boolean existsInAddresses(InetAddress addr) {
		if (s_CurrentAddresses.isEmpty()) {
			return true;
		}
		for (InetAddress inetAddress : s_CurrentAddresses) {
			if (addr.equals(inetAddress)) {
				return true;
			}
		}
		return false;
	}*/
	
	@Override
	protected void createChannel() throws ChannelException {
		try {
			m_WorkSocket = new DatagramSocket();
			m_WorkSocket.setSoTimeout(100);
		} catch (SocketException e) {
			PlatformTools.logError(Tools.getExceptionInfo(e));
			throw new ChannelException("CreateChannel", e);
		}
	}

	@Override
	protected void closeChannel() {
		super.closeChannel();
		if (m_WorkSocket != null) {
			m_WorkSocket.close();
			m_WorkSocket = null;
		}
	}

	@Override
	protected ChannelData makeReceiveData() throws ChannelException {
		ChannelData data = null;
		try {
			DatagramPacket packet = new DatagramPacket(m_ReceiveBuffer, 0, m_ReceiveBuffer.length);
			m_WorkSocket.receive(packet);
			IPEndPoint remoteEndPoint = new IPEndPoint(packet.getAddress(), packet.getPort());
			PlatformTools.logInformation(Tools.getMethodName() + ": received bytes:" + packet.getLength() + " Endpoint:" + remoteEndPoint.toString());

			data = ChannelData.createFromRawData(m_ReceiveBuffer, 0, packet.getLength(), remoteEndPoint);
		} catch (SocketTimeoutException e) {
		} catch (IOException e) {
			PlatformTools.logError(Tools.getExceptionInfo(e));
			throw new ChannelException("makeReceiveData", e);
		}
		return data;
	}

	@Override
	protected void makeSendData(ChannelData data) throws ChannelException {
		if (data != null) {
			try {
				byte[] rawData = data.getChannelRawData();
				if (rawData != null && rawData.length <= s_MaximumBufferSize) {
					PlatformTools.logInformation(Tools.getMethodName() + ": sent bytes:"  + rawData.length + " Id:" + data.getPacketId() + " Endpoint:" + (data.getEndPoint() != null ? data.getEndPoint().toString() : "default (" + m_WorkEndPoint.toString() + ")"));

					DatagramPacket packet = new DatagramPacket(rawData, 0, rawData.length, m_WorkEndPoint.address, m_WorkEndPoint.port);
					m_WorkSocket.send(packet);
				}
			} catch (IOException e) {
				PlatformTools.logError(Tools.getExceptionInfo(e));
				throw new ChannelException("makeSendData", e);
			}
		}
	}
}
