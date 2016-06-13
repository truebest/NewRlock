package ru.start_car.newrlock.common.network;

public class WaitAcknowledgment {
	/**
	 * Maximum time to wait acknowledgment in milliseconds
	 */
	public static final int WAIT_ACK_MSEC = 5000;
	/**
	 * Rest count to repeat sending data
	 */
	private int m_RepeatCount;
	/**
	 * Time when waiting is out
	 */
	private long m_TimeOff;

	private final ChannelData m_Data;
	/**
	 *  Waiting DataChannel Id to confirm data was sent successfully
	 */
	public ChannelData getData() {
		return m_Data;
	}
	/**
	 * Get value indicating there are some tries to repeat sending data to channel
	 * @return True if there are some tries
	 */
	public boolean getHaveTries() {
		return m_RepeatCount > 0;
	}

	/**
	 * Create instance of class for given ChannelData object
	 * @param data Last sent data to channel
	 */
	public WaitAcknowledgment(ChannelData data) {
		m_Data = data;
		m_TimeOff = System.currentTimeMillis() + WAIT_ACK_MSEC;
		m_RepeatCount = 2; // after first sending there are yet two attempts
	}

	/**
	 * Check id is equal to stored one
	 * @param packetId Check equation of saved packet id and given one
	 * @return True if equals
	 */
	public boolean isIdEqual(int packetId) {
		return m_Data.getPacketId() == packetId;
	}

	/**
	 * Check if waiting time is off and there are some tries to send data is available
	 * @return True if can resend
	 */
	public boolean checkConditionsToResend() {
		long dt = System.currentTimeMillis();
		if (m_RepeatCount > 0 && dt > m_TimeOff) {
			m_TimeOff = dt + WAIT_ACK_MSEC;
			m_RepeatCount--;
			return true;
		}
		return false;
	}
}
