package dev.lb.simplebase.net.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Contains helper methods to restrict access to certain internal APIs depending on the caller class.
 */
@StaticType
public class InternalAccess {

	private InternalAccess() {}
	
	/**
	 * Ensures that a method was called from a certain class, and throws an exception if that is
	 * not the case.
	 * @param callerClass The required caller class
	 * @param popExtraFrames The amount of extra stack frames to pop to find the caller to check
	 * @param errorMessage The error message in case of an invalid caller
	 * @throws InternalAPIException If the caller class is different from the required class
	 */
	public static void assertCaller(Class<?> callerClass, int popExtraFrames, String errorMessage) throws InternalAPIException {
		Objects.requireNonNull(callerClass, "'callerClass' parameter must not be null");
		if(popExtraFrames < 0) throw new IllegalArgumentException("'popExtraFrames' must not be negative");
		
		assertCaller(callerClass.getName(), popExtraFrames + 1, errorMessage);
	}
	
	private static void assertCaller(String className, int popExtraFrames, String errorMessage) {
		final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		//Pop: this method, the method that called this, 1 for getStackTrace() method
		final int actualPop = 3 + popExtraFrames;
		
		if(actualPop < stacktrace.length) {
			final StackTraceElement element = stacktrace[actualPop];
			if(Objects.equals(element.getClassName(), className)) {
				return; //success - valid caller
			} else {
				throw new InternalAPIException(errorMessage, element.getClassName(),  className);
			}
		} else {
			throw new RuntimeException("Not enough stack frames to pop");
		}
	}
	
	/**
	 * Can be used in {@link Packet#readData(dev.lb.simplebase.net.io.ReadableByteData)} to assert that a
	 * call has been made from a valid location.
	 */
	public static void assertPacketRead() {
		assertCaller("dev.lb.simplebase.net.packet.format.NetworkPacketFormat1Packet", 0, "Cannot call Packet.readData() manually");
	}
	
	/**
	 * A type of {@link RuntimeException} that is thrown when {@link InternalAccess#assertCaller(Class, int, String)}
	 * has determined that the caller class is invalid.
	 */
	public static class InternalAPIException extends RuntimeException {
		private static final long serialVersionUID = 4527604330125529186L;

		private final String actualCaller;
		private final String requiredCaller;
		
		private InternalAPIException(String message, String actualCaller, String requiredCaller) {
			super(message);
			this.actualCaller = actualCaller;
			this.requiredCaller = requiredCaller;
		}
		
		/**
		 * The fully qualified name of the class that actually called the method
		 * @return The name of the actual caller class
		 */
		public String getActualCallerClass() {
			return actualCaller;
		}
		
		/**
		 * The fully qualified name of the class that is required to be the caller
		 * @return The name of the required caller class
		 */
		public String getRequiredCallerClass() {
			return requiredCaller;
		}

		@Override
		public String toString() {
			final String nameAndMessage = getLocalizedMessage() == null ? "InternalAPIException" :
				"InternalAPIException: " + getLocalizedMessage();
			return nameAndMessage + " (Called from " + actualCaller + ", requires " + requiredCaller + ")";
		}
	}
	
	/**
	 * Creates an {@link AtomicBoolean} that can be used to assert that only a single instance of a class is ever created.
	 * <p>
	 * The returned value should be stored in a {@code private static final} field in the singleton class.<br>
	 * In the constructor, call {@link InternalAccess#assertSingleton(AtomicBoolean, String)} with the returned
	 * {@code AtomicBoolean}.
	 * </p><p>
	 * The returned {@code AtomicBoolean} will initially be set to {@code false}. A value of {@code true} means that
	 * an instance exists.
	 * </p>
	 * @return An {@link AtomicBoolean} with value {@code false}
	 */
	public static AtomicBoolean createSingleton() {
		return new AtomicBoolean(false); //bool =^= instance created
	}
	
	/**
	 * Asserts that an instance of a singleton can be created.
	 * <p>
	 * The {@link AtomicBoolean} should be created as stated in {@link InternalAccess#createSingleton()}.
	 * This method should the be called in the constructor of the singleton class.
	 * It will return on success, or simply throw an unchecked exception to immediately exit the constructor.
	 * </p><p>
	 * If the method returns normally, the {@code AtomicBoolean} will have been updated to {@code true}.
	 * <p>
	 * @param staticBool The static {@link AtomicBoolean} of the class
	 * @param message The error message for the thrown exception
	 * @throws IllegalStateException When an instance already exists
	 */
	public static void assertSingleton(AtomicBoolean staticBool, String message) throws IllegalStateException {
		if(staticBool.getAndSet(true)) throw new IllegalStateException(message);
	}
	
	public static void freeSingleton(AtomicBoolean staticBool, String message) throws IllegalStateException {
		if(!staticBool.getAndSet(false)) throw new IllegalStateException(message);
	}
	
	public static boolean hasSingleton(AtomicBoolean staticBool) {
		return staticBool.get();
	}
}
