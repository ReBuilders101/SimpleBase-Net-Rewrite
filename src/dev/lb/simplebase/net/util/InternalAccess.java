package dev.lb.simplebase.net.util;

import java.util.Objects;

import dev.lb.simplebase.net.annotation.StaticType;

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
		
		final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		//Pop: this method, the method that called this, 1 for getStackTrace() method
		final int actualPop = 3 + popExtraFrames;
		
		if(actualPop < stacktrace.length) {
			final StackTraceElement element = stacktrace[actualPop];
			if(Objects.equals(element.getClassName(), callerClass.getName())) {
				return; //success - valid caller
			} else {
				throw new InternalAPIException(errorMessage, element.getClassName(),  callerClass.getName());
			}
		} else {
			throw new RuntimeException("Not enough stack frames to pop");
		}
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
}
