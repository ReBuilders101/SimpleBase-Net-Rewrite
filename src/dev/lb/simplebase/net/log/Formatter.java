package dev.lb.simplebase.net.log;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

/**
 * A {@link Formatter} creates a printable string representation of the logged data.
 * <p>
 * Instances can be created with static methods in this class.
 */
public interface Formatter {

	/**
	 * Creates the {@link CharSequence} that should be logged from a plaintext message
	 * @param level The log level of the message; will not be {@code null}
	 * @param rawMessage The message that should be logged; can be {@code null}
	 * @return The complete message; {@code null} is not allowed
	 */
	public CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage);
	
	/**
	 * Creates the {@link CharSequence} that should be logged from an exception
	 * @param level The log level of the message; will not be {@code null}
	 * @param message A description message for the error; can be {@code null}
	 * @param rawMessage The exception for which a message should be generated; can be {@code null}
	 * @return The complete message; {@code null} is not allowed
	 */
	public CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage);
	
	/**
	 * Creates the {@link CharSequence} that should be logged for a
	 * request to print the current stacktrace
	 * @param level The log level of the message; will not be {@code null}
	 * @param comment An additional message to log with the stacktrace; can be {@code null}
	 * @param usedStacktraceItems All stacktrace elements that should be logged; will never be {@code null}
	 * @return The complete message; {@code null} is not allowed
	 */
	public CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems);

	/**
	 * Creates the {@link CharSequence} that should be logged when entering/exiting a method
	 * @param level The log level of the message; will not be {@code null}
	 * @param comment  An additional message to log; can be {@code null}
	 * @param enteringMethod {@code true} if the method is entered, {@code false} if it is exited
	 * @param callingMethodElement The Method that was entered/exited, as a {@link StackTraceElement}; will never be {@code null}
	 * @return The complete message; {@code null} is not allowed
	 */
	public CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement);
	
	/**
	 * Composes a Formatter by applying a {@link Function} to the output of an existing {@link Formatter}.
	 * @param baseFormatter The base Formatter that generates the base strings
	 * @param additionalFormat The additional Formatter function that changes the base string to the proper output. <b>The function must not return {@code null}</b>
	 * @return A new Formatter that combines the base Formatter and the Function
	 * @throws NullPointerException When {@code baseFormatter} or {@code additionalFormat} are {@code null}
	 */
	public static Formatter compose(Formatter baseFormatter, Function<String, CharSequence> additionalFormat) {
		Objects.requireNonNull(baseFormatter, "'baseFormatter' parameter must not be null");
		Objects.requireNonNull(additionalFormat, "'additionalFormat' parameter must not be null");
		return new ComposedFormatter(baseFormatter, additionalFormat);
	}
	
	/**
	 * A default {@link Formatter} implementation that converts log messages into strings
	 * @return A new default formatter instance
	 */
	public static Formatter getDefault() {
		return new DefaultFormatter();
	}
	
	/**
	 * Creates a {@link Formatter} that consists of the outputs of all formatters in the varargs array, separated by space characters
	 * @param elements The formatters that provide the elements
	 * @return The complex composed formatter
	 */
	public static Formatter getComplex(Formatter...elements) {
		Objects.requireNonNull(elements, "'elements' parameter must not be null");
		if(elements.length == 0) {
			throw new IllegalArgumentException("'elements' parameter must contain at least one element");
		}
		//Check for null elements
		for(Formatter f : elements) {
			if(f == null) throw new NullPointerException("'elements' parameter must not contain any null elements");
		}
		return new ComplexFormatter(elements);
	}
	
	/**
	 * Creates a {@link Formatter} that only prints the current thread name enclosed in square brackets:
	 * <br><code>'[<i>threadname</i>]'</code>
	 * <p>Designed to be used with {@link #getComplex(Formatter...)}.
	 * @return The described Formatter.
	 */
	public static Formatter getThreadName() {
		return new BasicFormatter() {
			@Override protected CharSequence format(AbstractLogLevel level) {
				return "[" + Thread.currentThread().getName() + "]";
			}
		};
	}
	
	/**
	 * Creates a {@link Formatter} that only prints the current time enclosed in square brackets:
	 * <br><code>'[<i>currentTime</i>]'</code><br>
	 * The time is converted to a string using {@link DateFormat#getTimeInstance()}.
	 * <p>Designed to be used with {@link #getComplex(Formatter...)}.
	 * @return The described Formatter
	 */
	public static Formatter getCurrentTime() {
		return new BasicFormatter() {
			@Override protected CharSequence format(AbstractLogLevel level) {
				return "[" + ComposedFormatter.DATE_TO_TIME.format(new Date()) + "]";
			}
		};
	}
	
	/**
	 * Creates a {@link Formatter} that only prints the message log level enclosed in square brackets:
	 * <br><code>'[<i>loglevel</i>]'</code>
	 * <p>Designed to be used with {@link #getComplex(Formatter...)}.
	 * @return The described Formatter
	 */
	public static Formatter getLogLevel() {
		return new BasicFormatter() {
			@Override protected CharSequence format(AbstractLogLevel level) {
				if(level instanceof Enum) { //Use the enum constant name
					return "[" + level.toString() + "]";
				} else {
					return "[CustomPriority:" + level.getPriority() + "]";
				}
			}
		};
	}
	
	/**
	 * Creates a {@link Formatter} that only prints a static string enclosed in square brackets:
	 * <br><code>'[<i>string</i>]'</code>
	 * <p>Designed to be used with {@link #getComplex(Formatter...)}.
	 * @param text The string to print
	 * @return The described Formatter
	 */
	public static Formatter getStaticText(String text) {
		return new BasicFormatter() {
			@Override protected CharSequence format(AbstractLogLevel level) {
				return "[" + text + "]";
			}
		};
	}
	
	/**
	 * Prefixes the string with the threadname enclosed in square brackets, followed by a trailing space and the
	 * base text ('[<i>treadname</i>] <i>baseString</i>')
	 * <p>
	 * Can be used as a method reference in {@link #compose(Formatter, Function)} to create a new {@link Formatter}.<br>
	 * Method is not meant to be called directly.
	 * <p>
	 * Example code:<br>
	 * <code><pre>Formatter composed = Formatter.compose(baseFormatter, Formatter::prefixThreadName);</pre></code>
	 * @param baseString The base text provided by the base Formatter
	 * @return The resulting text, prefixed with the name of the current thread
	 */
	public static CharSequence prefixThreadName(String baseString) {
		final StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(Thread.currentThread().getName());
		builder.append("] ");
		builder.append(baseString);
		return builder;
	}
	
	/**
	 * Prefixes the string with the current time enclosed in square brackets, followed by a trailing space and the
	 * base text ('[<i>currenttime</i>] <i>baseString</i>').<br>
	 * The time string is generated by {@link DateFormat#getTimeInstance()}.
	 * <p>
	 * Can be used as a method reference in {@link #compose(Formatter, Function)} to create a new {@link Formatter}.<br>
	 * Method is not meant to be called directly.
	 * <p>
	 * Example code:<br>
	 * <code><pre>Formatter composed = Formatter.compose(baseFormatter, Formatter::prefixCurrentTime);</pre></code>
	 * @param baseString The base text provided by the base Formatter
	 * @return The resulting text, prefixed with the current time
	 */
	public static CharSequence prefixCurrentTime(String baseString) {
		final StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(ComposedFormatter.DATE_TO_TIME.format(new Date()));
		builder.append("] ");
		builder.append(baseString);
		return builder;
	}
	
}
