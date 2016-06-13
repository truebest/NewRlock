package ru.start_car.newrlock.common.messages;

public enum SerializableTypes
{
	/**
	 * Data not specified
	 */
	Unknown(0),


	/**
	 * Authentication message
	 */
	Authentication(20),


	/**
	 * DateTime message
	 */
	DateTimeUtc(25),
	

	/**
	 * Data received from device
	 */
	DeviceData(50),



	/**
	 * For test purposes only
	 */
	TempObject(51);
	
	private final int id;
	SerializableTypes(int id) { this.id = id; }
	public int getValue() { return id; }

	private static final SerializableTypes[] values = SerializableTypes.values();
	public static SerializableTypes fromInt(int i) {
		for (SerializableTypes t : values) {
			if (t.id == i) {
				return t;
			}
		}
		return SerializableTypes.Unknown;
	}
}