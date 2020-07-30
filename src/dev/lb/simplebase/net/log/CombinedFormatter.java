package dev.lb.simplebase.net.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CombinedFormatter implements BasicFormatter {

	private final List<BasicFormatter> parts;

	protected CombinedFormatter(BasicFormatter[] parts) {
		this.parts = new ArrayList<>(Arrays.asList(parts));
	}
	
	protected CombinedFormatter(Collection<BasicFormatter> parts) {
		this.parts = new ArrayList<>(parts);
	}

	@Override
	public CharSequence format(AbstractLogLevel level) {
		return parts.stream().map((format) -> format.format(level)).collect(Collectors.joining(" "));
	}
	
}
