package ru.start_car.newrlock.common.messages;

import java.nio.ByteBuffer;

import ru.start_car.newrlock.common.aids.IntPtr;

public class TempObject extends SerializableObject {
	public int intVal;
	public DateTimeUtc dateVal;
	public String textVal;

	public TempObject() { }

	public TempObject(byte[] data, IntPtr offset) {
		IntPtr i = new IntPtr(0);
		if (getSIntVar(i, data, offset)) {
			intVal = i.value;
		}
		if (offset.value < data.length) {
			Object obj = SerializableObject.createInstance(data, offset);
			dateVal = (obj instanceof DateTimeUtc ? (DateTimeUtc)obj : null);
		}
		if (offset.value < data.length) {
			textVal = getString(data, offset);
		}
	}

	protected byte[] getByteList() {
		byte[] bi = toSIntVar(intVal);
		byte[] bd = SerializableObject.instanceToBytes(dateVal);
		byte[] bs = getStringAsSerializedBytes(textVal);
		
		ByteBuffer res = ByteBuffer.allocate(bi.length + bd.length + bs.length);
		res.put(bi);
		res.put(bd);
		res.put(bs);
		return res.array();
	}

	public SerializableTypes getSerializableType() {
		return SerializableTypes.TempObject;
	}
}
