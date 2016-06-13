package ru.start_car.newrlock.common.network;

/**
 * It Occurs on channel error (logical or physical)
 * @author Worker
 *
 */
@SuppressWarnings("serial")
public class ChannelException extends Exception {
	public ChannelException(String detailMessage) {
		super(detailMessage);
	}
	public ChannelException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
