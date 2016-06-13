package ru.start_car.newrlock.common.network;

import java.net.InetAddress;

public final class IPEndPoint {
	public final InetAddress address;
	public final int port;
	
	public IPEndPoint(final InetAddress address, final int port) {
		this.address = address;
		this.port = port;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = address.hashCode();
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return equals((obj instanceof IPEndPoint) ? (IPEndPoint)obj : null);
	}

	public boolean equals(IPEndPoint ep) {
		return ep != null && ep.address.equals(address) && ep.port == port;
	}

	@Override
	public String toString() {
		return (address != null ? address.toString() : "?") + ":" + port;
	}
}
