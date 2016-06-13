package ru.start_car.newrlock.common.network;

public enum ServiceMessageType {
	None(0),

	Acknowledgment(1),
	Ping(2),
	ReconnectNeeded(3),
	CloseConnection(4);
	
	
	private final int id;
	ServiceMessageType(int id) { this.id = id; }
	public int getValue() { return id; }

	private static final ServiceMessageType[] values = ServiceMessageType.values();
	public static ServiceMessageType fromInt(int i) {
		for (ServiceMessageType t : values) {
			if (t.id == i) {
				return t;
			}
		}
		return ServiceMessageType.None;
	}
}
