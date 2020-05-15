package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <ul>
 * <li>For classes: All methods are threadsafe, instances can be accessed by any number of threads at the same time.
 * If the superclass of a class has this annotation, all methods of the subclass must be threadsafe as well</li>
 * <li>For methods: Calls to this method can be made form any number of threads at the same time</li>
 * <li>For fields: The implementation used in this field is guaranteed to be threadsafe as if its class had the annotation</li>
 * </ul>
 * <b>Deprecated members and methods</b> of an annotated class might not be threadsafe.
 */
@Retention(CLASS)
@Target({ FIELD, METHOD, TYPE})
public @interface Threadsafe {

}
