package dev.lb.simplebase.net.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A class annotated with {@link ValueType} is similar to a struct in .NET, it is mainly a container for values with some
 * additional functionaliy.<p>A ValueType always:
 * <ul>
 * <li>Overwrites {@link #equals(Object)} and {@link #hashCode()} properly for itself and all subclasses</li>
 * <li>Overwrites {@link #toString()} to show all fields and their values</li>
 * </ul>
 * </p>
 */
@Retention(CLASS)
@Target(TYPE)
public @interface ValueType {}
