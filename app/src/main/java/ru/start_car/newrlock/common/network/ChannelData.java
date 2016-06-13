package ru.start_car.newrlock.common.network;

import java.util.concurrent.atomic.AtomicInteger;

import ru.start_car.newrlock.common.aids.PlatformTools;
import ru.start_car.newrlock.common.aids.Tools;

/**
 * Channel received information including data, endPoint etc.
 * Data packet structure:
 * start
 * byte count
 * 0..  2 - packet size (size of all bellow data)
 * 2..  1 - packet version (0..3 - version, 4..7 - header)
 * 3..  1 - header
 * 4..  4 - packet Id (to skip old packets)
 * 8..  n - message body (size from 0 to n bytes)
 * 8+n..2 - crc of all above data not including size bytes
 * 
 * header bits:
 * 0 - message body is ciphered (if set to 1)
 * 1 - require acknowledgment for data message (if set to 1)
 * 2 - 
 * 3..7 - reserved
 */
public final class ChannelData {
	/**
	 * Minimum size of the packet (if it has no message body)
	 */
	private static final int MINIMUM_PACKET_SIZE = 10;
	/**
	 * size to message body starts plus crc size
	 */
	private static final int SIZE_LENGTH_IN_BYTES = 2;
	/**
	 * Length of size field in packet in bytes
	 */
	private static final int CRC_LENGTH_IN_BYTES = 2;
	/**
	 * Mask for ciphered messages
	 */
	private static final int MASK_CIPHERED = 0x01;
	/**
	 * Mask for require acknowledgment (set to 1) or without it (set to 0)
	 */
	private static final int MASK_ACKNOWLEDGMENT_REQUIRED = 0x02;

	/**
	 * Minimum (initial) value for packet id. Before sending must be incremented by 1 (Don't send with min value)
	 */
	//public static final int intMinValue = 0x80000000;
	/**
	 * Global unique identifier of every Channel Data object
	 */
	private static final AtomicInteger s_Counter = new AtomicInteger(Integer.MIN_VALUE);

	private int m_PacketId;
	/**
	 * Id of the instance
	 * @return This instance id
	 */
	public int getPacketId() {
		return m_PacketId;
	}

	/**
	 * Protocol version
	 */
	private byte m_Version = 1;

	private short m_Header;
	
	
	public ServiceMessageType getServiceMessageType() {
		return ServiceMessageType.fromInt(m_Header);
	}
	public void setServiceMessageType(ServiceMessageType t) {
		m_Header = (short)(t.getValue() & 0x0FFF);
	}
	
	public boolean getIsCiphered() {
		return (m_Header & MASK_CIPHERED) != 0;
	}
	public void setIsCiphered(boolean value) {
		if (value) {
			m_Header |= MASK_CIPHERED;
		} else {
			int v = m_Header & ~MASK_CIPHERED;
			m_Header = (short)(v & 0x0FFF);
		}
	}

	public boolean getIsService() {
		return m_Data == null || m_Data.length == 0;
	}

	public boolean getIsAcknowledgmentRequired() {
		return (m_Header & MASK_ACKNOWLEDGMENT_REQUIRED) != 0;
	}
	public void setIsAcknowledgmentRequired(boolean value) {
		if (value) {
			m_Header |= MASK_ACKNOWLEDGMENT_REQUIRED;
		} else {
			int v = m_Header & ~MASK_ACKNOWLEDGMENT_REQUIRED;
			m_Header = (short)(v & 0x0FFF);
		}
	}


	private Object m_EndPoint;
	/**
	 * Key of data creator
	 * @return Data creator/recipient endpoint
	 */
	public Object getEndPoint() {
		return m_EndPoint;
	}

	private byte[] m_Data;
	/**
	 * Binary data received from channel
	 * @return Data bytes
	 */
	public byte[] getData() {
		return m_Data;
	}
	/**
	 * Binary data to send via channel
	 * @param value Data bytes
	 */
	public void setData(byte[] value) {
		m_Data = value;
	}

	/**
	 * Create instance for Data message, Not ciphered, Set new Id
	 */
	public ChannelData() {
		m_PacketId = s_Counter.incrementAndGet();
		if (m_PacketId == Integer.MIN_VALUE) {
			m_PacketId = s_Counter.incrementAndGet();
		}
	}

	/**
	 * Create instance for Data message, Not ciphered, Set new Id and set key
	 * @param endPoint server end point
	 */
	public ChannelData(Object endPoint) {
		this();
		m_EndPoint = endPoint;
	}

	/**
	 * To create acknowledgment message mainly
	 * @param packetId Id for packet
	 * @param endPoint Endpoint for packet
	 */
	private ChannelData(int packetId, Object endPoint) {
		m_PacketId = packetId;
		m_EndPoint = endPoint;
	}

	public static ChannelData createAcknowledgmentFor(ChannelData data) {
		if (data != null) {
			ChannelData cd = new ChannelData(data.getPacketId(), data.getEndPoint());
			cd.setServiceMessageType(ServiceMessageType.Acknowledgment);
			return cd;
		}
		return null;
	}

	public static ChannelData createServiceMessage(ServiceMessageType msg, Object endPoint) {
		ChannelData cd = new ChannelData(endPoint);
		cd.setServiceMessageType(msg);
		return  cd;
	}
	
	private ChannelData(int packetId, Object endPoint, short header, byte[] data, byte version) {
		m_PacketId = packetId;
		m_EndPoint = endPoint;
		m_Header = header;
		m_Data = data;
		m_Version = version;
	}

	/**
	 * Create instance of the object from bytes received via channel
	 * @param rawData Byte array received from channel
	 * @param index Start index to process the byte array
	 * @param count Count of bytes to proceed
	 * @param endPoint Endpoint of remote channel
	 * @return ChannelData created from received bytes
	 */
	public static ChannelData createFromRawData(byte[] rawData, int index, int count, Object endPoint) {
		if (rawData != null && rawData.length >= MINIMUM_PACKET_SIZE && index >= 0 && (index + count) <= rawData.length) {
			int size = (rawData[index++] & 0xFF) | (rawData[index++] & 0xFF) << 8;
			if (size >= (MINIMUM_PACKET_SIZE - SIZE_LENGTH_IN_BYTES) && size <= (count - SIZE_LENGTH_IN_BYTES)) {
				int crcPos = (SIZE_LENGTH_IN_BYTES - CRC_LENGTH_IN_BYTES) + size;

				int crcCalculated = Tools.Crc16(rawData, SIZE_LENGTH_IN_BYTES, crcPos - SIZE_LENGTH_IN_BYTES);
				int crcReceived = (rawData[crcPos] & 0xFF) | (rawData[crcPos + 1] & 0xFF) << 8;
				if (crcCalculated == crcReceived) {
					byte b = rawData[index++];
					byte version = (byte)(b & 0x0F);
					short header = (short)(rawData[index++] | ((b & 0xF0) << 4));
					int packetId = (rawData[index++] & 0xFF) | ((rawData[index++] & 0xFF) << 8) | ((rawData[index++] & 0xFF) << 16) | ((rawData[index++] & 0xFF) << 24);
					int dataLen = crcPos - index;
					byte[] data = null;
					if (dataLen > 0) {
						if (version >= 1) {
							data = new byte[dataLen];
							System.arraycopy(rawData, index, data, 0, dataLen);
						}
					}
					ChannelData res =new ChannelData(packetId, endPoint, header, data, version);
					PlatformTools.logInformation(Tools.getMethodName() + ": is Ok. Id=" + res.getPacketId());
					return res;
				} else {
					PlatformTools.logInformation(Tools.getMethodName() + ": crc error");
				}
				return null;
			} else {
				PlatformTools.logInformation(Tools.getMethodName() + ": data size error");
			}
		} else {
			PlatformTools.logInformation(Tools.getMethodName() + ": raw data error");
		}
		return null;
	}


	/**
	 * Binary Data with channel protocol additional bytes
	 * @return Byte array from this object
	 */
	public byte[] getChannelRawData() {
		int dataLen = m_Data != null ? m_Data.length : 0;
		byte[] rawData = new byte[dataLen + MINIMUM_PACKET_SIZE];

		int len = rawData.length - SIZE_LENGTH_IN_BYTES;
		int index = 0;
		rawData[index++] = (byte)(len & 0xFF);
		rawData[index++] = (byte)((len >> 8) & 0xFF);
		rawData[index++] = (byte)(((m_Version & 0x0F) | ((m_Header & 0x0F00) >> 4)) & 0xFF);
		rawData[index++] = (byte)(m_Header & 0xFF);

		rawData[index++] = (byte)(m_PacketId & 0xFF);
		rawData[index++] = (byte)((m_PacketId >> 8) & 0xFF);
		rawData[index++] = (byte)((m_PacketId >> 16) & 0xFF);
		rawData[index++] = (byte)((m_PacketId >> 24) & 0xFF);

		if (dataLen > 0) {
			System.arraycopy(m_Data, 0, rawData, index, dataLen);
		}
		int crcCalculated = Tools.Crc16(rawData, SIZE_LENGTH_IN_BYTES, rawData.length - (SIZE_LENGTH_IN_BYTES + CRC_LENGTH_IN_BYTES));
		index = rawData.length - CRC_LENGTH_IN_BYTES;
		rawData[index] = (byte)(crcCalculated & 0xFF);
		rawData[index + 1] = (byte)((crcCalculated >> 8) & 0xFF);

		return rawData;
	}
}
