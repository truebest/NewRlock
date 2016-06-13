package ru.start_car.newrlock.common.messages;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import ru.start_car.newrlock.common.aids.IntPtr;

public final class DateTimeUtc extends SerializableObject {
	public final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

	/*
	public Date getDateValue() {
		return calendar.getTime();
	}*/
	
	public DateTimeUtc(Date date) {
		calendar.setTime(date);
	}

	public DateTimeUtc(byte[] data, IntPtr offset) {
		int year, month, day, hour, minute, second, millisecond;
		IntPtr i = new IntPtr(0);
		if (getSIntVar(i, data, offset)) {
			year = i.value;
			if (getSIntVar(i, data, offset)) {
				month = i.value;
				if (getSIntVar(i, data, offset)) {
					day = i.value;
					if (getSIntVar(i, data, offset)) {
						hour = i.value;
						if (getSIntVar(i, data, offset)) {
							minute = i.value;
							if (getSIntVar(i, data, offset)) {
								second = i.value;
								if (getSIntVar(i, data, offset)) {
									millisecond = i.value;
									calendar.set(year, month, day, hour, minute, second);
									calendar.set(Calendar.MILLISECOND, millisecond);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected byte[] getByteList() {
		byte[] b1 = toSIntVar(calendar.get(Calendar.YEAR));
		byte[] b2 = toSIntVar(calendar.get(Calendar.MONTH));
		byte[] b3 = toSIntVar(calendar.get(Calendar.DATE));
		byte[] b4 = toSIntVar(calendar.get(Calendar.HOUR_OF_DAY));
		byte[] b5 = toSIntVar(calendar.get(Calendar.MINUTE));
		byte[] b6 = toSIntVar(calendar.get(Calendar.SECOND));
		byte[] b7 = toSIntVar(calendar.get(Calendar.MILLISECOND));
		
		ByteBuffer res = ByteBuffer.allocate(b1.length + b2.length + b3.length + b4.length + b5.length + b6.length + b7.length);
		res.put(b1);
		res.put(b2);
		res.put(b3);
		res.put(b4);
		res.put(b5);
		res.put(b6);
		res.put(b7);
		return res.array();
	}
	
	@Override
	public SerializableTypes getSerializableType() {
		return SerializableTypes.DateTimeUtc;
	}
}
