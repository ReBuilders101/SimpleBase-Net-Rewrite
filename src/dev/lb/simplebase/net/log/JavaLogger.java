package dev.lb.simplebase.net.log;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a java {@link Logger} in an {@link AbstractLogger} implementation.
 * <p>
 * Special considerations:<br>
 * Log levels are mapped according to the following table. The numerical values are the same for Java and NetAPI levels
 * <table>
 * <tr><th> Java {@link Level} constant 	</th><th> NetAPI {@link AbstractLogLevel} constant 	</th><th> Numerical value </th></tr>
 * <tr><td> {@link Level#OFF} 				</td><td> {@link LogLevel#HIGHEST} 					</td><td> {@link Integer#MAX_VALUE} </td></tr>
 * <tr><td> {@link JavaLogger#JAVA_FATAL} 	</td><td> {@link LogLevel#FATAL} 					</td><td> {@code 1100} </td></tr>
 * <tr><td> {@link Level#SEVERE} 			</td><td> {@link LogLevel#ERROR} 					</td><td> {@code 1000} </td></tr>
 * <tr><td> {@link Level#WARNING} 			</td><td> {@link LogLevel#WARNING} 					</td><td> {@code 900} </td></tr>
 * <tr><td> {@link Level#INFO} 				</td><td> {@link LogLevel#INFO} 					</td><td> {@code 800} </td></tr>
 * <tr><td> {@link Level#CONFIG} 			</td><td> {@link LogLevel#DEBUG} 					</td><td> {@code 700} </td></tr>
 * <tr><td> {@link Level#FINE}				</td><td> {@link LogLevel#METHOD} 					</td><td> {@code 500} </td></tr>
 * <tr><td> {@link Level#FINER}				</td><td> {@link JavaLogger#NET_FINER}				</td><td> {@code 400} </td></tr>
 * <tr><td> {@link Level#FINEST}			</td><td> {@link JavaLogger#NET_FINEST} 			</td><td> {@code 300} </td></tr>
 * <tr><td> {@link Level#ALL}				</td><td> {@link LogLevel#LOWEST}			 		</td><td> {@link Integer#MIN_VALUE} </td></tr>
 * </table>
 */
public class JavaLogger implements AbstractLogger {

	public static final Level JAVA_FATAL = new LevelExtend("FATAL", 1100);
	
	public static final AbstractLogLevel NET_FINER = CustomLogLevel.create(400);
	public static final AbstractLogLevel NET_FINEST = CustomLogLevel.create(300);
	
	private final Logger javaLogger;
	
	protected JavaLogger(Logger wrappedLogger) {
		this.javaLogger = wrappedLogger;
	}
	
	
	private static AbstractLogLevel fromJava(Level level) {
		return CustomLogLevel.create(level.intValue(), NET_FINER, NET_FINEST);
	}
	
	private static Level toJava(AbstractLogLevel level) {
		switch (level.getPriority()) {
		case Integer.MIN_VALUE: return Level.ALL;
		case  300: return Level.FINEST;
		case  400: return Level.FINER;
		case  500: return Level.FINE;
		case  700: return Level.CONFIG;
		case  800: return Level.INFO;
		case  900: return Level.WARNING;
		case 1000: return Level.SEVERE;
		case 1100: return JAVA_FATAL;
		default: return new LevelExtend("CustomPriority:" + level.getPriority(), level.getPriority());
		}
	}
	
	private static StackTraceElement stackPop(int amount) {
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		//We have to access currentStack[stackPop]
		if(amount > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + amount + " for logging");
		}
		return currentStack[amount];
	}
	
	
	@Override
	public void enterMethod(String comment) {
		final StackTraceElement method = stackPop(1);
		javaLogger.entering(method.getClassName(), method.getMethodName());
	}

	@Override
	public void exitMethod(String comment) {
		final StackTraceElement method = stackPop(1);
		javaLogger.exiting(method.getClassName(), method.getMethodName());
	}

	@Override
	public void log(AbstractLogLevel level, String message) {
		javaLogger.log(toJava(level), message);
	}

	@Override
	public void log(AbstractLogLevel level, Supplier<String> message) {
		javaLogger.log(toJava(level), message);
	}

	@Override
	public void log(AbstractLogLevel level, String formatString, Object... objects) {
		javaLogger.log(toJava(level), String.format(formatString, objects));
	}

	@Override
	public void log(AbstractLogLevel level, Exception messageAndStacktrace) {
		javaLogger.log(toJava(level), messageAndStacktrace.getMessage(), messageAndStacktrace);
	}
	
	@Override
	public void log(AbstractLogLevel level, String message, Exception stacktrace) {
		javaLogger.log(toJava(level), message, stacktrace);
	}

	@Override
	public void stack(AbstractLogLevel level) {
		stack(level, 2, null);
	}

	@Override
	public void stack(AbstractLogLevel level, int popEntries) {
		stack(level, popEntries + 2, null);
	}

	@Override
	public void stack(AbstractLogLevel level, String comment) {
		stack(level, 2, comment);
	}

	@Override
	public void stack(AbstractLogLevel level, int popEntries, String comment) {
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		if(popEntries > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + popEntries + " for logging");
		}
		//pop off the top
		final StackTraceElement[] partialStack = new StackTraceElement[currentStack.length - popEntries];
		System.arraycopy(currentStack, popEntries, partialStack, 0, currentStack.length - popEntries);
		//Now read the finished stacktrace to the stringbuilder
		final StringBuilder stackTrace = new StringBuilder(comment == null ? "" : comment);
		stackTrace.append("\n");
		DefaultFormatter.appendStacktrace(stackTrace, currentStack);
		javaLogger.log(toJava(level), stackTrace.toString());
	}

	@Override
	public AbstractLogLevel getLogLevel() {
		return fromJava(javaLogger.getLevel());
	}

	@Override
	public void setLogLevel(AbstractLogLevel logLevel) {
		javaLogger.setLevel(toJava(logLevel));
	}
	
	private static class LevelExtend extends Level {
		private static final long serialVersionUID = -4251851510252873036L;

		protected LevelExtend(String string, int n) {
			super(string, n);
		}
		
	}
}
