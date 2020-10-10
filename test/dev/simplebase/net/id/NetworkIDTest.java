package dev.simplebase.net.id;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;

class NetworkIDTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testLocal() {
		NetworkID local1 = NetworkID.createID("Local-01");
		NetworkID local2 = NetworkID.createID("Local-01");
		NetworkID local3 = NetworkID.createID("Local-AB");
		assertEquals(local1, local2, "Ids are not equal (1)");
		assertNotEquals(local1, local3, "Ids are equal (1)");
		assertNotEquals(local2, local3, "Ids are equal (2)");
		
		assertTrue(local1.hasFeature(NetworkIDFeature.INTERNAL), "No LOCAL function");
		assertFalse(local1.hasFeature(NetworkIDFeature.NETWORK), "Unexpected NETWORK function");
		assertFalse(local1.hasFeature(NetworkIDFeature.BIND), "Unexpected BIND function");
		assertFalse(local1.hasFeature(NetworkIDFeature.CONNECT), "Unexpected CONNECT function");
		assertFalse(local1.hasFeature(null), "Unexpected <null> function");
		
		assertEquals(local2.getFeature(NetworkIDFeature.INTERNAL), local2.getDescription(), "LOCAL does not match description");
		assertThrows(UnsupportedOperationException.class, () -> local2.getFeature(NetworkIDFeature.NETWORK), "NETWORK get does not throw");
		assertThrows(UnsupportedOperationException.class, () -> local2.getFeature(NetworkIDFeature.BIND), "BIND get does not throw");
		assertThrows(UnsupportedOperationException.class, () -> local2.getFeature(NetworkIDFeature.CONNECT), "CONNECT get does not throw");
		assertThrows(UnsupportedOperationException.class, () -> local2.getFeature(null), "<null> get does not throw");
		
		assertEquals(local3.getDescription(), "Local-AB", "Description incorrect");
		assertEquals(local3.clone(), local3, "Clone not identical");
		assertEquals(local3.clone().hashCode(), local3.hashCode(), "Clone hash not identical");
		
		NetworkID local4 = local3.clone("Test");
		assertEquals(local4.getDescription(), "Test", "Cloned description not equal");
		assertNotEquals(local3, local4, "Cloned derived ids are equal");
		NetworkID local5 = NetworkID.createID("Test");
		assertEquals(local4, local5, "Ids are not equal (2)");
		
		NetworkID local6 = local5.clone((s) -> s + "_Test");
		assertEquals(local6.getDescription(), "Test_Test", "Cloned description not equal");
		assertNotEquals(local5, local6, "Cloned derived ids are equal");
		NetworkID local7 = NetworkID.createID("Test_Test");
		assertEquals(local6, local7, "Ids are not equal (2)");
	}
	
	@Test
	void testBind() {
		NetworkID bind1 = NetworkID.createID("Bind-01", 1234);
		NetworkID bind2 = NetworkID.createID("Bind-01", 1234);
		NetworkID bind3 = NetworkID.createID("Bind-01", 4321);
		NetworkID bind4 = NetworkID.createID("Bind-AB", 4321);
		
		assertEquals(bind1, bind2, "Ids are not equal (1)");
		assertNotEquals(bind2, bind3, "Ids are equal (1)");
		assertNotEquals(bind3, bind4, "Ids are equal (2)");
		
		assertFalse(bind1.hasFeature(NetworkIDFeature.INTERNAL), "Unexpected LOCAL function");
		assertTrue(bind1.hasFeature(NetworkIDFeature.NETWORK), "No NETWORK function");
		assertTrue(bind1.hasFeature(NetworkIDFeature.BIND), "No BIND function");
		assertFalse(bind1.hasFeature(NetworkIDFeature.CONNECT), "Unexpected CONNECT function");
		assertFalse(bind1.hasFeature(null), "Unexpected <null> function");
		
		assertThrows(UnsupportedOperationException.class, () -> bind2.getFeature(NetworkIDFeature.INTERNAL), "LOCAL get does not throw");
		assertEquals(bind2.getFeature(NetworkIDFeature.NETWORK), NetworkIDFeature.BIND, "NETWORK does not match <BIND function>");
//		assertDoesNotThrow(() -> bind2.getFunction(NetworkIDFunction.BIND), "BIND throws exception");
		assertTrue(bind2.getFeature(NetworkIDFeature.BIND) instanceof SocketAddress, "BIND does not match type <SocketAddress>");
		assertThrows(UnsupportedOperationException.class, () -> bind2.getFeature(NetworkIDFeature.CONNECT), "CONNECT get does not throw");
		assertThrows(UnsupportedOperationException.class, () -> bind2.getFeature(null), "<null> get does not throw");
		
		SocketAddress address1 = bind3.getFeature(NetworkIDFeature.BIND);
		assertTrue(address1 instanceof InetSocketAddress, "Address is not a InetSocketAddress. Not an error, but tests have to be rewritten");
		InetSocketAddress address2 = (InetSocketAddress) address1;
		assertEquals(address2.getPort(), 4321, "Port not equal");
		assertTrue(address2.getAddress().isAnyLocalAddress(), "Not a local wildcard");
		
		NetworkID bind5 = bind4.clone("NewTitle");
		NetworkID bind6 = NetworkID.createID("NewTitle", 4321);
		assertEquals(bind4, bind4.clone(), "Cloned id is not equal");
		assertNotEquals(bind4, bind5, "Cloned derived id is equal");
		assertEquals(bind5, bind6, "Ids are not equal (2)");
		
//		assertDoesNotThrow(() -> bind4.getFunction(bind4.getFunction(NetworkIDFunction.NETWORK)), "Could not get socketaddress function");
	}

	@Test
	void testConnect() throws UnknownHostException {
		NetworkID connect1 = NetworkID.createID("Connect-01", InetAddress.getLocalHost(), 1234);
		NetworkID connect2 = NetworkID.createID("Connect-01", InetAddress.getLocalHost(), 1234);
		NetworkID connect3 = NetworkID.createID("Connect-01", new InetSocketAddress(0).getAddress(), 4321); //Wildcard local
		NetworkID connect4 = NetworkID.createID("Connect-AB", InetAddress.getLocalHost(), 4321);
		
		assertEquals(connect1, connect2, "Ids are not equal (1)");
		assertNotEquals(connect2, connect3, "Ids are equal (1)");
		assertNotEquals(connect3, connect4, "Ids are equal (2)");
		
		assertFalse(connect1.hasFeature(NetworkIDFeature.INTERNAL), "Unexpected LOCAL function");
		assertTrue(connect1.hasFeature(NetworkIDFeature.NETWORK), "No NETWORK function");
		assertFalse(connect1.hasFeature(NetworkIDFeature.BIND), "Unexpected BIND function");
		assertTrue(connect1.hasFeature(NetworkIDFeature.CONNECT), "No CONNECT function");
		assertFalse(connect1.hasFeature(null), "Unexpected <null> function");
		
		assertThrows(UnsupportedOperationException.class, () -> connect2.getFeature(NetworkIDFeature.INTERNAL), "LOCAL get does not throw");
		assertEquals(connect2.getFeature(NetworkIDFeature.NETWORK), NetworkIDFeature.CONNECT, "NETWORK does not match <CONNECT function>");
//		assertDoesNotThrow(() -> connect2.getFunction(NetworkIDFunction.CONNECT), "CONNECT throws exception");
		assertTrue(connect2.getFeature(NetworkIDFeature.CONNECT) instanceof SocketAddress, "CONNECT does not match type <SocketAddress>");
		assertThrows(UnsupportedOperationException.class, () -> connect2.getFeature(NetworkIDFeature.BIND), "BIND get does not throw");
		assertThrows(UnsupportedOperationException.class, () -> connect2.getFeature(null), "<null> get does not throw");
		
		SocketAddress address1 = connect2.getFeature(NetworkIDFeature.CONNECT);
		assertTrue(address1 instanceof InetSocketAddress, "Address is not a InetSocketAddress. Not an error, but tests have to be rewritten");
		InetSocketAddress address2 = (InetSocketAddress) address1;
		assertEquals(address2.getAddress(), InetAddress.getLocalHost(), "Address is not localhost");
		assertEquals(address2.getPort(), 1234, "Port is not 1234");
		assertTrue(((InetSocketAddress) connect3.getFeature(NetworkIDFeature.CONNECT)).getAddress().isAnyLocalAddress(), "Not a wildcard");
		
//		assertDoesNotThrow(() -> connect4.getFunction(connect4.getFunction(NetworkIDFunction.NETWORK)), "Could not get socketaddress function");
	}
	
	@Test
	void typeTest() {
		NetworkID local = NetworkID.createID("ABC");
		NetworkID bind = NetworkID.createID("ABC", 1234);
		NetworkID connect = NetworkID.createID("ABC", InetAddress.getLoopbackAddress(), 1234);
		
		assertNotEquals(local, bind);
		assertNotEquals(local, connect);
		assertNotEquals(bind, connect);
	}
	
	
	
}
