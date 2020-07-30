package dev.lb.simplebase.net.event;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class EventDispatchChain<E extends Event> {
	protected final EventDispatcher dispatcher;
	
	protected EventDispatchChain(EventDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	public abstract boolean tryPost(Object...params);
	
	public static <T1, T2, E extends Event> EventDispatchChain.P2<T1, T2, ?> P2(EventDispatcher dispatcher, EventAccessor<E> event, 
			BiFunction<T1, T2, E> eventInstanceBuilder) {
		return new P2<>(dispatcher, event, eventInstanceBuilder);
	}
	
	public static <T1, E extends Event> EventDispatchChain.P1<T1, ?> P1(EventDispatcher dispatcher, EventAccessor<E> event, 
			Function<T1, E> eventInstanceBuilder) {
		return new P1<>(dispatcher, event, eventInstanceBuilder);
	}
	
	public static <E extends Event> EventDispatchChain.P0<?> P0(EventDispatcher dispatcher, EventAccessor<E> event, 
			Supplier<E> eventInstanceBuilder) {
		return new P0<>(dispatcher, event, eventInstanceBuilder);
	}
	
	public static class P2<T1, T2, E extends Event> extends EventDispatchChain<E> {
		
		protected P2(EventDispatcher dispatcher, EventAccessor<E> event, BiFunction<T1, T2, E> builder) {
			super(dispatcher);
			this.event = event;
			this.builder = builder;
		}

		private final EventAccessor<E> event;
		private final BiFunction<T1, T2, E> builder;
		
		public boolean post(T1 t1, T2 t2) {
			return dispatcher.post(event, builder.apply(t1, t2));
		}
		
		public P0<E> bind(T1 t1, T2 t2) {
			return new P0<>(dispatcher, event, () -> builder.apply(t1, t2));
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean tryPost(Object... params) {
			if(params.length != 2) {
				throw new IllegalArgumentException("EventDispatcherChain.P2 requires exactly 2 parameters");
			}
			try {
				return post((T1) params[0], (T2) params[1]);
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
	
	public static class P1<T1, E extends Event> extends EventDispatchChain<E> {
		
		protected P1(EventDispatcher dispatcher, EventAccessor<E> event, Function<T1, E> builder) {
			super(dispatcher);
			this.event = event;
			this.builder = builder;
		}

		private final EventAccessor<E> event;
		private final Function<T1, E> builder;
		
		public boolean post(T1 t1) {
			return dispatcher.post(event, builder.apply(t1));
		}
		
		public P0<E> bind(T1 t1) {
			return new P0<>(dispatcher, event, () -> builder.apply(t1));
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean tryPost(Object... params) {
			if(params.length != 1) {
				throw new IllegalArgumentException("EventDispatcherChain.P2 requires exactly 1 parameter");
			}
			try {
				return post((T1) params[0]);
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
	
	public static class P0<E extends Event> extends EventDispatchChain<E> {
		
		protected P0(EventDispatcher dispatcher, EventAccessor<E> event, Supplier<E> builder) {
			super(dispatcher);
			this.event = event;
			this.builder = builder;
		}

		private final EventAccessor<E> event;
		private final Supplier<E> builder;
		
		public boolean post() {
			return dispatcher.post(event, builder.get());
		}

		@Override
		public boolean tryPost(Object... params) {
			if(params.length != 0) {
				throw new IllegalArgumentException("EventDispatcherChain.P2 requires exactly 0 parameters");
			}
			try {
				return post();
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
	
}