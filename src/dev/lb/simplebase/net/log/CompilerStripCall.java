package dev.lb.simplebase.net.log;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A call to a method with this annotation can be stripped from the source code by an optional
 * preprocessor that runs before compiling.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface CompilerStripCall {

	/**
	 * <p>
	 * The log priority that the annotated method uses to log a message.
	 * </p><p>
	 * This should only be used if the {@link LogLevel} enum doesn't define a level matching the
	 * desired priority. It is only considered of the value of {@link #value()} is {@code null}.
	 * </p>
	 * @return The {@link AbstractLogLevel#getPriority()} that the annotated method will exclusively log at
	 */
	public int priority() default 0;
	
	/**
	 * <p>
	 * A {@link LogLevel} that the annotated method uses to log a message.
	 * </p><p>
	 * If the target log level of the preprocessor is higher than the loglevel defined here,
	 * the method call can be stripped.
	 * </p><p>
	 * If {@link #value()} is {@code null}, the value of {@link #priority()} should be used as a log priority instead.
	 * </p>
	 * @return The {@link LogLevel} that the annotated method will exclusively log at
	 */
	public LogLevel value();
	
}
