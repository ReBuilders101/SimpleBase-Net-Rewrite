package dev.lb.simplebase.net.util;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class CombinedTask extends AwaitableTask {
	public CombinedTask(Task...tasks) {
		super(tasks.length);
		for(Task t : tasks) t.then(this::release);
	}
}
