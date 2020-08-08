package dev.lb.simplebase.net.connection;

import java.io.IOException;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.id.NetworkID;

@Internal
@FunctionalInterface
public interface ConnectionConstructor {

	public NetworkConnection construct(NetworkID id, Object customData) throws IOException;
	
}
