package dev.lb.simplebase.net.events;

import java.net.InetSocketAddress;

import dev.lb.simplebase.net.event.Event;

/**
 * 
 */
public class FilterRawConnectionEvent extends Event {

	private final InetSocketAddress address;
	private String name;
	
	public FilterRawConnectionEvent(InetSocketAddress source, String name) {
		super(true, false);
		this.address = source;
		this.name = name;
	}
	
	public InetSocketAddress getRemoteAddress() {
		return address;
	}
	
	public String getNetworkIdName() {
		return name;
	}
	
	public void setNetworkIdName(String name) {
		this.name = name;
	}

}
