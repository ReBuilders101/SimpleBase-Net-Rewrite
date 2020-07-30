package dev.lb.simplebase.net.log;

public class DelegateFormattableLogger extends FormattableLogger {

	private final FormattableLogger delegate;
	private final BasicFormatter morePrefix;
	
	public DelegateFormattableLogger(FormattableLogger delegate, BasicFormatter morePrefix) {
		super(delegate.getLogLevel(), false, null, null);
		this.delegate = delegate;
		this.morePrefix = morePrefix;
	}

	@Override
	protected void postLogMessage(CharSequence prefix, CharSequence message) {
		final CharSequence newPrefix = String.join(" ", prefix, morePrefix.format(null));
		delegate.postLogMessage(newPrefix, message);
	}

	@Override
	public AbstractLogLevel getLogLevel() {
		return delegate.getLogLevel();
	}

	@Override
	public void setLogLevel(AbstractLogLevel logLevel) {
		throw new UnsupportedOperationException("The DelegateFormattableLogger implementation does not allow changing the Log Level");
	}

	@Override
	protected BasicFormatter getPrefixFormatter() {
		return delegate.getPrefixFormatter();
	}

	@Override
	protected Formatter getMessageFormatter() {
		return delegate.getMessageFormatter();
	}

}
