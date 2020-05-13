package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * All types with this annotation have no mutable members.
 * Immutable types are by design {@link Threadsafe}.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface Immutable {

}
