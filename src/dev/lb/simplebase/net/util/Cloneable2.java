package dev.lb.simplebase.net.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An improved {@link Cloneable} extension that explicitly declares a {@code public} {@link #clone()} method, so that
 * {@code Cloneable2} can be used e.g. as a generic type and provide the {@code clone()} method without reflection. 
 */
public interface Cloneable2 extends Cloneable {

	/**
	 * Creates an exact, usually flat copy of this object. Always returns a new instance.
	 * <p>There are some general requirements for an implementation of {@link #clone()}:
	 * <ul>
	 * <p><li>The returned object should be different from this object, so that:<br>
	 * <code>x.clone() != x</code> (as in {@link Object#clone()})<br>
	 * If this behavior is not required (e.g. for immutable types), use {@link #copy()}, which acts as {@link #clone()}
	 * without this limitation, and may simply return {@code this} for an immutable or effectively immutable type.
	 * </li></p>
	 * <p><li>The actual return type of the method should be the exact type of this object. The return type is declared as
	 * {@link Object} to avoid problems with type parameters on the {@code Cloneable2} interface, but the actually returned type
	 * must still be the type of the cloned object.
	 * A subclass of a class that implements {@code Cloneable2} <b>must override</b> the existing {@code clone()} implementation to return
	 * the correct type, so that:<br>
	 * <code>x.clone().getClass() == x.getClass()</code> (as in {@link Object#clone()})
	 * </li></p>
	 * <p><li>
	 * The returned object must be {@code equal()} to this object (and as {@link #equals(Object)} states, the objects must also have the same
	 * {@link #hashCode()}), so that:<br>
	 * <code>x.clone().equals(x)</code> (this is recommended in {@link Object#clone()}, but required here)<br>
	 * If the implementing class has some members or states that are not considered by {@link #equals(Object)}, it is recommended
	 * but not required that they are also cloned.
	 * </p></li>
	 * </ul>
	 * </p>
	 * <p>
	 * Implementations of this method should change the return type to the correct type in their method declarations, so that
	 * unchecked casts can be avoided. If the implementation returns {@link Object}, the {@link #typedClone(Cloneable2)} method can be
	 * used to get a copy of the correct type.
	 * </p><p>
	 * Assuming that an objects {@link #equals(Object)} and {@link #hashCode()} methods only depend on the states of all or some of
	 * the classes fields, the default {@link Object#clone()} implementation satisfies all requirements.
	 * </p>
	 * @return The cloned object. The actual return type is the runtime type of the objecton which {@code clone()} was called,
	 * which is at least the declared type of that object or a subtype.
	 * @see #typedClone(Cloneable2)
	 */
	public Object clone();
	
	/**
	 * Creates an exact, usually flat copy of this object. Unlike {@link #clone()}, it might return the same instance for effectively immutabale types.
	 * <p>There are some general requirements for an implementation of {@link #copy()}:
	 * <ul>
	 * <p><li>The returned object should not be altered by changes made to the original object. For mutable types, a new and independent instance is
	 * required, but immutable objects can be reused. The copy still is a flat copy, so changes of a members state might be shared by both original and copy,
	 * as it is the case with the {@code clone()} method too.
	 * </li></p>
	 * <p><li>The actual return type of the method should be the exact type of this object. The return type is declared as
	 * {@link Object} to avoid problems with type parameters on the {@code Cloneable2} interface, but the actually returned type
	 * must still be the type of the cloned object.
	 * A subclass of a class that implements {@code Cloneable2} <b>must override</b> the existing {@code clone()} implementation to return
	 * the correct type, so that:<br>
	 * <code>x.clone().getClass() == x.getClass()</code> (as in {@link Object#clone()})
	 * </li></p>
	 * <p><li>
	 * The returned object must be {@code equal()} to this object (and as {@link #equals(Object)} states, the objects must also have the same
	 * {@link #hashCode()}), so that:<br>
	 * <code>x.clone().equals(x)</code> (this is recommended in {@link Object#clone()}, but required here)<br>
	 * If the implementing class has some members or states that are not considered by {@link #equals(Object)}, it is recommended
	 * but not required that they are also cloned.
	 * </p></li>
	 * </ul>
	 * </p>
	 * <p>
	 * Implementations of this method should change the return type to the correct type in their method declarations, so that
	 * unchecked casts can be avoided.<br>
	 * The default implementation of {@link #copy()} is simply to call the {@link #clone()} method, which will always produce a valid result. 
	 * </p>
	 * @return The copied object. The actual return type is the runtime type of the objecton which {@code clone()} was called,
	 * which is at least the declared type of that object or a subtype.
	 * @see #typedCopy(Cloneable2)
	 */
	public default Object copy() {
		return clone();
	}
	
	/**
	 * Calls {@link #clone()} on the supplied object and automatically casts the result to the correct type.<br>
	 * Can be useful if a class does not redeclare the {@code clone()} methods return type properly, or if
	 * the cloneable is a generic type variable.
	 * <p>
	 * Assumes that the {@code clone()} method is implemented properly in regards to return type. If not, this
	 * method will fail with a {@link ClassCastException}.
	 * </p>
	 * <p>
	 * If {@code object} is {@code null}, this method returns {@code null} and does <b>not</b> throw a {@link NullPointerException}
	 * (as it would be the case when calling <code>object.clone()</code> directly. If this behavior is desired, use<br>
	 * <code>x2 = typedClone(Objects.requireNonNull(x1));</code>.
	 * </p>
	 * @param <T> The exact implementation of {@code Cloneable2}
	 * @param object The object to clone
	 * @return The cloned object
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Cloneable2> T typedClone(T object) {
		return object == null ? null : (T) ((Cloneable2) object).clone();
	}
	
	/**
	 * Attempts to clone any {@link Cloneable} by using reflection. The method finds a way to clone in the following order:
	 * <ol>
	 * <li>If {@code object} is {@code null}, this method immediately returns {@code null}.</li>
	 * <li>If {@code object} implements the {@link Cloneable2} interface, it will be cast to that interface and the {@link #clone()}
	 * method will be called. The result will be cast back to {@code T}, which is valid assuming
	 * the {@code clone()} method is implemented properly. Any {@link ClassCastException} that occurs will be
	 * wrapped in a {@link CloneNotSupportedException} with the {@code ClassCastException} as the cause.</li>
	 * <li>Next the method attempts to find a public method called {@code 'clone'} without any parameters declared on the objects class or a superclass.
	 * The exact rules for finding the method are those of {@link Class#getMethod(String, Class...)}. If successful,
	 *  the reflecetd method will be invoked as in step 5.</li>
	 * <li>If no public clone method was found, the {@link Object#clone()} method is reflected 
	 * and made accessible. It will be used as the cloning method in step 5.</li>
	 * <li>The cloning method is invoked using {@link Method#invoke(Object, Object...)}. If this invocation succeeds
	 * withou any exceptions, the returned value is cast to {@code T} and returned. If the cast to {@code T} is not possible,
	 * the {@link ClassCastException} will be wrapped in an {@link CloneNotSupportedException}.</li>
	 * <li>If the invocation results in an {@link IllegalAccessException} or {@link IllegalArgumentException}, the exception
	 * will be wrapped in a {@code CloneNotSupportedException}<br>
	 * If the invocation results in an {@link InvocationTargetException}, the cause of that exception is examined:
	 * <ul><li>If the cause is an instance of {@code CloneNotSupportedException}, it is thrown directly</li>
	 * <li>Otherwise, the cause of the {@code InvocationTargetException} is set as the cause of a new
	 * {@code CloneNotSupportedException}, which is thrown.</li></ul>
	 * </li>
	 * </ol>
	 * @param <T> The exact type of {@link Cloneable} implementation
	 * @param object The object to clone
	 * @return The cloned object
	 * @throws CloneNotSupportedException When the object does not support cloning
	 * @throws SecurityException When any of the reflection operations throws a {@code SecurityException}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Cloneable> T tryClone(T object) throws CloneNotSupportedException, SecurityException {
		//Null check
		if(object == null) return null;
		//Is it a Cloneable2? 
		try {
			if(object instanceof Cloneable2) return (T) ((Cloneable2) object).clone();
		} catch (ClassCastException cce) {
			//Wrap invalid return type in a checked exception so it can be handled properly
			final CloneNotSupportedException cnse = new CloneNotSupportedException("Cloneable2 implementation returns wrong type");
			cnse.initCause(cce);
			throw cnse;
		}
		//Does it declare a public clone method?
		Method cloneMethod;
		try {
			cloneMethod = object.getClass().getMethod("clone", (Class<?>[]) null);
		} catch (NoSuchMethodException nsme1) {
			//Clone using objects native clone method
			try {
				cloneMethod = Object.class.getDeclaredMethod("clone", (Class<?>[]) null);
				//setAccessible because Object.clone() is protected
				cloneMethod.setAccessible(true);
			} catch (NoSuchMethodException nsme2) {
				//How does this even happen
				throw new RuntimeException("Object.class is missing clone() method", nsme2);
			}
		}
		
		try {
			//Call the method and cast
			try {
				return (T) cloneMethod.invoke(object, (Object[]) null);
			} catch (ClassCastException cce) {
				//Wrap invalid return type in a checked exception so it can be handled properly
				final CloneNotSupportedException cnse = new CloneNotSupportedException("Clone method returns wrong type");
				cnse.initCause(cce);
				throw cnse;
			}
		} catch (IllegalAccessException iae) {
			final CloneNotSupportedException cnse = new CloneNotSupportedException("No accessible clone method (Illegal Access)");
			cnse.initCause(iae);
			throw cnse;
		} catch (IllegalArgumentException iae) {
			final CloneNotSupportedException cnse = new CloneNotSupportedException("No accessible clone method (Illegal Argument)");
			cnse.initCause(iae);
			throw cnse;
		} catch (InvocationTargetException ite) {
			final Throwable cause = ite.getCause();
			if(cause instanceof CloneNotSupportedException) {
				throw (CloneNotSupportedException) cause;
			} else {
				final CloneNotSupportedException cnse = new CloneNotSupportedException("Exception thrown by clone method");
				cnse.initCause(cause);
				throw cnse;
			}
		}
	}
	
	/**
	 * Calls {@link #copy()} on the supplied object and automatically casts the result to the correct type.<br>
	 * Can be useful if a class does not redeclare the {@code copy()} methods return type properly, or if
	 * the cloneable is a generic type variable.
	 * <p>
	 * Assumes that the {@code clone()} method is implemented properly in regards to return type. If not, this
	 * method will fail with a {@link ClassCastException}.
	 * </p>
	 * <p>
	 * If {@code object} is {@code null}, this method returns {@code null} and does <b>not</b> throw a {@link NullPointerException}
	 * (as it would be the case when calling <code>object.copy()</code> directly. If this behavior is desired, use<br>
	 * <code>x2 = typedCopy(Objects.requireNonNull(x1));</code>.
	 * </p>
	 * @param <T> The exact implementation of {@code Cloneable2}
	 * @param object The object to copy
	 * @return The copied object
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Cloneable2> T typedCopy(T object) {
		return object == null ? null : (T) ((Cloneable2) object).copy();
	}
	
}
